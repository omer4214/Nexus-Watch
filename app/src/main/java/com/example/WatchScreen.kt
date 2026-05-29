package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

// Structure representing custom styled theme presets for the watch
data class WatchThemePreset(
    val name: String,
    val bgColors: List<Color>,
    val dialBgColor: Color,
    val timeTextColor: Color,
    val batteryActiveColor: Color,
    val accentTextColor: Color,
    val indicatorColor: Color,
    val isDark: Boolean
)

// Active local watch presets
val watchThemes = listOf(
    WatchThemePreset(
        name = "Elegant Dark",
        bgColors = listOf(Color(0xFF0C0E12), Color(0xFF1C1F26)),
        dialBgColor = Color(0xFF0C0E12), 
        timeTextColor = Color(0xFFFFFFFF), 
        batteryActiveColor = Color(0xFFD0E4FF), 
        accentTextColor = Color(0xFFD0E4FF), 
        indicatorColor = Color(0xFF2A3B52), 
        isDark = true
    ),
    WatchThemePreset(
        name = "Cosmic Slate",
        bgColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)), //Slate gradient
        dialBgColor = Color(0xFF020617), //Near absolute black
        timeTextColor = Color(0xFFF1F5F9), //Bright gray
        batteryActiveColor = Color(0xFF10B981), //Emerald
        accentTextColor = Color(0xFF38BDF8), //Sky blue
        indicatorColor = Color(0xFF64748B), //Slate secondary
        isDark = true
    ),
    WatchThemePreset(
        name = "Nebula Neon",
        bgColors = listOf(Color(0xFF1E1B4B), Color(0xFF311042)), //Dark indigo
        dialBgColor = Color(0xFF0B0424), //Nebula core black
        timeTextColor = Color(0xFFF472B6), //Neon pink
        batteryActiveColor = Color(0xFF22D3EE), //Cyan
        accentTextColor = Color(0xFFC084FC), //Lilac purple
        indicatorColor = Color(0xFF818CF8), //Indigo secondary
        isDark = true
    ),
    WatchThemePreset(
        name = "Solar Flare",
        bgColors = listOf(Color(0xFF2D0A02), Color(0xFF1C0D02)), //Burnt orange
        dialBgColor = Color(0xFF0F0400), //Pure obsidian
        timeTextColor = Color(0xFFFBBF24), //Sun gold yellow
        batteryActiveColor = Color(0xFFF97316), //Orange flare
        accentTextColor = Color(0xFFFCD34D), //Warm straw
        indicatorColor = Color(0xFFB45309), //Rich copper
        isDark = true
    ),
    WatchThemePreset(
        name = "Aurora Mint",
        bgColors = listOf(Color(0xFF062321), Color(0xFF0B1424)), //Deep forest
        dialBgColor = Color(0xFF02171E), //Deep navy-teal
        timeTextColor = Color(0xFF34D399), //Radiant emerald mint
        batteryActiveColor = Color(0xFF0284C7), //Aqua marine
        accentTextColor = Color(0xFFE2E8F0), //Ethereal white
        indicatorColor = Color(0xFF10B981), //Vivid green
        isDark = true
    ),
    WatchThemePreset(
        name = "Arctic Monolithic",
        bgColors = listOf(Color(0xFFE2E8F0), Color(0xFFFFFFFF)), //Bright slate-white
        dialBgColor = Color(0xFFF8FAFC), //Pure polar snow
        timeTextColor = Color(0xFF0F172A), //Coal black hours
        batteryActiveColor = Color(0xFF0284C7), //Iceberg blue
        accentTextColor = Color(0xFF0369A1), //Ocean deep
        indicatorColor = Color(0xFF94A3B8), //Silver outline
        isDark = false
    )
)

@Composable
fun WatchScreen(viewModel: WatchViewModel = viewModel()) {
    val currentTime by viewModel.currentTime.collectAsState()
    val themeIndex by viewModel.selectedThemeIndex.collectAsState()
    val condition by viewModel.currentWeather.collectAsState()
    val isCelsius by viewModel.isCelsius.collectAsState()
    val isBatteryOverride by viewModel.isBatteryOverrideActive.collectAsState()
    val simBatteryLvl by viewModel.simulatedBatteryLevel.collectAsState()
    val simIsCharging by viewModel.simulatedIsCharging.collectAsState()

    // Amazfit Companion Sim states
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val activeDeviceThemeIndex by viewModel.activeWatchFaceOnDeviceIndex.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    // Real system battery observer
    val realBattery = rememberBatteryState()
    
    // Resolve states based on override settings
    val activeBatteryLevel = if (isBatteryOverride) simBatteryLvl else realBattery.level
    val activeIsCharging = if (isBatteryOverride) simIsCharging else realBattery.isCharging

    val activePreset = watchThemes.getOrElse(themeIndex) { watchThemes[0] }
    val activeDevicePreset = watchThemes.getOrElse(activeDeviceThemeIndex) { watchThemes[0] }
    val weatherDetails = viewModel.getWeatherDetails(condition)

    // Setup periodic 1-second clock ticker updates
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateTime()
            kotlinx.coroutines.delay(1000)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (activePreset.isDark) Color(0xFF0C0E12) else Color(0xFFF8FAFC))
            .drawBehind {
                if (activePreset.isDark) {
                    val w = size.width
                    val h = size.height
                    
                    // Ambient radial #2A3B52 gradient at top-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2A3B52), Color.Transparent),
                            center = Offset(w * 0.8f, h * 0.2f),
                            radius = w * 0.75f
                        ),
                        radius = w * 0.75f,
                        center = Offset(w * 0.8f, h * 0.2f)
                    )
                    
                    // Ambient radial #1A1F2B gradient at bottom-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1A1F2B), Color.Transparent),
                            center = Offset(w * 0.2f, h * 0.8f),
                            radius = w * 0.85f
                        ),
                        radius = w * 0.85f,
                        center = Offset(w * 0.2f, h * 0.8f)
                    )
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val isWideScreen = maxWidth > 600.dp

        if (isWideScreen) {
            // landscape List-Detail side-by-side mode for tablet or foldables
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Immersive Simulated Smartwatch Model
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    WatchModel(
                        currentTime = currentTime,
                        previewPreset = activePreset,
                        activeDeviceThemePreset = activeDevicePreset,
                        condition = condition,
                        weatherDetails = weatherDetails,
                        isCelsius = isCelsius,
                        batteryLevel = activeBatteryLevel,
                        isCharging = activeIsCharging,
                        connectedDevice = connectedDevice,
                        syncProgress = syncProgress,
                        syncMessage = syncMessage,
                        modifier = Modifier.testTag("watch_model_container")
                    )
                }

                // Right Pane: Elegantly scrolling watch options controller
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                        .background(
                            if (activePreset.isDark) Color(0xFF1C1F26) else Color.White,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            1.dp,
                            if (activePreset.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ControlModule(
                            viewModel = viewModel,
                            themeIndex = themeIndex,
                            condition = condition,
                            isCelsius = isCelsius,
                            isBatteryOverride = isBatteryOverride,
                            simBatteryLvl = simBatteryLvl,
                            simIsCharging = simIsCharging,
                            realBatteryLevel = realBattery.level,
                            realIsCharging = realBattery.isCharging,
                            activePreset = activePreset,
                            connectedDevice = connectedDevice,
                            isScanning = isScanning,
                            discoveredDevices = discoveredDevices,
                            isConnecting = isConnecting,
                            activeDeviceThemeIndex = activeDeviceThemeIndex,
                            syncProgress = syncProgress,
                            syncMessage = syncMessage
                        )
                    }
                }
            }
        } else {
            // Standard vertical phone stacked layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top watch dial preview module
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WatchModel(
                        currentTime = currentTime,
                        previewPreset = activePreset,
                        activeDeviceThemePreset = activeDevicePreset,
                        condition = condition,
                        weatherDetails = weatherDetails,
                        isCelsius = isCelsius,
                        batteryLevel = activeBatteryLevel,
                        isCharging = activeIsCharging,
                        connectedDevice = connectedDevice,
                        syncProgress = syncProgress,
                        syncMessage = syncMessage,
                        modifier = Modifier.testTag("watch_model_container")
                    )
                }

                // Bottom Dashboard module
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 16.dp)
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            if (activePreset.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                            RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activePreset.isDark) Color(0xFF1C1F26) else Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ControlModule(
                            viewModel = viewModel,
                            themeIndex = themeIndex,
                            condition = condition,
                            isCelsius = isCelsius,
                            isBatteryOverride = isBatteryOverride,
                            simBatteryLvl = simBatteryLvl,
                            simIsCharging = simIsCharging,
                            realBatteryLevel = realBattery.level,
                            realIsCharging = realBattery.isCharging,
                            activePreset = activePreset,
                            connectedDevice = connectedDevice,
                            isScanning = isScanning,
                            discoveredDevices = discoveredDevices,
                            isConnecting = isConnecting,
                            activeDeviceThemeIndex = activeDeviceThemeIndex,
                            syncProgress = syncProgress,
                            syncMessage = syncMessage
                        )
                    }
                }
            }
        }
    }
}

// Simulated High-Fidelity physical smartwatch frame with glass reflects + physical crown buttons
@Composable
fun WatchModel(
    currentTime: java.time.LocalDateTime,
    previewPreset: WatchThemePreset,
    activeDeviceThemePreset: WatchThemePreset,
    condition: WeatherCondition,
    weatherDetails: WeatherDetails,
    isCelsius: Boolean,
    batteryLevel: Int,
    isCharging: Boolean,
    connectedDevice: String?,
    syncProgress: Int?,
    syncMessage: String,
    modifier: Modifier = Modifier
) {
    val dialBackgroundColor = animateColorAsState(previewPreset.dialBgColor, animationSpec = tween(600), label = "dial_bg")
    val casingColor = if (previewPreset.isDark) Color(0xFF334155) else Color(0xFF94A3B8)
    val innerShadowColor = if (previewPreset.isDark) Color(0xFF030712) else Color(0xFFCBD5E1)

    Box(
        modifier = modifier
            .size(340.dp)
            .padding(12.dp)
            .shadow(24.dp, shape = CircleShape, clip = false, spotColor = Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        // Physical Watch Case Base Circle (Steel/Chrono Bezel)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2

            // Watch strap connections background hint (simulated)
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(size.width / 2 - 40.dp.toPx(), 0f),
                size = Size(80.dp.toPx(), size.height),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                alpha = 0.85f
            )

            // Outer Steel/Chrome Bezel Shadow
            drawCircle(
                color = innerShadowColor,
                radius = outerRadius,
                center = center
            )

            // Metal Bezel Ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(casingColor.copy(alpha = 0.9f), casingColor.darken(0.3f), casingColor.copy(alpha = 0.9f))
                ),
                radius = outerRadius - 4.dp.toPx(),
                center = center
            )

            // Outer Bezel Inner outline
            drawCircle(
                color = Color.Black.copy(alpha = 0.8f),
                radius = outerRadius - 10.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inner Chrono dial numbers & ticks
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val tickRadius = (size.width / 2) - 8.dp.toPx()

            // Drawing 12 stylish dial marker ticks around the chrono bezel
            for (angle in 0 until 360 step 30) {
                val rad = Math.toRadians(angle.toDouble())
                val startX = (center.x + (tickRadius - 6.dp.toPx()) * Math.cos(rad)).toFloat()
                val startY = (center.y + (tickRadius - 6.dp.toPx()) * Math.sin(rad)).toFloat()
                val endX = (center.x + tickRadius * Math.cos(rad)).toFloat()
                val endY = (center.y + tickRadius * Math.sin(rad)).toFloat()

                // Highlight important anchors (12, 3, 6, 9)
                val isMajor = angle % 90 == 0
                drawContext.canvas.drawLine(
                    p1 = Offset(startX, startY),
                    p2 = Offset(endX, endY),
                    paint = androidx.compose.ui.graphics.Paint().apply {
                        color = if (isMajor) previewPreset.batteryActiveColor else previewPreset.indicatorColor.copy(alpha = 0.7f)
                        strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()
                    }
                )
            }
        }

        // RIGHT PHYSICAL CROWN / BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 12.dp)
                .size(width = 16.dp, height = 36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(casingColor, casingColor.darken(0.5f), casingColor)
                    )
                )
                .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        )

        // SCREEN / DIAL CONTAINER (Atmospheric active display screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(CircleShape)
                .background(dialBackgroundColor.value)
        ) {
            // Dynamic Active Weather Particle overlay background
            WeatherParticleBackground(condition = condition, themePreset = previewPreset)

            // Actual Digital Clock Display & Battery Circle
            WatchDialDisplay(
                currentTime = currentTime,
                activePreset = previewPreset,
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                condition = condition,
                weatherDetails = weatherDetails,
                isCelsius = isCelsius
            )

            // Connection/Sync Status Indicator Overlay Badge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (syncProgress == null) {
                    val badgeBg = if (connectedDevice == null) {
                        Color(0xFFEF4444).copy(alpha = 0.15f)
                    } else if (previewPreset == activeDeviceThemePreset) {
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    } else {
                        Color(0xFF3B82F6).copy(alpha = 0.15f)
                    }
                    val badgeBorder = if (connectedDevice == null) {
                        Color(0xFFEF4444).copy(alpha = 0.3f)
                    } else if (previewPreset == activeDeviceThemePreset) {
                        Color(0xFF10B981).copy(alpha = 0.3f)
                    } else {
                        Color(0xFF3B82F6).copy(alpha = 0.3f)
                    }
                    val badgeText = if (connectedDevice == null) {
                        "⚠️ CİHAZ BAĞLI DEĞİL"
                    } else if (previewPreset == activeDeviceThemePreset) {
                        "🟢 KADRAN SAATTE AKTİF"
                    } else {
                        "⚡ AKTARMAYA HAZIR"
                    }
                    val badgeTextColor = if (connectedDevice == null) {
                        Color(0xFFFCA5A5)
                    } else if (previewPreset == activeDeviceThemePreset) {
                        Color(0xFF86EFAC)
                    } else {
                        Color(0xFF93C5FD)
                    }

                    Row(
                        modifier = Modifier
                            .background(badgeBg, shape = RoundedCornerShape(20.dp))
                            .border(1.dp, badgeBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = badgeText,
                            style = TextStyle(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }

            // FULL DIRECT DIAL SYNC PROGRESS OVERLAY (Highly immersive physical loading blocker)
            AnimatedVisibility(
                visible = syncProgress != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                            CircularProgressIndicator(
                                progress = { (syncProgress ?: 0) / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = previewPreset.accentTextColor,
                                strokeWidth = 4.dp,
                                trackColor = previewPreset.accentTextColor.copy(alpha = 0.15f),
                            )
                            Text(
                                text = "${syncProgress ?: 0}%",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "KADRAN AKTARILIYOR",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = previewPreset.accentTextColor,
                                letterSpacing = 1.2.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = syncMessage,
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // Glass Reflection Surface Curved Overlay (Gives absolute premium sleek 3D depth)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glassBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.02f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.15f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                drawCircle(brush = glassBrush)

                // High light crescent reflection shine
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = -110f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                    size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx())
                )
            }
        }
    }
}

// Dynamic animated particles system drawn behind watch details based on condition
@Composable
fun WeatherParticleBackground(condition: WeatherCondition, themePreset: WatchThemePreset) {
    val (particles, tick) = rememberWeatherStateParticles(condition = condition)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val dummy = tick // Forces Canvas redrawing invalidation on tick change
        val width = size.width
        val height = size.height

        when (condition) {
            WeatherCondition.SUNNY -> {
                // Drawing radiant pulsing sun heat rays or soft halo gradient
                val center = Offset(width / 2f, height * 0.28f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themePreset.accentTextColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = width * 0.45f
                    ),
                    center = center,
                    radius = width * 0.45f
                )
            }
            WeatherCondition.OVERCAST -> {
                // Large drifting fuzzy cloud or mist dots
                particles.forEach { p ->
                    val opacity = p.alpha * 0.35f
                    drawCircle(
                        color = if (themePreset.isDark) Color.White.copy(alpha = opacity) else Color(0xFF475569).copy(alpha = opacity),
                        radius = p.size.dp.toPx(),
                        center = Offset(p.x * width, p.y * height)
                    )
                }
            }
            WeatherCondition.RAINY, WeatherCondition.STORMY -> {
                // Rain streaking lines falling downward diagonally
                particles.forEach { p ->
                    val opacity = p.alpha * 0.7f
                    drawLine(
                        color = themePreset.accentTextColor.copy(alpha = opacity),
                        start = Offset(p.x * width, p.y * height),
                        end = Offset((p.x - 0.05f) * width, (p.y + 0.12f) * height),
                        strokeWidth = p.size.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                // Lightning flash trigger for thunderstorms
                if (condition == WeatherCondition.STORMY) {
                    val frameTimer = System.currentTimeMillis()
                    // Random trigger based on system clock to make it sporadic & realistic
                    if (frameTimer % 3700 < 110) {
                        drawRect(
                            color = Color(0xFFF0FDF4).copy(alpha = 0.4f),
                            size = Size(width, height)
                        )
                    }
                }
            }
            WeatherCondition.SNOWY -> {
                // Hexagonal snow or fuzzy dots cascading
                particles.forEach { p ->
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha),
                        radius = p.size.dp.toPx() / 2,
                        center = Offset(p.x * width, p.y * height)
                    )
                }
            }
        }
    }
}

// Live clock state renderer inside dial
@Composable
fun WatchDialDisplay(
    currentTime: java.time.LocalDateTime,
    activePreset: WatchThemePreset,
    batteryLevel: Int,
    isCharging: Boolean,
    condition: WeatherCondition,
    weatherDetails: WeatherDetails,
    isCelsius: Boolean
) {
    val locale = Locale.getDefault()
    val hourString = currentTime.format(DateTimeFormatter.ofPattern("HH"))
    val minuteString = currentTime.format(DateTimeFormatter.ofPattern("mm"))
    val secondsString = currentTime.format(DateTimeFormatter.ofPattern("ss"))
    val dateString = currentTime.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)).uppercase()

    // Smooth color change of elements depending on battery alerts
    val batteryWarnState = batteryLevel <= 20
    val ringBaseColor = if (activePreset.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    
    val ringColorAnimate by animateColorAsState(
        targetValue = when {
            isCharging -> Color(0xFF10B981) //Charging Green
            batteryWarnState -> Color(0xFFEF4444) //Low Battery Pulsing Alert Red
            batteryLevel <= 40 -> Color(0xFFF97316) //Warning Amber
            else -> activePreset.batteryActiveColor
        },
        animationSpec = tween(500),
        label = "ring_color"
    )

    // Animated sweeping arc representing current remaining battery percentage
    val animBatterySweepAngle by animateFloatAsState(
        targetValue = (batteryLevel / 100f) * 240f, //Arc range is 240 degrees at bottom
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "battery_sweep"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Battery status arc ring on the outer face radius
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val ringRadius = (size.width / 2) - 8.dp.toPx()
            val startAngle = 150f //Centered bottom arc sweep

            // Background arc channel
            drawArc(
                color = ringBaseColor,
                startAngle = startAngle,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Animated actual active battery percentage arc
            drawArc(
                color = ringColorAnimate,
                startAngle = startAngle,
                sweepAngle = animBatterySweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (isCharging) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
                )
            )
        }

        // Inner Chronometer content elements
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp, horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Current Weather Icon & Temp
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Custom drawn mini vector icon based on weather
                MiniWeatherSymbol(
                    condition = condition,
                    color = activePreset.accentTextColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = weatherDetails.getTemperatureString(isCelsius),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = activePreset.accentTextColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // 2. High-Readability Sleek Digital Hours, Minutes and Seconds
            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour indicator
                    Text(
                        text = hourString,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = activePreset.timeTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 44.sp,
                            lineHeight = 44.sp,
                            letterSpacing = (-1.5).sp
                        )
                    )
                    
                    // Custom pulsing glowing spacer colon
                    Text(
                        text = ":",
                        modifier = Modifier.padding(horizontal = 2.dp),
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = activePreset.timeTextColor.copy(alpha = if (currentTime.second % 2 == 0) 1.0f else 0.3f),
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 44.sp,
                            lineHeight = 44.sp
                        )
                    )

                    // Minute indicator
                    Text(
                        text = minuteString,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = activePreset.timeTextColor,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 44.sp,
                            lineHeight = 44.sp,
                            letterSpacing = (-1.5).sp
                        )
                    )

                    // Compact floating seconds
                    Text(
                        text = secondsString,
                        modifier = Modifier
                            .padding(bottom = 6.dp, start = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = activePreset.accentTextColor,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                // Sleek capitalized Date line
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            // 3. Compact Battery percentage tracker details
            Row(
                modifier = Modifier
                    .weight(0.8f)
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(visible = isCharging, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = "⚡",
                        modifier = Modifier.padding(end = 4.dp),
                        style = TextStyle(
                            color = Color(0xFF10B981),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = "$batteryLevel%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (batteryWarnState) Color(0xFFEF4444) else (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                )

                AnimatedVisibility(visible = batteryWarnState, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = " BATT LOW",
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.2.sp
                        )
                    )
                }
            }
        }
    }
}

// Mini custom graphic layouts representing weather symbols inside dial
@Composable
fun MiniWeatherSymbol(condition: WeatherCondition, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (condition) {
            WeatherCondition.SUNNY -> {
                drawCircle(
                    color = color,
                    radius = w * 0.35f,
                    center = Offset(w / 2, h / 2)
                )
                // Small solar rays
                for (angle in 0 until 360 step 60) {
                    val rad = Math.toRadians(angle.toDouble())
                    val sx = (w / 2 + w * 0.38f * Math.cos(rad)).toFloat()
                    val sy = (h / 2 + h * 0.38f * Math.sin(rad)).toFloat()
                    val ex = (w / 2 + w * 0.52f * Math.cos(rad)).toFloat()
                    val ey = (h / 2 + h * 0.52f * Math.sin(rad)).toFloat()
                    drawLine(color = color, start = Offset(sx, sy), end = Offset(ex, ey), strokeWidth = 1.5.dp.toPx())
                }
            }
            WeatherCondition.OVERCAST -> {
                // Sleek minimalist overlapping cloud geometry
                val cloudPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.25f, h * 0.7f)
                    cubicTo(w * 0.1f, h * 0.7f, w * 0.1f, h * 0.5f, w * 0.25f, h * 0.5f)
                    cubicTo(w * 0.3f, h * 0.25f, w * 0.65f, h * 0.25f, w * 0.7f, h * 0.5f)
                    cubicTo(w * 0.85f, h * 0.5f, w * 0.85f, h * 0.7f, w * 0.75f, h * 0.7f)
                    close()
                }
                drawPath(path = cloudPath, color = color)
            }
            WeatherCondition.RAINY -> {
                // Cloud outline plus tiny teardrop lines
                val cloudPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.25f, h * 0.6f)
                    cubicTo(w * 0.1f, h * 0.6f, w * 0.1f, h * 0.4f, w * 0.25f, h * 0.4f)
                    cubicTo(w * 0.3f, h * 0.2f, w * 0.65f, h * 0.2f, w * 0.7f, h * 0.4f)
                    cubicTo(w * 0.85f, h * 0.4f, w * 0.85f, h * 0.6f, w * 0.75f, h * 0.6f)
                    close()
                }
                drawPath(path = cloudPath, color = color.copy(alpha = 0.5f))
                
                // Falling raindrops paths
                drawLine(color = color, start = Offset(w * 0.35f, h * 0.7f), end = Offset(w * 0.3f, h * 0.9f), strokeWidth = 1.5.dp.toPx())
                drawLine(color = color, start = Offset(w * 0.55f, h * 0.72f), end = Offset(w * 0.5f, h * 0.92f), strokeWidth = 1.5.dp.toPx())
            }
            WeatherCondition.STORMY -> {
                // Cloud with zigzag yellow storm bolt
                val cloudPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.25f, h * 0.55f)
                    cubicTo(w * 0.1f, h * 0.55f, w * 0.1f, h * 0.35f, w * 0.25f, h * 0.35f)
                    cubicTo(w * 0.3f, h * 0.15f, w * 0.65f, h * 0.15f, w * 0.7f, h * 0.35f)
                    cubicTo(w * 0.85f, h * 0.35f, w * 0.85f, h * 0.55f, w * 0.75f, h * 0.55f)
                    close()
                }
                drawPath(path = cloudPath, color = color.copy(alpha = 0.5f))
                
                val boltPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.55f, h * 0.50f)
                    lineTo(w * 0.42f, h * 0.75f)
                    lineTo(w * 0.53f, h * 0.75f)
                    lineTo(w * 0.42f, h * 0.98f)
                }
                drawPath(
                    path = boltPath,
                    color = Color(0xFFFBBF24),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            WeatherCondition.SNOWY -> {
                // Small hexagonal crystalline structure
                val cx = w / 2
                val cy = h / 2
                for (a in 0 until 360 step 60) {
                    val rad = Math.toRadians(a.toDouble())
                    val ex = (cx + w * 0.4f * Math.cos(rad)).toFloat()
                    val ey = (cy + h * 0.4f * Math.sin(rad)).toFloat()
                    drawLine(color = color, start = Offset(cx, cy), end = Offset(ex, ey), strokeWidth = 1.5.dp.toPx())
                    
                    // Tiny crossbar elements
                    val bx1 = (cx + w * 0.25f * Math.cos(rad)).toFloat()
                    val by1 = (cy + h * 0.25f * Math.sin(rad)).toFloat()
                    val dxL = (bx1 + w * 0.12f * Math.cos(rad + Math.toRadians(45.0))).toFloat()
                    val dyL = (by1 + h * 0.12f * Math.sin(rad + Math.toRadians(45.0))).toFloat()
                    val dxR = (bx1 + w * 0.12f * Math.cos(rad - Math.toRadians(45.0))).toFloat()
                    val dyR = (by1 + h * 0.12f * Math.sin(rad - Math.toRadians(45.0))).toFloat()
                    drawLine(color = color, start = Offset(bx1, by1), end = Offset(dxL, dyL), strokeWidth = 1.2.dp.toPx())
                    drawLine(color = color, start = Offset(bx1, by1), end = Offset(dxR, dyR), strokeWidth = 1.2.dp.toPx())
                }
            }
        }
    }
}

// Controller elements separated out for scrollability, efficiency, and clarity
@Composable
fun ColumnScope.ControlModule(
    viewModel: WatchViewModel,
    themeIndex: Int,
    condition: WeatherCondition,
    isCelsius: Boolean,
    isBatteryOverride: Boolean,
    simBatteryLvl: Int,
    simIsCharging: Boolean,
    realBatteryLevel: Int,
    realIsCharging: Boolean,
    activePreset: WatchThemePreset,
    connectedDevice: String?,
    isScanning: Boolean,
    discoveredDevices: List<String>,
    isConnecting: Boolean,
    activeDeviceThemeIndex: Int,
    syncProgress: Int?,
    syncMessage: String
) {
    val subtitleColor = if (activePreset.isDark) Color(0xFFD0E4FF).copy(alpha = 0.6f) else Color(0xFF475569)
    val textBaseColor = if (activePreset.isDark) Color(0xFFE2E2E6) else Color(0xFF0F172A)
    val containerBgColor = if (activePreset.isDark) Color(0xFF0C0E12) else Color(0xFFF1F5F9)
    val panelBgColor = if (activePreset.isDark) Color(0xFF0C0E12) else Color(0xFFF8FAFC)
    val batteryLevel = if (isBatteryOverride) simBatteryLvl else realBatteryLevel

    // A. HEADER TITLE
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CHRONO CLOUD",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp,
                color = activePreset.timeTextColor
            )
        )
        Text(
            text = "Amazfit Companion & Dial Controller",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = subtitleColor
            )
        )
    }

    HorizontalDivider(color = (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.08f))

    // A2. AMAZFIT DEVICE CONTAINER
    Text(
        text = "AKILLI SAAT BAĞLANTISI (BLUETOOTH)",
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = activePreset.accentTextColor,
            letterSpacing = 0.5.sp
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = panelBgColor,
        border = BorderStroke(1.dp, (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (connectedDevice == null) {
                // Disconnected view
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saat Bağlı Değil ⚠️",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        )
                        Text(
                            text = "Yazılımları aktarmak için cihaz bulun",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = subtitleColor
                            )
                        )
                    }

                    if (!isScanning && !isConnecting) {
                        Button(
                            onClick = { viewModel.startScanningNearbyWatches() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activePreset.accentTextColor,
                                contentColor = if (activePreset.isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Cihaz Ara", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    } else if (isScanning) {
                        Button(
                            onClick = { viewModel.stopScanning() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = textBaseColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Durdur", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                if (isScanning) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = activePreset.accentTextColor,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Yakındaki Amazfit cihazları sorgulanıyor...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = subtitleColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                if (isConnecting) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = activePreset.accentTextColor,
                            trackColor = activePreset.accentTextColor.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Parametreler eşitleniyor ve bağlanıyor...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = subtitleColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Discovered devices list
                if (discoveredDevices.isNotEmpty() && !isConnecting) {
                    HorizontalDivider(color = (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.05f))
                    Text(
                        text = "EDİNİLEN AKTİF YAKIN CİHAZLAR:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = subtitleColor,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        discoveredDevices.forEach { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activePreset.isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f))
                                    .border(1.dp, (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.connectDevice(dev) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("⌚", fontSize = 16.sp)
                                    Text(
                                        text = dev,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = textBaseColor
                                        )
                                    )
                                }
                                Text(
                                    text = "Bağlan",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = activePreset.accentTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

            } else {
                // Connected State Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(activePreset.accentTextColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⌚", fontSize = 18.sp)
                        }
                        
                        Column {
                            Text(
                                text = connectedDevice,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textBaseColor
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "Bağlandı (Pil: %${batteryLevel})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.disconnectDevice() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (activePreset.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
                            .size(32.dp)
                    ) {
                        Text("✕", style = TextStyle(fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 11.sp))
                    }
                }

                HorizontalDivider(color = (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.05f))

                // Watch face sync status details
                val isCurrentFaceSynced = themeIndex == activeDeviceThemeIndex
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCurrentFaceSynced) "Arayüz Saatte Aktif ✓" else "Yeni Arayüz Aktarılmayı Bekliyor",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentFaceSynced) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        )
                        Text(
                            text = "Seçilen Stil: ${activePreset.name}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = subtitleColor
                            )
                        )
                    }

                    if (syncProgress == null) {
                        Button(
                            onClick = { viewModel.syncWatchFaceToDevice(themeIndex) },
                            enabled = !isCurrentFaceSynced,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrentFaceSynced) Color.Gray.copy(alpha = 0.2f) else activePreset.accentTextColor,
                                contentColor = if (activePreset.isDark) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isCurrentFaceSynced) "Aktarıldı ✓" else "Saate Aktar",
                                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = activePreset.accentTextColor,
                                strokeWidth = 2.0.dp
                            )
                            Text(
                                text = "Yükleniyor %${syncProgress}",
                                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textBaseColor)
                            )
                        }
                    }
                }
            }
        }
    }

    HorizontalDivider(color = (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.08f))

    // B. WATCH THEME PRESETS SELECTOR
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "PRESET STYLES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = activePreset.accentTextColor,
                letterSpacing = 0.5.sp
            )
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            itemsIndexed(watchThemes) { index, item ->
                val isSelected = index == themeIndex
                val borderAnimate = if (isSelected) {
                    BorderStroke(2.dp, activePreset.accentTextColor)
                } else {
                    BorderStroke(1.dp, (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.05f))
                }
                
                Card(
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("theme_preset_${index}")
                        .clickable {
                            viewModel.setThemeIndex(index)
                        },
                    shape = RoundedCornerShape(12.dp),
                    border = borderAnimate,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            item.dialBgColor
                        } else {
                            panelBgColor
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Colored Dot indication
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(item.timeTextColor)
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) item.timeTextColor else textBaseColor
                            )
                        )
                    }
                }
            }
        }
    }

    // C. WEATHER CONDITION CONTROLLERS
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DYNAMIC WEATHER CONTEXT",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = activePreset.accentTextColor,
                letterSpacing = 0.5.sp
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val conditions = WeatherCondition.values()
            // Grid flow inside row of modern visual weather pill buttons
            conditions.forEach { cond ->
                val isSelected = cond == condition
                val containerColor = if (isSelected) {
                    activePreset.accentTextColor
                } else {
                    panelBgColor
                }
                val valueColor = if (isSelected) {
                    if (activePreset.isDark) Color.Black else Color.White
                } else {
                    textBaseColor
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weather_btn_${cond.name.lowercase()}")
                        .clip(RoundedCornerShape(10.dp))
                        .background(containerColor)
                        .border(
                            1.dp,
                            if (isSelected) Color.Transparent else (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.setWeatherCondition(cond) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (cond) {
                                WeatherCondition.SUNNY -> "☀️"
                                WeatherCondition.RAINY -> "🌧️"
                                WeatherCondition.SNOWY -> "❄️"
                                WeatherCondition.STORMY -> "⛈️"
                                WeatherCondition.OVERCAST -> "☁️"
                            },
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = cond.name.substring(0, 3),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = valueColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Informative active weather description sheet showing simulated location data
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = panelBgColor,
            border = BorderStroke(1.dp, (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.getWeatherDetails(condition).location,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textBaseColor
                        )
                    )
                    Text(
                        text = viewModel.getWeatherDetails(condition).phrase,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Wind: ${viewModel.getWeatherDetails(condition).windSpeed}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = subtitleColor,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = "Sunset: ${viewModel.getWeatherDetails(condition).sunsetTime}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = subtitleColor,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }

    // D. SYSTEM HARDWARE INTEGRATIONS & MANUAL TEST BENCH
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL HARDWARE & TEST PANEL",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = activePreset.accentTextColor,
                    letterSpacing = 0.5.sp
                )
            )

            // Dynamic F/C degree switch
            AssistChip(
                onClick = { viewModel.toggleTemperatureUnit() },
                label = {
                    Text(
                        text = if (isCelsius) "UNIT: °C" else "UNIT: °F",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = textBaseColor,
                    containerColor = panelBgColor
                )
            )
        }

        // Live battery detail row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = panelBgColor,
            border = BorderStroke(1.dp, (if (activePreset.isDark) Color.White else Color.Black).copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Battery Source",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = textBaseColor
                        )
                    )

                    // Display active indicator tag
                    Text(
                        text = if (isBatteryOverride) "SIMULATING OVERRIDE" else "LIVE HARDWARE CONNECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isBatteryOverride) Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontSize = 9.sp
                        )
                    )
                }

                // Override Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manual simulation override",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = subtitleColor
                        )
                    )
                    Switch(
                        checked = isBatteryOverride,
                        onCheckedChange = { active -> viewModel.setBatteryOverride(active, simBatteryLvl, simIsCharging) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = activePreset.batteryActiveColor,
                            checkedTrackColor = activePreset.batteryActiveColor.copy(alpha = 0.4f),
                            uncheckedThumbColor = subtitleColor,
                            uncheckedTrackColor = containerBgColor
                        ),
                        modifier = Modifier.testTag("battery_override_switch")
                    )
                }

                // Sliding Controllers visible only if active
                if (isBatteryOverride) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Simulated Battery Level: $simBatteryLvl%",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = textBaseColor
                                )
                            )
                        }
                        Slider(
                            value = simBatteryLvl.toFloat(),
                            onValueChange = { lvl -> viewModel.setBatteryOverride(true, lvl.toInt(), simIsCharging) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = activePreset.batteryActiveColor,
                                activeTrackColor = activePreset.batteryActiveColor,
                                inactiveTrackColor = containerBgColor
                            ),
                            modifier = Modifier.testTag("battery_level_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulate Charging (Plugged)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = subtitleColor
                                )
                            )
                            Checkbox(
                                checked = simIsCharging,
                                onCheckedChange = { charging -> viewModel.setBatteryOverride(true, simBatteryLvl, charging) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF10B981)
                                ),
                                modifier = Modifier.testTag("battery_charging_checkbox")
                            )
                        }
                    }
                } else {
                    // Live state summary when override is absent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected phone battery level: $realBatteryLevel%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = subtitleColor,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Text(
                            text = if (realIsCharging) "⚡ PLUGGED" else "🔋 UNPLUGGED",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (realIsCharging) Color(0xFF10B981) else subtitleColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// Inline helper extension for simple physical metal bezel gradients
private fun Color.darken(factor: Float): Color {
    val r = (this.red * (1 - factor)).coerceIn(0f, 1f)
    val g = (this.green * (1 - factor)).coerceIn(0f, 1f)
    val b = (this.blue * (1 - factor)).coerceIn(0f, 1f)
    return Color(red = r, green = g, blue = b, alpha = this.alpha)
}

// Reactive component following strict system battery change broad receiver rule
@Composable
fun rememberBatteryState(): BatteryInfo {
    val context = LocalContext.current
    var batteryInfo by remember { mutableStateOf(BatteryInfo(level = 90, isCharging = false)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 90
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                 status == BatteryManager.BATTERY_STATUS_FULL
                batteryInfo = BatteryInfo(level = batteryPct, isCharging = isCharging)
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        // Initial check immediately inside effect
        try {
            val lastBatteryIntent = context.registerReceiver(null, filter)
            if (lastBatteryIntent != null) {
                val level = lastBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = lastBatteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 90
                val status = lastBatteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                 status == BatteryManager.BATTERY_STATUS_FULL
                batteryInfo = BatteryInfo(level = batteryPct, isCharging = isCharging)
            }
        } catch (_: Exception) {}

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }
    return batteryInfo
}

data class BatteryInfo(val level: Int, val isCharging: Boolean)

// Stable wrapper class tracking animated weather particles
@Stable
class WeatherParticle(
    val id: Int,
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var size: Float,
    val alpha: Float
)

@Composable
fun rememberWeatherStateParticles(condition: WeatherCondition): Pair<List<WeatherParticle>, Long> {
    var tick by remember { mutableStateOf(0L) }
    
    val particles = remember(condition) {
        val count = when(condition) {
            WeatherCondition.SUNNY -> 0 // No falling elements for clear sky
            WeatherCondition.RAINY -> 45
            WeatherCondition.SNOWY -> 35
            WeatherCondition.STORMY -> 65
            WeatherCondition.OVERCAST -> 8 // Misty drifts
        }
        val list = mutableListOf<WeatherParticle>()
        for (i in 0 until count) {
            list.add(
                WeatherParticle(
                    id = i,
                    x = (0..1000).random().toFloat() / 1000f,
                    y = (0..1000).random().toFloat() / 1000f,
                    speedX = when(condition) {
                        WeatherCondition.RAINY -> -0.04f - (0..30).random().toFloat() / 1000f
                        WeatherCondition.SNOWY -> -0.02f + (0..40).random().toFloat() / 1000f
                        WeatherCondition.STORMY -> -0.12f - (0..80).random().toFloat() / 1000f
                        WeatherCondition.OVERCAST -> 0.02f + (0..40).random().toFloat() / 1000f
                        else -> 0f
                    },
                    speedY = when(condition) {
                        WeatherCondition.RAINY -> 0.45f + (0..350).random().toFloat() / 1000f
                        WeatherCondition.SNOWY -> 0.08f + (0..60).random().toFloat() / 1000f
                        WeatherCondition.STORMY -> 0.65f + (0..550).random().toFloat() / 1000f
                        WeatherCondition.OVERCAST -> 0.005f + (0..15).random().toFloat() / 1000f
                        else -> 0f
                    },
                    size = when(condition) {
                        WeatherCondition.RAINY -> 1.2f + (0..2).random().toFloat() / 2f
                        WeatherCondition.SNOWY -> 2.5f + (0..4).random().toFloat()
                        WeatherCondition.STORMY -> 1.5f + (0..3).random().toFloat() / 2f
                        WeatherCondition.OVERCAST -> 35f + (0..45).random().toFloat()
                        else -> 0f
                    },
                    alpha = when(condition) {
                        WeatherCondition.RAINY -> 0.25f + (0..350).random().toFloat() / 1000f
                        WeatherCondition.SNOWY -> 0.4f + (0..450).random().toFloat() / 1000f
                        WeatherCondition.STORMY -> 0.35f + (0..450).random().toFloat() / 1000f
                        WeatherCondition.OVERCAST -> 0.08f + (0..120).random().toFloat() / 1000f
                        else -> 0f
                    }
                )
            )
        }
        list
    }
    
    // Seed and animate particles in custom game tick loop
    LaunchedEffect(condition) {
        if (particles.isEmpty()) return@LaunchedEffect
        
        var lastTime = System.nanoTime()
        while(true) {
            withFrameNanos { frameTime ->
                val delta = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                
                for (p in particles) {
                    p.x += p.speedX * delta
                    p.y += p.speedY * delta
                    
                    // Wrapping borders smoothly
                    if (p.x < -0.1f) p.x = 1.1f
                    if (p.x > 1.1f) p.x = -0.1f
                    if (p.y > 1.05f) {
                        p.y = -0.05f
                        p.x = (0..1000).random().toFloat() / 1000f
                    }
                }
                tick++
            }
        }
    }
    
    return Pair(particles, tick)
}
