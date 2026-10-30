package com.example.robot

import com.example.data.ArmPose
import com.example.data.ArmPreset
import com.example.data.MacroKeyframe
import com.example.data.MacroSequence
import com.example.data.SerialCommand
import com.example.data.ServoConfig
import com.example.usb.UsbSerialManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * High-level Controller for the 6-DOF Robotic Arm.
 * Manages kinematic state, soft angle clamping, calibration trims, presets,
 * Cartesian directional stepping, and macro recording & playback.
 */
class ServoControlManager(
    private val usbSerialManager: UsbSerialManager,
    private val scope: CoroutineScope
) {
    // Initial 6-DOF servo configurations with safety limits, motor models (3x MG996R + 3x SG90), and Arduino PWM pins
    private val defaultServos = listOf(
        ServoConfig(id = 1, name = "Base", motorModel = "MG996R", pin = 3, currentAngle = 90, minAngle = 0, maxAngle = 180, description = "Waist rotation (MG996R High Torque)"),
        ServoConfig(id = 2, name = "Shoulder", motorModel = "MG996R", pin = 5, currentAngle = 90, minAngle = 15, maxAngle = 165, description = "Shoulder elevation (MG996R High Torque)"),
        ServoConfig(id = 3, name = "Elbow", motorModel = "MG996R", pin = 6, currentAngle = 90, minAngle = 15, maxAngle = 165, description = "Forearm pitch (MG996R High Torque)"),
        ServoConfig(id = 4, name = "Wrist Pitch", motorModel = "SG90", pin = 9, currentAngle = 90, minAngle = 0, maxAngle = 180, description = "Wrist tilt (SG90 Micro Servo)"),
        ServoConfig(id = 5, name = "Wrist Roll", motorModel = "SG90", pin = 10, currentAngle = 90, minAngle = 0, maxAngle = 180, description = "Wrist axial rotation (SG90 Micro Servo)"),
        ServoConfig(id = 6, name = "Gripper", motorModel = "SG90", pin = 11, currentAngle = 45, minAngle = 0, maxAngle = 90, description = "Claw manipulator (SG90 Micro Servo)")
    )

    private val _servos = MutableStateFlow(defaultServos)
    val servos: StateFlow<List<ServoConfig>> = _servos.asStateFlow()

    private val _currentPose = MutableStateFlow(ArmPose.HOME)
    val currentPose: StateFlow<ArmPose> = _currentPose.asStateFlow()

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    private val _stepSize = MutableStateFlow(5)
    val stepSize: StateFlow<Int> = _stepSize.asStateFlow()

    // Built-in presets
    private val defaultPresets = listOf(
        ArmPreset("preset_home", "Home Position", "Default upright standby pose", ArmPose.HOME),
        ArmPreset("preset_rest", "Rest / Sleep", "Folded safely to minimize torque on joints", ArmPose.REST),
        ArmPreset("preset_pick", "Pick Ground", "Extended low with open gripper ready to grab", ArmPose.PICK_READY),
        ArmPreset("preset_lift", "Lift Object", "Elevated cargo hold with closed gripper", ArmPose.LIFT),
        ArmPreset("preset_place", "Place Box", "Rotated 60° to container unload zone", ArmPose.PLACE_BOX),
        ArmPreset("preset_inspect", "Inspect Camera", "Aligned horizontally for camera inspection", ArmPose.INSPECT)
    )

    private val _presets = MutableStateFlow(defaultPresets)
    val presets: StateFlow<List<ArmPreset>> = _presets.asStateFlow()

    // Macro Recording & Sequencer state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedKeyframes = MutableStateFlow<List<MacroKeyframe>>(emptyList())
    val recordedKeyframes: StateFlow<List<MacroKeyframe>> = _recordedKeyframes.asStateFlow()

    private val _isPlayingMacro = MutableStateFlow(false)
    val isPlayingMacro: StateFlow<Boolean> = _isPlayingMacro.asStateFlow()

    private val _macroLoop = MutableStateFlow(false)
    val macroLoop: StateFlow<Boolean> = _macroLoop.asStateFlow()

    private var playbackJob: Job? = null

    init {
        // Listen to status responses from Arduino to update angles
        scope.launch {
            usbSerialManager.incomingLines.collect { line ->
                when (val result = SerialCommand.parseLine(line)) {
                    is SerialCommand.ParseResult.Status -> {
                        if (result.angles.size == 6) {
                            updateInternalPose(
                                ArmPose(
                                    s1Base = result.angles[0],
                                    s2Shoulder = result.angles[1],
                                    s3Elbow = result.angles[2],
                                    s4WristPitch = result.angles[3],
                                    s5WristRoll = result.angles[4],
                                    s6Gripper = result.angles[5]
                                )
                            )
                        }
                    }
                    is SerialCommand.ParseResult.Stopped -> {
                        _isEmergencyStopped.value = true
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Move a single servo with bounds checking and calibration offset.
     */
    fun setServoAngle(id: Int, targetAngle: Int) {
        if (_isEmergencyStopped.value) return
        val currentList = _servos.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index == -1) return

        val config = currentList[index]
        val clamped = targetAngle.coerceIn(config.minAngle, config.maxAngle)
        val withTrim = (clamped + config.trimOffset).coerceIn(config.minAngle, config.maxAngle)

        currentList[index] = config.copy(currentAngle = clamped, targetAngle = clamped)
        _servos.value = currentList
        syncPoseFromServos()

        // Send protocol command
        usbSerialManager.sendCommand(SerialCommand.setServo(id, withTrim))

        if (_isRecording.value) {
            recordCurrentKeyframe()
        }
    }

    /**
     * Set all 6 servos to an explicit ArmPose.
     */
    fun setPose(pose: ArmPose) {
        if (_isEmergencyStopped.value) return
        updateInternalPose(pose)
        val currentList = _servos.value
        val s1 = (pose.s1Base + currentList[0].trimOffset).coerceIn(currentList[0].minAngle, currentList[0].maxAngle)
        val s2 = (pose.s2Shoulder + currentList[1].trimOffset).coerceIn(currentList[1].minAngle, currentList[1].maxAngle)
        val s3 = (pose.s3Elbow + currentList[2].trimOffset).coerceIn(currentList[2].minAngle, currentList[2].maxAngle)
        val s4 = (pose.s4WristPitch + currentList[3].trimOffset).coerceIn(currentList[3].minAngle, currentList[3].maxAngle)
        val s5 = (pose.s5WristRoll + currentList[4].trimOffset).coerceIn(currentList[4].minAngle, currentList[4].maxAngle)
        val s6 = (pose.s6Gripper + currentList[5].trimOffset).coerceIn(currentList[5].minAngle, currentList[5].maxAngle)

        usbSerialManager.sendCommand(SerialCommand.setAll(s1, s2, s3, s4, s5, s6))
    }

    /**
     * Return robotic arm to calibrated HOME position.
     */
    fun home() {
        _isEmergencyStopped.value = false
        stopMacroPlayback()
        setPose(ArmPose.HOME)
        usbSerialManager.sendCommand(SerialCommand.CMD_HOME)
    }

    /**
     * Immediate Emergency Stop. Freezes all movements and halts sequencer.
     */
    fun emergencyStop() {
        _isEmergencyStopped.value = true
        stopMacroPlayback()
        _isRecording.value = false
        usbSerialManager.sendCommand(SerialCommand.CMD_STOP)
    }

    /**
     * Reset Emergency Stop flag so movements can resume.
     */
    fun resetEmergencyStop() {
        _isEmergencyStopped.value = false
        home()
    }

    /**
     * Quick toggle for Gripper Open (85°) / Close (15°).
     */
    fun toggleGripper() {
        val currentGripper = _servos.value.firstOrNull { it.id == 6 }?.currentAngle ?: 45
        val newTarget = if (currentGripper > 50) 15 else 85
        setServoAngle(6, newTarget)
    }

    /**
     * Directional D-pad kinematic steps.
     */
    fun stepDirection(direction: String) {
        if (_isEmergencyStopped.value) return
        val step = _stepSize.value
        val pose = _currentPose.value

        when (direction.uppercase()) {
            "BASE_LEFT" -> setServoAngle(1, pose.s1Base + step)
            "BASE_RIGHT" -> setServoAngle(1, pose.s1Base - step)
            "ARM_FORWARD" -> {
                // Forward reach: lower shoulder, raise elbow
                setServoAngle(2, pose.s2Shoulder + step)
                setServoAngle(3, pose.s3Elbow - (step / 2))
            }
            "ARM_BACKWARD" -> {
                setServoAngle(2, pose.s2Shoulder - step)
                setServoAngle(3, pose.s3Elbow + (step / 2))
            }
            "ARM_UP" -> {
                setServoAngle(2, pose.s2Shoulder - step)
                setServoAngle(3, pose.s3Elbow - step)
            }
            "ARM_DOWN" -> {
                setServoAngle(2, pose.s2Shoulder + step)
                setServoAngle(3, pose.s3Elbow + step)
            }
            "WRIST_UP" -> setServoAngle(4, pose.s4WristPitch + step)
            "WRIST_DOWN" -> setServoAngle(4, pose.s4WristPitch - step)
            "WRIST_CW" -> setServoAngle(5, pose.s5WristRoll + step)
            "WRIST_CCW" -> setServoAngle(5, pose.s5WristRoll - step)
            "GRIP_OPEN" -> setServoAngle(6, pose.s6Gripper + step)
            "GRIP_CLOSE" -> setServoAngle(6, pose.s6Gripper - step)
        }
    }

    fun setStepSize(size: Int) {
        _stepSize.value = size.coerceIn(1, 20)
    }

    /**
     * Calibration: update soft limits and trim offsets.
     */
    fun updateCalibration(id: Int, minAngle: Int, maxAngle: Int, trimOffset: Int) {
        val currentList = _servos.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index == -1) return

        currentList[index] = currentList[index].copy(
            minAngle = minAngle.coerceIn(0, 180),
            maxAngle = maxAngle.coerceIn(0, 180),
            trimOffset = trimOffset.coerceIn(-30, 30)
        )
        _servos.value = currentList
    }

    /**
     * Add a custom preset from current arm pose.
     */
    fun saveCurrentPoseAsPreset(name: String, subtitle: String) {
        val newPreset = ArmPreset(
            id = "custom_${UUID.randomUUID()}",
            name = name,
            subtitle = subtitle,
            pose = _currentPose.value,
            isSystemPreset = false
        )
        _presets.value = _presets.value + newPreset
    }

    fun deletePreset(id: String) {
        _presets.value = _presets.value.filter { it.id != id || it.isSystemPreset }
    }

    // Macro Recording & Playback
    fun toggleRecording() {
        val newRecordingState = !_isRecording.value
        _isRecording.value = newRecordingState
        if (newRecordingState && _recordedKeyframes.value.isEmpty()) {
            recordCurrentKeyframe()
        }
    }

    fun recordCurrentKeyframe(delayMs: Long = 800L) {
        val keyframe = MacroKeyframe(
            id = UUID.randomUUID().toString(),
            pose = _currentPose.value,
            delayMs = delayMs
        )
        _recordedKeyframes.value = _recordedKeyframes.value + keyframe
    }

    fun removeKeyframe(id: String) {
        _recordedKeyframes.value = _recordedKeyframes.value.filter { it.id != id }
    }

    fun clearKeyframes() {
        _recordedKeyframes.value = emptyList()
    }

    fun toggleMacroLoop() {
        _macroLoop.value = !_macroLoop.value
    }

    fun playMacro() {
        val frames = _recordedKeyframes.value
        if (frames.isEmpty()) return
        stopMacroPlayback()
        _isPlayingMacro.value = true

        playbackJob = scope.launch(Dispatchers.Default) {
            do {
                for (frame in frames) {
                    if (!isActive || _isEmergencyStopped.value) break
                    setPose(frame.pose)
                    delay(frame.delayMs)
                }
            } while (_macroLoop.value && isActive && !_isEmergencyStopped.value)
            _isPlayingMacro.value = false
        }
    }

    fun stopMacroPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _isPlayingMacro.value = false
    }

    private fun updateInternalPose(pose: ArmPose) {
        _currentPose.value = pose
        val currentList = _servos.value.toMutableList()
        currentList[0] = currentList[0].copy(currentAngle = pose.s1Base)
        currentList[1] = currentList[1].copy(currentAngle = pose.s2Shoulder)
        currentList[2] = currentList[2].copy(currentAngle = pose.s3Elbow)
        currentList[3] = currentList[3].copy(currentAngle = pose.s4WristPitch)
        currentList[4] = currentList[4].copy(currentAngle = pose.s5WristRoll)
        currentList[5] = currentList[5].copy(currentAngle = pose.s6Gripper)
        _servos.value = currentList
    }

    private fun syncPoseFromServos() {
        val list = _servos.value
        _currentPose.value = ArmPose(
            s1Base = list.getOrNull(0)?.currentAngle ?: 90,
            s2Shoulder = list.getOrNull(1)?.currentAngle ?: 90,
            s3Elbow = list.getOrNull(2)?.currentAngle ?: 90,
            s4WristPitch = list.getOrNull(3)?.currentAngle ?: 90,
            s5WristRoll = list.getOrNull(4)?.currentAngle ?: 90,
            s6Gripper = list.getOrNull(5)?.currentAngle ?: 45
        )
    }
}
