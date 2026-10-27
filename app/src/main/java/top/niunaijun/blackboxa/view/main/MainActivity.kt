package top.niunaijun.blackboxa.view.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.pointer.pointerInput
import android.Manifest
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import top.niunaijun.blackboxa.util.DeviceSpoofManager
import top.niunaijun.blackboxa.view.devicespoof.DeviceSpoofActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.ui.components.AppIconView
import top.niunaijun.blackboxa.ui.theme.BlackBoxTheme
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.util.ShortcutUtil
import top.niunaijun.blackboxa.view.apps.AppsViewModel
import top.niunaijun.blackboxa.view.fake.FakeManagerActivity
import top.niunaijun.blackboxa.view.list.ListActivity
import top.niunaijun.blackboxa.view.setting.SettingActivity

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: AppsViewModel
    private var currentUser by mutableIntStateOf(0)
    private var availableUsers by mutableStateOf<List<Int>>(listOf(0))
    private var hasStoragePermission by mutableStateOf(true)
    private var hasNotificationPermission by mutableStateOf(true)
    private var hasBatteryOptimizationExempt by mutableStateOf(true)
    private var showPermissionOnboarding by mutableStateOf(false)

    private val apkPathResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val userId = data.getIntExtra("userID", currentUser)
                    val source = data.getStringExtra("source")
                    if (source != null) {
                        Toast.makeText(this, "Installing package...", Toast.LENGTH_SHORT).show()
                        viewModel.install(source, userId)
                    }
                }
            }
        }

    private val storagePermissionResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissions()
        }

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            refreshPermissions()
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshPermissions()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            BlackBoxCore.get().onBeforeMainActivityOnCreate(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onBeforeMainActivityOnCreate: ${e.message}")
        }

        viewModel = ViewModelProvider(this, InjectionUtil.getAppsFactory())
            .get(AppsViewModel::class.java)

        refreshUsers()
        refreshPermissions()
        checkVpnPermission()

        setContent {
            BlackBoxTheme {
                if (showPermissionOnboarding) {
                    PermissionOnboardingScreen(
                        hasStorage = hasStoragePermission,
                        hasNotification = hasNotificationPermission,
                        hasBatteryOpt = hasBatteryOptimizationExempt,
                        onRequestStorage = { requestStorageAccess() },
                        onRequestNotification = { requestNotificationPermission() },
                        onRequestBatteryOpt = { requestBatteryOptimization() },
                        onContinue = { completeOnboarding() }
                    )
                } else {
                    MainDashboardScreen(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        availableUsers = availableUsers,
                        hasStoragePermission = hasStoragePermission,
                        onUserChanged = { userId ->
                            currentUser = userId
                        },
                        onAddUser = {
                            val nextId = (availableUsers.maxOrNull() ?: 0) + 1
                            try {
                                BlackBoxCore.get().createUser(nextId)
                                // Auto-generate distinct randomized device profile for this new user
                                DeviceSpoofManager.getConfig(this, nextId)
                                refreshUsers()
                                currentUser = nextId
                                viewModel.getInstalledAppsWithRetry(nextId)
                                Toast.makeText(this, "Created User Profile $nextId", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this, "Failed to create user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRequestStoragePermission = { requestStorageAccess() },
                        onOpenAppList = {
                            val intent = Intent(this, ListActivity::class.java)
                            intent.putExtra("userID", currentUser)
                            apkPathResult.launch(intent)
                        },
                        onOpenSettings = { SettingActivity.start(this) },
                        onOpenFakeLocation = {
                            val intent = Intent(this, FakeManagerActivity::class.java)
                            intent.putExtra("userID", currentUser)
                            startActivity(intent)
                        },
                        onOpenDeviceSpoof = { targetUser ->
                            DeviceSpoofActivity.start(this, targetUser)
                        },
                        onDeleteProfile = { targetUser ->
                            try {
                                BlackBoxCore.get().deleteUser(targetUser)
                                AppManager.mRemarkSharedPreferences.edit {
                                    remove("Remark$targetUser")
                                    remove("AppList$targetUser")
                                }
                                DeviceSpoofManager.clearConfig(this, targetUser)
                                refreshUsers()
                                currentUser = 0
                                viewModel.getInstalledAppsWithRetry(0)
                                Toast.makeText(this, "Profile $targetUser deleted", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(this, "Failed to delete profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        try {
            BlackBoxCore.get().onAfterMainActivityOnCreate(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAfterMainActivityOnCreate: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        refreshUsers()
        viewModel.getInstalledAppsWithRetry(currentUser)
    }

    fun showFloatButton(show: Boolean) {}

    fun scanUser() {
        refreshUsers()
    }

    private fun refreshUsers() {
        try {
            val users = BlackBoxCore.get().users
            val list = if (users.isNullOrEmpty()) listOf(0) else users.map { it.id }
            availableUsers = list
            if (!list.contains(currentUser)) {
                currentUser = list.firstOrNull() ?: 0
            }
        } catch (_: Exception) {
            availableUsers = listOf(0)
        }
    }

    private fun refreshPermissions() {
        hasStoragePermission = top.niunaijun.blackbox.utils.StoragePermissionHelper.hasStoragePermission(this)
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        hasBatteryOptimizationExempt = powerManager?.isIgnoringBatteryOptimizations(packageName) ?: true

        val prefs = getSharedPreferences("app_permission_prefs", Context.MODE_PRIVATE)
        val hasCompleted = prefs.getBoolean("has_completed_onboarding", false)
        if (!hasCompleted && (!hasStoragePermission || !hasNotificationPermission)) {
            showPermissionOnboarding = true
        } else {
            showPermissionOnboarding = false
        }
    }

    private fun completeOnboarding() {
        getSharedPreferences("app_permission_prefs", Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean("has_completed_onboarding", true)
        }
        showPermissionOnboarding = false
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {}
        }
    }

    private fun requestStorageAccess() {
        Toast.makeText(this, "Storage Access Framework is active. Isolated app storage ready.", Toast.LENGTH_SHORT).show()
        refreshPermissions()
    }

    private fun requestRuntimeStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        requestMultiplePermissionsLauncher.launch(permissions)
    }

    private fun checkVpnPermission() {
        try {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                // Non-blocking VPN check
            }
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "MainActivity"
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainDashboardScreen(
    viewModel: AppsViewModel,
    currentUser: Int,
    availableUsers: List<Int>,
    hasStoragePermission: Boolean,
    onUserChanged: (Int) -> Unit,
    onAddUser: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onOpenAppList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFakeLocation: () -> Unit,
    onOpenDeviceSpoof: (Int) -> Unit,
    onDeleteProfile: (Int) -> Unit
) {
    val context = LocalContext.current
    val apps by viewModel.appsLiveData.observeAsState(initial = emptyList())
    val resultMessage by viewModel.resultLiveData.observeAsState()
    val launchResult by viewModel.launchLiveData.observeAsState()

    var isLaunching by remember { mutableStateOf(false) }
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var appFilterType by remember { mutableStateOf("ALL") }

    var selectedProfileForAction by remember { mutableIntStateOf(currentUser) }
    var showProfileOptionsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var profileToDeleteAppCount by remember { mutableIntStateOf(0) }
    var targetRenameUser by remember { mutableIntStateOf(currentUser) }

    // User remark
    var userRemark by remember(currentUser) {
        val saved = AppManager.mRemarkSharedPreferences.getString("Remark$currentUser", "User $currentUser")
        mutableStateOf(if (saved.isNullOrEmpty()) "User $currentUser" else saved)
    }

    // Refresh on user change
    LaunchedEffect(currentUser) {
        viewModel.getInstalledAppsWithRetry(currentUser)
    }

    // Observe result messages
    LaunchedEffect(resultMessage) {
        resultMessage?.let { msg ->
            if (msg.isNotEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.getInstalledAppsWithRetry(currentUser)
                viewModel.resultLiveData.value = null
            }
        }
    }

    // Observe launch status
    LaunchedEffect(launchResult) {
        launchResult?.let { success ->
            isLaunching = false
            if (!success) {
                Toast.makeText(context, R.string.start_fail, Toast.LENGTH_SHORT).show()
            }
            viewModel.launchLiveData.value = null
        }
    }

    val filteredApps by remember(apps, appFilterType) {
        derivedStateOf {
            val list = apps ?: emptyList()
            when (appFilterType) {
                "MODULES" -> list.filter { it.isXpModule }
                "APPS" -> list.filter { !it.isXpModule }
                else -> list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Sandboxed",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = userRemark,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.combinedClickable(
                                onClick = { showRenameDialog = true },
                                onLongClick = { showRenameDialog = true }
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onOpenDeviceSpoof(currentUser) },
                        modifier = Modifier.testTag("btn_device_spoof")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Device Spoofing"
                        )
                    }
                    IconButton(
                        onClick = onOpenFakeLocation,
                        modifier = Modifier.testTag("btn_fake_location")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Fake Location"
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("btn_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Profile") },
                                onClick = {
                                    showMoreMenu = false
                                    targetRenameUser = currentUser
                                    showRenameDialog = true
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            if (currentUser != 0) {
                                DropdownMenuItem(
                                    text = { Text("Delete Profile", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        selectedProfileForAction = currentUser
                                        val count = try {
                                            BlackBoxCore.get().getInstalledApplications(0, currentUser)?.size ?: 0
                                        } catch (e: Exception) { 0 }
                                        profileToDeleteAppCount = count
                                        showDeleteConfirmDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Telegram Community") },
                                onClick = {
                                    showMoreMenu = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/newblackboxa"))
                                    context.startActivity(intent)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("GitHub Source") },
                                onClick = {
                                    showMoreMenu = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ALEX5402/NewBlackbox"))
                                    context.startActivity(intent)
                                },
                                leadingIcon = {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (filteredApps.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onOpenAppList,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Clone App") },
                    modifier = Modifier.testTag("fab_clone_app"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Storage Permission Warning Banner
            if (!hasStoragePermission) {
                StoragePermissionBanner(onRequestPermission = onRequestStoragePermission)
            }

            // User Profiles Tab Bar
            ScrollableTabRow(
                selectedTabIndex = availableUsers.indexOf(currentUser).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                availableUsers.forEach { uId ->
                    val isSelected = uId == currentUser
                    val profileName = remember(uId, userRemark) {
                        AppManager.mRemarkSharedPreferences.getString("Remark$uId", "User $uId") ?: "User $uId"
                    }
                    Tab(
                        selected = isSelected,
                        onClick = { onUserChanged(uId) },
                        text = {
                            Text(
                                text = profileName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
                Tab(
                    selected = false,
                    onClick = onAddUser,
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Profile",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                )
            }

            // Quick Filters (All / Apps / Xposed Modules)
            val hasModules = remember(apps) { apps?.any { it.isXpModule } == true }
            if (hasModules) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { appFilterType = "ALL" },
                        label = { Text("All (${apps?.size ?: 0})") }
                    )
                    AssistChip(
                        onClick = { appFilterType = "APPS" },
                        label = { Text("Apps (${apps?.count { !it.isXpModule } ?: 0})") }
                    )
                    AssistChip(
                        onClick = { appFilterType = "MODULES" },
                        label = { Text("Xposed (${apps?.count { it.isXpModule } ?: 0})") }
                    )
                }
            }

            // Grid Content / Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Crossfade(
                    targetState = filteredApps,
                    animationSpec = tween(150),
                    label = "apps_transition"
                ) { currentApps ->
                    if (currentApps.isEmpty()) {
                        EmptyAppsDashboard(onCloneFirstApp = onOpenAppList)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("cloned_apps_grid")
                        ) {
                            items(currentApps, key = { it.packageName }) { app ->
                                VirtualAppItem(
                                    app = app,
                                    onClick = {
                                        isLaunching = true
                                        Toast.makeText(context, "Starting ${app.name}...", Toast.LENGTH_SHORT).show()
                                        viewModel.launchApk(app.packageName, currentUser)
                                    },
                                    onLongClick = { selectedAppForMenu = app }
                                )
                            }
                        }
                    }
                }

                // Launching indicator overlay
                if (isLaunching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Launching Virtual Environment...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Installing progress overlay with small icon and round progress
                val installProgress by viewModel.installProgressLiveData.observeAsState()
                if (installProgress?.isInstalling == true) {
                    val progress = installProgress!!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .padding(24.dp)
                                .widthIn(max = 320.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(76.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(76.dp),
                                        strokeWidth = 3.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(54.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        if (progress.icon != null) {
                                            AppIconView(
                                                icon = progress.icon,
                                                contentDescription = progress.appName,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(6.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp)
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (progress.appName.isNotEmpty()) "Installing ${progress.appName}" else stringResource(R.string.installing_app_title),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.installing_app_sub),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // App Management Bottom Sheet
    selectedAppForMenu?.let { app ->
        AppActionBottomSheet(
            app = app,
            currentUser = currentUser,
            onDismiss = { selectedAppForMenu = null },
            onLaunch = {
                selectedAppForMenu = null
                isLaunching = true
                viewModel.launchApk(app.packageName, currentUser)
            },
            onStop = {
                selectedAppForMenu = null
                try {
                    BlackBoxCore.get().stopPackage(app.packageName, currentUser)
                    Toast.makeText(context, "${app.name} stopped", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to stop app: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onClearData = {
                selectedAppForMenu = null
                Toast.makeText(context, "Clearing app data...", Toast.LENGTH_SHORT).show()
                viewModel.clearApkData(app.packageName, currentUser)
            },
            onCreateShortcut = {
                selectedAppForMenu = null
                ShortcutUtil.createShortcut(context, currentUser, app)
            },
            onUninstall = {
                selectedAppForMenu = null
                Toast.makeText(context, "Uninstalling ${app.name}...", Toast.LENGTH_SHORT).show()
                viewModel.unInstall(app.packageName, currentUser)
            }
        )
    }

    // Profile Management Options Bottom Sheet (Triggered by Long Press on Profile Tab)
    if (showProfileOptionsSheet) {
        val targetUser = selectedProfileForAction
        val targetName = remember(targetUser, userRemark) {
            AppManager.mRemarkSharedPreferences.getString("Remark$targetUser", "User $targetUser") ?: "User $targetUser"
        }
        ModalBottomSheet(
            onDismissRequest = { showProfileOptionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$targetUser",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = targetName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (targetUser == 0) "Primary Master Profile" else "Virtual Profile #$targetUser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                if (targetUser != currentUser) {
                    FilledTonalButton(
                        onClick = {
                            showProfileOptionsSheet = false
                            onUserChanged(targetUser)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch to Profile")
                    }
                }

                OutlinedButton(
                    onClick = {
                        showProfileOptionsSheet = false
                        targetRenameUser = targetUser
                        showRenameDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rename Profile")
                }

                OutlinedButton(
                    onClick = {
                        showProfileOptionsSheet = false
                        onOpenDeviceSpoof(targetUser)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Device Identity & Hardware Spoofing")
                }

                if (targetUser != 0) {
                    Button(
                        onClick = {
                            showProfileOptionsSheet = false
                            val count = try {
                                BlackBoxCore.get().getInstalledApplications(0, targetUser)?.size ?: 0
                            } catch (e: Exception) { 0 }
                            profileToDeleteAppCount = count
                            showDeleteConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Profile")
                    }
                } else {
                    Text(
                        text = "Note: Primary Profile 0 is the root virtual user and cannot be deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Delete Profile Confirmation Dialog with 3-second timer and Cloned Apps Count
    if (showDeleteConfirmDialog) {
        val targetUser = selectedProfileForAction
        val targetName = remember(targetUser) {
            AppManager.mRemarkSharedPreferences.getString("Remark$targetUser", "User $targetUser") ?: "User $targetUser"
        }
        var countdown by remember(showDeleteConfirmDialog) { mutableIntStateOf(3) }

        LaunchedEffect(showDeleteConfirmDialog) {
            if (showDeleteConfirmDialog) {
                countdown = 3
                while (countdown > 0) {
                    delay(1000)
                    countdown--
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Profile $targetUser?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Do you want to delete profile \"$targetName\"?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Total virtual apps cloned: $profileToDeleteAppCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All applications, user data, app accounts, and isolated sandbox storage in this profile will be permanently deleted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteProfile(targetUser)
                    },
                    enabled = countdown == 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                        disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_profile")
                ) {
                    if (countdown > 0) {
                        Text("Delete ($countdown s)")
                    } else {
                        Text("Delete Profile")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // User Remark Rename Dialog
    if (showRenameDialog) {
        val currentLabel = remember(targetRenameUser) {
            AppManager.mRemarkSharedPreferences.getString("Remark$targetRenameUser", "User $targetRenameUser") ?: "User $targetRenameUser"
        }
        var newRemark by remember(targetRenameUser) { mutableStateOf(currentLabel) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Profile $targetRenameUser") },
            text = {
                OutlinedTextField(
                    value = newRemark,
                    onValueChange = { newRemark = it },
                    label = { Text("Profile Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newRemark.trim()
                        if (trimmed.isNotEmpty()) {
                            AppManager.mRemarkSharedPreferences.edit {
                                putString("Remark$targetRenameUser", trimmed)
                            }
                            if (targetRenameUser == currentUser) {
                                userRemark = trimmed
                            }
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VirtualAppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
            .testTag("app_grid_${app.packageName}")
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            AppIconView(icon = app.icon, size = 56.dp)
            if (app.isXpModule) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Xposed Module",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyAppsDashboard(onCloneFirstApp: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = "No Virtual Apps Cloned",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Clone installed applications or select external APKs into this isolated virtual sandbox container.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                FilledTonalButton(
                    onClick = onCloneFirstApp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clone First Application")
                }
            }
        }
    }
}

@Composable
fun StoragePermissionBanner(onRequestPermission: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage Access Required",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Sandboxed apps need All Files Access permission to operate properly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "Grant", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionBottomSheet(
    app: AppInfo,
    currentUser: Int,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onClearData: () -> Unit,
    onCreateShortcut: () -> Unit,
    onUninstall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                AppIconView(icon = app.icon, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Actions
            ActionRowItem(
                icon = Icons.Default.PlayArrow,
                title = "Launch Application",
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onLaunch() }
                }
            )
            ActionRowItem(
                icon = Icons.Default.Shortcut,
                title = "Add to Home Screen",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onCreateShortcut() }
                }
            )
            ActionRowItem(
                icon = Icons.Default.Stop,
                title = "Force Stop",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onStop() }
                }
            )
            ActionRowItem(
                icon = Icons.Default.CleaningServices,
                title = "Clear Data & Cache",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onClearData() }
                }
            )
            ActionRowItem(
                icon = Icons.Default.Delete,
                title = "Uninstall from Profile",
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onUninstall() }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ActionRowItem(
    icon: ImageVector,
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = color
        )
    }
}

@Composable
fun PermissionOnboardingScreen(
    hasStorage: Boolean,
    hasNotification: Boolean,
    hasBatteryOpt: Boolean,
    onRequestStorage: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBatteryOpt: () -> Unit,
    onContinue: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.permission_guide_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.permission_guide_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Step 1: Storage
            PermissionStepCard(
                icon = Icons.Default.FolderOpen,
                title = stringResource(R.string.permission_storage_title),
                description = stringResource(R.string.permission_storage_desc),
                isGranted = hasStorage,
                actionText = stringResource(R.string.permission_grant),
                onAction = onRequestStorage
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 2: Notifications
            PermissionStepCard(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.permission_notification_title),
                description = stringResource(R.string.permission_notification_desc),
                isGranted = hasNotification,
                actionText = stringResource(R.string.permission_grant),
                onAction = onRequestNotification
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Step 3: Battery Optimization
            PermissionStepCard(
                icon = Icons.Default.Power,
                title = stringResource(R.string.permission_battery_title),
                description = stringResource(R.string.permission_battery_desc),
                isGranted = hasBatteryOpt,
                actionText = stringResource(R.string.permission_grant),
                onAction = onRequestBatteryOpt
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_permission_continue"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.permission_continue),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PermissionStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isGranted) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isGranted)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.Check else icon,
                        contentDescription = null,
                        tint = if (isGranted)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.permission_granted),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

