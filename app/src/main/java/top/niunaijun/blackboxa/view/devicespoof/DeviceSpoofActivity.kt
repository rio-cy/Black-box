package top.niunaijun.blackboxa.view.devicespoof

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.ui.theme.BlackBoxTheme
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
                        Toast.makeText(this, "Hardware & Indian SIM spoofing saved!", Toast.LENGTH_SHORT).show()
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 400.dp
        val horizontalPadding = if (isCompactWidth) 10.dp else 16.dp
        val titleStyle = if (isCompactWidth) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
        val labelStyle = if (isCompactWidth) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Device Identity & Indian SIM",
                                style = titleStyle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target: $profileRemark (Profile $userId)",
                                style = labelStyle,
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
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", style = labelStyle)
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
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .padding(horizontalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Hardware & Telephony Spoofing",
                                style = titleStyle,
                                fontWeight = FontWeight.Bold,
                                color = if (config.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (config.isEnabled)
                                    "Cloned apps read Indian SIM (Jio, Airtel, Vi, BSNL) & spoofed hardware."
                                else
                                    "Apps read real device hardware information.",
                                style = labelStyle,
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

                // Indian SIM Operator Spoofing Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Indian SIM Operator Telephony",
                                style = titleStyle,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Select Indian SIM provider & telephony network parameters for cloned apps:",
                            style = labelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DeviceSpoofManager.INDIAN_CARRIERS.forEach { carrier ->
                                val isSelected = config.simCarrierName == carrier.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        config = config.copy(
                                            simCarrierName = carrier.name,
                                            simOperator = carrier.operatorCode
                                        )
                                        Toast.makeText(context, "Selected Indian SIM: ${carrier.name} (${carrier.operatorCode})", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("${carrier.name} (${carrier.operatorCode})", style = labelStyle) },
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

                        // Phone Number
                        OutlinedTextField(
                            value = config.simPhoneNumber,
                            onValueChange = { config = config.copy(simPhoneNumber = it) },
                            label = { Text("Indian Mobile Number (+91)", style = labelStyle) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_sim_phone"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    config = config.copy(simPhoneNumber = DeviceSpoofManager.generateRandomIndianPhoneNumber())
                                }) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Randomize Indian Mobile Number")
                                }
                            }
                        )
                    }
                }

                // Quick Hardware Presets
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontalPadding),
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
                                text = "Device Hardware Presets",
                                style = titleStyle,
                                fontWeight = FontWeight.Bold
                            )
                        }

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
                                            serialNumber = config.serialNumber,
                                            simCarrierName = config.simCarrierName,
                                            simOperator = config.simOperator,
                                            simPhoneNumber = config.simPhoneNumber
                                        )
                                        Toast.makeText(context, "Applied $name preset", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text(name, style = labelStyle) },
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
                            .padding(horizontalPadding),
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
                                style = titleStyle,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Randomize Android ID, IMEI, Serial, Indian Carrier & Phone Number in 1-tap.",
                            style = labelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                config = DeviceSpoofManager.generateRandomConfig(config)
                                Toast.makeText(context, "Generated new Indian SIM & device identity!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_random_generator")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Randomize Hardware & Indian SIM", style = labelStyle)
                        }
                    }
                }

                // Manual fields
                Text(
                    text = "Custom Parameters",
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Brand & Model
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.brand,
                        onValueChange = { config = config.copy(brand = it) },
                        label = { Text("Brand", style = labelStyle) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_brand"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = config.model,
                        onValueChange = { config = config.copy(model = it) },
                        label = { Text("Model", style = labelStyle) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_model"),
                        singleLine = true
                    )
                }

                // Android ID
                OutlinedTextField(
                    value = config.androidId,
                    onValueChange = { config = config.copy(androidId = it) },
                    label = { Text("Android ID (16 hex digits)", style = labelStyle) },
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
                    label = { Text("IMEI (15 digits)", style = labelStyle) },
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

                // Serial
                OutlinedTextField(
                    value = config.serialNumber,
                    onValueChange = { config = config.copy(serialNumber = it) },
                    label = { Text("Serial Number", style = labelStyle) },
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

                Spacer(modifier = Modifier.height(8.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            config = DeviceSpoofManager.PRESET_PIXEL_8_PRO
                            selectedPresetName = "Pixel 8 Pro"
                            Toast.makeText(context, "Reset defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", style = labelStyle)
                    }

                    Button(
                        onClick = {
                            DeviceSpoofManager.saveConfig(context, userId, config)
                            onSaved()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Config", style = labelStyle)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
