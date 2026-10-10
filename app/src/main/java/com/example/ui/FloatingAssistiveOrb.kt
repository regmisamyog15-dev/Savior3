package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.QuickCirclePreferences
import com.example.model.QuickActionType
import com.example.recorder.ScreenRecorderService
import com.example.service.AppSystemActionController
import com.example.ui.theme.Brass
import com.example.ui.theme.Dim
import com.example.ui.theme.Graphite
import com.example.ui.theme.Line
import com.example.ui.theme.LineBorder
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.Pearl
import com.example.ui.theme.RoseError
import com.example.ui.theme.Sage
import kotlin.math.roundToInt

/**
 * Pure Jetpack Compose Floating Assistive Controller (Floating Orb).
 *
 * Implements discrete, non-overlapping click targets and uniquely scoped onClick lambdas
 * for every floating action button, completely eliminating event propagation conflicts
 * or cross-triggering between functionalities.
 */
@Composable
fun FloatingAssistiveOrb(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = remember { QuickCirclePreferences.getInstance(context) }
    val isOrbEnabled by preferences.isEnabled.collectAsState()
    val bubbleSizeDp by preferences.bubbleSizeDp.collectAsState()
    val bubbleOpacity by preferences.bubbleOpacity.collectAsState()

    if (!isOrbEnabled) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val actionController = remember { AppSystemActionController(context) }
    var isExpanded by remember { mutableStateOf(false) }

    // Initial position on screen right edge
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - with(density) { (bubbleSizeDp + 16).dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx * 0.45f) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "orb_rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Scrim Backdrop when expanded to catch outside taps and close menu
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isExpanded = false
                        }
                    }
                    .testTag("floating_scrim_backdrop")
            )
        }

        // 2. Expanded Floating Actions Column (Non-overlapping, uniquely scoped)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .offset {
                    val menuW = with(density) { 220.dp.toPx() }
                    val targetX = if (offsetX > screenWidthPx / 2f) {
                        (offsetX - menuW + with(density) { 48.dp.toPx() }).coerceAtLeast(16f)
                    } else {
                        (offsetX + with(density) { 16.dp.toPx() }).coerceAtMost(screenWidthPx - menuW - 16f)
                    }
                    val targetY = (offsetY - with(density) { 340.dp.toPx() }).coerceIn(
                        with(density) { 40.dp.toPx() },
                        screenHeightPx - with(density) { 420.dp.toPx() }
                    )
                    IntOffset(targetX.roundToInt(), targetY.roundToInt())
                }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = if (offsetX > screenWidthPx / 2f) Alignment.End else Alignment.Start,
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("floating_buttons_container")
            ) {
                // Header badge
                Surface(
                    color = PanelElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "SAVIOR QUICK ACTIONS",
                        color = Brass,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // 1. Volume Action Button (Functionality 1)
                FloatingActionButtonRow(
                    action = QuickActionType.VOLUME,
                    iconRes = R.drawable.ic_action_volume_up,
                    label = "Volume HUD",
                    accentColor = Pearl,
                    testTag = "floating_btn_volume",
                    onClick = {
                        isExpanded = false
                        val info = actionController.getVolumeInfo()
                        actionController.showVolumeHud()
                    }
                )

                // 2. Live Screenshot Action Button (Functionality 2)
                FloatingActionButtonRow(
                    action = QuickActionType.LIVE_SCREENSHOT,
                    iconRes = R.drawable.ic_action_screenshot,
                    label = "Live Screenshot",
                    accentColor = Brass,
                    testTag = "floating_btn_live_ss",
                    onClick = {
                        isExpanded = false
                        com.example.livess.LiveSsPermissionActivity.launchForScreenshot(context)
                    }
                )

                // 3. Screen Recorder Action Button (Functionality 3)
                val isRecording = ScreenRecorderService.isRunning
                FloatingActionButtonRow(
                    action = QuickActionType.SCREEN_RECORDER,
                    iconRes = R.drawable.ic_action_screen_recorder,
                    label = if (isRecording) "Stop Recording 🔴" else "Screen Recorder",
                    accentColor = if (isRecording) RoseError else Brass,
                    testTag = "floating_btn_recorder",
                    onClick = {
                        isExpanded = false
                        if (isRecording) {
                            val stopIntent = Intent(context, ScreenRecorderService::class.java).apply {
                                action = ScreenRecorderService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                        } else {
                            com.example.livess.LiveSsPermissionActivity.launchForRecording(context)
                        }
                    }
                )

                // 4. Instant Screenshot Action Button (Functionality 4)
                FloatingActionButtonRow(
                    action = QuickActionType.SCREENSHOT,
                    iconRes = R.drawable.ic_action_screenshot,
                    label = "Instant Screenshot",
                    accentColor = Pearl,
                    testTag = "floating_btn_screenshot",
                    onClick = {
                        isExpanded = false
                        actionController.performAction(QuickActionType.SCREENSHOT)
                    }
                )

                // 5. Background Play Action Button (Functionality 5)
                FloatingActionButtonRow(
                    action = QuickActionType.BACKGROUND_PLAY,
                    iconRes = R.drawable.ic_action_background_play,
                    label = "Background Play",
                    accentColor = Sage,
                    testTag = "floating_btn_bg_play",
                    onClick = {
                        isExpanded = false
                        com.example.browser.SaviorBrowserActivity.launch(context)
                    }
                )

                // 6. Lock Screen Action Button (Functionality 6)
                FloatingActionButtonRow(
                    action = QuickActionType.LOCK_SCREEN,
                    iconRes = R.drawable.ic_action_lock,
                    label = "Lock Screen",
                    accentColor = Dim,
                    testTag = "floating_btn_lock",
                    onClick = {
                        isExpanded = false
                        actionController.performAction(QuickActionType.LOCK_SCREEN)
                    }
                )
            }
        }

        // 3. Floating Orb Trigger (Draggable & Clickable)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(bubbleSizeDp.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Graphite.copy(alpha = bubbleOpacity))
                .border(1.5.dp, if (isExpanded) Brass else Pearl.copy(alpha = 0.5f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap to closest screen edge (Left or Right)
                            val bubbleSizePx = with(density) { bubbleSizeDp.dp.toPx() }
                            val midpoint = screenWidthPx / 2f
                            offsetX = if (offsetX + (bubbleSizePx / 2f) >= midpoint) {
                                screenWidthPx - bubbleSizePx - with(density) { 10.dp.toPx() }
                            } else {
                                with(density) { 10.dp.toPx() }
                            }
                            preferences.savePosition(offsetX.toInt(), offsetY.toInt())
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val bubbleSizePx = with(density) { bubbleSizeDp.dp.toPx() }
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - bubbleSizePx)
                        offsetY = (offsetY + dragAmount.y).coerceIn(
                            with(density) { 48.dp.toPx() },
                            screenHeightPx - bubbleSizePx - with(density) { 96.dp.toPx() }
                        )
                    }
                }
                .clickable {
                    isExpanded = !isExpanded
                }
                .testTag("floating_orb_trigger"),
            contentAlignment = Alignment.Center
        ) {
            // Inner Core & Highlight
            Box(
                modifier = Modifier
                    .size((bubbleSizeDp * 0.45f).dp)
                    .clip(CircleShape)
                    .background(if (isExpanded) Brass else Brass.copy(alpha = 0.85f))
                    .rotate(rotationAngle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (isExpanded) R.drawable.ic_action_close else R.drawable.ic_action_quick_settings
                    ),
                    contentDescription = if (isExpanded) "Close Savior Menu" else "Open Savior Menu",
                    tint = Graphite,
                    modifier = Modifier.size((bubbleSizeDp * 0.28f).dp)
                )
            }
        }
    }
}

/**
 * A discrete, isolated Floating Action Button row.
 * Guarantees zero event bubbling or overlapping hit targets.
 */
@Composable
private fun FloatingActionButtonRow(
    action: QuickActionType,
    iconRes: Int,
    label: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        // 1. Text Label Pill
        Surface(
            color = PanelElevated,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            modifier = Modifier
                .clickable { onClick() }
                .testTag("${testTag}_label")
        ) {
            Text(
                text = label,
                color = Pearl,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // 2. Tactile Floating Button (Minimum 48dp x 48dp target for accessibility)
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .shadow(8.dp, CircleShape)
                .border(1.2.dp, accentColor.copy(alpha = 0.7f), CircleShape)
                .testTag(testTag),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Graphite,
                contentColor = accentColor
            )
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
