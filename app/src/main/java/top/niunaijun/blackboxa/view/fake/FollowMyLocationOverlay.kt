package top.niunaijun.blackboxa.view.fake

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import top.niunaijun.blackbox.entity.location.BLocation
import top.niunaijun.blackbox.fake.frameworks.BLocationManager
import top.niunaijun.blackboxa.databinding.ActivityOsmdroidBinding
import top.niunaijun.blackboxa.ui.theme.BlackBoxTheme
import top.niunaijun.blackboxa.util.inflate
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class LocationSimulationMode {
    SINGLE_PIN,
    ROUTE_WALKING
}

enum class WalkingState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

class FollowMyLocationOverlay : AppCompatActivity() {

    private val binding: ActivityOsmdroidBinding by inflate()

    private var targetPkg: String? = null
    private var userId: Int = 0

    // Coordinates state
    private var currentPoint = GeoPoint(30.2736, 120.1563)
    private var singleMarker: Marker? = null
    private var walkingMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private val waypointMarkers = mutableListOf<Marker>()

    // Route state observable by Compose
    private var currentMode by mutableStateOf(LocationSimulationMode.SINGLE_PIN)
    private var walkingState by mutableStateOf(WalkingState.IDLE)
    private var selectedSpeedKmH by mutableFloatStateOf(5.0f) // 5 km/h walk default
    private var routeDistanceMeters by mutableDoubleStateOf(0.0)
    private var walkedDistanceMeters by mutableDoubleStateOf(0.0)
    private val waypointsState = mutableStateListOf<GeoPoint>()
    private var currentStepLat by mutableDoubleStateOf(30.2736)
    private var currentStepLng by mutableDoubleStateOf(120.1563)

    private var simulationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(binding.root)

        val location: BLocation? = intent.getParcelableExtra("location")
        targetPkg = intent.getStringExtra("pkg")
        userId = intent.getIntExtra("userID", 0)

        if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
            currentPoint = GeoPoint(location.latitude, location.longitude)
        }
        currentStepLat = currentPoint.latitude
        currentStepLng = currentPoint.longitude

        initMap()
        initComposeOverlay()
    }

    private fun initMap() {
        val map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        map.controller.setCenter(currentPoint)

        // Single location pin marker
        singleMarker = Marker(map).apply {
            position = currentPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Target Location"
        }
        map.overlays.add(singleMarker)

        // Walking simulation marker
        walkingMarker = Marker(map).apply {
            position = currentPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Simulated Location"
            // Marker will become visible during route walking
            isEnabled = false
        }
        map.overlays.add(walkingMarker)

        // Polyline overlay
        routePolyline = Polyline(map).apply {
            outlinePaint.color = Color.parseColor("#3F51B5")
            outlinePaint.strokeWidth = 10f
        }
        map.overlays.add(routePolyline)

        // Map events
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onMapTapped(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                return false
            }
        }
        map.overlays.add(MapEventsOverlay(receiver))
    }

    private fun onMapTapped(p: GeoPoint) {
        when (currentMode) {
            LocationSimulationMode.SINGLE_PIN -> {
                currentPoint = p
                currentStepLat = p.latitude
                currentStepLng = p.longitude
                singleMarker?.position = p
                binding.map.invalidate()
                pushLocationToSandbox(p.latitude, p.longitude)
            }
            LocationSimulationMode.ROUTE_WALKING -> {
                if (walkingState == WalkingState.RUNNING) {
                    Toast.makeText(this, "Pause simulation to add new waypoints", Toast.LENGTH_SHORT).show()
                    return
                }
                addWaypoint(p)
            }
        }
    }

    private fun addWaypoint(p: GeoPoint) {
        waypointsState.add(p)
        routePolyline?.addPoint(p)

        val marker = Marker(binding.map).apply {
            position = p
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Waypoint #${waypointsState.size}"
        }
        waypointMarkers.add(marker)
        binding.map.overlays.add(marker)

        recalculateRouteDistance()
        binding.map.invalidate()

        if (waypointsState.size == 1) {
            currentPoint = p
            currentStepLat = p.latitude
            currentStepLng = p.longitude
            walkingMarker?.position = p
        }
    }

    private fun clearRoute() {
        stopSimulation()
        waypointsState.clear()
        routePolyline?.setPoints(emptyList())
        waypointMarkers.forEach { binding.map.overlays.remove(it) }
        waypointMarkers.clear()
        routeDistanceMeters = 0.0
        walkedDistanceMeters = 0.0
        walkingState = WalkingState.IDLE
        binding.map.invalidate()
    }

    private fun recalculateRouteDistance() {
        if (waypointsState.size < 2) {
            routeDistanceMeters = 0.0
            return
        }
        var total = 0.0
        for (i in 0 until waypointsState.size - 1) {
            total += calculateDistance(waypointsState[i], waypointsState[i + 1])
        }
        routeDistanceMeters = total
    }

    private fun startRouteSimulation() {
        if (waypointsState.size < 2) {
            Toast.makeText(this, "Please tap at least 2 points on the map to create a route", Toast.LENGTH_SHORT).show()
            return
        }

        walkingState = WalkingState.RUNNING
        walkingMarker?.isEnabled = true

        simulationJob?.cancel()
        simulationJob = lifecycleScope.launch {
            // Speed in meters per second
            var segmentIndex = 0
            var segmentFraction = 0.0

            while (isActive && segmentIndex < waypointsState.size - 1) {
                val speedMps = (selectedSpeedKmH * 1000f) / 3600f
                val p1 = waypointsState[segmentIndex]
                val p2 = waypointsState[segmentIndex + 1]
                val segmentDistance = calculateDistance(p1, p2)

                if (segmentDistance <= 0.0) {
                    segmentIndex++
                    segmentFraction = 0.0
                    continue
                }

                // Advance by speedMps each second
                val stepFraction = (speedMps / segmentDistance).coerceAtLeast(0.0001)
                segmentFraction += stepFraction

                if (segmentFraction >= 1.0) {
                    segmentIndex++
                    segmentFraction = 0.0
                    if (segmentIndex >= waypointsState.size - 1) {
                        // Finished route
                        val finalPoint = waypointsState.last()
                        updateSimulatedPosition(finalPoint)
                        walkedDistanceMeters = routeDistanceMeters
                        walkingState = WalkingState.COMPLETED
                        Toast.makeText(this@FollowMyLocationOverlay, "Route walking simulation completed!", Toast.LENGTH_LONG).show()
                        break
                    }
                }

                val interpolatedLat = p1.latitude + (p2.latitude - p1.latitude) * segmentFraction
                val interpolatedLng = p1.longitude + (p2.longitude - p1.longitude) * segmentFraction
                val simulatedPoint = GeoPoint(interpolatedLat, interpolatedLng)

                updateSimulatedPosition(simulatedPoint)

                // Recalculate walked distance
                var walked = 0.0
                for (k in 0 until segmentIndex) {
                    walked += calculateDistance(waypointsState[k], waypointsState[k + 1])
                }
                walked += segmentDistance * segmentFraction
                walkedDistanceMeters = walked

                delay(1000)
            }
        }
    }

    private fun pauseSimulation() {
        simulationJob?.cancel()
        walkingState = WalkingState.PAUSED
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        walkingState = WalkingState.IDLE
        walkedDistanceMeters = 0.0
        if (waypointsState.isNotEmpty()) {
            val start = waypointsState.first()
            updateSimulatedPosition(start)
        }
    }

    private fun updateSimulatedPosition(point: GeoPoint) {
        currentPoint = point
        currentStepLat = point.latitude
        currentStepLng = point.longitude
        walkingMarker?.position = point
        binding.map.invalidate()
        pushLocationToSandbox(point.latitude, point.longitude)
    }

    private fun pushLocationToSandbox(lat: Double, lng: Double) {
        val bLocation = BLocation(lat, lng)
        val pkg = targetPkg
        if (pkg != null && pkg.isNotEmpty()) {
            BLocationManager.get().setPattern(userId, pkg, BLocationManager.OWN_MODE)
            BLocationManager.get().setLocation(userId, pkg, bLocation)
        } else {
            BLocationManager.get().setGlobalLocation(bLocation)
        }
    }

    private fun finishWithCurrentResult() {
        intent.putExtra("latitude", currentPoint.latitude)
        intent.putExtra("longitude", currentPoint.longitude)
        intent.putExtra("pkg", targetPkg)
        setResult(Activity.RESULT_OK, intent)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        window.peekDecorView()?.run {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
        finish()
    }

    private fun initComposeOverlay() {
        binding.composeOverlay.setContent {
            BlackBoxTheme {
                LocationSimulationOverlayUI(
                    targetPkg = targetPkg,
                    currentMode = currentMode,
                    onModeChanged = { newMode ->
                        if (walkingState == WalkingState.RUNNING) {
                            pauseSimulation()
                        }
                        currentMode = newMode
                        singleMarker?.isEnabled = (newMode == LocationSimulationMode.SINGLE_PIN)
                        binding.map.invalidate()
                    },
                    currentLat = currentStepLat,
                    currentLng = currentStepLng,
                    waypointsCount = waypointsState.size,
                    totalDistanceM = routeDistanceMeters,
                    walkedDistanceM = walkedDistanceMeters,
                    walkingState = walkingState,
                    speedKmH = selectedSpeedKmH,
                    onSpeedChanged = { selectedSpeedKmH = it },
                    onStartRoute = { startRouteSimulation() },
                    onPauseRoute = { pauseSimulation() },
                    onStopRoute = { stopSimulation() },
                    onClearRoute = { clearRoute() },
                    onRecenter = {
                        binding.map.controller.animateTo(currentPoint)
                    },
                    onApplyAndExit = { finishWithCurrentResult() },
                    onBack = { finishWithCurrentResult() }
                )
            }
        }
    }

    override fun onBackPressed() {
        finishWithCurrentResult()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        simulationJob?.cancel()
    }

    private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSimulationOverlayUI(
    targetPkg: String?,
    currentMode: LocationSimulationMode,
    onModeChanged: (LocationSimulationMode) -> Unit,
    currentLat: Double,
    currentLng: Double,
    waypointsCount: Int,
    totalDistanceM: Double,
    walkedDistanceM: Double,
    walkingState: WalkingState,
    speedKmH: Float,
    onSpeedChanged: (Float) -> Unit,
    onStartRoute: () -> Unit,
    onPauseRoute: () -> Unit,
    onStopRoute: () -> Unit,
    onClearRoute: () -> Unit,
    onRecenter: () -> Unit,
    onApplyAndExit: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "World Map Location Simulator",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (targetPkg.isNullOrEmpty()) "Global Sandbox Mocking" else "App: $targetPkg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_map")) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            Button(
                                onClick = onApplyAndExit,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("btn_apply_location")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )

                    // Mode Selector Tabs
                    TabRow(
                        selectedTabIndex = if (currentMode == LocationSimulationMode.SINGLE_PIN) 0 else 1,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = currentMode == LocationSimulationMode.SINGLE_PIN,
                            onClick = { onModeChanged(LocationSimulationMode.SINGLE_PIN) },
                            icon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text("Single Pin") }
                        )
                        Tab(
                            selected = currentMode == LocationSimulationMode.ROUTE_WALKING,
                            onClick = { onModeChanged(LocationSimulationMode.ROUTE_WALKING) },
                            icon = { Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text("Route Walking") }
                        )
                    }
                }
            }

            // Quick Coordinate badge
            Surface(
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = String.format(Locale.US, "GPS: %.6f, %.6f", currentLat, currentLng),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Floating Recenter Button
        IconButton(
            onClick = onRecenter,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .size(48.dp)
                .testTag("btn_recenter_map")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Bottom Controls Card
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentMode == LocationSimulationMode.SINGLE_PIN) {
                    Text(
                        text = "Tap anywhere on the world map to drop a pin. Cloned apps in this profile will immediately perceive this exact location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Route Walking Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Route Walking Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val distanceText = if (totalDistanceM > 1000) {
                                String.format(Locale.US, "%.2f km", totalDistanceM / 1000)
                            } else {
                                String.format(Locale.US, "%.0f m", totalDistanceM)
                            }
                            Text(
                                text = "Waypoints: $waypointsCount • Distance: $distanceText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (waypointsCount > 0) {
                            OutlinedButton(
                                onClick = onClearRoute,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    // Progress indicator if simulation running
                    if (totalDistanceM > 0) {
                        val progress = (walkedDistanceM / totalDistanceM).toFloat().coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format(Locale.US, "Progress: %.0f%%", progress * 100),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Status: ${walkingState.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (walkingState == WalkingState.RUNNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Speed Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = speedKmH == 5f,
                            onClick = { onSpeedChanged(5f) },
                            label = { Text("🚶 Walk (5 km/h)") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        FilterChip(
                            selected = speedKmH == 20f,
                            onClick = { onSpeedChanged(20f) },
                            label = { Text("🚴 Cycle (20 km/h)") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        FilterChip(
                            selected = speedKmH == 60f,
                            onClick = { onSpeedChanged(60f) },
                            label = { Text("🚗 Drive (60 km/h)") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Simulation Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (walkingState == WalkingState.RUNNING) {
                            Button(
                                onClick = onPauseRoute,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause")
                            }
                            OutlinedButton(
                                onClick = onStopRoute,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop")
                            }
                        } else {
                            Button(
                                onClick = onStartRoute,
                                enabled = waypointsCount >= 2,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_start_simulation")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (walkingState == WalkingState.PAUSED) "Resume Walk" else "Start Route Walk")
                            }
                            if (walkingState == WalkingState.PAUSED) {
                                OutlinedButton(
                                    onClick = onStopRoute,
                                    modifier = Modifier.weight(0.6f)
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}