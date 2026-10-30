package com.example.vision

import com.example.data.TrackingTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Computer Vision Target Tracking Engine.
 * Features:
 * - Bounding box calculation
 * - Center dead-zone filtering (prevents servo jitter, overheating, and oscillation)
 * - Proportional angle delta output for Base (S1) and Shoulder/Elbow (S2) joints
 */
class TrackingEngine {

    private val _target = MutableStateFlow(
        TrackingTarget(
            centerX = 0.5f,
            centerY = 0.5f,
            width = 0.22f,
            height = 0.22f,
            label = "Workpiece Target",
            isLocked = false
        )
    )
    val target: StateFlow<TrackingTarget> = _target.asStateFlow()

    private val _isTrackingActive = MutableStateFlow(false)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    private val _deadZoneRadius = MutableStateFlow(0.12f) // 12% normalized radius around center
    val deadZoneRadius: StateFlow<Float> = _deadZoneRadius.asStateFlow()

    // Tracking mode: AUTO_OBJECT, COLOR_RED, COLOR_BLUE, COLOR_GREEN, MANUAL_TOUCH
    private val _trackingMode = MutableStateFlow("AUTO_OBJECT")
    val trackingMode: StateFlow<String> = _trackingMode.asStateFlow()

    fun toggleTracking(enabled: Boolean) {
        _isTrackingActive.value = enabled
    }

    fun setTrackingMode(mode: String) {
        _trackingMode.value = mode
    }

    fun setDeadZone(radius: Float) {
        _deadZoneRadius.value = radius.coerceIn(0.05f, 0.35f)
    }

    /**
     * Update target position (e.g. from touch coordinates or vision detector).
     * Applies dead-zone filtering and calculates corresponding servo angle deltas.
     */
    fun updateTargetPosition(normX: Float, normY: Float, label: String = "Target") {
        val clampedX = normX.coerceIn(0.1f, 0.9f)
        val clampedY = normY.coerceIn(0.1f, 0.9f)

        // Error relative to camera center (0.5, 0.5)
        val errX = clampedX - 0.5f
        val errY = clampedY - 0.5f
        val distance = hypot(errX, errY)

        val threshold = _deadZoneRadius.value
        val (deltaBase, deltaShoulder) = if (distance < threshold) {
            // Target is inside the dead-zone: zero out delta to avoid jitter
            0 to 0
        } else {
            // Proportional correction: scale error to degrees (e.g. max ±15°)
            val pGainX = 30f
            val pGainY = 25f
            val dB = (-errX * pGainX).toInt() // Invert X for base rotation
            val dS = (errY * pGainY).toInt()  // Shoulder elevation
            dB to dS
        }

        _target.value = _target.value.copy(
            centerX = clampedX,
            centerY = clampedY,
            label = label,
            isLocked = distance < threshold,
            deltaBase = deltaBase,
            deltaShoulder = deltaShoulder
        )
    }

    /**
     * Simulates target drift for demonstration and testing when physical camera target is moving.
     */
    fun simulateDrift(timeSec: Float) {
        val simulatedX = 0.5f + 0.22f * kotlin.math.sin(timeSec * 0.8f)
        val simulatedY = 0.5f + 0.15f * kotlin.math.cos(timeSec * 0.6f)
        updateTargetPosition(simulatedX.toFloat(), simulatedY.toFloat(), "Demo Workpiece")
    }
}
