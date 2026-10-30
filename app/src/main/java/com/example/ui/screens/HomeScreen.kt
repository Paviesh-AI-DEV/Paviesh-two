package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.VoiceAssistantState
import com.example.ui.components.ConnectionPillBadge
import com.example.ui.components.HeroActionCard
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AppBackgroundDark
import com.example.ui.theme.AppBorderSubtle
import com.example.ui.theme.AppCardDark
import com.example.ui.theme.ArmControlGradient
import com.example.ui.theme.ArmControlIconBg
import com.example.ui.theme.LiveCameraGradient
import com.example.ui.theme.LiveCameraIconBg
import com.example.ui.theme.SettingsGradient
import com.example.ui.theme.SettingsIconBg
import com.example.ui.theme.TextLightGrey
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextWhite

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToArm: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val usbStatus by viewModel.usbStatus.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val currentPose by viewModel.currentPose.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Bar matching Screen 1 in reference
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNavigateToSettings() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = TextWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Default.PrecisionManufacturing,
                        contentDescription = "Arm Icon",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "AI Robotic Arm",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Control • Camera • Arduino",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLightGrey,
                            fontSize = 11.sp
                        )
                    }
                }

                // Green "Connected Arduino Uno" pill
                ConnectionPillBadge(
                    usbStatus = usbStatus,
                    onClick = onNavigateToSettings
                )
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 2. Three Large Hero Action Cards matching Screen 1
        // Card 1: Live Camera
        item {
            HeroActionCard(
                title = "Live Camera",
                subtitle = "Open camera in full screen",
                icon = Icons.Default.CameraAlt,
                gradient = LiveCameraGradient,
                iconBgColor = LiveCameraIconBg,
                onClick = onNavigateToCamera,
                tag = "home_card_live_camera"
            )
        }

        // Card 2: Arm Control
        item {
            HeroActionCard(
                title = "Arm Control",
                subtitle = "Manual / AI Mode",
                icon = Icons.Default.PrecisionManufacturing,
                gradient = ArmControlGradient,
                iconBgColor = ArmControlIconBg,
                onClick = onNavigateToArm,
                tag = "home_card_arm_control"
            )
        }

        // Card 3: Settings
        item {
            HeroActionCard(
                title = "Settings",
                subtitle = "Camera, Arduino & App",
                icon = Icons.Default.Settings,
                gradient = SettingsGradient,
                iconBgColor = SettingsIconBg,
                onClick = onNavigateToSettings,
                tag = "home_card_settings"
            )
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 3. Telemetry & Voice Assistant Quick Status Widget
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CURRENT ARM TELEMETRY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedGrey
                        )
                        Text(
                            text = "S1-S6: [${currentPose.toAngleList().joinToString(", ")}°]",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Voice assistant quick transcription preview
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF090E1A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val voiceHint = when (val state = assistantState) {
                                is VoiceAssistantState.Listening -> "Listening... Speak in Tamil, English, or Tanglish"
                                is VoiceAssistantState.Processing -> "Analyzing: \"${state.spokenQuery}\""
                                is VoiceAssistantState.Speaking -> "Speaking: ${state.message}"
                                is VoiceAssistantState.ActionExecuted -> "Executed: ${state.actionName}"
                                is VoiceAssistantState.Error -> state.message
                                VoiceAssistantState.Idle -> "Voice Assistant: \"ஹோம் போ\", \"Open Gripper\", or solve math"
                            }
                            Text(
                                text = voiceHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLightGrey,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
