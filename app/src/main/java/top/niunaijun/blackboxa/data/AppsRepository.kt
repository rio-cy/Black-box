package top.niunaijun.blackboxa.data

import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import java.io.File
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.utils.AbiUtils
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.InstalledAppBean
import top.niunaijun.blackboxa.util.MemoryManager
import top.niunaijun.blackboxa.util.getString
import top.niunaijun.blackboxa.view.apps.InstallProgress

class AppsRepository {
    val TAG: String = "AppsRepository"
    private var mInstalledList = mutableListOf<AppInfo>()

    
    private fun safeLoadAppLabel(applicationInfo: ApplicationInfo): String {
        return try {
            BlackBoxCore.getPackageManager().getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load label for ${applicationInfo.packageName}: ${e.message}")
            applicationInfo.packageName 
        }
    }

    
    private fun safeLoadAppIcon(
            applicationInfo: ApplicationInfo
    ): android.graphics.drawable.Drawable? {
        return try {
            
            if (MemoryManager.shouldSkipIconLoading()) {
                Log.w(
                        TAG,
                        "Memory usage high (${MemoryManager.getMemoryUsagePercentage()}%), skipping icon for ${applicationInfo.packageName}"
                )
                return null
            }

            val icon = BlackBoxCore.getPackageManager().getApplicationIcon(applicationInfo)

            
            if (icon is android.graphics.drawable.BitmapDrawable) {
                val bitmap = icon.bitmap
                
                if (bitmap.width > 96 || bitmap.height > 96) {
                    try {
                        val scaledBitmap =
                                android.graphics.Bitmap.createScaledBitmap(bitmap, 96, 96, true)
                        android.graphics.drawable.BitmapDrawable(
                                BlackBoxCore.getPackageManager()
                                        .getResourcesForApplication(applicationInfo.packageName),
                                scaledBitmap
                        )
                    } catch (e: Exception) {
                        Log.w(
                                TAG,
                                "Failed to scale icon for ${applicationInfo.packageName}: ${e.message}"
                        )
                        icon
                    }
                } else {
                    icon
                }
            } else {
                icon
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load icon for ${applicationInfo.packageName}: ${e.message}")
            null 
        }
    }

    fun previewInstallList() {
        try {
            synchronized(mInstalledList) {
                val installedApplications: List<ApplicationInfo> =
                        BlackBoxCore.getPackageManager().getInstalledApplications(0)
                val installedList = mutableListOf<AppInfo>()

                for (installedApplication in installedApplications) {
                    try {
                        val sourceDir = installedApplication.sourceDir ?: continue
                        val file = File(sourceDir)
                        if (!file.exists()) continue

                        if (!AbiUtils.isSupport(file)) continue

                        if (BlackBoxCore.get().isBlackBoxApp(installedApplication.packageName)) {
                            continue
                        }

                        val isSystem = (installedApplication.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        val info =
                                AppInfo(
                                        safeLoadAppLabel(installedApplication),
                                        safeLoadAppIcon(
                                                installedApplication
                                        ), 
                                        installedApplication.packageName,
                                        sourceDir,
                                        isSystem
                                )
                        installedList.add(info)
                    } catch (e: Exception) {
                        Log.e(
                                TAG,
                                "Error processing app ${installedApplication.packageName}: ${e.message}"
                        )
                    }
                }
                this.mInstalledList.clear()
                this.mInstalledList.addAll(installedList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in previewInstallList: ${e.message}")
        }
    }

    fun getInstalledAppList(
            userID: Int,
            loadingLiveData: MutableLiveData<Boolean>,
            appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        try {
            loadingLiveData.postValue(true)
            synchronized(mInstalledList) {
                if (mInstalledList.isEmpty()) {
                    previewInstallList()
                }
                val blackBoxCore = BlackBoxCore.get()
                val newInstalledList =
                        mInstalledList.map {
                            InstalledAppBean(
                                    it.name,
                                    it.icon, 
                                    it.packageName,
                                    it.sourceDir,
                                    blackBoxCore.isInstalled(it.packageName, userID),
                                    it.isXpModule // Used to flag isSystemApp
                            )
                        }
                appsLiveData.postValue(newInstalledList)
                loadingLiveData.postValue(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getInstalledAppList: ${e.message}")
            loadingLiveData.postValue(false)
            appsLiveData.postValue(emptyList())
        }
    }

    fun getVmInstallList(userId: Int, appsLiveData: MutableLiveData<List<AppInfo>>, onLoaded: ((List<AppInfo>) -> Unit)? = null) {
        try {
            
            if (MemoryManager.isMemoryCritical()) {
                Log.w(
                        TAG,
                        "Memory critical (${MemoryManager.getMemoryUsagePercentage()}%), forcing garbage collection"
                )
                MemoryManager.forceGarbageCollectionIfNeeded()
            }

            val blackBoxCore = BlackBoxCore.get()

            
            val users = blackBoxCore.users
            Log.d(TAG, "getVmInstallList: userId=$userId, total users=${users.size}")

            val sortListData = AppManager.mRemarkSharedPreferences.getString("AppList$userId", "")
            val sortList = sortListData?.split(",")

            var applicationList: List<ApplicationInfo>? = null
            var retryCount = 0
            val maxRetries = 2

            while (applicationList == null && retryCount < maxRetries) {
                try {
                    applicationList = blackBoxCore.getInstalledApplications(0, userId)
                    if (applicationList == null) {
                        retryCount++
                        Thread.sleep(50)
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        Thread.sleep(50)
                    }
                }
            }

            if (applicationList == null) {
                appsLiveData.postValue(emptyList())
                onLoaded?.invoke(emptyList())
                return
            }

            val appInfoList = mutableListOf<AppInfo>()

            val sortedApplicationList =
                    if (!sortList.isNullOrEmpty()) {
                        try {
                            applicationList.sortedWith(AppsSortComparator(sortList))
                        } catch (e: Exception) {
                            applicationList
                        }
                    } else {
                        applicationList
                    }

            sortedApplicationList.forEachIndexed { index, applicationInfo ->
                try {
                    if (applicationInfo.packageName.isNullOrBlank()) {
                        return@forEachIndexed
                    }

                    val info =
                            AppInfo(
                                    safeLoadAppLabel(applicationInfo),
                                    safeLoadAppIcon(applicationInfo),
                                    applicationInfo.packageName,
                                    applicationInfo.sourceDir ?: "",
                                    false
                            )

                    appInfoList.add(info)
                } catch (e: Exception) {
                    Log.e(TAG, "getVmInstallList: Error processing app: ${e.message}")
                }
            }

            onLoaded?.invoke(appInfoList)
            appsLiveData.postValue(appInfoList)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getVmInstallList: ${e.message}")
            appsLiveData.postValue(emptyList())
            onLoaded?.invoke(emptyList())
        }
    }

    fun installApk(
            source: String,
            userId: Int,
            resultLiveData: MutableLiveData<String>,
            progressLiveData: MutableLiveData<InstallProgress>? = null
    ) {
        try {
            val uri = try { Uri.parse(source) } catch (_: Exception) { null }
            val isUri = uri != null && (uri.scheme == "content" || uri.scheme == "file")
            val isApkFile = !isUri && (source.endsWith(".apk", ignoreCase = true) || File(source).exists())

            var previewName = "Application"
            var previewIcon: android.graphics.drawable.Drawable? = null
            try {
                if (isUri) {
                    previewName = uri?.lastPathSegment ?: "APK File"
                } else if (isApkFile) {
                    val archive = BlackBoxCore.getPackageManager().getPackageArchiveInfo(source, 0)
                    val appInfo = archive?.applicationInfo
                    if (appInfo != null) {
                        previewName = safeLoadAppLabel(appInfo)
                        previewIcon = safeLoadAppIcon(appInfo)
                    }
                } else {
                    val app = BlackBoxCore.getPackageManager().getApplicationInfo(source, 0)
                    if (app != null) {
                        previewName = safeLoadAppLabel(app)
                        previewIcon = safeLoadAppIcon(app)
                    }
                }
            } catch (_: Exception) {}

            progressLiveData?.postValue(
                    InstallProgress(
                            isInstalling = true,
                            appName = previewName,
                            packageName = source,
                            icon = previewIcon
                    )
            )

            if (source.contains("blackbox") ||
                            source.contains("niunaijun") ||
                            source.contains("vspace") ||
                            source.contains("virtual")
            ) {
                try {
                    val hostPackageName = BlackBoxCore.getHostPkg()
                    if (!isUri && !isApkFile) {
                        if (source == hostPackageName) {
                            progressLiveData?.postValue(InstallProgress(isInstalling = false))
                            resultLiveData.postValue(
                                    "Cannot install BlackBox app from within BlackBox. This would create infinite recursion and is not allowed for security reasons."
                            )
                            return
                        }
                    } else if (isApkFile) {
                        val file = File(source)
                        if (file.exists()) {
                            val packageInfo =
                                    BlackBoxCore.getPackageManager()
                                            .getPackageArchiveInfo(source, 0)
                            if (packageInfo != null && packageInfo.packageName == hostPackageName) {
                                progressLiveData?.postValue(InstallProgress(isInstalling = false))
                                resultLiveData.postValue(
                                        "Cannot install BlackBox app from within BlackBox. This would create infinite recursion and is not allowed for security reasons."
                                )
                                return
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not verify if this is BlackBox app: ${e.message}")
                }
            }

            val blackBoxCore = BlackBoxCore.get()
            val installResult = when {
                isUri -> blackBoxCore.installPackageAsUser(uri!!, userId)
                isApkFile -> blackBoxCore.installPackageAsUser(File(source), userId)
                else -> blackBoxCore.installPackageAsUser(source, userId)
            }

            progressLiveData?.postValue(InstallProgress(isInstalling = false))

            if (installResult.success) {
                updateAppSortList(userId, installResult.packageName, true)
                resultLiveData.postValue(getString(R.string.install_success))
            } else {
                resultLiveData.postValue(getString(R.string.install_fail, installResult.msg))
            }
            scanUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}")
            progressLiveData?.postValue(InstallProgress(isInstalling = false))
            resultLiveData.postValue("Installation failed: ${e.message}")
        }
    }

    fun unInstall(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            // First stop virtual app processes to release open SQLite / APK file locks
            try {
                BlackBoxCore.get().stopPackage(packageName, userID)
                Thread.sleep(150)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping package before uninstall: ${e.message}")
            }
            BlackBoxCore.get().uninstallPackageAsUser(packageName, userID)
            updateAppSortList(userID, packageName, false)
            scanUser()
            resultLiveData.postValue(getString(R.string.uninstall_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling APK: ${e.message}")
            resultLiveData.postValue("Uninstallation failed: ${e.message}")
        }
    }

    fun launchApk(packageName: String, userId: Int, launchLiveData: MutableLiveData<Boolean>) {
        try {
            val result = BlackBoxCore.get().launchApk(packageName, userId)
            launchLiveData.postValue(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK: ${e.message}")
            launchLiveData.postValue(false)
        }
    }

    fun clearApkData(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            BlackBoxCore.get().clearPackage(packageName, userID)
            resultLiveData.postValue(getString(R.string.clear_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing APK data: ${e.message}")
            resultLiveData.postValue("Clear failed: ${e.message}")
        }
    }

    
    private fun scanUser() {
        // Preserved user profiles - auto-deletion disabled
    }

    
    private fun updateAppSortList(userID: Int, pkg: String, isAdd: Boolean) {
        try {
            val savedSortList = AppManager.mRemarkSharedPreferences.getString("AppList$userID", "")

            val sortList = linkedSetOf<String>()
            if (savedSortList != null) {
                sortList.addAll(savedSortList.split(","))
            }

            if (isAdd) {
                sortList.add(pkg)
            } else {
                sortList.remove(pkg)
            }

            AppManager.mRemarkSharedPreferences.edit().apply {
                putString("AppList$userID", sortList.joinToString(","))
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app sort list: ${e.message}")
        }
    }

    
    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        try {
            AppManager.mRemarkSharedPreferences.edit().apply {
                putString("AppList$userID", dataList.joinToString(",") { it.packageName })
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK order: ${e.message}")
        }
    }
}
