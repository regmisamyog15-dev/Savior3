package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.Sage

@Composable
fun PermissionSetupCard(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allGranted = hasOverlayPermission && hasAccessibilityPermission

    // Automatically remove/hide the setup card once all permissions are granted
    AnimatedVisibility(
        visible = !allGranted,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("permission_setup_card"),
            colors = CardDefaults.cardColors(containerColor = PanelElevated),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Brass.copy(alpha = 0.6f)))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header with Info Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brass.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Brass,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.perm_dialog_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Pearl
                            )
                            Text(
                                text = "Required once for assistive actions",
                                fontSize = 11.sp,
                                color = Dim
                            )
                        }
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Line)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${if (hasOverlayPermission) 1 else 0 + if (hasAccessibilityPermission) 1 else 0}/2 Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brass
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.perm_dialog_desc),
                    fontSize = 12.sp,
                    color = Dim,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Line)
                Spacer(modifier = Modifier.height(12.dp))

                // Permission Item 1: Overlay Permission
                PermissionItemRow(
                    title = stringResource(R.string.perm_overlay_title),
                    description = stringResource(R.string.perm_overlay_desc),
                    isGranted = hasOverlayPermission,
                    onRequest = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    },
                    tag = "btn_grant_overlay"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Item 2: Accessibility Permission
                PermissionItemRow(
                    title = stringResource(R.string.perm_accessibility_title),
                    description = stringResource(R.string.perm_accessibility_desc),
                    isGranted = hasAccessibilityPermission,
                    onRequest = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    tag = "btn_grant_accessibility"
                )
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, if (isGranted) Sage.copy(alpha = 0.3f) else LineBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = Sage,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isGranted) Sage else Pearl
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = Dim,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Sage.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.perm_status_granted),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Sage
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brass)
                    .clickable { onRequest() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag(tag),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.perm_status_needed),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Graphite
                )
            }
        }
    }
}
