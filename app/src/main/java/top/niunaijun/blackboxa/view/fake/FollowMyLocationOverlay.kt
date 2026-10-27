package top.niunaijun.blackboxa.view.fake

import android.app.Activity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackbox.fake.frameworks.BLocationManager
import top.niunaijun.blackboxa.ui.theme.BlackBoxTheme
import java.util.Locale
import kotlin.random.Random

class FollowMyLocationOverlay : ComponentActivity() {

    private var targetPkg: String? = null
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val location: BLocation? = intent.getParcelableExtra("location")
        targetPkg = intent.getStringExtra("pkg")
        userId = intent.getIntExtra("userID", 0)

        var initLat = 28.6139 // New Delhi default
        var initLng = 77.2090

        if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
            initLat = location.latitude
            initLng = location.longitude
        }

        setContent {
            BlackBoxTheme {
                LocationSettingsScreen(
                    targetPkg = targetPkg,
                    userId = userId,
                    initialLat = initLat,
                    initialLng = initLng,
                    onApply = { lat, lng ->
                        applyLocation(lat, lng)
                        finishWithResult(lat, lng)
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun applyLocation(lat: Double, lng: Double) {
        val bLocation = BLocation(lat, lng)
        val pkg = targetPkg
        if (!pkg.isNullOrEmpty()) {
            BLocationManager.get().setPattern(userId, pkg, BLocationManager.OWN_MODE)
            BLocationManager.get().setLocation(userId, pkg, bLocation)
        } else {
            BLocationManager.get().setGlobalLocation(bLocation)
        }
    }

    private fun finishWithResult(lat: Double, lng: Double) {
        intent.putExtra("latitude", lat)
        intent.putExtra("longitude", lng)
        intent.putExtra("pkg", targetPkg)
        setResult(Activity.RESULT_OK, intent)

        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        window.peekDecorView()?.run {
            imm?.hideSoftInputFromWindow(windowToken, 0)
        }
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingsScreen(
    targetPkg: String?,
    userId: Int,
    initialLat: Double,
    initialLng: Double,
    onApply: (Double, Double) -> Unit,
    onBack: () -> Unit
) {
    var latitudeText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialLat)) }
    var longitudeText by remember { mutableStateOf(String.format(Locale.US, "%.6f", initialLng)) }

    val presetLocations = listOf(
        "New Delhi, IN" to (28.6139 to 77.2090),
        "Mumbai, IN" to (19.0760 to 72.8777),
        "Bengaluru, IN" to (12.9716 to 77.5946),
        "Hyderabad, IN" to (17.3850 to 78.4867),
        "Kolkata, IN" to (22.5726 to 88.3639),
        "Chennai, IN" to (13.0827 to 80.2707)
    )

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
                                text = "Virtual Location Settings",
                                style = titleStyle,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Virtual Location Sandbox",
                                style = labelStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_location")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                val lat = latitudeText.toDoubleOrNull() ?: initialLat
                                val lng = longitudeText.toDoubleOrNull() ?: initialLng
                                onApply(lat, lng)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("btn_apply_location")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Apply", style = labelStyle)
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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "GPS Coordinate Configuration",
                                style = titleStyle,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Set exact GPS latitude and longitude coordinates for sandboxed applications.",
                            style = labelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = latitudeText,
                                onValueChange = { latitudeText = it },
                                label = { Text("Latitude", style = labelStyle) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_latitude"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = longitudeText,
                                onValueChange = { longitudeText = it },
                                label = { Text("Longitude", style = labelStyle) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_longitude"),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                val randomLat = 12.0 + Random.nextDouble() * 16.0
                                val randomLng = 72.0 + Random.nextDouble() * 16.0
                                latitudeText = String.format(Locale.US, "%.6f", randomLat)
                                longitudeText = String.format(Locale.US, "%.6f", randomLng)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_randomize_gps")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Randomize Coordinates", style = labelStyle)
                        }
                    }
                }

                // Presets
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontalPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "City Presets",
                            style = titleStyle,
                            fontWeight = FontWeight.Bold
                        )

                        presetLocations.chunked(2).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPresets.forEach { (cityName, coords) ->
                                    FilterChip(
                                        selected = (latitudeText == String.format(Locale.US, "%.6f", coords.first) &&
                                                longitudeText == String.format(Locale.US, "%.6f", coords.second)),
                                        onClick = {
                                            latitudeText = String.format(Locale.US, "%.6f", coords.first)
                                            longitudeText = String.format(Locale.US, "%.6f", coords.second)
                                        },
                                        label = { Text(cityName, style = labelStyle) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", style = labelStyle)
                    }

                    Button(
                        onClick = {
                            val lat = latitudeText.toDoubleOrNull() ?: initialLat
                            val lng = longitudeText.toDoubleOrNull() ?: initialLng
                            onApply(lat, lng)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Location", style = labelStyle)
                    }
                }
            }
        }
    }
}