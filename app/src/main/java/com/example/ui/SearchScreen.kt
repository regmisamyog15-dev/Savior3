package com.example.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.browser.SaviorBrowserActivity
import com.example.model.QuickActionType
import com.example.service.QuickCircleAccessibilityService
import com.example.ui.theme.Brass
import com.example.ui.theme.Dim
import com.example.ui.theme.Graphite
import com.example.ui.theme.Line
import com.example.ui.theme.LineBorder
import com.example.ui.theme.Panel
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.Pearl

data class SearchableTool(
    val title: String,
    val subtitle: String,
    val category: String,
    val iconRes: Int,
    val action: (android.content.Context) -> Unit
)

@Composable
fun SearchScreen(
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val allTools = remember {
        listOf(
            SearchableTool(
                title = "Background Play",
                subtitle = "Multitask with YouTube & web video/audio playback",
                category = "Media",
                iconRes = R.drawable.ic_action_background_play,
                action = { ctx -> SaviorBrowserActivity.launch(ctx, "https://m.youtube.com") }
            ),
            SearchableTool(
                title = "YouTube",
                subtitle = "https://m.youtube.com",
                category = "Web Shortcut",
                iconRes = R.drawable.ic_action_background_play,
                action = { ctx -> SaviorBrowserActivity.launch(ctx, "https://m.youtube.com") }
            ),
            SearchableTool(
                title = "YouTube Music",
                subtitle = "https://music.youtube.com",
                category = "Web Shortcut",
                iconRes = R.drawable.ic_action_background_play,
                action = { ctx -> SaviorBrowserActivity.launch(ctx, "https://music.youtube.com") }
            ),
            SearchableTool(
                title = "Google Podcasts",
                subtitle = "https://podcasts.google.com",
                category = "Web Shortcut",
                iconRes = R.drawable.ic_action_background_play,
                action = { ctx -> SaviorBrowserActivity.launch(ctx, "https://podcasts.google.com") }
            ),
            SearchableTool(
                title = "Instant Screenshot",
                subtitle = "Hardware-free screen capture",
                category = "Assistive",
                iconRes = R.drawable.ic_action_screenshot,
                action = { ctx ->
                    val controller = com.example.service.AppSystemActionController(ctx)
                    controller.performAction(QuickActionType.SCREENSHOT)
                }
            ),
            SearchableTool(
                title = "Software Volume",
                subtitle = "Up, down, and mute sliders",
                category = "Assistive",
                iconRes = R.drawable.ic_action_volume_up,
                action = { ctx ->
                    com.example.service.FloatingBubbleHost.showVolumeHud(ctx)
                }
            ),
            SearchableTool(
                title = "Screen Lock",
                subtitle = "Turn screen off without power button",
                category = "Assistive",
                iconRes = R.drawable.ic_action_lock,
                action = { ctx ->
                    val controller = com.example.service.AppSystemActionController(ctx)
                    controller.performAction(QuickActionType.LOCK_SCREEN)
                }
            ),
            SearchableTool(
                title = "Orb Appearance Settings",
                subtitle = "Size, opacity, dock edge, radial items",
                category = "Settings",
                iconRes = R.drawable.ic_action_quick_settings,
                action = { onNavigateToSettings() }
            )
        )
    }

    val filteredTools = remember(query) {
        if (query.isBlank()) {
            allTools
        } else {
            val q = query.trim().lowercase()
            allTools.filter {
                it.title.lowercase().contains(q) ||
                it.subtitle.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "SEARCH",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Brass,
            letterSpacing = 2.sp
        )
        Text(
            text = "Tools & Web Shortcuts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Pearl
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Panel)
                .border(1.dp, LineBorder, RoundedCornerShape(25.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (query.isNotBlank()) Brass else Dim,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Pearl, fontSize = 14.sp),
                cursorBrush = SolidColor(Brass),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank()) {
                            SaviorBrowserActivity.launch(context, query)
                        }
                    }
                ),
                modifier = Modifier.weight(1f).testTag("search_text_input")
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Dim, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Direct Web Go Card if query is typed
            if (query.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { SaviorBrowserActivity.launch(context, query) }
                            .testTag("search_direct_web_card"),
                        colors = CardDefaults.cardColors(containerColor = PanelElevated),
                        shape = RoundedCornerShape(14.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Brass))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_action_background_play), contentDescription = null, tint = Brass, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Open \"$query\" in Savior Browser", color = Pearl, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = "Search Google or load direct URL", color = Dim, fontSize = 11.sp)
                            }
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Brass, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            items(filteredTools) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tool.action(context) }
                        .testTag("search_item_${tool.title.lowercase().replace(" ", "_")}"),
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Line),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(tool.iconRes),
                                contentDescription = null,
                                tint = Pearl,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = tool.title, color = Pearl, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = tool.category, color = Dim, fontSize = 10.sp)
                            }
                            Text(text = tool.subtitle, color = Dim, fontSize = 12.sp, maxLines = 1)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Dim, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
