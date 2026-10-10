package com.example.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.QuickCirclePreferences
import com.example.livess.LiveSsPermissionActivity
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

@Composable
fun ScreenRecorderDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { QuickCirclePreferences.getInstance(context) }

    val quality by prefs.recorderQuality.collectAsState()
    val audioSource by prefs.recorderAudioSource.collectAsState()
    val cleanCapture by prefs.recorderCleanCapture.collectAsState()
    val controlsMode by prefs.recorderControlsMode.collectAsState()

    var showMicPermissionNeeded by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LiveSsPermissionActivity.launchForRecording(context)
            onDismissRequest()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice audio", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PanelElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, Brass.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header: Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RoseError.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_screen_recorder),
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Screen Recorder",
                                color = Pearl,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High-Quality Clean Capture",
                                color = Dim,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Dim)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Video Quality Selection
                Text(
                    text = "VIDEO QUALITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brass,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoQualityPreset.values().forEach { preset ->
                        val selected = quality == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Brass else Panel)
                                .border(1.dp, if (selected) Brass else LineBorder, RoundedCornerShape(10.dp))
                                .clickable { prefs.setRecorderQuality(preset) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = preset.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Graphite else Pearl
                                )
                                Text(
                                    text = if (preset == VideoQualityPreset.MAXIMUM) "Native" else "${preset.targetFps} FPS",
                                    fontSize = 10.sp,
                                    color = if (selected) Graphite.copy(alpha = 0.8f) else Dim
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Line)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. Audio Source Selection
                Text(
                    text = "AUDIO SOURCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brass,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AudioRecordingSource.values().forEach { src ->
                        val selected = audioSource == src
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Sage else Panel)
                                .border(1.dp, if (selected) Sage else LineBorder, RoundedCornerShape(8.dp))
                                .clickable { prefs.setRecorderAudioSource(src) }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = src.label.replace("Audio", "").trim(),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Graphite else Pearl,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Line)
                Spacer(modifier = Modifier.height(12.dp))

                // 3. Clean Recording Switch (Default ON)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clean Capture",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Pearl
                        )
                        Text(
                            text = "Savior guarantees that its own overlays are removed before capture begins and are not intentionally included in the capture.",
                            fontSize = 11.sp,
                            color = Sage
                        )
                    }
                    Switch(
                        checked = cleanCapture,
                        onCheckedChange = { prefs.setRecorderCleanCapture(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Graphite,
                            checkedTrackColor = Brass,
                            uncheckedThumbColor = Dim,
                            uncheckedTrackColor = Line
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Recording Controls Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Floating Controls Pill",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Pearl
                        )
                        Text(
                            text = if (controlsMode == RecordingControlsMode.FLOATING_AND_NOTIFICATION) "Draggable pill on screen" else "Status notification only (cleanest)",
                            fontSize = 11.sp,
                            color = Dim
                        )
                    }
                    Switch(
                        checked = controlsMode == RecordingControlsMode.FLOATING_AND_NOTIFICATION,
                        onCheckedChange = { checked ->
                            prefs.setRecorderControlsMode(
                                if (checked) RecordingControlsMode.FLOATING_AND_NOTIFICATION
                                else RecordingControlsMode.NOTIFICATION_ONLY
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Graphite,
                            checkedTrackColor = Brass,
                            uncheckedThumbColor = Dim,
                            uncheckedTrackColor = Line
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Start Recording Button
                Button(
                    onClick = {
                        val requiresMic = audioSource == AudioRecordingSource.MIC ||
                                audioSource == AudioRecordingSource.DEVICE_AND_MIC
                        if (requiresMic && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            LiveSsPermissionActivity.launchForRecording(context)
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_start_screen_recording"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError, contentColor = Pearl),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_action_record_dot), contentDescription = null, tint = Pearl, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Recording", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
