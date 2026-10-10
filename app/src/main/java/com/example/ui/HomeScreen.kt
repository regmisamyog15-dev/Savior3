package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.browser.BrowserMediaState
import com.example.browser.BrowserRecentStore
import com.example.browser.RecentItem
import com.example.browser.SaviorBrowserActivity
import com.example.browser.SaviorMediaManager
import com.example.data.QuickCirclePreferences
import com.example.model.QuickActionType
import com.example.recorder.ScreenRecorderDialog
import com.example.recorder.ScreenRecorderService
import com.example.recorder.VideoEditorActivity
import com.example.livess.ScreenshotEditorActivity
import com.example.service.QuickCircleAccessibilityService
import com.example.ui.theme.Brass
import com.example.ui.theme.Dim
import com.example.ui.theme.Graphite
import com.example.ui.theme.Line
import com.example.ui.theme.LineBorder
import com.example.ui.theme.Panel
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.Pearl
import com.example.ui.theme.RoseError
import com.example.ui.theme.Sage
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToRecent: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val preferences = QuickCirclePreferences.getInstance(context)
    val recentStore = BrowserRecentStore.getInstance(context)

    val isOrbEnabled by preferences.isEnabled.collectAsState()
    val mediaState by SaviorMediaManager.mediaState.collectAsState()
    val mediaTitle by SaviorMediaManager.mediaTitle.collectAsState()
    val recentItems by recentStore.recentItems.collectAsState()
    var showRecorderDialog by remember { mutableStateOf(false) }

    if (showRecorderDialog) {
        ScreenRecorderDialog(onDismissRequest = { showRecorderDialog = false })
    }

    val greeting = rememberGreeting()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(6.dp)) }

        // Top Greeting Header
        item {
            Column {
                Text(
                    text = "SAVIOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brass,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = greeting,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pearl
                )
            }
        }

        // Search Bar Trigger
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Panel)
                    .border(1.dp, LineBorder, RoundedCornerShape(24.dp))
                    .clickable { onNavigateToSearch() }
                    .padding(horizontal = 16.dp)
                    .testTag("home_search_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Dim,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search Savior tools & shortcuts...",
                    color = Dim,
                    fontSize = 14.sp
                )
            }
        }

        item {
            Text(
                text = "UTILITY SUITE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Dim,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Feature 1: Background Play Card (Primary Highlight)
        item {
            val isPlaying = mediaState == BrowserMediaState.PLAYING
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SaviorBrowserActivity.launch(context)
                        recentStore.addToolUsage("Background Play", "Opened Savior Browser", "background_play")
                    }
                    .testTag("card_background_play"),
                colors = CardDefaults.cardColors(containerColor = PanelElevated),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isPlaying) Sage else Brass))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Sage.copy(alpha = 0.2f) else Brass.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_background_play),
                                    contentDescription = null,
                                    tint = if (isPlaying) Sage else Brass,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Background Play",
                                    color = Pearl,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isPlaying) "Active in background • $mediaTitle" else "Play YouTube & web media while multitasking",
                                    color = if (isPlaying) Sage else Dim,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Launch",
                            tint = if (isPlaying) Sage else Brass,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Sage else Dim)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Savior Browser Engine • ${mediaState.label}",
                                fontSize = 11.sp,
                                color = Dim
                            )
                        }

                        Text(
                            text = "Open Browser →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brass
                        )
                    }
                }
            }
        }

        // Feature 2: Software Volume & Assistive Controls (Primary Tool)
        item {
            val audioController = remember { com.example.service.AppSystemActionController(context) }
            val currentStream by preferences.defaultStreamType.collectAsState()

            var volumeInfo by remember(currentStream) {
                mutableStateOf(audioController.getStreamVolumeInfo(currentStream.streamId))
            }
            val (currentVol, maxVol, isMuted) = volumeInfo
            val volumePercent = if (maxVol > 0) ((currentVol.toFloat() / maxVol) * 100).toInt() else 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_orb_volume"),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header Row: Icon, Title, Live Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Brass.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isMuted || currentVol == 0) R.drawable.ic_action_volume_mute
                                        else R.drawable.ic_action_volume_up
                                    ),
                                    contentDescription = "Volume Icon",
                                    tint = if (isMuted) RoseError else Brass,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Volume & Assistive Controls",
                                    color = Pearl,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isMuted) "${currentStream.title} • Muted" else "${currentStream.title} • $volumePercent% ($currentVol/$maxVol)",
                                    color = if (isMuted) RoseError else Sage,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Orb Toggle Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOrbEnabled) "Orb On" else "Orb Off",
                                color = Dim,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Switch(
                                checked = isOrbEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && !Settings.canDrawOverlays(context)) {
                                        Toast.makeText(context, "Please allow overlay permission to display the floating orb", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        context.startActivity(intent)
                                    } else {
                                        preferences.setEnabled(enable)
                                        com.example.service.FloatingBubbleHost.syncState(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Graphite,
                                    checkedTrackColor = Brass,
                                    uncheckedThumbColor = Dim,
                                    uncheckedTrackColor = Line
                                ),
                                modifier = Modifier.testTag("switch_orb_home")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stream Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        com.example.model.StreamType.values().forEach { st ->
                            val selected = currentStream == st
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Brass else PanelElevated)
                                    .clickable {
                                        preferences.setDefaultStreamType(st)
                                        volumeInfo = audioController.getStreamVolumeInfo(st.streamId)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st.title,
                                    color = if (selected) Graphite else Pearl,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Volume Tactile Buttons Row: [-] [===Slider===] [+] [Mute]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Down Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PanelElevated)
                                .border(1.dp, LineBorder, CircleShape)
                                .clickable {
                                    val target = (currentVol - 1).coerceAtLeast(0)
                                    audioController.setStreamVolume(currentStream.streamId, target, playSound = true) { c, m, mu ->
                                        volumeInfo = Triple(c, m, mu)
                                    }
                                    recentStore.addToolUsage("Volume Down", "${currentStream.title} lowered to $target", "volume_orb")
                                }
                                .testTag("home_btn_volume_down"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_volume_down),
                                contentDescription = "Volume Down",
                                tint = Pearl,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Interactive Volume Slider
                        Slider(
                            value = if (maxVol > 0) currentVol.toFloat() / maxVol.toFloat() else 0f,
                            onValueChange = { frac ->
                                val target = (frac * maxVol).toInt()
                                audioController.setStreamVolume(currentStream.streamId, target, playSound = false) { c, m, mu ->
                                    volumeInfo = Triple(c, m, mu)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_slider_volume"),
                            colors = SliderDefaults.colors(
                                thumbColor = Brass,
                                activeTrackColor = Brass,
                                inactiveTrackColor = Line
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Volume Up Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PanelElevated)
                                .border(1.dp, LineBorder, CircleShape)
                                .clickable {
                                    val target = (currentVol + 1).coerceAtMost(maxVol)
                                    audioController.setStreamVolume(currentStream.streamId, target, playSound = true) { c, m, mu ->
                                        volumeInfo = Triple(c, m, mu)
                                    }
                                    recentStore.addToolUsage("Volume Up", "${currentStream.title} raised to $target", "volume_orb")
                                }
                                .testTag("home_btn_volume_up"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_volume_up),
                                contentDescription = "Volume Up",
                                tint = Brass,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Mute Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) RoseError.copy(alpha = 0.2f) else PanelElevated)
                                .border(1.dp, if (isMuted) RoseError else LineBorder, CircleShape)
                                .clickable {
                                    audioController.toggleStreamMute(currentStream.streamId) { c, m, mu ->
                                        volumeInfo = Triple(c, m, mu)
                                    }
                                    recentStore.addToolUsage("Volume Mute", "${currentStream.title} mute toggled", "volume_orb")
                                }
                                .testTag("home_btn_volume_mute"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_volume_mute),
                                contentDescription = "Toggle Mute",
                                tint = if (isMuted) RoseError else Dim,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Action Strip: Test Overlay HUD and Configure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test On-Screen HUD →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brass,
                            modifier = Modifier
                                .clickable {
                                    if (!Settings.canDrawOverlays(context)) {
                                        Toast.makeText(context, "Grant overlay permission to show the on-screen volume panel", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        context.startActivity(intent)
                                    } else {
                                        com.example.service.FloatingBubbleHost.showVolumeHud(context)
                                    }
                                }
                                .testTag("home_btn_show_hud")
                        )

                        Text(
                            text = "Orb Settings →",
                            fontSize = 12.sp,
                            color = Dim,
                            modifier = Modifier.clickable { onNavigateToSettings() }
                        )
                    }
                }
            }
        }

        // Feature 3: Live Screenshot (Privacy-first on-demand capture)
        item {
            val liveSsRepo = com.example.livess.LiveSsRepository.getInstance(context)
            val liveSsItems by liveSsRepo.items.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_live_ss"),
                colors = CardDefaults.cardColors(containerColor = PanelElevated),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Brass))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                com.example.livess.LiveSsPermissionActivity.launchForScreenshot(context)
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Brass.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_screenshot),
                                    contentDescription = "Live Screenshot",
                                    tint = Brass,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Live Screenshot",
                                    color = Pearl,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Privacy-first • Clean capture with preview pill",
                                    color = Dim,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brass)
                                .clickable {
                                    com.example.livess.LiveSsPermissionActivity.launchForScreenshot(context)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("btn_capture_live_ss")
                        ) {
                            Text(
                                text = "Capture",
                                color = Graphite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Line)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("✨ 100% Clean • Overlays Hidden", color = Pearl, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${liveSsItems.size} captures", color = Dim, fontSize = 11.sp)
                        }

                        Text(
                            text = "View Gallery →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brass,
                            modifier = Modifier.clickable { onNavigateToRecent() }
                        )
                    }
                }
            }
        }

        // Feature 4: Instant Screenshot
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val a11y = QuickCircleAccessibilityService.instance
                        if (a11y != null) {
                            a11y.performAction(QuickActionType.SCREENSHOT)
                            recentStore.addToolUsage("Screenshot", "Screen capture triggered", "screenshot")
                            Toast.makeText(context, "Instant screenshot captured", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enable Savior in Accessibility Settings for instant screenshots", Toast.LENGTH_LONG).show()
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                    .testTag("card_screenshot"),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Line),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_screenshot),
                                contentDescription = null,
                                tint = Pearl,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Instant Screenshot",
                                color = Pearl,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "One-tap hardware-free screen capture",
                                color = Dim,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Line)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Capture", color = Pearl, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Tools Row: Screen Recorder & Savior Editor
        item {
            val isRecorderRunning = ScreenRecorderService.isRunning
            val liveSsRepo = com.example.livess.LiveSsRepository.getInstance(context)
            val liveSsItems by liveSsRepo.items.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Screen Recorder (Active Feature)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isRecorderRunning) {
                                val stopIntent = Intent(context, ScreenRecorderService::class.java).apply {
                                    action = ScreenRecorderService.ACTION_STOP
                                }
                                context.startService(stopIntent)
                            } else {
                                showRecorderDialog = true
                            }
                        }
                        .testTag("card_recorder"),
                    colors = CardDefaults.cardColors(containerColor = PanelElevated),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isRecorderRunning) RoseError else Brass)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecorderRunning) RoseError.copy(alpha = 0.2f) else Brass.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_screen_recorder),
                                    contentDescription = "Screen Recorder",
                                    tint = if (isRecorderRunning) RoseError else Brass,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isRecorderRunning) RoseError else Brass)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isRecorderRunning) "STOP 🔴" else "RECORD",
                                    fontSize = 9.sp,
                                    color = Graphite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Screen Recorder", color = Pearl, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isRecorderRunning) "Tap to finish recording" else "1080p / 60 FPS where supported",
                            color = if (isRecorderRunning) RoseError else Dim,
                            fontSize = 11.sp
                        )
                    }
                }

                // Savior Editor (Active Feature)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val latestRec = liveSsItems.firstOrNull { it.isVideo && it.videoUri != null }
                            val latestStill = liveSsItems.firstOrNull { !it.isVideo }
                            when {
                                latestRec != null -> VideoEditorActivity.launch(context, latestRec.videoUri!!)
                                latestStill != null -> ScreenshotEditorActivity.launch(context, latestStill.stillUri)
                                else -> onNavigateToRecent()
                            }
                        }
                        .testTag("card_editor"),
                    colors = CardDefaults.cardColors(containerColor = PanelElevated),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Line),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_action_edit),
                                    contentDescription = "Editor",
                                    tint = Pearl,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Line)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EDITOR", fontSize = 9.sp, color = Dim, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Savior Editor", color = Pearl, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Trim video • Markup image", color = Dim, fontSize = 11.sp)
                    }
                }
            }
        }

        // Recent Activity Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Dim,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "View All →",
                    fontSize = 12.sp,
                    color = Brass,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToRecent() }
                )
            }
        }

        if (recentItems.isEmpty()) {
            item {
                Text(
                    text = "No recent activity yet.",
                    fontSize = 13.sp,
                    color = Dim,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(recentItems.take(3)) { item ->
                RecentRow(item = item, onClick = {
                    if (item.data.startsWith("http")) {
                        SaviorBrowserActivity.launch(context, item.data)
                    }
                })
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun RecentRow(item: RecentItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Line),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (item.data.startsWith("http")) R.drawable.ic_action_background_play else R.drawable.ic_action_home
                ),
                contentDescription = null,
                tint = Pearl,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, color = Pearl, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(text = item.subtitle, color = Dim, fontSize = 11.sp, maxLines = 1)
        }
    }
}

private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}
