package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.ArmPose
import com.example.data.ArmPreset
import com.example.data.AssistantLanguage
import com.example.data.MacroKeyframe
import com.example.data.SerialLogEntry
import com.example.data.ServoConfig
import com.example.data.TrackingTarget
import com.example.data.UsbStatus
import com.example.data.VoiceAssistantState
import com.example.robot.ServoControlManager
import com.example.usb.UsbSerialManager
import com.example.vision.TrackingEngine
import com.example.voice.RobotVoiceAction
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.ai.GeminiChatMessage

sealed interface AiAnalysisState {
    data object Idle : AiAnalysisState
    data class Loading(val message: String) : AiAnalysisState
    data class Success(
        val resultText: String,
        val isMathProblem: Boolean,
        val snapshot: Bitmap? = null
    ) : AiAnalysisState
    data class Error(val message: String) : AiAnalysisState
}

/**
 * State for the interactive Gemini Live section on the Camera screen.
 */
data class GeminiLiveState(
    val isActive: Boolean = false,
    val isListening: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isSpeaking: Boolean = false,
    val userQuery: String = "",
    val detectedLanguage: String = "en-US", // "ta-IN" or "en-US"
    val responseText: String = "",
    val spokenSummary: String = "",
    val searchQueries: List<String> = emptyList(),
    val webSources: List<Pair<String, String>> = emptyList(),
    val isGoogleSearched: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val usbSerialManager = UsbSerialManager(application, viewModelScope)
    val servoControlManager = ServoControlManager(usbSerialManager, viewModelScope)
    val geminiService = GeminiService()
    val trackingEngine = TrackingEngine()

    private val _currentTab = MutableStateFlow(0) // 0: Home, 1: Camera, 2: Arm, 3: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _aiState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiState: StateFlow<AiAnalysisState> = _aiState.asStateFlow()

    private val _geminiLiveState = MutableStateFlow(GeminiLiveState())
    val geminiLiveState: StateFlow<GeminiLiveState> = _geminiLiveState.asStateFlow()

    private val _customSystemInstructions = MutableStateFlow("You are a friendly, highly intelligent AI companion like ChatGPT & Gemini Live. You communicate naturally through live camera and voice in Tamil and English, answer general questions, solve math problems step-by-step, recognize people and hand gestures (👋 wave, 👍 thumbs up, 🖐️ open hand), and assist in safely controlling the six-servo Arduino arm via USB OTG.")
    val customSystemInstructions: StateFlow<String> = _customSystemInstructions.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<GeminiChatMessage>>(emptyList())
    val conversationHistory: StateFlow<List<GeminiChatMessage>> = _conversationHistory.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _isAutoTrackingArm = MutableStateFlow(false)
    val isAutoTrackingArm: StateFlow<Boolean> = _isAutoTrackingArm.asStateFlow()

    private var autoTrackingJob: Job? = null

    // Voice Assistant integration
    val voiceAssistantManager: VoiceAssistantManager = VoiceAssistantManager(
        context = application,
        onRobotCommandRecognized = { action ->
            executeVoiceRobotAction(action)
        },
        onConversationalQueryRecognized = { query, lang ->
            handleVoiceConversationalQuery(query, lang)
        }
    )

    // Proxy state flows from sub-managers for clean Composable consumption
    val usbStatus: StateFlow<UsbStatus> = usbSerialManager.usbStatus
    val serialLogs: StateFlow<List<SerialLogEntry>> = usbSerialManager.logs
    val servos: StateFlow<List<ServoConfig>> = servoControlManager.servos
    val currentPose: StateFlow<ArmPose> = servoControlManager.currentPose
    val isEmergencyStopped: StateFlow<Boolean> = servoControlManager.isEmergencyStopped
    val stepSize: StateFlow<Int> = servoControlManager.stepSize
    val presets: StateFlow<List<ArmPreset>> = servoControlManager.presets
    val isRecordingMacro: StateFlow<Boolean> = servoControlManager.isRecording
    val recordedKeyframes: StateFlow<List<MacroKeyframe>> = servoControlManager.recordedKeyframes
    val isPlayingMacro: StateFlow<Boolean> = servoControlManager.isPlayingMacro
    val macroLoop: StateFlow<Boolean> = servoControlManager.macroLoop

    val trackingTarget: StateFlow<TrackingTarget> = trackingEngine.target
    val isTrackingActive: StateFlow<Boolean> = trackingEngine.isTrackingActive
    val trackingMode: StateFlow<String> = trackingEngine.trackingMode
    val deadZoneRadius: StateFlow<Float> = trackingEngine.deadZoneRadius

    val assistantState: StateFlow<VoiceAssistantState> = voiceAssistantManager.assistantState
    val assistantLanguage: StateFlow<AssistantLanguage> = voiceAssistantManager.selectedLanguage
    val isTtsEnabled: StateFlow<Boolean> = voiceAssistantManager.isTtsEnabled

    init {
        // Start simulated vision drift for test mode demonstration
        viewModelScope.launch {
            var time = 0f
            while (isActive) {
                delay(80)
                time += 0.08f
                if (trackingEngine.isTrackingActive.value && trackingEngine.trackingMode.value == "AUTO_OBJECT") {
                    trackingEngine.simulateDrift(time)
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _currentTab.value = index.coerceIn(0, 3)
    }

    // Arm Control operations
    fun setServoAngle(id: Int, angle: Int) {
        servoControlManager.setServoAngle(id, angle)
    }

    fun stepDirection(dir: String) {
        servoControlManager.stepDirection(dir)
    }

    fun setStepSize(size: Int) {
        servoControlManager.setStepSize(size)
    }

    fun applyPreset(preset: ArmPreset) {
        servoControlManager.setPose(preset.pose)
    }

    fun emergencyStop() {
        servoControlManager.emergencyStop()
        stopAutoTracking()
    }

    fun home() {
        servoControlManager.home()
    }

    fun toggleGripper() {
        servoControlManager.toggleGripper()
    }

    fun updateCalibration(id: Int, min: Int, max: Int, trim: Int) {
        servoControlManager.updateCalibration(id, min, max, trim)
    }

    fun saveCurrentPoseAsPreset(name: String, subtitle: String) {
        servoControlManager.saveCurrentPoseAsPreset(name, subtitle)
    }

    fun deletePreset(id: String) {
        servoControlManager.deletePreset(id)
    }

    // Macro recorder operations
    fun toggleRecording() = servoControlManager.toggleRecording()
    fun addKeyframe(delayMs: Long = 800L) = servoControlManager.recordCurrentKeyframe(delayMs)
    fun removeKeyframe(id: String) = servoControlManager.removeKeyframe(id)
    fun clearKeyframes() = servoControlManager.clearKeyframes()
    fun playMacro() = servoControlManager.playMacro()
    fun stopMacro() = servoControlManager.stopMacroPlayback()
    fun toggleMacroLoop() = servoControlManager.toggleMacroLoop()

    // Camera & Gemini AI operations
    fun setCapturedBitmap(bitmap: Bitmap?) {
        _capturedBitmap.value = bitmap
    }

    fun runVisualQA(prompt: String) {
        val bitmap = _capturedBitmap.value
        viewModelScope.launch {
            _aiState.value = AiAnalysisState.Loading("Analyzing image with Gemini Vision...")
            val result = geminiService.analyzeImageWithPrompt(bitmap, prompt)
            result.fold(
                onSuccess = { answer ->
                    _aiState.value = AiAnalysisState.Success(
                        resultText = answer,
                        isMathProblem = false,
                        snapshot = bitmap
                    )
                },
                onFailure = { error ->
                    _aiState.value = AiAnalysisState.Error(error.localizedMessage ?: "Vision analysis failed")
                }
            )
        }
    }

    fun solveMathFromCamera(additionalNotes: String = "") {
        val bitmap = _capturedBitmap.value
        viewModelScope.launch {
            _aiState.value = AiAnalysisState.Loading("Solving mathematical problem step-by-step...")
            val result = geminiService.solveMathFromImage(bitmap, additionalNotes)
            result.fold(
                onSuccess = { mathSolution ->
                    _aiState.value = AiAnalysisState.Success(
                        resultText = mathSolution,
                        isMathProblem = true,
                        snapshot = bitmap
                    )
                    // Optionally speak out the summary
                    voiceAssistantManager.speak("Math solution ready. Check step-by-step solution on screen.")
                },
                onFailure = { error ->
                    _aiState.value = AiAnalysisState.Error(error.localizedMessage ?: "Math solving failed")
                }
            )
        }
    }

    fun clearAiResult() {
        _aiState.value = AiAnalysisState.Idle
    }

    // Tracking operations
    fun toggleTracking(enabled: Boolean) {
        trackingEngine.toggleTracking(enabled)
        if (!enabled) {
            stopAutoTracking()
        }
    }

    fun setTrackingMode(mode: String) {
        trackingEngine.setTrackingMode(mode)
    }

    fun updateTargetPosition(x: Float, y: Float) {
        trackingEngine.updateTargetPosition(x, y, "Selected Target")
    }

    fun toggleAutoTrackingArm(enabled: Boolean) {
        _isAutoTrackingArm.value = enabled
        if (enabled) {
            startAutoTracking()
        } else {
            stopAutoTracking()
        }
    }

    private fun startAutoTracking() {
        autoTrackingJob?.cancel()
        autoTrackingJob = viewModelScope.launch {
            while (isActive) {
                delay(300) // Control frequency ~3Hz
                if (!trackingEngine.isTrackingActive.value || servoControlManager.isEmergencyStopped.value) continue

                val target = trackingEngine.target.value
                if (!target.isLocked) {
                    val pose = servoControlManager.currentPose.value
                    if (target.deltaBase != 0) {
                        val newBase = (pose.s1Base + target.deltaBase).coerceIn(0, 180)
                        servoControlManager.setServoAngle(1, newBase)
                    }
                    if (target.deltaShoulder != 0) {
                        val newShoulder = (pose.s2Shoulder + target.deltaShoulder).coerceIn(15, 165)
                        servoControlManager.setServoAngle(2, newShoulder)
                    }
                }
            }
        }
    }

    private fun stopAutoTracking() {
        autoTrackingJob?.cancel()
        autoTrackingJob = null
        _isAutoTrackingArm.value = false
    }

    // Voice command dispatch with strict safety verification
    private fun executeVoiceRobotAction(action: RobotVoiceAction) {
        when (action) {
            RobotVoiceAction.EmergencyStop -> emergencyStop()
            RobotVoiceAction.Home -> home()
            RobotVoiceAction.GripperOpen -> servoControlManager.setServoAngle(6, 85)
            RobotVoiceAction.GripperClose -> servoControlManager.setServoAngle(6, 15)
            is RobotVoiceAction.Preset -> {
                val found = servoControlManager.presets.value.firstOrNull { it.id == action.presetId }
                if (found != null) {
                    applyPreset(found)
                }
            }
            is RobotVoiceAction.SetServoAngle -> {
                servoControlManager.setServoAngle(action.servoId, action.angle)
            }
        }
    }

    private fun handleVoiceConversationalQuery(query: String, detectedLang: String) {
        val bitmap = _capturedBitmap.value
        triggerGeminiLive(bitmap = bitmap, userSpokenQuery = query, preferredLangCode = detectedLang)
    }

    fun updateCustomSystemInstructions(instructions: String) {
        _customSystemInstructions.value = instructions
    }

    fun clearConversationHistory() {
        _conversationHistory.value = emptyList()
    }

    private fun addChatMessage(role: String, text: String) {
        if (text.isBlank()) return
        val current = _conversationHistory.value.toMutableList()
        current.add(GeminiChatMessage(role = role, text = text))
        _conversationHistory.value = current
    }

    /**
     * Executes the Gemini Live Section workflow:
     * 1. Captures live camera workspace snapshot
     * 2. Multimodal analysis via Gemini AI (gemini-3.8-flash)
     * 3. Real-time Google Search grounding
     * 4. Bilingual speech response (Tamil / English) via Text-To-Speech
     * 5. Retains conversation history context across turns
     */
    fun triggerGeminiLive(
        bitmap: Bitmap?,
        userSpokenQuery: String,
        preferredLangCode: String = "auto",
        enableGoogleSearch: Boolean = true
    ) {
        viewModelScope.launch {
            _geminiLiveState.value = GeminiLiveState(
                isActive = true,
                isListening = false,
                isAnalyzing = true,
                isSpeaking = false,
                userQuery = userSpokenQuery,
                detectedLanguage = preferredLangCode,
                errorMessage = null
            )

            val result = geminiService.queryGeminiLiveWithVision(
                bitmap = bitmap,
                spokenQuery = userSpokenQuery,
                preferredLanguage = preferredLangCode,
                enableGoogleSearch = enableGoogleSearch,
                customSystemInstructions = _customSystemInstructions.value,
                conversationHistory = _conversationHistory.value
            )

            result.fold(
                onSuccess = { liveRes ->
                    _geminiLiveState.value = _geminiLiveState.value.copy(
                        isActive = true,
                        isAnalyzing = false,
                        isSpeaking = true,
                        responseText = liveRes.responseText,
                        spokenSummary = liveRes.spokenSummary,
                        detectedLanguage = liveRes.detectedLanguage,
                        searchQueries = liveRes.searchQueries,
                        webSources = liveRes.webSources,
                        isGoogleSearched = liveRes.isGoogleSearched
                    )

                    // Retain user and AI turns in conversation history
                    addChatMessage("user", userSpokenQuery)
                    addChatMessage("model", liveRes.responseText)

                    // Voice speech: speaks in Tamil if Tamil detected, or English if English
                    val textToSpeak = liveRes.spokenSummary.ifBlank { liveRes.responseText }
                    voiceAssistantManager.speak(textToSpeak, liveRes.detectedLanguage)
                },
                onFailure = { error ->
                    _geminiLiveState.value = _geminiLiveState.value.copy(
                        isActive = true,
                        isAnalyzing = false,
                        errorMessage = error.localizedMessage ?: "Gemini Live request failed"
                    )
                    val fallback = if (preferredLangCode == "ta-IN") {
                        "மன்னிக்கவும், ஜெமினியுடன் இணைக்க முடியவில்லை."
                    } else {
                        "Sorry, could not connect to Gemini Live."
                    }
                    voiceAssistantManager.speak(fallback, preferredLangCode)
                }
            )
        }
    }

    fun dismissGeminiLive() {
        voiceAssistantManager.stopSpeaking()
        _geminiLiveState.value = GeminiLiveState(isActive = false)
    }

    fun replayGeminiLiveSpeech() {
        val state = _geminiLiveState.value
        val text = state.spokenSummary.ifBlank { state.responseText }
        if (text.isNotBlank()) {
            voiceAssistantManager.speak(text, state.detectedLanguage)
        }
    }

    fun stopGeminiLiveSpeech() {
        voiceAssistantManager.stopSpeaking()
        _geminiLiveState.value = _geminiLiveState.value.copy(isSpeaking = false)
    }

    fun sendRawSerialCommand(cmd: String) {
        usbSerialManager.sendCommand(cmd)
    }

    fun clearSerialLogs() {
        usbSerialManager.clearLogs()
    }

    fun toggleSimulationMode(enable: Boolean) {
        usbSerialManager.toggleSimulationMode(enable)
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoTracking()
        usbSerialManager.cleanup()
        voiceAssistantManager.cleanup()
    }
}
