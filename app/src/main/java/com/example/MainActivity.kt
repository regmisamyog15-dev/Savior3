package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.QuickCircleAccessibilityService
import com.example.service.QuickCircleNotificationListenerService
import com.example.ui.HomeScreen
import com.example.ui.RecentScreen
import com.example.ui.SearchScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.Brass
import com.example.ui.theme.Dim
import com.example.ui.theme.Graphite
import com.example.ui.theme.Line
import com.example.ui.theme.LineBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Panel
import com.example.ui.theme.PanelElevated
import com.example.ui.theme.Pearl
import com.example.ui.theme.RoseError
import com.example.ui.theme.Sage

enum class MainNavTab(val label: String, val iconRes: Int) {
    HOME("Home", R.drawable.ic_action_home),
    SEARCH("Search", R.drawable.ic_action_quick_settings),
    RECENT("Recent", R.drawable.ic_action_recents),
    SETTINGS("Settings", R.drawable.ic_action_quick_settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(MainNavTab.HOME) }

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasAccessibilityPermission by remember {
        mutableStateOf(QuickCircleAccessibilityService.isServiceRunning)
    }

    // Refresh permissions automatically whenever the activity resumes from system Settings
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasAccessibilityPermission = QuickCircleAccessibilityService.isServiceRunning
        com.example.service.FloatingBubbleHost.syncState(context)
    }

    BackHandler(enabled = selectedTab != MainNavTab.HOME) {
        selectedTab = MainNavTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Graphite,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = PanelElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainNavTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .testTag("tab_${tab.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = tab.label,
                                tint = if (isSelected) Brass else Dim,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Pearl else Dim
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Permission Banner: cleanly asks for permissions and is automatically removed once granted
                com.example.ui.PermissionSetupCard(
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRefreshPermissions = {
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                        hasAccessibilityPermission = QuickCircleAccessibilityService.isServiceRunning
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // Screen Content
                when (selectedTab) {
                    MainNavTab.HOME -> {
                        HomeScreen(
                            onNavigateToSearch = { selectedTab = MainNavTab.SEARCH },
                            onNavigateToRecent = { selectedTab = MainNavTab.RECENT },
                            onNavigateToSettings = { selectedTab = MainNavTab.SETTINGS }
                        )
                    }
                    MainNavTab.SEARCH -> {
                        SearchScreen(
                            onNavigateToSettings = { selectedTab = MainNavTab.SETTINGS }
                        )
                    }
                    MainNavTab.RECENT -> {
                        RecentScreen()
                    }
                    MainNavTab.SETTINGS -> {
                        SettingsScreen()
                    }
                }
            }

            // In-App Compose Floating Assistive Orb with discrete, uniquely scoped actions
            com.example.ui.FloatingAssistiveOrb(
                onNavigateToSettings = { selectedTab = MainNavTab.SETTINGS }
            )
        }
    }
}
