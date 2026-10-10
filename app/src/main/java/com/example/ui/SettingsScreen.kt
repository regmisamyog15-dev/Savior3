package com.example.ui

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.browser.BrowserMediaState
import com.example.browser.BrowserRecentStore
import com.example.browser.SaviorMediaManager
import com.example.data.QuickCirclePreferences
import com.example.model.DockEdge
import com.example.model.StreamType
import com.example.service.QuickCircleAccessibilityService
import com.example.ui.theme.Brass
import com.example.ui.theme.Dim
import com.example.ui.theme.Graphite
import com.example.ui.theme.Line
import com.example.ui.theme.LineBorder
import com.example.ui.theme.Panel
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.Pearl
import com.example.overlay.CapturePrivacyState
import com.example.overlay.OverlayStateManager
import com.example.ui.theme.RoseError
import com.example.ui.theme.Sage

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferences = QuickCirclePreferences.getInstance(context)
    val recentStore = BrowserRecentStore.getInstance(context)

    // Orb States
    val isOrbEnabled by preferences.isEnabled.collectAsState()
    val isPermanent by preferences.isPermanent.collectAsState()
    val bubbleSizeDp by preferences.bubbleSizeDp.collectAsState()
    val bubbleOpacity by preferences.bubbleOpacity.collectAsState()
    val dockEdge by preferences.dockEdge.collectAsState()

    // Screen Capture Privacy State
    val capturePrivacyState by OverlayStateManager.privacyState.collectAsState()

    // Background Play States
    val autoPipOnHome by preferences.autoPipOnHome.collectAsState()
    val keepAudioActive by preferences.keepAudioActiveInBackground.collectAsState()
    val mediaNotification by preferences.mediaNotificationEnabled.collectAsState()

    // Software Volume States
    val isSoftwareVolumeEnabled by preferences.isSoftwareVolumeReplacementEnabled.collectAsState()
    val defaultStreamType by preferences.defaultStreamType.collectAsState()

    // Real-time Runtime Diagnostics
    val mediaState by SaviorMediaManager.mediaState.collectAsState()
    val mediaTitle by SaviorMediaManager.mediaTitle.collectAsState()
    val sessionActive by SaviorMediaManager.sessionActive.collectAsState()
    val isPiPActive by SaviorMediaManager.isPiPActive.collectAsState()
    val audioFocus by SaviorMediaManager.audioFocusState.collectAsState()

    var showDiagnosticsExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            Column {
                Text(
                    text = "SETTINGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brass,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Configuration & Diagnostics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pearl
                )
            }
        }

        // Section 1: Background Play Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_bg_play_card"),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_action_background_play), contentDescription = null, tint = Brass, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Background Play (Savior Browser)", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "Auto Picture-in-Picture on Home",
                        subtitle = "Enter native PiP window when leaving browser during video playback",
                        checked = autoPipOnHome,
                        onCheckedChange = { preferences.setAutoPipOnHome(it) },
                        tag = "toggle_auto_pip"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Maintain Audio in Background",
                        subtitle = "Keep audio track running when switching apps or locking screen",
                        checked = keepAudioActive,
                        onCheckedChange = { preferences.setKeepAudioActiveInBackground(it) },
                        tag = "toggle_keep_audio"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Media Notification Controls",
                        subtitle = "Expose play/pause & skip controls in notification and lock screen",
                        checked = mediaNotification,
                        onCheckedChange = { preferences.setMediaNotificationEnabled(it) },
                        tag = "toggle_media_notification"
                    )
                }
            }
        }

        // Section 2: Floating Orb Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_orb_card"),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_action_quick_settings), contentDescription = null, tint = Pearl, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Floating Orb Appearance", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "Enable Floating Orb",
                        subtitle = "Persistent assistive controller on screen edge",
                        checked = isOrbEnabled,
                        onCheckedChange = { preferences.setEnabled(it) },
                        tag = "toggle_orb_enable"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Size Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Orb Size", color = Pearl, fontSize = 13.sp)
                            Text("${bubbleSizeDp} dp", color = Brass, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Slider(
                            value = bubbleSizeDp.toFloat(),
                            onValueChange = { preferences.setBubbleSizeDp(it.toInt()) },
                            valueRange = 36f..72f,
                            colors = SliderDefaults.colors(thumbColor = Brass, activeTrackColor = Brass, inactiveTrackColor = Line),
                            modifier = Modifier.testTag("slider_orb_size")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opacity Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Orb Opacity", color = Pearl, fontSize = 13.sp)
                            Text("${(bubbleOpacity * 100).toInt()}%", color = Brass, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Slider(
                            value = bubbleOpacity,
                            onValueChange = { preferences.setBubbleOpacity(it) },
                            valueRange = 0.3f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = Brass, activeTrackColor = Brass, inactiveTrackColor = Line),
                            modifier = Modifier.testTag("slider_orb_opacity")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dock Edge
                    Text("Dock Edge Snapping", color = Pearl, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DockEdge.values().forEach { edge ->
                            val selected = dockEdge == edge
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Brass else PanelElevated)
                                    .clickable { preferences.setDockEdge(edge) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = edge.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = if (selected) Graphite else Pearl,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Software Volume Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_volume_card"),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_action_volume_up), contentDescription = null, tint = Pearl, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Software Volume Replacement", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "Software Controls",
                        subtitle = "Hardware-free volume adjustment for worn rocker keys",
                        checked = isSoftwareVolumeEnabled,
                        onCheckedChange = { preferences.setSoftwareVolumeReplacementEnabled(it) },
                        tag = "toggle_software_volume"
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Default Audio Stream", color = Pearl, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StreamType.values().forEach { st ->
                            val selected = defaultStreamType == st
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Brass else PanelElevated)
                                    .clickable { preferences.setDefaultStreamType(st) }
                                    .padding(vertical = 8.dp),
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
                    HorizontalDivider(color = Line)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Hardware-Free Volume Tester
                    val audioController = remember { com.example.service.AppSystemActionController(context) }
                    var testVolumeInfo by remember(defaultStreamType) {
                        mutableStateOf(audioController.getStreamVolumeInfo(defaultStreamType.streamId))
                    }
                    val (curVol, maxVol, muted) = testVolumeInfo

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Interactive Volume Controls", color = Pearl, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (muted) "Muted" else "$curVol / $maxVol",
                            color = if (muted) com.example.ui.theme.RoseError else Brass,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Down Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PanelElevated)
                                .border(1.dp, LineBorder, CircleShape)
                                .clickable {
                                    val target = (curVol - 1).coerceAtLeast(0)
                                    audioController.setStreamVolume(defaultStreamType.streamId, target, playSound = true) { c, m, mu ->
                                        testVolumeInfo = Triple(c, m, mu)
                                    }
                                }
                                .testTag("settings_btn_volume_down"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_action_volume_down), contentDescription = "Down", tint = Pearl, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Slider(
                            value = if (maxVol > 0) curVol.toFloat() / maxVol.toFloat() else 0f,
                            onValueChange = { frac ->
                                val target = (frac * maxVol).toInt()
                                audioController.setStreamVolume(defaultStreamType.streamId, target, playSound = false) { c, m, mu ->
                                    testVolumeInfo = Triple(c, m, mu)
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("settings_slider_volume"),
                            colors = SliderDefaults.colors(thumbColor = Brass, activeTrackColor = Brass, inactiveTrackColor = Line)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Volume Up Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PanelElevated)
                                .border(1.dp, LineBorder, CircleShape)
                                .clickable {
                                    val target = (curVol + 1).coerceAtMost(maxVol)
                                    audioController.setStreamVolume(defaultStreamType.streamId, target, playSound = true) { c, m, mu ->
                                        testVolumeInfo = Triple(c, m, mu)
                                    }
                                }
                                .testTag("settings_btn_volume_up"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_action_volume_up), contentDescription = "Up", tint = Brass, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Mute Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (muted) com.example.ui.theme.RoseError.copy(alpha = 0.2f) else PanelElevated)
                                .border(1.dp, if (muted) com.example.ui.theme.RoseError else LineBorder, CircleShape)
                                .clickable {
                                    audioController.toggleStreamMute(defaultStreamType.streamId) { c, m, mu ->
                                        testVolumeInfo = Triple(c, m, mu)
                                    }
                                }
                                .testTag("settings_btn_volume_mute"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_action_volume_mute), contentDescription = "Mute", tint = if (muted) com.example.ui.theme.RoseError else Dim, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Test Floating Volume HUD →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brass,
                            modifier = Modifier.clickable {
                                com.example.service.FloatingBubbleHost.showVolumeHud(context)
                            }
                        )
                    }
                }
            }
        }

        // Section 4: Real Runtime Diagnostics (IMPORTANT DIAGNOSTIC UI)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDiagnosticsExpanded = !showDiagnosticsExpanded }
                    .testTag("settings_diagnostics_card"),
                colors = CardDefaults.cardColors(containerColor = PanelElevated),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Brass, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Background Play Diagnostics", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "Real runtime state verification", color = Dim, fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = if (showDiagnosticsExpanded) "Hide ▲" else "View ▼",
                            color = Brass,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showDiagnosticsExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Line)
                        Spacer(modifier = Modifier.height(12.dp))

                        DiagRow("Browser Engine", "READY (Android WebView)")
                        DiagRow("Media Playback", mediaState.name)
                        DiagRow("MediaSession", if (sessionActive) "ACTIVE" else "INACTIVE")
                        DiagRow("Picture-in-Picture", if (isPiPActive) "ACTIVE" else "AVAILABLE (API 26+)")
                        DiagRow("Audio Focus", audioFocus)
                        DiagRow("Active Media", mediaTitle)
                    }
                }
            }
        }

        // Section 5: Screen Capture Privacy Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_capture_privacy_card"),
                colors = CardDefaults.cardColors(containerColor = PanelElevated),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        when (capturePrivacyState) {
                            CapturePrivacyState.RECORDING_ACTIVE -> RoseError
                            CapturePrivacyState.CAPTURE_ENDED -> Sage
                            CapturePrivacyState.NOT_ACTIVE -> Brass
                        }
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Screen Capture Privacy",
                            color = Pearl,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (capturePrivacyState) {
                                        CapturePrivacyState.RECORDING_ACTIVE -> RoseError.copy(alpha = 0.2f)
                                        CapturePrivacyState.CAPTURE_ENDED -> Sage.copy(alpha = 0.2f)
                                        CapturePrivacyState.NOT_ACTIVE -> Line
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (capturePrivacyState) {
                                    CapturePrivacyState.RECORDING_ACTIVE -> "ACTIVE 🔴"
                                    CapturePrivacyState.CAPTURE_ENDED -> "ENDED ✓"
                                    CapturePrivacyState.NOT_ACTIVE -> "NOT ACTIVE"
                                },
                                color = when (capturePrivacyState) {
                                    CapturePrivacyState.RECORDING_ACTIVE -> RoseError
                                    CapturePrivacyState.CAPTURE_ENDED -> Sage
                                    CapturePrivacyState.NOT_ACTIVE -> Dim
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (capturePrivacyState) {
                        CapturePrivacyState.NOT_ACTIVE -> {
                            Text(
                                text = "● Not active",
                                color = Sage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Savior isn't accessing your screen. When you take a screenshot or start recording, Android will ask for screen-capture permission.",
                                color = Dim,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                        CapturePrivacyState.RECORDING_ACTIVE -> {
                            Text(
                                text = "🔴 Screen capture active",
                                color = RoseError,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Recording in progress. Savior overlays are hidden so they do not contaminate your capture.",
                                color = Dim,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                        CapturePrivacyState.CAPTURE_ENDED -> {
                            Text(
                                text = "✓ Screen capture ended",
                                color = Sage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Temporary session terminated and destroyed. Savior has returned to idle.",
                                color = Dim,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 6: Privacy & Local Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_privacy_card"),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Privacy & Local Data", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Savior operates 100% on-device. No telemetry, no cloud sync, no tracking.", color = Dim, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                CookieManager.getInstance().removeAllCookies(null)
                                WebStorage.getInstance().deleteAllData()
                                Toast.makeText(context, "Browser cache cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("btn_clear_cookies"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear Browser Data", fontSize = 12.sp, color = Pearl)
                        }

                        OutlinedButton(
                            onClick = {
                                recentStore.clearAll()
                                Toast.makeText(context, "Recent history cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("btn_clear_history"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Clear Recent History", fontSize = 12.sp, color = Pearl)
                        }
                    }
                }
            }
        }

        // Section 6: About Savior
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(18.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "About Savior", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Version 1.0 • Android Utility Suite", color = Brass, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Minimal assistive controller with software volume, screenshot capture, Picture-in-Picture, and legitimate HTML5 background playback.", color = Dim, fontSize = 12.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Pearl, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Dim, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Graphite,
                checkedTrackColor = Brass,
                uncheckedThumbColor = Dim,
                uncheckedTrackColor = Line
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Dim, fontSize = 12.sp)
        Text(text = value, color = Pearl, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
