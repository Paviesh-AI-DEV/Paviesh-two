package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.R
import com.example.ui.components.ConnectionPillBadge
import com.example.ui.components.PresetPillButton
import com.example.ui.components.SegmentedPillToggle
import com.example.ui.components.SleekServoRow
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AppBackgroundDark
import com.example.ui.theme.AppBorderSubtle
import com.example.ui.theme.AppCardDark
import com.example.ui.theme.TextLightGrey
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.WarningAmber

@Composable
fun ArmControlScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {}
) {
    val usbStatus by viewModel.usbStatus.collectAsState()
    val servos by viewModel.servos.collectAsState()
    val currentPose by viewModel.currentPose.collectAsState()
    val isEmergencyStopped by viewModel.isEmergencyStopped.collectAsState()
    val isAutoTrackingArm by viewModel.isAutoTrackingArm.collectAsState()

    var selectedModeIndex by remember { mutableIntStateOf(0) } // 0: Manual, 1: AI Mode
    var activePresetIndex by remember { mutableIntStateOf(0) } // 0: P1, 1: P2, 2: P3, 3: P4

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header Bar: Back arrow, "Arm Control" title, Green status pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Arm Control",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        fontSize = 19.sp
                    )
                }

                ConnectionPillBadge(
                    usbStatus = usbStatus,
                    onClick = {}
                )
            }
        }

        // 2. Segmented Pill Toggle: [ Manual ] | [ AI Mode ]
        item {
            SegmentedPillToggle(
                selectedIndex = selectedModeIndex,
                onOptionSelected = {
                    selectedModeIndex = it
                    if (it == 1) {
                        viewModel.toggleAutoTrackingArm(true)
                    } else {
                        viewModel.toggleAutoTrackingArm(false)
                    }
                }
            )
        }

        // 3. Glowing 3D Robotic Arm Graphic matching Screen 3 in screenshot
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0A101E))
                    .border(1.dp, AppBorderSubtle, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_paviesh_arm_1793321799618),
                    contentDescription = "3D Robotic Arm Visual",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(190.dp)
                )

                if (isEmergencyStopped) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentRed.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "E-STOP ENGAGED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (selectedModeIndex == 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentCyan.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "AI AUTO-AIM ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 4. Servo Sliders List matching Screen 3 layout
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Base (S1) - MG996R
                    SleekServoRow(
                        label = "Base",
                        subLabel = "S1",
                        motorModel = "MG996R (Metal Gear)",
                        angle = currentPose.s1Base,
                        minAngle = 0,
                        maxAngle = 180,
                        onAngleChange = { viewModel.setServoAngle(1, it) }
                    )

                    // Shoulder (S2) - MG996R
                    SleekServoRow(
                        label = "Shoulder",
                        subLabel = "S2",
                        motorModel = "MG996R (Metal Gear)",
                        angle = currentPose.s2Shoulder,
                        minAngle = 15,
                        maxAngle = 165,
                        onAngleChange = { viewModel.setServoAngle(2, it) }
                    )

                    // Elbow (S3) - MG996R
                    SleekServoRow(
                        label = "Elbow",
                        subLabel = "S3",
                        motorModel = "MG996R (Metal Gear)",
                        angle = currentPose.s3Elbow,
                        minAngle = 15,
                        maxAngle = 165,
                        onAngleChange = { viewModel.setServoAngle(3, it) }
                    )

                    // Wrist Pitch (S4) - SG90
                    SleekServoRow(
                        label = "Wrist",
                        subLabel = "S4",
                        motorModel = "SG90 (Micro 9g)",
                        angle = currentPose.s4WristPitch,
                        minAngle = 0,
                        maxAngle = 180,
                        onAngleChange = { viewModel.setServoAngle(4, it) }
                    )

                    // Wrist Roll (S5) - SG90
                    SleekServoRow(
                        label = "Wrist Roll",
                        subLabel = "S5",
                        motorModel = "SG90 (Micro 9g)",
                        angle = currentPose.s5WristRoll,
                        minAngle = 0,
                        maxAngle = 180,
                        onAngleChange = { viewModel.setServoAngle(5, it) }
                    )

                    // Gripper (S6) - SG90
                    SleekServoRow(
                        label = "Gripper",
                        subLabel = "S6",
                        motorModel = "SG90 (Micro 9g)",
                        angle = currentPose.s6Gripper,
                        minAngle = 0,
                        maxAngle = 90,
                        onAngleChange = { viewModel.setServoAngle(6, it) }
                    )
                }
            }
        }

        // 5. Presets Row: [ P1 ] [ P2 ] [ P3 ] [ P4 ] matching Screen 3
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val presetLabels = listOf("P1" to "Home", "P2" to "Pick", "P3" to "Lift", "P4" to "Place")
                presetLabels.forEachIndexed { index, (label, name) ->
                    PresetPillButton(
                        label = label,
                        isSelected = activePresetIndex == index,
                        onClick = {
                            activePresetIndex = index
                            when (index) {
                                0 -> viewModel.home()
                                1 -> viewModel.setServoAngle(6, 85)
                                2 -> viewModel.setServoAngle(2, 60)
                                3 -> viewModel.setServoAngle(1, 150)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 6. Quick Action Row: Home & Emergency Stop
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.home() },
                    colors = ButtonDefaults.buttonColors(containerColor = AppCardDark, contentColor = TextWhite),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(1.dp, AppBorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HOME", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (isEmergencyStopped) viewModel.home() else viewModel.emergencyStop()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEmergencyStopped) WarningAmber else AccentRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Dangerous, contentDescription = "E-Stop", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEmergencyStopped) "RESET STOP" else "E-STOP", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}
