package com.example.ui

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.browser.BrowserRecentStore
import com.example.browser.RecentItem
import com.example.browser.RecentItemType
import com.example.browser.SaviorBrowserActivity
import com.example.livess.LiveSsItem
import com.example.livess.LiveSsRepository
import com.example.livess.ScreenshotEditorActivity
import com.example.model.QuickActionType
import com.example.recorder.VideoEditorActivity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentScreen() {
    val context = LocalContext.current
    val recentStore = remember { BrowserRecentStore.getInstance(context) }
    val liveSsRepo = remember { LiveSsRepository.getInstance(context) }

    val recentItems by recentStore.recentItems.collectAsState()
    val captures by liveSsRepo.items.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Captures, 1 = Activity
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SAVIOR GALLERY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brass,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Captures & Recents",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pearl
                )
            }

            if (selectedTab == 0 && captures.isNotEmpty()) {
                TextButton(
                    onClick = { liveSsRepo.clearAll() },
                    modifier = Modifier.testTag("btn_clear_captures")
                ) {
                    Text("Clear All", color = Dim, fontSize = 12.sp)
                }
            } else if (selectedTab == 1 && recentItems.isNotEmpty()) {
                TextButton(
                    onClick = { recentStore.clearAll() },
                    modifier = Modifier.testTag("btn_clear_recents")
                ) {
                    Text("Clear All", color = Dim, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Selector: Captures vs Activity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .border(1.dp, LineBorder, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 0) Brass else Panel)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Captures (${captures.size})",
                    color = if (selectedTab == 0) Graphite else Pearl,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedTab == 1) Brass else Panel)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Activity (${recentItems.size})",
                    color = if (selectedTab == 1) Graphite else Pearl,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Captures Tab
            if (captures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_action_screenshot),
                            contentDescription = null,
                            tint = Dim,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "No screen captures yet", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap Live Screenshot or Screen Recorder from Savior bubble",
                            color = Dim,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(captures, key = { it.id }) { item ->
                        CaptureItemCard(
                            item = item,
                            dateFormat = dateFormat,
                            onOpen = {
                                if (item.isVideo && item.videoUri != null) {
                                    VideoEditorActivity.launch(context, item.videoUri)
                                } else {
                                    ScreenshotEditorActivity.launch(context, item.stillUri)
                                }
                            },
                            onShare = {
                                val uriToShare = item.videoUri ?: item.stillUri
                                val mime = if (item.isVideo) "video/mp4" else "image/png"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = mime
                                    putExtra(Intent.EXTRA_STREAM, uriToShare)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share capture").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            onDelete = {
                                liveSsRepo.deleteItem(item.id)
                                Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        } else {
            // Activity Tab
            if (recentItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Dim,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No recent items yet", color = Pearl, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Websites and tool shortcuts will appear here", color = Dim, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recentItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.data.startsWith("http")) {
                                        SaviorBrowserActivity.launch(context, item.data)
                                    } else if (item.data == "background_play") {
                                        SaviorBrowserActivity.launch(context)
                                    } else if (item.data == "screenshot") {
                                        QuickCircleAccessibilityService.instance?.performAction(QuickActionType.SCREENSHOT)
                                    }
                                }
                                .testTag("recent_item_${item.id}"),
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            shape = RoundedCornerShape(14.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Line),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            when (item.type) {
                                                RecentItemType.BROWSER_SITE -> R.drawable.ic_action_background_play
                                                RecentItemType.SCREENSHOT -> R.drawable.ic_action_screenshot
                                                else -> R.drawable.ic_action_quick_settings
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = Pearl,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Pearl,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.subtitle} • ${dateFormat.format(Date(item.timestamp))}",
                                        color = Dim,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Dim,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CaptureItemCard(
    item: LiveSsItem,
    dateFormat: SimpleDateFormat,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("capture_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = PanelElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(LineBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (item.isVideo) RoseError.copy(alpha = 0.2f) else Brass.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (item.isVideo) R.drawable.ic_action_screen_recorder else R.drawable.ic_action_screenshot
                    ),
                    contentDescription = null,
                    tint = if (item.isVideo) RoseError else Brass,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = Pearl,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Line)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (item.isVideo) "VIDEO" else "CLEAN SS",
                            color = if (item.isVideo) RoseError else Brass,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${dateFormat.format(Date(item.createdAt))} • Tap to Edit",
                    color = Dim,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Brass,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Dim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
