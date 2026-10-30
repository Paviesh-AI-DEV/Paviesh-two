package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.AiAnalysisState
import com.example.GeminiLiveState
import com.example.MainViewModel
import com.example.data.AssistantLanguage
import com.example.data.VoiceAssistantState
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AppBorderGlowing
import com.example.ui.theme.AppBorderSubtle
import com.example.ui.theme.AppCardDark
import com.example.ui.theme.AppSurfaceDark
import com.example.ui.theme.TextLightGrey
import com.example.ui.theme.TextMutedGrey
import com.example.ui.theme.TextWhite
import com.example.ui.theme.WarningAmber
import java.nio.ByteBuffer

@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val assistantState by viewModel.assistantState.collectAsState()
    val assistantLanguage by viewModel.assistantLanguage.collectAsState()
    val aiState by viewModel.aiState.collectAsState()
    val geminiLiveState by viewModel.geminiLiveState.collectAsState()
    val customSystemInstructions by viewModel.customSystemInstructions.collectAsState()
    val conversationHistory by viewModel.conversationHistory.collectAsState()

    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }

    var showAiOverlay by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Helper to grab instantaneous frame from previewView (or fallback imageCapture)
    fun grabCurrentFrame(onReady: (Bitmap) -> Unit) {
        val bmp = previewView.bitmap
        if (bmp != null) {
            viewModel.setCapturedBitmap(bmp)
            onReady(bmp)
        } else {
            captureFrameBitmap(imageCapture, context) { captured ->
                viewModel.setCapturedBitmap(captured)
                onReady(captured)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. CameraX Preview Feed (Clean, unobstructed live workspace view)
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_camera_view")
        )

        // 2. Top Floating Controls: Back button, Torch, Bilingual Language Pill, Solve Math
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }

                // Bilingual Speech Language Selector: [ Auto | English | தமிழ் ]
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            AssistantLanguage.AUTO to "Auto",
                            AssistantLanguage.ENGLISH to "EN",
                            AssistantLanguage.TAMIL to "தமிழ்"
                        ).forEach { (lang, label) ->
                            val isSelected = assistantLanguage == lang
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) AccentCyan.copy(alpha = 0.3f) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AccentCyan) else null,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.voiceAssistantManager.setLanguage(lang)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) AccentCyan else TextLightGrey,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Torch toggle
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            camera?.cameraControl?.enableTorch(isTorchOn)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isTorchOn) WarningAmber else Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = "Torch")
                    }

                    // Step-by-Step Math Solver
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF13366D).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue),
                        modifier = Modifier
                            .height(40.dp)
                            .clickable {
                                grabCurrentFrame { bitmap ->
                                    viewModel.solveMathFromCamera()
                                    showAiOverlay = true
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Math", tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Solve Math", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-badge: Gemini AI Live indicator with Google Search & Gesture Perception
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                        Text(
                            text = "GEMINI LIVE COMPANION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentCyan,
                            maxLines = 1
                        )
                        Text("•", color = TextMutedGrey, fontSize = 10.sp)
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AccentGreen,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Google Grounded",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentGreen,
                            maxLines = 1
                        )
                        Text("•", color = TextMutedGrey, fontSize = 10.sp)
                        Text(
                            text = "👋 Gestures Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 3. Step-by-step Math Solution Overlay (if triggered via Solve Math)
        if (showAiOverlay && aiState !is AiAnalysisState.Idle) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurfaceDark.copy(alpha = 0.95f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = AccentCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini Step-by-Step Math Solution", fontWeight = FontWeight.Bold, color = TextWhite)
                        }
                        Row {
                            if (aiState is AiAnalysisState.Success) {
                                IconButton(
                                    onClick = {
                                        viewModel.voiceAssistantManager.speak((aiState as AiAnalysisState.Success).resultText)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read aloud", tint = AccentCyan)
                                }
                            }
                            IconButton(
                                onClick = {
                                    showAiOverlay = false
                                    viewModel.clearAiResult()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextLightGrey)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = aiState) {
                        is AiAnalysisState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(state.message, color = TextLightGrey)
                                }
                            }
                        }
                        is AiAnalysisState.Success -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = state.resultText,
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                        is AiAnalysisState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "⚠️ ${state.message}", color = WarningAmber)
                            }
                        }
                        AiAnalysisState.Idle -> Unit
                    }
                }
            }
        }

        // 4. Interactive GEMINI LIVE Overlay Card (Bilingual English & Tamil + Google Search Grounded)
        AnimatedVisibility(
            visible = geminiLiveState.isActive,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 14.dp, end = 14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurfaceDark.copy(alpha = 0.96f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 340.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Card Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(AccentCyan, AccentBlue))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Gemini Live Vision",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextWhite,
                                        fontSize = 14.sp
                                    )
                                    if (conversationHistory.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AccentGreen.copy(alpha = 0.25f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen)
                                        ) {
                                            Text(
                                                text = "${conversationHistory.size} Turns",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentGreen,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (geminiLiveState.detectedLanguage == "ta-IN") "தமிழ் (Tamil Spoken Mode)" else "English Spoken Mode",
                                    fontSize = 11.sp,
                                    color = AccentCyan
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (geminiLiveState.isSpeaking) {
                                IconButton(
                                    onClick = { viewModel.stopGeminiLiveSpeech() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = AccentRed)
                                }
                            } else if (geminiLiveState.responseText.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.replayGeminiLiveSpeech() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay", tint = AccentCyan)
                                }
                            }

                            IconButton(
                                onClick = { viewModel.dismissGeminiLive() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextLightGrey)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // User Spoken Prompt Display
                    if (geminiLiveState.userQuery.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF090F1E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "User", tint = AccentCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "\"${geminiLiveState.userQuery}\"",
                                    color = TextLightGrey,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Dynamic Audio Waveform or Loading State
                    if (geminiLiveState.isAnalyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (geminiLiveState.detectedLanguage == "ta-IN") "கூகுளில் தேடி, ஜெமினி ஆய்வு செய்கிறது..." else "Searching Google & reasoning with Gemini...",
                                    fontSize = 12.sp,
                                    color = TextLightGrey
                                )
                            }
                        }
                    } else if (geminiLiveState.errorMessage != null) {
                        Text(
                            text = "⚠️ ${geminiLiveState.errorMessage}",
                            color = WarningAmber,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        // Response text with Google Search chips and audio wave
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Speaking waveform visualizer
                            if (geminiLiveState.isSpeaking) {
                                GeminiLiveAudioWave(language = geminiLiveState.detectedLanguage)
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Google Search Grounding badges
                            if (geminiLiveState.isGoogleSearched && geminiLiveState.searchQueries.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentGreen, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "Google Search:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen
                                    )
                                    geminiLiveState.searchQueries.take(2).forEach { query ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF09291C),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = query,
                                                fontSize = 10.sp,
                                                color = AccentGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = geminiLiveState.responseText,
                                color = TextWhite,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Action Row inside Card: [ Ask Again (Mic) ] | [ Stop ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentBlue.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clickable {
                                    grabCurrentFrame { bmp ->
                                        viewModel.voiceAssistantManager.startListening(assistantLanguage)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Mic", tint = AccentCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask Another Question", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }

                        if (geminiLiveState.isSpeaking) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF381010),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed),
                                modifier = Modifier.clickable { viewModel.stopGeminiLiveSpeech() }
                            ) {
                                Text(
                                    text = "Stop Audio",
                                    fontSize = 11.sp,
                                    color = AccentRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Floating Bottom Control Bar matching reference screenshot:
        // Left: "Voice" with mic icon
        // Center: "Voice Control / Gemini Live" with pulsing animated ring
        // Right: "Flip" camera flip icon
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xD90D1424),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .height(72.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Action: Voice Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (assistantState is VoiceAssistantState.Listening) {
                            viewModel.voiceAssistantManager.stopListening()
                        } else {
                            grabCurrentFrame { bmp ->
                                viewModel.voiceAssistantManager.startListening(assistantLanguage)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = if (assistantState is VoiceAssistantState.Listening) AccentCyan else TextLightGrey,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Voice",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLightGrey,
                        fontSize = 11.sp
                    )
                }

                // Center Action: Voice Control / Gemini Live with pulsating wave ring
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (assistantState is VoiceAssistantState.Listening || geminiLiveState.isListening) 1.18f else 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (assistantState is VoiceAssistantState.Listening) {
                            viewModel.voiceAssistantManager.stopListening()
                        } else {
                            grabCurrentFrame { bmp ->
                                viewModel.voiceAssistantManager.startListening(assistantLanguage)
                            }
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(if (assistantState is VoiceAssistantState.Listening) AccentBlue else Color(0xFF1E3A6C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini Live",
                            tint = if (assistantState is VoiceAssistantState.Listening) AccentCyan else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (assistantState is VoiceAssistantState.Listening) {
                            if (assistantLanguage == AssistantLanguage.TAMIL) "பேசுங்கள்... அவசரமில்லை" else "Listening... Take your time"
                        } else "Gemini Live Voice",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (assistantState is VoiceAssistantState.Listening) AccentCyan else TextWhite,
                        fontSize = 11.sp
                    )
                }

                // Right Action: Flip camera
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip",
                        tint = TextLightGrey,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Flip",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLightGrey,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Animated sound wave visualizer for Gemini Live speaking in English or Tamil.
 */
@Composable
private fun GeminiLiveAudioWave(language: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val heights = listOf(
        infiniteTransition.animateFloat(initialValue = 6f, targetValue = 20f, animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "h1"),
        infiniteTransition.animateFloat(initialValue = 18f, targetValue = 8f, animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "h2"),
        infiniteTransition.animateFloat(initialValue = 10f, targetValue = 24f, animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "h3"),
        infiniteTransition.animateFloat(initialValue = 22f, targetValue = 10f, animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "h4"),
        infiniteTransition.animateFloat(initialValue = 8f, targetValue = 18f, animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "h5")
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF09142A),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speaking",
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == "ta-IN") "தமிழ்க் குரலில் பேசுகிறது..." else "Speaking aloud in English...",
                    fontSize = 11.sp,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                heights.forEach { hState ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(hState.value.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentCyan)
                    )
                }
            }
        }
    }
}

private fun captureFrameBitmap(
    imageCapture: ImageCapture?,
    context: android.content.Context,
    onBitmapReady: (Bitmap) -> Unit
) {
    val capture = imageCapture ?: return
    capture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val rotation = image.imageInfo.rotationDegrees
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                    onBitmapReady(rotated)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}
