package com.example.browser

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
fun MediaControlPanel(
    onDismiss: () -> Unit,
    onEnterPiP: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mediaState by SaviorMediaManager.mediaState.collectAsState()
    val mediaTitle by SaviorMediaManager.mediaTitle.collectAsState()
    val mediaSource by SaviorMediaManager.mediaSource.collectAsState()
    val isPlaying = mediaState == BrowserMediaState.PLAYING
    val isMediaAvailable = mediaState != BrowserMediaState.NONE

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var volume by remember {
        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        mutableFloatStateOf(cur.toFloat() / max.coerceAtLeast(1).toFloat())
    }
    var showDiagnostics by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_control_panel"),
        colors = CardDefaults.cardColors(containerColor = PanelElevated),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Bar
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
                            .background(
                                when (mediaState) {
                                    BrowserMediaState.PLAYING -> Sage
                                    BrowserMediaState.BUFFERING -> Brass
                                    BrowserMediaState.PAUSED -> Dim
                                    else -> RoseError
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Background Play Controls",
                        color = Pearl,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showDiagnostics = !showDiagnostics },
                        modifier = Modifier.size(32.dp).testTag("btn_media_diag")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnostics",
                            tint = if (showDiagnostics) Brass else Dim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("btn_close_panel")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Dim,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Media Info Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Panel)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPlaying) Sage.copy(alpha = 0.2f) else Line),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_background_play),
                        contentDescription = null,
                        tint = if (isPlaying) Sage else Dim,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaTitle,
                        color = Pearl,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$mediaSource • ${mediaState.label}",
                        color = Dim,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Playback Controls: ⏮ ▶/⏸ ⏭
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                IconButton(
                    onClick = { SaviorMediaManager.onPreviousRequested?.invoke() },
                    enabled = isMediaAvailable,
                    modifier = Modifier.size(48.dp).testTag("btn_media_prev")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_recents),
                        contentDescription = "Previous",
                        tint = if (isMediaAvailable) Pearl else Dim.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Play / Pause Button
                FilledIconButton(
                    onClick = {
                        if (isPlaying) {
                            SaviorMediaManager.onPauseRequested?.invoke()
                        } else {
                            SaviorMediaManager.onPlayRequested?.invoke()
                        }
                    },
                    enabled = isMediaAvailable,
                    modifier = Modifier.size(60.dp).testTag("btn_media_play_pause"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Brass,
                        contentColor = Graphite,
                        disabledContainerColor = Line,
                        disabledContentColor = Dim
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Next Button
                IconButton(
                    onClick = { SaviorMediaManager.onNextRequested?.invoke() },
                    enabled = isMediaAvailable,
                    modifier = Modifier.size(48.dp).testTag("btn_media_next")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_recents),
                        contentDescription = "Next",
                        tint = if (isMediaAvailable) Pearl else Dim.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PiP Button & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onEnterPiP,
                    modifier = Modifier.weight(1f).height(44.dp).testTag("btn_enter_pip"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMediaAvailable) Sage else Line)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_split_screen),
                        contentDescription = null,
                        tint = if (isMediaAvailable) Sage else Dim,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter Picture-in-Picture",
                        color = if (isMediaAvailable) Sage else Dim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Control Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
                        val target = (cur - 1).coerceAtLeast(0)
                        audioManager?.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            target,
                            AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
                        )
                        volume = target.toFloat() / max.coerceAtLeast(1).toFloat()
                    },
                    modifier = Modifier.size(36.dp).testTag("btn_panel_volume_down")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_volume_down),
                        contentDescription = "Volume Down",
                        tint = Pearl,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Slider(
                    value = volume,
                    onValueChange = { newVal ->
                        volume = newVal
                        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                        val targetVol = (newVal * max).toInt()
                        audioManager?.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            targetVol,
                            AudioManager.FLAG_SHOW_UI
                        )
                    },
                    modifier = Modifier.weight(1f).testTag("slider_panel_volume"),
                    colors = SliderDefaults.colors(
                        thumbColor = Brass,
                        activeTrackColor = Brass,
                        inactiveTrackColor = Line
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
                        val cur = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
                        val target = (cur + 1).coerceAtMost(max)
                        audioManager?.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            target,
                            AudioManager.FLAG_SHOW_UI or AudioManager.FLAG_PLAY_SOUND
                        )
                        volume = target.toFloat() / max.coerceAtLeast(1).toFloat()
                    },
                    modifier = Modifier.size(36.dp).testTag("btn_panel_volume_up")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_volume_up),
                        contentDescription = "Volume Up",
                        tint = Pearl,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Real State Diagnostics (Optional expand)
            if (showDiagnostics) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Line)
                Spacer(modifier = Modifier.height(10.dp))

                val sessionActive by SaviorMediaManager.sessionActive.collectAsState()
                val audioFocus by SaviorMediaManager.audioFocusState.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Graphite)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "REAL RUNTIME DIAGNOSTICS",
                        color = Dim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Media State: ${mediaState.name}", color = Pearl, fontSize = 11.sp)
                    Text(text = "MediaSession: ${if (sessionActive) "ACTIVE" else "INACTIVE"}", color = Pearl, fontSize = 11.sp)
                    Text(text = "Audio Focus: $audioFocus", color = Pearl, fontSize = 11.sp)
                    Text(text = "Picture-in-Picture: Available (API 26+)", color = Pearl, fontSize = 11.sp)
                }
            }
        }
    }
}
