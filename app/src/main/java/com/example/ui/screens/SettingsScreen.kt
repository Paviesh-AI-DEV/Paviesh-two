package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.UsbStatus
import com.example.ui.components.SettingsCategoryCard
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AppBackgroundDark
import com.example.ui.theme.AppBorderSubtle
import com.example.ui.theme.AppCardDark
import com.example.ui.theme.AppSurfaceDark
import com.example.ui.theme.ConnectedGreenBg
import com.example.ui.theme.ConnectedGreenText
import com.example.ui.theme.TextLightGrey
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.WarningAmber

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology

enum class SettingsSubScreen {
    MAIN,
    CAMERA_SETTINGS,
    ARDUINO_CONNECTION,
    AI_INSTRUCTIONS,
    APP_INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val usbStatus by viewModel.usbStatus.collectAsState()

    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Intercept hardware/gestural back to navigate up inside Settings
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    when (currentSubScreen) {
        SettingsSubScreen.MAIN -> {
            MainSettingsList(
                onBack = onBackToHome,
                onNavigateToCameraSettings = { currentSubScreen = SettingsSubScreen.CAMERA_SETTINGS },
                onNavigateToArduinoConnection = { currentSubScreen = SettingsSubScreen.ARDUINO_CONNECTION },
                onNavigateToAiInstructions = { currentSubScreen = SettingsSubScreen.AI_INSTRUCTIONS },
                onNavigateToAppInfo = { currentSubScreen = SettingsSubScreen.APP_INFO }
            )
        }
        SettingsSubScreen.CAMERA_SETTINGS -> {
            CameraSettingsSubScreen(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.ARDUINO_CONNECTION -> {
            ArduinoConnectionSubScreen(
                viewModel = viewModel,
                usbStatus = usbStatus,
                context = context,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.AI_INSTRUCTIONS -> {
            AiInstructionsSubScreen(
                viewModel = viewModel,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
        SettingsSubScreen.APP_INFO -> {
            AppInfoSubScreen(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN }
            )
        }
    }
}

/**
 * Screen 4: Main Settings Category List matching reference screenshot
 */
@Composable
private fun MainSettingsList(
    onBack: () -> Unit,
    onNavigateToCameraSettings: () -> Unit,
    onNavigateToArduinoConnection: () -> Unit,
    onNavigateToAiInstructions: () -> Unit,
    onNavigateToAppInfo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header: Back arrow + "Settings"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 20.sp
                )
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }

        // Card 1: Camera Settings
        item {
            SettingsCategoryCard(
                title = "Camera Settings",
                subtitle = "Resolution • FPS • Focus",
                icon = Icons.Default.CameraAlt,
                iconColor = AccentBlue,
                onClick = onNavigateToCameraSettings
            )
        }

        // Card 2: Arduino Connection
        item {
            SettingsCategoryCard(
                title = "Arduino Connection",
                subtitle = "COM Port • Baud Rate",
                icon = Icons.Default.Link,
                iconColor = ConnectedGreenText,
                onClick = onNavigateToArduinoConnection
            )
        }

        // Card 3: AI System Instructions & Memory
        item {
            SettingsCategoryCard(
                title = "AI Instructions & Memory",
                subtitle = "Custom Persona • Conversation History",
                icon = Icons.Default.AutoAwesome,
                iconColor = AccentCyan,
                onClick = onNavigateToAiInstructions
            )
        }

        // Card 4: App Information
        item {
            SettingsCategoryCard(
                title = "App Information",
                subtitle = "Version 1.0.0",
                icon = Icons.Default.Info,
                iconColor = AccentGreen,
                onClick = onNavigateToAppInfo
            )
        }
    }
}

/**
 * Screen 5: Arduino Connection Sub-screen matching reference screenshot
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArduinoConnectionSubScreen(
    viewModel: MainViewModel,
    usbStatus: UsbStatus,
    context: Context,
    onBack: () -> Unit
) {
    val isConnected = usbStatus is UsbStatus.Connected || usbStatus is UsbStatus.Simulation
    val availableDevices by viewModel.usbSerialManager.availableDevices.collectAsState()
    var portDropdownExpanded by remember { mutableStateOf(false) }
    var selectedPort by remember { mutableStateOf("USB Host (OTG)") }

    var baudDropdownExpanded by remember { mutableStateOf(false) }
    val baudOptions = listOf(9600, 115200, 57600)

    val firmwareCode = remember {
        try {
            context.assets.open("ArduinoFirmware.ino").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "// ArduinoFirmware.ino"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Arduino Connection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 19.sp
                )
            }
        }

        // Large Connected Status Card matching Screen 5
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) ConnectedGreenBg else Color(0xFF2E1010)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Connection",
                            tint = if (isConnected) ConnectedGreenText else AccentRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isConnected) ConnectedGreenText else AccentRed,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (usbStatus is UsbStatus.Connected) "Arduino Uno (Hardware)" else "Arduino Uno (USB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightGrey
                        )
                    }
                }
            }
        }

        // Port Dropdown Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Port",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextLightGrey,
                    fontWeight = FontWeight.SemiBold
                )

                ExposedDropdownMenuBox(
                    expanded = portDropdownExpanded,
                    onExpandedChange = { portDropdownExpanded = it }
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppCardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(selectedPort, color = TextWhite, fontSize = 13.sp)
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = portDropdownExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = portDropdownExpanded,
                        onDismissRequest = { portDropdownExpanded = false }
                    ) {
                        availableDevices.forEach { port ->
                            DropdownMenuItem(
                                text = { Text(port) },
                                onClick = {
                                    selectedPort = port
                                    portDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Baud Rate Dropdown Selector matching Screen 5
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Baud Rate",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextLightGrey,
                    fontWeight = FontWeight.SemiBold
                )

                ExposedDropdownMenuBox(
                    expanded = baudDropdownExpanded,
                    onExpandedChange = { baudDropdownExpanded = it }
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppCardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                        modifier = Modifier
                            .width(170.dp)
                            .height(44.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${viewModel.usbSerialManager.currentBaudRate}", color = TextWhite, fontSize = 13.sp)
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = baudDropdownExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = baudDropdownExpanded,
                        onDismissRequest = { baudDropdownExpanded = false }
                    ) {
                        baudOptions.forEach { rate ->
                            DropdownMenuItem(
                                text = { Text("$rate") },
                                onClick = {
                                    viewModel.usbSerialManager.scanAndConnect(rate)
                                    baudDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Big Blue Disconnect / Connect Button matching Screen 5
        item {
            Button(
                onClick = {
                    if (isConnected) {
                        viewModel.usbSerialManager.disconnect()
                    } else {
                        viewModel.usbSerialManager.scanAndConnect()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) AccentBlue else Color(0xFF1E3A6C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.LinkOff else Icons.Default.Link,
                    contentDescription = "Toggle",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Disconnect" else "Connect Arduino",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // 6-DOF Servo Hardware Architecture Card (3x MG996R + 3x SG90)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Physical Servo Hardware Layout",
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3x MG996R (Metal Gear) + 3x SG90 (Micro 9g)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentCyan,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // MG996R Section
                    Text("HEAVY JOINTS (3x MG996R):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedGrey)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("• S1 Base: Pin 3 PWM (0° - 180°)", fontSize = 12.sp, color = TextLightGrey)
                    Text("• S2 Shoulder: Pin 5 PWM (15° - 165°)", fontSize = 12.sp, color = TextLightGrey)
                    Text("• S3 Elbow: Pin 6 PWM (15° - 165°)", fontSize = 12.sp, color = TextLightGrey)

                    Spacer(modifier = Modifier.height(8.dp))

                    // SG90 Section
                    Text("END EFFECTOR (3x SG90):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedGrey)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("• S4 Wrist Pitch: Pin 9 PWM (0° - 180°)", fontSize = 12.sp, color = TextLightGrey)
                    Text("• S5 Wrist Roll: Pin 10 PWM (0° - 180°)", fontSize = 12.sp, color = TextLightGrey)
                    Text("• S6 Claw Gripper: Pin 11 PWM (0° - 90°)", fontSize = 12.sp, color = TextLightGrey)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Power warning note
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1700),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ Dedicated 5V/6V 5A DC power supply required. Connect External PSU GND to Arduino GND (Common Ground).",
                            color = WarningAmber,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // Copy Arduino C++ Firmware Button
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Arduino C++ Firmware", fontWeight = FontWeight.Bold, color = TextWhite)
                            Text("Arduino Uno (ATmega328P) @ 9600 Baud", style = MaterialTheme.typography.bodySmall, color = TextMutedGrey)
                        }
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ArduinoFirmware.ino", firmwareCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Firmware copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162A4E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 6: Camera Settings Sub-screen matching reference screenshot
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSettingsSubScreen(
    onBack: () -> Unit
) {
    var resolutionExpanded by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf("1920 x 1080 (Full HD)") }
    val resolutionOptions = listOf("1920 x 1080 (Full HD)", "1280 x 720 (HD)", "640 x 480 (VGA)")

    var fpsExpanded by remember { mutableStateOf(false) }
    var selectedFps by remember { mutableStateOf("30 FPS") }
    val fpsOptions = listOf("30 FPS", "60 FPS")

    var isAutoFocusOn by remember { mutableStateOf(true) }
    var isLowLightModeOn by remember { mutableStateOf(false) }
    var isImageStabilizationOn by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Camera Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 19.sp
                )
            }
        }

        // Resolution Dropdown matching Screen 6
        item {
            Column {
                Text(
                    text = "Resolution",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextLightGrey,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = resolutionExpanded,
                    onExpandedChange = { resolutionExpanded = it }
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppCardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(selectedResolution, color = TextWhite, fontSize = 14.sp)
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = resolutionExpanded,
                        onDismissRequest = { resolutionExpanded = false }
                    ) {
                        resolutionOptions.forEach { res ->
                            DropdownMenuItem(
                                text = { Text(res) },
                                onClick = {
                                    selectedResolution = res
                                    resolutionExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // FPS Dropdown matching Screen 6
        item {
            Column {
                Text(
                    text = "FPS",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextLightGrey,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = fpsExpanded,
                    onExpandedChange = { fpsExpanded = it }
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppCardDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(selectedFps, color = TextWhite, fontSize = 14.sp)
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fpsExpanded)
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = fpsExpanded,
                        onDismissRequest = { fpsExpanded = false }
                    ) {
                        fpsOptions.forEach { fps ->
                            DropdownMenuItem(
                                text = { Text(fps) },
                                onClick = {
                                    selectedFps = fps
                                    fpsExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Switch 1: Auto Focus
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Focus", tint = TextLightGrey, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Auto Focus", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Switch(
                    checked = isAutoFocusOn,
                    onCheckedChange = { isAutoFocusOn = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                )
            }
        }

        // Switch 2: Low Light Mode
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Light", tint = TextLightGrey, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Low Light Mode", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Switch(
                    checked = isLowLightModeOn,
                    onCheckedChange = { isLowLightModeOn = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                )
            }
        }

        // Switch 3: Image Stabilization
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MotionPhotosOn, contentDescription = "Stabilization", tint = TextLightGrey, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Image Stabilization", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Switch(
                    checked = isImageStabilizationOn,
                    onCheckedChange = { isImageStabilizationOn = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentBlue)
                )
            }
        }
    }
}

/**
 * App Information Sub-screen
 */
@Composable
private fun AppInfoSubScreen(
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 19.sp
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("PAVIESH AI Robotic Arm Controller", fontWeight = FontWeight.ExtraBold, color = TextWhite, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 1.0.0 (Release Build)", color = AccentCyan, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Features:\n" +
                                "• 6-DOF Robotic Arm USB OTG Host control at 9600 baud\n" +
                                "• Live Camera workspace stream with Target Tracking\n" +
                                "• Google Gemini Vision AI Step-by-Step Math Problem Solver\n" +
                                "• Bilingual Voice Assistant (Tamil, English, Tanglish)\n" +
                                "• Custom AI Persona System Instructions & Conversation History Context\n" +
                                "• Safety PING Watchdog and E-Stop interrupt protection",
                        color = TextLightGrey,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

/**
 * AI System Instructions & Memory Sub-screen
 */
@Composable
private fun AiInstructionsSubScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val customInstructions by viewModel.customSystemInstructions.collectAsState()
    val history by viewModel.conversationHistory.collectAsState()
    val context = LocalContext.current

    var textInput by remember(customInstructions) { mutableStateOf(customInstructions) }
    var isSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AI System Instructions & History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 19.sp
                )
            }
        }

        // Section 1: AI System Instructions Box
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppCardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom AI Persona & Instructions",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Define custom persona, behavioral rules, or instructions for Gemini AI during camera interaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedGrey,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            isSaved = false
                        },
                        placeholder = {
                            Text("e.g. Always reply politely in Tamil when asked in Tamil. Act as a robotics lab assistant.", color = TextMutedGrey, fontSize = 13.sp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF090F1E),
                            unfocusedContainerColor = Color(0xFF090F1E),
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = AppBorderSubtle,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.updateCustomSystemInstructions(textInput)
                            isSaved = true
                            Toast.makeText(context, "AI Instructions saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSaved) "✓ Instructions Saved" else "Save AI Instructions", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Conversation History Memory & Storage
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = "History", tint = AccentGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Stored Conversation Memory",
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${history.size} message turns in context",
                                    fontSize = 11.sp,
                                    color = AccentGreen
                                )
                            }
                        }

                        if (history.isNotEmpty()) {
                            Button(
                                onClick = {
                                    viewModel.clearConversationHistory()
                                    Toast.makeText(context, "Conversation history cleared", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B1212)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = AccentRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Memory", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (history.isEmpty()) {
                        Text(
                            text = "No conversation turns stored yet. Talk to Gemini AI in the Live Camera view to build history.",
                            color = TextMutedGrey,
                            fontSize = 12.sp
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            history.takeLast(6).forEach { msg ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (msg.role == "user") Color(0xFF132743) else Color(0xFF0D1E36),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (msg.role == "user") "👤 User Query" else "🤖 Gemini Live Response",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.role == "user") AccentCyan else AccentGreen
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.text,
                                            fontSize = 12.sp,
                                            color = TextWhite,
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

