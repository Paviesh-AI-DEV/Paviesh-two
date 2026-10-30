package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServoConfig
import com.example.data.UsbStatus
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.AppBorderGlowing
import com.example.ui.theme.AppBorderSubtle
import com.example.ui.theme.AppCardDark
import com.example.ui.theme.AppSurfaceDark
import com.example.ui.theme.ConnectedGreenBg
import com.example.ui.theme.ConnectedGreenText
import com.example.ui.theme.TextLightGrey
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextWhite

/**
 * Top-Right Connection Status Pill matching reference screenshot.
 * Displays green dot, "Connected", and "Arduino Uno".
 */
@Composable
fun ConnectionPillBadge(
    usbStatus: UsbStatus,
    onClick: () -> Unit = {}
) {
    val isConnected = usbStatus is UsbStatus.Connected || usbStatus is UsbStatus.Simulation
    val (statusTitle, statusSub) = when (usbStatus) {
        is UsbStatus.Connected -> "Connected" to "Arduino Uno"
        is UsbStatus.Simulation -> "Connected" to "Virtual Uno"
        is UsbStatus.Connecting -> "Connecting" to "Scanning..."
        is UsbStatus.Disconnected -> "Disconnected" to "USB Port"
        is UsbStatus.Error -> "Error" to "Check Cable"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isConnected) ConnectedGreenBg else Color(0xFF221111),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected) ConnectedGreenText.copy(alpha = 0.5f) else AccentRed.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("connection_pill_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) ConnectedGreenText else AccentRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) ConnectedGreenText else AccentRed,
                    fontSize = 11.sp
                )
                Text(
                    text = statusSub,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLightGrey,
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Large Hero Action Card matching the 3 cards on Screen 1:
 * - Gradient background
 * - Left rounded vibrant icon container
 * - Title and subtitle
 * - Right arrow
 */
@Composable
fun HeroActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    iconBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "hero_action_card"
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderGlowing.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Left rounded vibrant icon
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightGrey.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Right arrow
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = TextWhite.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Segmented Pill Toggle matching Screen 3:
 * [ Manual ] | [ AI Mode ]
 */
@Composable
fun SegmentedPillToggle(
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AppCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Manual Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedIndex == 0) AccentBlue else Color.Transparent)
                    .clickable { onOptionSelected(0) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Manual",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedIndex == 0) TextWhite else TextMutedGrey
                )
            }

            // AI Mode Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedIndex == 1) AccentBlue else Color.Transparent)
                    .clickable { onOptionSelected(1) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedIndex == 1) TextWhite else TextMutedGrey
                )
            }
        }
    }
}

/**
 * Sleek Servo Slider Row matching Screen 3:
 * Base (S1) -----------●----------- 90°
 */
@Composable
fun SleekServoRow(
    label: String,
    subLabel: String,
    motorModel: String = "",
    angle: Int,
    minAngle: Int = 0,
    maxAngle: Int = 180,
    onAngleChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label on left
        Column(
            modifier = Modifier.width(122.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($subLabel)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            if (motorModel.isNotEmpty()) {
                Text(
                    text = motorModel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (motorModel.contains("MG996R")) AccentAmber else AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        // Slider track
        Slider(
            value = angle.toFloat(),
            onValueChange = { onAngleChange(it.toInt()) },
            valueRange = minAngle.toFloat()..maxAngle.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color(0xFF142036)
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // Degree readout on right
        Text(
            text = "$angle°",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextLightGrey,
            modifier = Modifier.width(36.dp)
        )
    }
}

/**
 * Preset Pill Button for [ P1 ] [ P2 ] [ P3 ] [ P4 ] row on Screen 3
 */
@Composable
fun PresetPillButton(
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AccentBlue.copy(alpha = 0.3f) else AppCardDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AccentBlue else AppBorderSubtle
        ),
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) AccentCyan else TextLightGrey
            )
        }
    }
}

/**
 * Settings Screen Category Card matching Screen 4:
 * Circular icon badge, title, subtitle, right chevron.
 */
@Composable
fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedGrey,
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Go",
                tint = TextMutedGrey,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
