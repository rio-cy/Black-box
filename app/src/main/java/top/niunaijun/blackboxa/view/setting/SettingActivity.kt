package top.niunaijun.blackboxa.view.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.ui.theme.BlackBoxTheme
import top.niunaijun.blackboxa.view.gms.GmsManagerActivity

class SettingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BlackBoxTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SettingActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var hideRoot by remember {
        mutableStateOf(try { AppManager.mBlackBoxLoader.hideRoot() } catch (_: Exception) { false })
    }
    var daemonEnable by remember {
        mutableStateOf(try { AppManager.mBlackBoxLoader.daemonEnable() } catch (_: Exception) { false })
    }
    var useVpn by remember {
        mutableStateOf(try { AppManager.mBlackBoxLoader.useVpnNetwork() } catch (_: Exception) { false })
    }
    var disableFlagSecure by remember {
        mutableStateOf(try { AppManager.mBlackBoxLoader.disableFlagSecure() } catch (_: Exception) { false })
    }
    var isSendingLogs by remember { mutableStateOf(false) }

    val isSupportGms = remember {
        try { BlackBoxCore.get().isSupportGms } catch (_: Exception) { false }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 400.dp
        val horizontalPadding = if (isCompactWidth) 10.dp else 16.dp
        val cardSpacing = if (isCompactWidth) 10.dp else 16.dp
        val titleStyle = if (isCompactWidth) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.setting),
                            style = titleStyle.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(cardSpacing)
            ) {
                // Section: Security & Privacy
                SettingsSectionHeader(title = "Security & Privacy")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsSwitchRow(
                            title = stringResource(R.string.hide_root),
                            description = "Hide root & su binary traces from sandboxed apps",
                            icon = Icons.Default.Security,
                            checked = hideRoot,
                            onCheckedChange = { checked ->
                                hideRoot = checked
                                try {
                                    AppManager.mBlackBoxLoader.invalidHideRoot(checked)
                                    Toast.makeText(context, R.string.restart_module, Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            },
                            testTag = "switch_hide_root",
                            isCompact = isCompactWidth
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        SettingsSwitchRow(
                            title = "Disable Flag Secure",
                            description = "Permit screenshots & recording inside secured apps",
                            icon = Icons.Default.Lock,
                            checked = disableFlagSecure,
                            onCheckedChange = { checked ->
                                disableFlagSecure = checked
                                try {
                                    AppManager.mBlackBoxLoader.invalidDisableFlagSecure(checked)
                                    Toast.makeText(context, R.string.restart_module, Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            },
                            testTag = "switch_disable_flag_secure",
                            isCompact = isCompactWidth
                        )
                    }
                }

                // Section: Runtime & Networking
                SettingsSectionHeader(title = "Runtime & Networking")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsSwitchRow(
                            title = stringResource(R.string.daemon_enable),
                            description = "Keep sandboxed background services running reliably",
                            icon = Icons.Default.Sync,
                            checked = daemonEnable,
                            onCheckedChange = { checked ->
                                daemonEnable = checked
                                try {
                                    AppManager.mBlackBoxLoader.invalidDaemonEnable(checked)
                                    Toast.makeText(context, R.string.restart_module, Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            },
                            testTag = "switch_daemon_enable",
                            isCompact = isCompactWidth
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        SettingsSwitchRow(
                            title = stringResource(R.string.use_vpn_network),
                            description = stringResource(R.string.use_vpn_network_summary),
                            icon = Icons.Default.VpnKey,
                            checked = useVpn,
                            onCheckedChange = { checked ->
                                useVpn = checked
                                try {
                                    AppManager.mBlackBoxLoader.invalidUseVpnNetwork(checked)
                                    Toast.makeText(context, R.string.restart_module, Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            },
                            testTag = "switch_use_vpn",
                            isCompact = isCompactWidth
                        )
                    }
                }

                // Section: Integrations
                SettingsSectionHeader(title = "Google Services & Add-ons")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    SettingsActionRow(
                        title = "Google Mobile Services (GMS)",
                        description = if (isSupportGms) "Manage Google Play Services per user" else stringResource(R.string.no_gms),
                        icon = Icons.Default.Public,
                        enabled = isSupportGms,
                        onClick = {
                            if (isSupportGms) {
                                GmsManagerActivity.start(context)
                            }
                        },
                        testTag = "btn_gms_manager",
                        isCompact = isCompactWidth
                    )
                }

                // Section: Diagnostics & Logs
                SettingsSectionHeader(title = "Diagnostics & Local Storage")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    SettingsActionRow(
                        title = "Save Diagnostic Logs Locally",
                        description = if (isSendingLogs) "Generating system dump..." else "Save BlackBox crash & debug log to private storage",
                        icon = Icons.Default.BugReport,
                        enabled = !isSendingLogs,
                        trailing = {
                            if (isSendingLogs) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            isSendingLogs = true
                            Toast.makeText(context, "Collecting diagnostic logs...", Toast.LENGTH_SHORT).show()
                            try {
                                BlackBoxCore.get().sendLogs(
                                    "Manual Log Save from Settings",
                                    true,
                                    object : BlackBoxCore.LogSendListener {
                                        override fun onSuccess() {
                                            isSendingLogs = false
                                        }

                                        override fun onFailure(error: String?) {
                                            isSendingLogs = false
                                            Toast.makeText(context, "Log save failed: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                isSendingLogs = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        testTag = "btn_export_logs",
                        isCompact = isCompactWidth
                    )
                }

                // Section: About
                SettingsSectionHeader(title = "About")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        SettingsActionRow(
                            title = "BlackBox Core Engine",
                            description = "Version 2.0 (Target Android 15 / API 35)",
                            icon = Icons.Default.Info,
                            onClick = {},
                            trailing = {},
                            isCompact = isCompactWidth
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        SettingsActionRow(
                            title = "Source Code & Updates",
                            description = "https://github.com/ALEX5402/NewBlackbox",
                            icon = Icons.Default.Public,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ALEX5402/NewBlackbox"))
                                context.startActivity(intent)
                            },
                            isCompact = isCompactWidth
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    isCompact: Boolean = false
) {
    val horizontalPadding = if (isCompact) 12.dp else 16.dp
    val verticalPadding = if (isCompact) 10.dp else 14.dp
    val titleStyle = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
    val descStyle = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(if (isCompact) 32.dp else 40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleStyle.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = descStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    },
    testTag: String = "",
    isCompact: Boolean = false
) {
    val horizontalPadding = if (isCompact) 12.dp else 16.dp
    val verticalPadding = if (isCompact) 10.dp else 14.dp
    val titleStyle = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
    val descStyle = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall

    val modifier = if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(if (isCompact) 32.dp else 40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleStyle.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = descStyle,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        trailing()
    }
}