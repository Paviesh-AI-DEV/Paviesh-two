package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.AssistantLanguage
import com.example.data.VoiceAssistantState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Bilingual Voice Assistant Manager supporting English, Tamil (தமிழ்), and Tanglish.
 * Features:
 * - Speech recognition via Android SpeechRecognizer
 * - Strict validation of explicit robot commands (prevents AI chat from triggering accidental arm movements)
 * - Text-To-Speech (TTS) audio feedback
 */
class VoiceAssistantManager(
    private val context: Context,
    private val onRobotCommandRecognized: (RobotVoiceAction) -> Unit,
    private val onConversationalQueryRecognized: (query: String, detectedLang: String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceAssistantManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _assistantState = MutableStateFlow<VoiceAssistantState>(VoiceAssistantState.Idle)
    val assistantState: StateFlow<VoiceAssistantState> = _assistantState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AssistantLanguage.AUTO)
    val selectedLanguage: StateFlow<AssistantLanguage> = _selectedLanguage.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    init {
        initSpeechRecognizer()
        tts = TextToSpeech(context, this)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceAssistantManager)
            }
        } else {
            Log.w(TAG, "Speech recognition not available on device")
        }
    }

    fun setLanguage(language: AssistantLanguage) {
        _selectedLanguage.value = language
    }

    fun toggleTts(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        if (!enabled) {
            tts?.stop()
        }
    }

    /**
     * Start listening for voice input with optional forced language (English or Tamil).
     */
    fun startListening(forcedLanguage: AssistantLanguage? = null) {
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }
        val recognizer = speechRecognizer ?: run {
            _assistantState.value = VoiceAssistantState.Error("Speech recognizer unavailable")
            return
        }

        val langToUse = forcedLanguage ?: _selectedLanguage.value
        val langCode = when (langToUse) {
            AssistantLanguage.ENGLISH -> "en-US"
            AssistantLanguage.TAMIL -> "ta-IN"
            AssistantLanguage.TANGLISH -> "ta-IN"
            AssistantLanguage.AUTO -> "en-US"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, langCode)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Extended listening timeouts: give user ample time to think and speak without early cutoff
            putExtra("android.speech.extra.DICTATION_MODE", true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3500L)
            // Extra support for Tamil / multilingual recognition
            if (langCode == "ta-IN") {
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ta-IN", "en-IN"))
            }
        }

        try {
            recognizer.startListening(intent)
            _assistantState.value = VoiceAssistantState.Listening(langToUse.displayName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting speech recognizer", e)
            _assistantState.value = VoiceAssistantState.Error("Failed to start listening: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        if (_assistantState.value is VoiceAssistantState.Listening) {
            _assistantState.value = VoiceAssistantState.Idle
        }
    }

    // SpeechRecognizer Callbacks
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        if (_assistantState.value is VoiceAssistantState.Listening) {
            _assistantState.value = VoiceAssistantState.Processing("Analyzing speech...")
        }
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly."
            SpeechRecognizer.ERROR_NETWORK -> "Network error in speech recognition."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
            else -> "Speech recognition error ($error)"
        }
        _assistantState.value = VoiceAssistantState.Error(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val spokenText = matches?.firstOrNull()?.trim() ?: ""
        if (spokenText.isBlank()) {
            _assistantState.value = VoiceAssistantState.Idle
            return
        }

        processSpokenInput(spokenText)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!partial.isNullOrBlank()) {
            _assistantState.value = VoiceAssistantState.Processing(partial)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    /**
     * Process spoken input with strict safety classification.
     * Prevents conversational AI chat from triggering accidental arm movements.
     */
    fun processSpokenInput(text: String) {
        _assistantState.value = VoiceAssistantState.Processing(text)
        val lower = text.lowercase(Locale.ROOT)
        val detectedLang = detectLanguage(text)

        // Strict validation for physical robot actuator actions
        val robotAction = parseExplicitRobotAction(lower)
        if (robotAction != null) {
            val responseSpeech = getActionFeedbackSpeech(robotAction, detectedLang)
            _assistantState.value = VoiceAssistantState.ActionExecuted(robotAction.title, text)
            onRobotCommandRecognized(robotAction)
            speak(responseSpeech, detectedLang)
        } else {
            // Conversational query: Route safely to Gemini AI with ZERO arm movement
            onConversationalQueryRecognized(text, detectedLang)
        }
    }

    /**
     * Strict explicit robot command validator.
     * Evaluates Tamil, English, and Tanglish phrases.
     */
    private fun parseExplicitRobotAction(text: String): RobotVoiceAction? {
        // 1. Emergency Stop commands
        if (text.contains("stop") || text.contains("emergency") || text.contains("நிறுத்து") || 
            text.contains("halt") || text.contains("freeze") || text.contains("stop pannu")) {
            return RobotVoiceAction.EmergencyStop
        }

        // 2. Home Position commands
        if (text.contains("home") || text.contains("ஹோம்") || text.contains("வீடு") || 
            text.contains("reset") || text.contains("home position") || text.contains("home ku po") ||
            text.contains("home-ku")) {
            return RobotVoiceAction.Home
        }

        // 3. Gripper Open commands
        if ((text.contains("gripper") && (text.contains("open") || text.contains("release") || text.contains("thira") || text.contains("திற"))) ||
            text.contains("open claw") || text.contains("கிரிப்பர் திற") || text.contains("gripper open pannu")) {
            return RobotVoiceAction.GripperOpen
        }

        // 4. Gripper Close commands
        if ((text.contains("gripper") && (text.contains("close") || text.contains("grip") || text.contains("moodu") || text.contains("மூடு"))) ||
            text.contains("close claw") || text.contains("கிரிப்பர் மூடு") || text.contains("gripper close pannu")) {
            return RobotVoiceAction.GripperClose
        }

        // 5. Presets
        if (text.contains("rest") || text.contains("தூங்கு") || text.contains("sleep")) {
            return RobotVoiceAction.Preset("preset_rest", "Rest Pose")
        }
        if (text.contains("pick") || text.contains("எடு") || text.contains("pick ground")) {
            return RobotVoiceAction.Preset("preset_pick", "Pick Ground")
        }
        if (text.contains("place") || text.contains("வை") || text.contains("place box")) {
            return RobotVoiceAction.Preset("preset_place", "Place Box")
        }
        if (text.contains("inspect") || text.contains("பார்") || text.contains("check camera")) {
            return RobotVoiceAction.Preset("preset_inspect", "Inspect Camera")
        }

        // 6. Explicit joint angles: e.g. "base 90", "set s1 to 45", "பேஸ் 90"
        val angleMatch = Regex("""(base|shoulder|elbow|wrist|s1|s2|s3|s4|s5|s6|பேஸ்|தோள்பட்டை)\s*(?:to|set|degree|டிகிரி)?\s*(\d{1,3})""").find(text)
        if (angleMatch != null) {
            val joint = angleMatch.groupValues[1]
            val angle = angleMatch.groupValues[2].toIntOrNull() ?: 90
            val servoId = when (joint) {
                "base", "s1", "பேஸ்" -> 1
                "shoulder", "s2", "தோள்பட்டை" -> 2
                "elbow", "s3" -> 3
                "wrist", "s4" -> 4
                "s5" -> 5
                "s6" -> 6
                else -> 1
            }
            return RobotVoiceAction.SetServoAngle(servoId, angle)
        }

        return null
    }

    private fun detectLanguage(text: String): String {
        // If text contains Tamil Unicode block (\u0B80-\u0BFF)
        val hasTamilChars = text.any { it in '\u0B80'..'\u0BFF' }
        if (hasTamilChars) return "ta-IN"

        // Check for common Tanglish words
        val tanglishKeywords = listOf("pannu", "kondu", "po", "thira", "moodu", "eppadi", "romba", "seri", "kitta", "mela", "keela")
        val isTanglish = tanglishKeywords.any { text.lowercase().contains(it) }
        if (isTanglish) return "tanglish"

        return "en-US"
    }

    private fun getActionFeedbackSpeech(action: RobotVoiceAction, language: String): String {
        val isTamilOrTanglish = language == "ta-IN" || language == "tanglish"
        return when (action) {
            RobotVoiceAction.EmergencyStop -> if (isTamilOrTanglish) "கை உடனே நிறுத்தப்பட்டது!" else "Emergency stop activated!"
            RobotVoiceAction.Home -> if (isTamilOrTanglish) "ஹோம் பொசிஷனுக்கு நகர்கிறது." else "Moving arm to home position."
            RobotVoiceAction.GripperOpen -> if (isTamilOrTanglish) "கிரிப்பர் திறக்கப்பட்டது." else "Gripper opened."
            RobotVoiceAction.GripperClose -> if (isTamilOrTanglish) "கிரிப்பர் மூடப்பட்டது." else "Gripper closed."
            is RobotVoiceAction.Preset -> if (isTamilOrTanglish) "${action.presetName} நிலைக்கு செல்கிறது." else "Applying preset ${action.presetName}."
            is RobotVoiceAction.SetServoAngle -> if (isTamilOrTanglish) "சர்வோ ${action.servoId} ${action.angle} டிகிரிக்கு மாற்றப்பட்டது." else "Setting servo ${action.servoId} to ${action.angle} degrees."
        }
    }

    /**
     * Text-To-Speech output with automatic English and Tamil vocal switching.
     */
    fun speak(text: String, languageCode: String = "auto") {
        if (!_isTtsEnabled.value || !isTtsReady || text.isBlank()) return

        val hasTamilChars = text.any { it in '\u0B80'..'\u0BFF' }
        val effectiveLang = if (languageCode == "ta-IN" || hasTamilChars) "ta-IN" else "en-US"

        _assistantState.value = VoiceAssistantState.Speaking(text, effectiveLang)
        try {
            val locale = if (effectiveLang == "ta-IN") Locale.forLanguageTag("ta-IN") else Locale.US
            val langResult = tts?.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Locale $locale not fully supported by TTS engine, falling back to default")
                tts?.setLanguage(Locale.US)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PAVIESH_VOICE_UTTERANCE")
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        if (_assistantState.value is VoiceAssistantState.Speaking) {
            _assistantState.value = VoiceAssistantState.Idle
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setSpeechRate(0.95f)
        } else {
            Log.e(TAG, "TextToSpeech init failed ($status)")
        }
    }

    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

sealed interface RobotVoiceAction {
    val title: String

    data object EmergencyStop : RobotVoiceAction {
        override val title = "Emergency Stop"
    }
    data object Home : RobotVoiceAction {
        override val title = "Home Position"
    }
    data object GripperOpen : RobotVoiceAction {
        override val title = "Open Gripper"
    }
    data object GripperClose : RobotVoiceAction {
        override val title = "Close Gripper"
    }
    data class Preset(val presetId: String, val presetName: String) : RobotVoiceAction {
        override val title = "Preset: $presetName"
    }
    data class SetServoAngle(val servoId: Int, val angle: Int) : RobotVoiceAction {
        override val title = "Servo $servoId -> $angle°"
    }
}
