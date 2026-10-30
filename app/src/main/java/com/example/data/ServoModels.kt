package com.example.data

/**
 * Data models for the 6-DOF Robotic Arm, USB Status, Telemetry, and Assistant.
 */

data class ServoConfig(
    val id: Int,                     // 1 to 6
    val name: String,                // e.g., "Base", "Shoulder", "Elbow", "Wrist Pitch", "Wrist Roll", "Gripper"
    val motorModel: String,          // "MG996R" (S1, S2, S3) or "SG90" (S4, S5, S6)
    val pin: Int,                    // Digital PWM Pin on Arduino (3, 5, 6, 9, 10, 11)
    val currentAngle: Int = 90,      // Degrees (0 to 180)
    val targetAngle: Int = 90,       // Commanded angle
    val minAngle: Int = 0,           // Soft lower limit
    val maxAngle: Int = 180,         // Soft upper limit
    val trimOffset: Int = 0,         // Calibration offset in degrees (-20 to +20)
    val description: String = ""
) {
    val effectiveAngle: Int
        get() = (currentAngle + trimOffset).coerceIn(minAngle, maxAngle)
}

data class ArmPose(
    val s1Base: Int = 90,
    val s2Shoulder: Int = 90,
    val s3Elbow: Int = 90,
    val s4WristPitch: Int = 90,
    val s5WristRoll: Int = 90,
    val s6Gripper: Int = 45,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toAngleList(): List<Int> = listOf(s1Base, s2Shoulder, s3Elbow, s4WristPitch, s5WristRoll, s6Gripper)

    companion object {
        val HOME = ArmPose(90, 90, 90, 90, 90, 45)
        val REST = ArmPose(90, 30, 150, 90, 90, 10)
        val PICK_READY = ArmPose(90, 130, 45, 120, 90, 85)
        val LIFT = ArmPose(90, 90, 60, 100, 90, 15)
        val PLACE_BOX = ArmPose(150, 110, 55, 110, 90, 85)
        val INSPECT = ArmPose(90, 75, 105, 90, 90, 45)
    }
}

data class ArmPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val pose: ArmPose,
    val isSystemPreset: Boolean = true
)

data class MacroKeyframe(
    val id: String,
    val pose: ArmPose,
    val delayMs: Long = 800L
)

data class MacroSequence(
    val id: String,
    val name: String,
    val keyframes: List<MacroKeyframe>,
    val loop: Boolean = false
)

data class TrackingTarget(
    val centerX: Float = 0.5f,       // Normalized 0.0 to 1.0 (X in camera frame)
    val centerY: Float = 0.5f,       // Normalized 0.0 to 1.0 (Y in camera frame)
    val width: Float = 0.2f,
    val height: Float = 0.2f,
    val label: String = "Target",
    val isLocked: Boolean = false,
    val deltaBase: Int = 0,          // Calculated base angle correction
    val deltaShoulder: Int = 0       // Calculated shoulder/elbow correction
)

sealed interface UsbStatus {
    data object Disconnected : UsbStatus
    data object Connecting : UsbStatus
    data class Connected(
        val deviceName: String,
        val vendorId: Int,
        val productId: Int,
        val baudRate: Int
    ) : UsbStatus
    data class Simulation(
        val baudRate: Int = 115200
    ) : UsbStatus
    data class Error(val message: String) : UsbStatus
}

data class SerialLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val direction: LogDirection,
    val content: String
)

enum class LogDirection {
    TX, // Transmitted from Android
    RX  // Received from Arduino
}

sealed interface VoiceAssistantState {
    data object Idle : VoiceAssistantState
    data class Listening(val language: String) : VoiceAssistantState
    data class Processing(val spokenQuery: String) : VoiceAssistantState
    data class Speaking(val message: String, val language: String) : VoiceAssistantState
    data class ActionExecuted(val actionName: String, val details: String) : VoiceAssistantState
    data class Error(val message: String) : VoiceAssistantState
}

enum class AssistantLanguage(val displayName: String, val code: String) {
    AUTO("Auto Detect", "auto"),
    ENGLISH("English", "en-US"),
    TAMIL("தமிழ் (Tamil)", "ta-IN"),
    TANGLISH("Tanglish (Tamil-English)", "tanglish")
}
