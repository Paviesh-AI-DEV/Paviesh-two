package com.example.data

/**
 * Protocol formatting and parsing for 115200 baud serial communication with Arduino Uno.
 */
object SerialCommand {
    const val CMD_HOME = "HOME\n"
    const val CMD_STOP = "STOP\n"
    const val CMD_PING = "PING\n"
    const val CMD_STATUS = "STATUS\n"

    fun setServo(id: Int, angle: Int): String {
        val clampedId = id.coerceIn(1, 6)
        val clampedAngle = angle.coerceIn(0, 180)
        return "S$clampedId:$clampedAngle\n"
    }

    fun setAll(s1: Int, s2: Int, s3: Int, s4: Int, s5: Int, s6: Int): String {
        return "ALL:$s1,$s2,$s3,$s4,$s5,$s6\n"
    }

    fun setAll(pose: ArmPose): String {
        return setAll(pose.s1Base, pose.s2Shoulder, pose.s3Elbow, pose.s4WristPitch, pose.s5WristRoll, pose.s6Gripper)
    }

    fun grip(open: Boolean): String {
        return "GRIP:${if (open) 1 else 0}\n"
    }

    sealed interface ParseResult {
        data object Pong : ParseResult
        data class Ok(val detail: String) : ParseResult
        data class Stopped(val detail: String) : ParseResult
        data class Status(val angles: List<Int>) : ParseResult
        data class Error(val message: String) : ParseResult
        data class Unknown(val raw: String) : ParseResult
    }

    fun parseLine(rawLine: String): ParseResult {
        val line = rawLine.trim()
        return when {
            line == "PONG" -> ParseResult.Pong
            line.startsWith("OK:") -> ParseResult.Ok(line.removePrefix("OK:"))
            line.startsWith("STOPPED:") -> ParseResult.Stopped(line)
            line.startsWith("STATUS:") -> {
                val values = line.removePrefix("STATUS:").split(",").mapNotNull { it.trim().toIntOrNull() }
                ParseResult.Status(values)
            }
            line.startsWith("ERR:") -> ParseResult.Error(line.removePrefix("ERR:"))
            else -> ParseResult.Unknown(line)
        }
    }
}
