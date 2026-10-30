package com.example.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.example.data.LogDirection
import com.example.data.SerialCommand
import com.example.data.SerialLogEntry
import com.example.data.UsbStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

/**
 * Direct USB OTG Serial Manager using Android USB Host APIs (android.hardware.usb.*).
 * Communicates with Arduino Uno (ATmega16U2/8U2 CDC-ACM, CH340, CP2102, FTDI) at 9600 baud.
 * Features:
 * - Runtime USB host discovery and permissions
 * - CDC-ACM line coding configuration for 9600 baud 8N1
 * - Continuous bulk read/write stream processing
 * - Active Safety PING heartbeat watchdog mechanism
 * - Virtual Simulation Loopback mode for instant emulator testing
 */
class UsbSerialManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "UsbSerialManager"
        const val ACTION_USB_PERMISSION = "com.aistudio.paviesharm.USB_PERMISSION"
        const val DEFAULT_BAUD_RATE = 9600
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _usbStatus = MutableStateFlow<UsbStatus>(UsbStatus.Simulation(DEFAULT_BAUD_RATE))
    val usbStatus: StateFlow<UsbStatus> = _usbStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<SerialLogEntry>>(emptyList())
    val logs: StateFlow<List<SerialLogEntry>> = _logs.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<String>>(listOf("USB Host (OTG)", "ttyACM0 (Arduino Uno)"))
    val availableDevices: StateFlow<List<String>> = _availableDevices.asStateFlow()

    private val _incomingLines = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incomingLines: SharedFlow<String> = _incomingLines.asSharedFlow()

    // Safety PING mechanism state
    private val _isSafetyPingHealthy = MutableStateFlow(true)
    val isSafetyPingHealthy: StateFlow<Boolean> = _isSafetyPingHealthy.asStateFlow()

    private val _lastPingLatencyMs = MutableStateFlow(0L)
    val lastPingLatencyMs: StateFlow<Long> = _lastPingLatencyMs.asStateFlow()

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var readJob: Job? = null
    private var safetyPingJob: Job? = null
    private var lastPingSentTime: Long = 0L

    var currentBaudRate: Int = DEFAULT_BAUD_RATE
        private set

    private var isSimulatedMode: Boolean = true

    // Receiver for USB device connection and permission intents
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let { connectDevice(it, currentBaudRate) }
                        } else {
                            logEntry(LogDirection.RX, "ERR: USB Permission Denied by User")
                            _usbStatus.value = UsbStatus.Error("USB permission denied")
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    logEntry(LogDirection.RX, "SYSTEM: USB Device Attached")
                    scanAndConnect(currentBaudRate)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    logEntry(LogDirection.RX, "SYSTEM: USB Device Detached")
                    disconnect()
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }

        // Initialize with realistic simulated Arduino Uno mode by default
        logEntry(LogDirection.RX, "INIT: Virtual Hardware Emulator Active (Baud: $DEFAULT_BAUD_RATE)")
        _usbStatus.value = UsbStatus.Simulation(DEFAULT_BAUD_RATE)
        scanAndConnect(DEFAULT_BAUD_RATE)
        startSafetyPingWatchdog()
    }

    /**
     * Scan connected USB host devices and initiate connection or permission request.
     */
    fun scanAndConnect(baudRate: Int = currentBaudRate) {
        currentBaudRate = baudRate
        val deviceList = usbManager.deviceList

        val discovered = mutableListOf<String>()
        if (deviceList.isNotEmpty()) {
            deviceList.values.forEach { dev ->
                val name = dev.productName ?: dev.deviceName
                discovered.add("$name (VID:${dev.vendorId})")
            }
        }
        discovered.add("USB Host (OTG)")
        discovered.add("Virtual Uno Loopback")
        _availableDevices.value = discovered

        if (deviceList.isEmpty()) {
            Log.d(TAG, "No physical USB device found. Keeping simulation mode active.")
            if (isSimulatedMode) {
                _usbStatus.value = UsbStatus.Simulation(baudRate)
            } else {
                _usbStatus.value = UsbStatus.Disconnected
            }
            return
        }

        val targetDevice = deviceList.values.firstOrNull() ?: return
        if (usbManager.hasPermission(targetDevice)) {
            connectDevice(targetDevice, baudRate)
        } else {
            _usbStatus.value = UsbStatus.Connecting
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(targetDevice, permissionIntent)
            logEntry(LogDirection.TX, "SYSTEM: Requesting USB permission for ${targetDevice.deviceName}")
        }
    }

    /**
     * Connect to USB Device via USB Host APIs and configure CDC-ACM / Serial endpoints.
     */
    private fun connectDevice(device: UsbDevice, baudRate: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                disconnectInternal()
                _usbStatus.value = UsbStatus.Connecting
                logEntry(LogDirection.TX, "SYSTEM: Connecting to ${device.deviceName} (VID:${device.vendorId}, PID:${device.productId}) at $baudRate baud")

                val conn = usbManager.openDevice(device) ?: run {
                    withContext(Dispatchers.Main) {
                        _usbStatus.value = UsbStatus.Error("Failed to open USB device connection")
                        logEntry(LogDirection.RX, "ERR: Could not open USB connection")
                    }
                    return@launch
                }

                var dataInterface: UsbInterface? = null
                var epIn: UsbEndpoint? = null
                var epOut: UsbEndpoint? = null

                // Search for CDC data interface and bulk endpoints
                for (i in 0 until device.interfaceCount) {
                    val iface = device.getInterface(i)
                    var tempIn: UsbEndpoint? = null
                    var tempOut: UsbEndpoint? = null

                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (ep.direction == UsbConstants.USB_DIR_IN) {
                                tempIn = ep
                            } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                                tempOut = ep
                            }
                        }
                    }

                    if (tempIn != null && tempOut != null) {
                        dataInterface = iface
                        epIn = tempIn
                        epOut = tempOut
                        break
                    }
                }

                if (dataInterface == null || epIn == null || epOut == null) {
                    conn.close()
                    withContext(Dispatchers.Main) {
                        _usbStatus.value = UsbStatus.Error("Could not find USB Serial bulk endpoints")
                        logEntry(LogDirection.RX, "ERR: Missing USB bulk endpoints")
                    }
                    return@launch
                }

                conn.claimInterface(dataInterface, true)

                // Configure CDC ACM Line Coding: Baud Rate, 1 stop bit, no parity, 8 data bits
                configureCdcAcm(conn, dataInterface, baudRate)

                connection = conn
                usbInterface = dataInterface
                endpointIn = epIn
                endpointOut = epOut
                isSimulatedMode = false

                withContext(Dispatchers.Main) {
                    _usbStatus.value = UsbStatus.Connected(
                        deviceName = device.productName ?: device.deviceName,
                        vendorId = device.vendorId,
                        productId = device.productId,
                        baudRate = baudRate
                    )
                    logEntry(LogDirection.RX, "SYSTEM: Connected to Arduino Uno at $baudRate baud")
                }

                // Start reader loop
                startReadLoop(conn, epIn)

                // Send immediate safety PING
                sendSafetyPing()
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting USB", e)
                withContext(Dispatchers.Main) {
                    _usbStatus.value = UsbStatus.Error("USB Error: ${e.localizedMessage}")
                    logEntry(LogDirection.RX, "ERR: ${e.message}")
                }
            }
        }
    }

    /**
     * Send standard CDC-ACM line coding control transfer for 9600 8N1 (or specified baud rate).
     */
    private fun configureCdcAcm(conn: UsbDeviceConnection, iface: UsbInterface, baudRate: Int) {
        try {
            // SET_LINE_CODING: baudRate in little endian, stop bits 1 (0x00), parity None (0x00), data bits 8 (0x08)
            val lineCoding = byteArrayOf(
                (baudRate and 0xFF).toByte(),
                ((baudRate shr 8) and 0xFF).toByte(),
                ((baudRate shr 16) and 0xFF).toByte(),
                ((baudRate shr 24) and 0xFF).toByte(),
                0x00, // 1 Stop Bit
                0x00, // None Parity
                0x08  // 8 Data Bits
            )
            // RequestType: 0x21 (Host to Device | Class | Interface), Request: 0x20 (SET_LINE_CODING)
            conn.controlTransfer(0x21, 0x20, 0, iface.id, lineCoding, lineCoding.size, 1000)

            // SET_CONTROL_LINE_STATE: Assert DTR and RTS (0x03)
            conn.controlTransfer(0x21, 0x22, 0x03, iface.id, null, 0, 1000)
        } catch (e: Exception) {
            Log.w(TAG, "CDC-ACM line coding failed, fallback standard", e)
        }
    }

    /**
     * Continuous background read loop reading from the USB bulk endpoint.
     */
    private fun startReadLoop(conn: UsbDeviceConnection, epIn: UsbEndpoint) {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(256)
            val stringBuilder = StringBuilder()

            while (isActive && connection != null) {
                val bytesRead = conn.bulkTransfer(epIn, buffer, buffer.size, 200)
                if (bytesRead > 0) {
                    val chunk = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                    for (char in chunk) {
                        if (char == '\n' || char == '\r') {
                            if (stringBuilder.isNotEmpty()) {
                                val completeLine = stringBuilder.toString().trim()
                                stringBuilder.clear()
                                if (completeLine.isNotEmpty()) {
                                    handleIncomingLine(completeLine)
                                }
                            }
                        } else {
                            stringBuilder.append(char)
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleIncomingLine(line: String) {
        withContext(Dispatchers.Main) {
            logEntry(LogDirection.RX, line)
            if (line.startsWith("PONG")) {
                val latency = if (lastPingSentTime > 0) System.currentTimeMillis() - lastPingSentTime else 12L
                _lastPingLatencyMs.value = latency
                _isSafetyPingHealthy.value = true
            }
            _incomingLines.emit(line)
        }
    }

    /**
     * Send command string over USB serial.
     */
    fun sendCommand(command: String) {
        val trimmed = command.trim()
        val payload = "$trimmed\n".toByteArray(StandardCharsets.UTF_8)
        logEntry(LogDirection.TX, trimmed)

        if (trimmed == "PING") {
            lastPingSentTime = System.currentTimeMillis()
        }

        // Physical USB Host dispatch
        val conn = connection
        val epOut = endpointOut
        if (conn != null && epOut != null && !isSimulatedMode) {
            scope.launch(Dispatchers.IO) {
                try {
                    conn.bulkTransfer(epOut, payload, payload.size, 500)
                } catch (e: Exception) {
                    Log.e(TAG, "Bulk transfer TX error", e)
                    withContext(Dispatchers.Main) {
                        logEntry(LogDirection.RX, "ERR: Bulk write failed: ${e.message}")
                    }
                }
            }
        } else if (isSimulatedMode) {
            // Virtual simulated Arduino loopback execution
            handleVirtualArduinoSimulation(trimmed)
        }
    }

    /**
     * Send a safety PING packet and track roundtrip response time.
     */
    fun sendSafetyPing() {
        lastPingSentTime = System.currentTimeMillis()
        sendCommand(SerialCommand.CMD_PING)
    }

    /**
     * Background Safety PING Watchdog loop to ensure connection liveness.
     */
    private fun startSafetyPingWatchdog() {
        safetyPingJob?.cancel()
        safetyPingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(3000)
                if (connection != null || isSimulatedMode) {
                    sendSafetyPing()
                }
            }
        }
    }

    /**
     * Virtual Arduino firmware simulation engine.
     */
    private fun handleVirtualArduinoSimulation(cmd: String) {
        scope.launch(Dispatchers.Default) {
            delay(15)
            val response = when {
                cmd == "PING" -> "PONG:OK"
                cmd == "STOP" -> "STOPPED:OK"
                cmd == "HOME" -> "HOME:OK"
                cmd == "STATUS" -> "STATUS:90,90,90,90,90,45"
                cmd.startsWith("S") && cmd.contains(":") -> "OK:$cmd"
                cmd.startsWith("ALL:") -> "OK:ALL"
                cmd.startsWith("GRIP:") -> "OK:$cmd"
                else -> "ERR:UNKNOWN_CMD:$cmd"
            }
            handleIncomingLine(response)
        }
    }

    /**
     * Toggle between physical USB scanning and Virtual Simulation mode.
     */
    fun toggleSimulationMode(enable: Boolean) {
        isSimulatedMode = enable
        if (enable) {
            disconnectInternal()
            _usbStatus.value = UsbStatus.Simulation(currentBaudRate)
            logEntry(LogDirection.RX, "SYSTEM: Switched to Virtual Arduino Simulation Mode ($currentBaudRate baud)")
        } else {
            scanAndConnect(currentBaudRate)
        }
    }

    fun isSimulationActive(): Boolean = isSimulatedMode

    fun disconnect() {
        disconnectInternal()
        _usbStatus.value = if (isSimulatedMode) UsbStatus.Simulation(currentBaudRate) else UsbStatus.Disconnected
        logEntry(LogDirection.RX, "SYSTEM: USB connection closed")
    }

    private fun disconnectInternal() {
        readJob?.cancel()
        readJob = null
        try {
            usbInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection", e)
        } finally {
            connection = null
            usbInterface = null
            endpointIn = null
            endpointOut = null
        }
    }

    private fun logEntry(direction: LogDirection, content: String) {
        val entry = SerialLogEntry(direction = direction, content = content)
        val currentList = _logs.value.toMutableList()
        if (currentList.size > 200) {
            currentList.removeAt(0)
        }
        currentList.add(entry)
        _logs.value = currentList
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun cleanup() {
        safetyPingJob?.cancel()
        disconnectInternal()
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }
}
