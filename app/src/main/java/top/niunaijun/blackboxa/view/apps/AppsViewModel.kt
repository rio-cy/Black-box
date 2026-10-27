package top.niunaijun.blackboxa.view.apps

import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.data.AppsRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

data class InstallProgress(
    val isInstalling: Boolean = false,
    val appName: String = "",
    val packageName: String = "",
    val icon: Drawable? = null
)

class AppsViewModel(private val repo: AppsRepository) : BaseViewModel() {

    val appsLiveData = MutableLiveData<List<AppInfo>>()
    val resultLiveData = MutableLiveData<String>()
    val launchLiveData = MutableLiveData<Boolean>()
    val updateSortLiveData = MutableLiveData<Boolean>()
    val installProgressLiveData = MutableLiveData<InstallProgress>()

    // In-memory cache per user profile to eliminate switching flicker
    private val userAppsCache = mutableMapOf<Int, List<AppInfo>>()

    fun getInstalledApps(userId: Int) {
        getInstalledAppsWithRetry(userId)
    }

    fun getInstalledAppsWithRetry(userId: Int) {
        // Emit cached apps immediately to prevent UI blanking
        val cached = userAppsCache[userId]
        if (cached != null) {
            appsLiveData.value = cached
        }

        launchOnUI {
            repo.getVmInstallList(userId, appsLiveData) { freshList ->
                userAppsCache[userId] = freshList
            }
        }
    }

    fun install(source: String, userID: Int) {
        launchOnUI {
            repo.installApk(source, userID, resultLiveData, installProgressLiveData)
        }
    }

    fun unInstall(packageName: String, userID: Int) {
        launchOnUI {
            repo.unInstall(packageName, userID, resultLiveData)
        }
    }

    fun clearApkData(packageName: String, userID: Int) {
        launchOnUI {
            repo.clearApkData(packageName, userID, resultLiveData)
        }
    }

    fun launchApk(packageName: String, userID: Int) {
        launchOnUI {
            repo.launchApk(packageName, userID, launchLiveData)
        }
    }

    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        userAppsCache[userID] = dataList
        launchOnUI {
            repo.updateApkOrder(userID, dataList)
        }
    }
}