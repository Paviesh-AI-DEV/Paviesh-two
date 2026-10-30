package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ArmControlScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AppBackgroundDark
import com.example.ui.theme.AppSurfaceDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextWhite

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    // Request Camera and Audio permissions at launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    // Predictive Back / Back button returns to Home screen if on secondary tabs
    BackHandler(enabled = currentTab != 0) {
        viewModel.selectTab(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom navigation bar matching Screen 1 (Home, Arm Control, Settings)
            // Hide bottom bar in full-screen camera view (tab 1) to match Screen 2!
            if (currentTab != 1) {
                NavigationBar(
                    containerColor = AppSurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    val navItems = listOf(
                        Triple(0, "Home", Icons.Default.Home),
                        Triple(2, "Arm Control", Icons.Default.PrecisionManufacturing),
                        Triple(3, "Settings", Icons.Default.Settings)
                    )

                    navItems.forEach { (tabIndex, title, icon) ->
                        val isSelected = currentTab == tabIndex
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tabIndex) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentCyan,
                                selectedTextColor = AccentCyan,
                                unselectedIconColor = TextMutedGrey,
                                unselectedTextColor = TextMutedGrey,
                                indicatorColor = AccentBlue.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag("nav_tab_$tabIndex")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentTab == 1) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                .background(AppBackgroundDark)
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToCamera = { viewModel.selectTab(1) },
                    onNavigateToArm = { viewModel.selectTab(2) },
                    onNavigateToSettings = { viewModel.selectTab(3) }
                )
                1 -> CameraScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.selectTab(0) }
                )
                2 -> ArmControlScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.selectTab(0) }
                )
                3 -> SettingsScreen(
                    viewModel = viewModel,
                    onBackToHome = { viewModel.selectTab(0) }
                )
            }
        }
    }
}
