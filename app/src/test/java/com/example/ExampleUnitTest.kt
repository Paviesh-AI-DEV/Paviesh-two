package com.example

import com.example.data.ArmPose
import com.example.data.SerialCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSerialCommandFormatting() {
        assertEquals("S1:90\n", SerialCommand.setServo(1, 90))
        assertEquals("S6:45\n", SerialCommand.setServo(6, 45))
        assertEquals("HOME\n", SerialCommand.CMD_HOME)
        assertEquals("STOP\n", SerialCommand.CMD_STOP)
        assertEquals("ALL:90,90,90,90,90,45\n", SerialCommand.setAll(ArmPose.HOME))
        assertEquals("GRIP:1\n", SerialCommand.grip(true))
        assertEquals("GRIP:0\n", SerialCommand.grip(false))
    }

    @Test
    fun testSerialResponseParsing() {
        val pong = SerialCommand.parseLine("PONG")
        assertTrue(pong is SerialCommand.ParseResult.Pong)

        val status = SerialCommand.parseLine("STATUS:90,80,70,60,50,40")
        assertTrue(status is SerialCommand.ParseResult.Status)
        val angles = (status as SerialCommand.ParseResult.Status).angles
        assertEquals(listOf(90, 80, 70, 60, 50, 40), angles)

        val okResult = SerialCommand.parseLine("OK:S1:90")
        assertTrue(okResult is SerialCommand.ParseResult.Ok)
    }
}
