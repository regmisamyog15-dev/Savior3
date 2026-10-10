package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PiPAttemptRecord
import com.example.service.PiPDiagnosticManager
import com.example.service.PiPResult
import com.example.service.QuickCircleAccessibilityService
import com.example.service.QuickCircleNotificationListenerService
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
import kotlinx.coroutines.delay

@Composable
fun PiPDiagnosticScreen(
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val history by PiPDiagnosticManager.attemptHistory.collectAsState()
    val currentLog by PiPDiagnosticManager.currentTimelineLog.collectAsState()
    val isRunning by PiPDiagnosticManager.isTestRunning.collectAsState()
    val countdown by PiPDiagnosticManager.countdownSeconds.collectAsState()
    var isYtActive by remember { mutableStateOf(false) }
    var ytTitle by remember { mutableStateOf("No active MediaSession found") }
    var ytState by remember { mutableStateOf("NONE") }

    // Refresh telemetry every 1.5s
    LaunchedEffect(Unit) {
        while (true) {
            val controllers = QuickCircleNotificationListenerService.getActiveControllers(context)
            val yt = controllers.firstOrNull { it.packageName == "com.google.android.youtube" } ?: controllers.firstOrNull()
            isYtActive = yt?.packageName == "com.google.android.youtube"
            ytTitle = yt?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "No media detected"
            val stateCode = yt?.playbackState?.state ?: android.media.session.PlaybackState.STATE_NONE
            ytState = PiPDiagnosticManager.playbackStateToString(stateCode)
            delay(1500L)
        }
    }

    val totalAttempts = history.size
    val pipSuccessCount = history.count { it.pipObserved }
    val successRate = if (totalAttempts > 0) "${pipSuccessCount * 100 / totalAttempts}%" else "--"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Graphite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("btn_back_pip_diag")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Pearl
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Background Play Debug",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pearl
                    )
                    Text(
                        text = "Deterministic PiP Trigger Diagnostic",
                        fontSize = 12.sp,
                        color = Dim
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Score & Status Card
                item {
                    ScoreAndStatusBar(
                        totalAttempts = totalAttempts,
                        pipSuccessCount = pipSuccessCount,
                        successRate = successRate,
                        isYouTubeActive = isYtActive,
                        ytTitle = ytTitle,
                        ytState = ytState
                    )
                }

                // Test Controls
                item {
                    TestControlsCard(
                        context = context,
                        isRunning = isRunning,
                        countdown = countdown
                    )
                }

                // Live Timeline Log Box
                if (currentLog.isNotEmpty()) {
                    item {
                        TimelineLogBox(currentLog = currentLog)
                    }
                }

                // Attempt History Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATTEMPT HISTORY (${history.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Pearl,
                            letterSpacing = 1.sp
                        )
                        if (history.isNotEmpty()) {
                            TextButton(
                                onClick = { PiPDiagnosticManager.clearHistory() },
                                modifier = Modifier.testTag("btn_clear_pip_history")
                            ) {
                                Text("Clear", color = Dim, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (history.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Dim,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No diagnostic attempts recorded yet.",
                                    fontSize = 14.sp,
                                    color = Dim
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Play a video in YouTube, then tap 'TEST PIP (3s Countdown)' to test reproducible trigger.",
                                    fontSize = 12.sp,
                                    color = Dim,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    items(history) { record ->
                        AttemptCard(record = record)
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun ScoreAndStatusBar(
    totalAttempts: Int,
    pipSuccessCount: Int,
    successRate: String,
    isYouTubeActive: Boolean,
    ytTitle: String,
    ytState: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelElevated),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "REPRODUCIBILITY SCORE", fontSize = 11.sp, color = Dim, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (totalAttempts > 0) "$pipSuccessCount / $totalAttempts" else "0 / 0",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pipSuccessCount > 0) Sage else Pearl
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (totalAttempts > 0 && pipSuccessCount > 0) Sage.copy(alpha = 0.15f) else Line)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Success: $successRate",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalAttempts > 0 && pipSuccessCount > 0) Sage else Pearl
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Line)
            Spacer(modifier = Modifier.height(12.dp))

            // YouTube Telemetry line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isYouTubeActive) Sage else RoseError)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isYouTubeActive) "YouTube Session Detected" else "No YouTube Session",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Pearl
                    )
                }
                Text(
                    text = ytState,
                    fontSize = 12.sp,
                    color = Brass,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isYouTubeActive) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ytTitle,
                    fontSize = 12.sp,
                    color = Dim,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TestControlsCard(
    context: Context,
    isRunning: Boolean,
    countdown: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "EXECUTE DIAGNOSTIC TRIGGER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Dim,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Test PiP entry under identical conditions. Tap button below, immediately switch to the active YouTube video, and wait for Home dispatch.",
                fontSize = 12.sp,
                color = Pearl,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isRunning && countdown > 0) {
                // Countdown active
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brass.copy(alpha = 0.15f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Brass,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SWITCH TO YOUTUBE NOW: $countdown s",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Brass
                    )
                }
            } else if (isRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Line)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Pearl,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyzing Windows & Session (+3.0s)...",
                        fontSize = 13.sp,
                        color = Pearl
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            PiPDiagnosticManager.startTestWithCountdown(context)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_pip_countdown"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Brass,
                            contentColor = Graphite
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TEST PIP (3s)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            PiPDiagnosticManager.runDiagnosticAttempt(
                                context = context,
                                triggerSource = "IN_APP_INSTANT_TEST",
                                settleDelayBeforeHomeMs = 50L
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_pip_instant"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Pearl)
                    ) {
                        Text("Instant Home", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineLogBox(currentLog: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelElevated),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "LIVE TIMELINE LOG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Dim,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Graphite)
                    .padding(10.dp)
            ) {
                Text(
                    text = currentLog,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Sage,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AttemptCard(record: PiPAttemptRecord) {
    var expanded by remember { mutableStateOf(false) }

    val verdictColor = when (record.finalResult) {
        PiPResult.PIP_OBSERVED -> Sage
        PiPResult.NO_PIP_OBSERVED -> RoseError
        PiPResult.UNKNOWN -> Brass
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("pip_attempt_${record.attemptNumber}"),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Attempt #${record.attemptNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Pearl
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.timestamp,
                        fontSize = 12.sp,
                        color = Dim
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(verdictColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.finalResult.displayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = verdictColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Pre-Home State", fontSize = 10.sp, color = Dim)
                    Text(text = record.youtubeStateBefore, fontSize = 12.sp, color = Pearl, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text(text = "Home Dispatched", fontSize = 10.sp, color = Dim)
                    Text(
                        text = if (record.homeDispatched) "SUCCESS" else "FAILED",
                        fontSize = 12.sp,
                        color = if (record.homeDispatched) Sage else RoseError,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(text = "PiP Observed", fontSize = 10.sp, color = Dim)
                    Text(
                        text = if (record.pipObserved) "YES" else "NO",
                        fontSize = 12.sp,
                        color = if (record.pipObserved) Sage else RoseError,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(text = "Elapsed", fontSize = 10.sp, color = Dim)
                    Text(text = "${record.timingMs}ms", fontSize = 12.sp, color = Pearl)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Line)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Raw Timeline:",
                    fontSize = 10.sp,
                    color = Dim,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Graphite)
                        .padding(8.dp)
                ) {
                    Text(
                        text = record.rawTimeline,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Pearl,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
