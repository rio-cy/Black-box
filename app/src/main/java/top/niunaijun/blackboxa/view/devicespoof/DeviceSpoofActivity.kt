package top.niunaijun.blackboxa.view.devicespoof

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.ui.theme.BlackBoxTheme
import top.niunaijun.blackboxa.util.DeviceSpoofConfig
import top.niunaijun.blackboxa.util.DeviceSpoofManager

class DeviceSpoofActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userId = intent.getIntExtra(EXTRA_USER_ID, 0)
        val profileRemark = AppManager.mRemarkSharedPreferences.getString("Remark$userId", "User $userId") ?: "User $userId"

        setContent {
            BlackBoxTheme {
                DeviceSpoofScreen(
                    userId = userId,
                    profileRemark = profileRemark,
                    onBack = { finish() },
                    onSaved = {
                        Toast.makeText(this, "Hardware spoofing profile saved!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"

        fun start(context: Context, userId: Int) {
            val intent = Intent(context, DeviceSpoofActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceSpoofScreen(
    userId: Int,
    profileRemark: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var config by remember { mutableStateOf(DeviceSpoofManager.getConfig(context, userId)) }
    var selectedPresetName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Device Identity Spoofing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Target: $profileRemark (Profile $userId)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            DeviceSpoofManager.saveConfig(context, userId, config)
                            onSaved()
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_save_spoof")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master toggle card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (config.isEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Hardware Spoofing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (config.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (config.isEnabled)
                                "Virtual apps will read spoofed device IDs and model props."
                            else
                                "Apps will read host device true hardware information.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (config.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { config = config.copy(isEnabled = it) },
                        modifier = Modifier.testTag("switch_hardware_spoof")
                    )
                }
            }

            // Quick Presets
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Device Presets",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Instantly load verified hardware fingerprints and specifications:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DeviceSpoofManager.PRESETS.forEach { (name, presetConfig) ->
                            val isSelected = selectedPresetName == name ||
                                (config.model == presetConfig.model && config.brand == presetConfig.brand)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPresetName = name
                                    config = presetConfig.copy(
                                        isEnabled = config.isEnabled,
                                        androidId = config.androidId,
                                        imei = config.imei,
                                        serialNumber = config.serialNumber
                                    )
                                    Toast.makeText(context, "Applied $name preset", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text(name) },
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // One-tap random generator
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Random One-Tap Generator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Generate authentic randomized Android IDs, Luhn-verified IMEIs, and unique serial strings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val newAndroidId = DeviceSpoofManager.generateRandomAndroidId()
                            val newImei = DeviceSpoofManager.generateRandomImei()
                            val newSerial = DeviceSpoofManager.generateRandomSerial()
                            config = config.copy(
                                androidId = newAndroidId,
                                imei = newImei,
                                serialNumber = newSerial
                            )
                            Toast.makeText(context, "Generated new unique hardware identifiers!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_random_generator")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Random Hardware IDs")
                    }
                }
            }

            // Custom manual input fields
            Text(
                text = "Hardware & Telephony Parameters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Brand & Model Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = config.brand,
                    onValueChange = { config = config.copy(brand = it) },
                    label = { Text("Brand") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_brand"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = config.model,
                    onValueChange = { config = config.copy(model = it) },
                    label = { Text("Model") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_model"),
                    singleLine = true
                )
            }

            // Manufacturer & Device Codename Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = config.manufacturer,
                    onValueChange = { config = config.copy(manufacturer = it) },
                    label = { Text("Manufacturer") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_manufacturer"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = config.device,
                    onValueChange = { config = config.copy(device = it) },
                    label = { Text("Device Codename") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_device"),
                    singleLine = true
                )
            }

            // Android ID
            OutlinedTextField(
                value = config.androidId,
                onValueChange = { config = config.copy(androidId = it) },
                label = { Text("Android ID (16 hex digits)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_android_id"),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        config = config.copy(androidId = DeviceSpoofManager.generateRandomAndroidId())
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Randomize Android ID")
                    }
                }
            )

            // IMEI
            OutlinedTextField(
                value = config.imei,
                onValueChange = { config = config.copy(imei = it) },
                label = { Text("IMEI (15 digits)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_imei"),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.SimCard, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        config = config.copy(imei = DeviceSpoofManager.generateRandomImei())
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Randomize IMEI")
                    }
                }
            )

            // Serial Number
            OutlinedTextField(
                value = config.serialNumber,
                onValueChange = { config = config.copy(serialNumber = it) },
                label = { Text("Hardware Serial Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_serial"),
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        config = config.copy(serialNumber = DeviceSpoofManager.generateRandomSerial())
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Randomize Serial")
                    }
                }
            )

            // Fingerprint
            OutlinedTextField(
                value = config.fingerprint,
                onValueChange = { config = config.copy(fingerprint = it) },
                label = { Text("Build Fingerprint") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_fingerprint"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        config = DeviceSpoofManager.PRESET_PIXEL_8_PRO
                        selectedPresetName = "Pixel 8 Pro"
                        Toast.makeText(context, "Reset to Pixel 8 Pro defaults", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Defaults")
                }

                Button(
                    onClick = {
                        DeviceSpoofManager.saveConfig(context, userId, config)
                        onSaved()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Config")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
