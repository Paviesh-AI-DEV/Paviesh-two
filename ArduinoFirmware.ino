/*
 * ============================================================================
 * PROJECT: PAVIESH AI ROBOTIC ARM CONTROLLER - ARDUINO UNO FIRMWARE
 * Target: Arduino Uno (ATmega328P) / Nano / Mega
 * Baud Rate: 9600 baud (Direct USB Serial / OTG Communication)
 * Protocol: Newline-delimited ASCII commands (e.g. S1:90\n, HOME\n, STOP\n, PING\n)
 * 
 * SERVO MOTOR ARCHITECTURE (3x MG996R + 3x SG90):
 *   HEAVY JOINTS (High-Torque Metal Gear):
 *     S1 - Base Waist Rotation:  MG996R -> Digital Pin 3  (PWM) [Range: 0 - 180 deg]
 *     S2 - Shoulder Joint:       MG996R -> Digital Pin 5  (PWM) [Range: 15 - 165 deg]
 *     S3 - Elbow Joint:          MG996R -> Digital Pin 6  (PWM) [Range: 15 - 165 deg]
 *   END EFFECTOR JOINTS (Lightweight Micro Servos):
 *     S4 - Wrist Pitch:          SG90   -> Digital Pin 9  (PWM) [Range: 0 - 180 deg]
 *     S5 - Wrist Roll:           SG90   -> Digital Pin 10 (PWM) [Range: 0 - 180 deg]
 *     S6 - Claw Gripper:         SG90   -> Digital Pin 11 (PWM) [Range: 0 - 90 deg]
 * 
 * CRITICAL SAFETY & EXTERNAL POWER REQUIREMENT:
 *   - NEVER power 6 servo motors directly from the Arduino 5V pin!
 *   - 3x MG996R (stall current ~1.5A each) + 3x SG90 (~500mA each) require 4.5A - 5A total peak current.
 *   - Use a dedicated EXTERNAL 5V to 6V (5A DC) regulated power supply for servo VCC (red wires).
 *   - MANDATORY: Connect External PSU Ground (black/GND) to Arduino GND (Common Ground).
 * ============================================================================
 */

#include <Servo.h>

#define NUM_SERVOS 6

// Pin assignments for standard PWM pins on Arduino Uno
const uint8_t SERVO_PINS[NUM_SERVOS] = {3, 5, 6, 9, 10, 11};

// Soft boundary angle limits (degrees) to protect gears and mechanical stops
const int MIN_ANGLES[NUM_SERVOS] = {0,  15, 15,  0,   0,   0};   // Base, Shoulder, Elbow, Wrist, WristRoll, Gripper
const int MAX_ANGLES[NUM_SERVOS] = {180, 165, 165, 180, 180, 90};  // Gripper: 0 (Closed) to 90 (Open)

// Calibrated HOME position angles
const int HOME_ANGLES[NUM_SERVOS] = {90, 90, 90, 90, 90, 45};

// Servo objects
Servo servos[NUM_SERVOS];

// Joint angles (current actual and target commanded)
int currentAngles[NUM_SERVOS];
int targetAngles[NUM_SERVOS];

// Onboard safety LED indicator
const int LED_PIN = 13;

// Emergency Stop Interrupt Flag
volatile bool isEmergencyStop = false;

// Safety PING Watchdog Mechanism
unsigned long lastPingTime = 0;
const unsigned long PING_TIMEOUT_MS = 10000; // 10-second safety watchdog timeout
bool isWatchdogActive = true;

// Serial input buffer
const int BUFFER_SIZE = 64;
char serialBuffer[BUFFER_SIZE];
int bufferIndex = 0;

// Smooth easing motion timing
unsigned long lastStepTime = 0;
const int STEP_INTERVAL_MS = 15; // 15ms per degree step prevents mechanical shaking

void setup() {
  // Initialize Serial at 9600 baud as requested
  Serial.begin(9600);
  while (!Serial && millis() < 3000); // 3-second safety window for serial enumeration

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH);

  // Attach servos and initialize at HOME position
  for (int i = 0; i < NUM_SERVOS; i++) {
    servos[i].attach(SERVO_PINS[i]);
    currentAngles[i] = HOME_ANGLES[i];
    targetAngles[i] = HOME_ANGLES[i];
    servos[i].write(currentAngles[i]);
    delay(40);
  }

  lastPingTime = millis();
  digitalWrite(LED_PIN, LOW);

  // Handshake greeting
  Serial.println("PAVIESH_ARM_ONLINE:9600:READY");
}

void loop() {
  // 1. Process incoming serial data stream
  while (Serial.available() > 0) {
    char inChar = (char)Serial.read();

    if (inChar == '\n' || inChar == '\r') {
      if (bufferIndex > 0) {
        serialBuffer[bufferIndex] = '\0';
        parseAndExecuteCommand(serialBuffer);
        bufferIndex = 0;
      }
    } else if (bufferIndex < BUFFER_SIZE - 1) {
      serialBuffer[bufferIndex++] = inChar;
    }
  }

  // 2. Safety PING Watchdog check
  // If no PING or valid command is received within PING_TIMEOUT_MS, trigger safety freeze
  if (isWatchdogActive && !isEmergencyStop && (millis() - lastPingTime > PING_TIMEOUT_MS)) {
    // Safety freeze: prevent unattended arm runaway
    for (int i = 0; i < NUM_SERVOS; i++) {
      targetAngles[i] = currentAngles[i];
    }
    digitalWrite(LED_PIN, HIGH);
  }

  // 3. Smooth kinematic easing step update (only when NOT stopped)
  if (!isEmergencyStop && (millis() - lastStepTime >= STEP_INTERVAL_MS)) {
    lastStepTime = millis();
    updateServoPositions();
  }
}

// Incrementally step current angles toward target angles
void updateServoPositions() {
  bool anyMoving = false;
  for (int i = 0; i < NUM_SERVOS; i++) {
    if (currentAngles[i] < targetAngles[i]) {
      currentAngles[i]++;
      servos[i].write(currentAngles[i]);
      anyMoving = true;
    } else if (currentAngles[i] > targetAngles[i]) {
      currentAngles[i]--;
      servos[i].write(currentAngles[i]);
      anyMoving = true;
    }
  }

  // Subtle LED activity pulse during movement
  if (anyMoving) {
    digitalWrite(LED_PIN, (millis() / 80) % 2 == 0 ? HIGH : LOW);
  } else if (!isEmergencyStop) {
    digitalWrite(LED_PIN, LOW);
  }
}

// Parse and validate newline-delimited command strings
void parseAndExecuteCommand(char* cmd) {
  // Skip leading whitespace
  while (*cmd == ' ') cmd++;

  // -------------------------------------------------------------
  // 1. SAFETY PING MECHANISM: Host sends "PING" -> Arduino replies "PONG:OK"
  // -------------------------------------------------------------
  if (strcmp(cmd, "PING") == 0) {
    lastPingTime = millis();
    if (!isEmergencyStop) {
      digitalWrite(LED_PIN, LOW);
    }
    Serial.println("PONG:OK");
    return;
  }

  // -------------------------------------------------------------
  // 2. EMERGENCY STOP COMMAND: Instant interrupt of all motions
  // -------------------------------------------------------------
  if (strcmp(cmd, "STOP") == 0) {
    isEmergencyStop = true;
    for (int i = 0; i < NUM_SERVOS; i++) {
      targetAngles[i] = currentAngles[i]; // Lock target to current angle immediately
      servos[i].write(currentAngles[i]);
    }
    digitalWrite(LED_PIN, HIGH); // Solid RED/Status LED signifies E-STOP engaged
    Serial.println("STOPPED:OK");
    return;
  }

  // Reset Emergency Stop when a valid motion command or HOME is received
  if (isEmergencyStop && (strcmp(cmd, "HOME") == 0 || strncmp(cmd, "S", 1) == 0 || strncmp(cmd, "ALL", 3) == 0)) {
    isEmergencyStop = false;
    digitalWrite(LED_PIN, LOW);
  }

  // -------------------------------------------------------------
  // 3. HOME COMMAND: Return all 6 servos to calibrated home pose
  // -------------------------------------------------------------
  if (strcmp(cmd, "HOME") == 0) {
    lastPingTime = millis();
    for (int i = 0; i < NUM_SERVOS; i++) {
      targetAngles[i] = HOME_ANGLES[i];
    }
    Serial.println("HOME:OK");
    return;
  }

  // -------------------------------------------------------------
  // 4. STATUS COMMAND: Returns CSV array of current joint angles
  // -------------------------------------------------------------
  if (strcmp(cmd, "STATUS") == 0) {
    lastPingTime = millis();
    reportStatus();
    return;
  }

  // -------------------------------------------------------------
  // 5. INDIVIDUAL SERVO COMMAND: S<1-6>:<0-180> (e.g., S1:90, S6:45)
  // -------------------------------------------------------------
  if (cmd[0] == 'S' && cmd[1] >= '1' && cmd[1] <= '6' && cmd[2] == ':') {
    lastPingTime = millis();
    int servoIndex = cmd[1] - '1'; // 0-based index
    int angle = atoi(&cmd[3]);

    // Enforce mechanical soft boundaries
    if (angle < MIN_ANGLES[servoIndex]) angle = MIN_ANGLES[servoIndex];
    if (angle > MAX_ANGLES[servoIndex]) angle = MAX_ANGLES[servoIndex];

    targetAngles[servoIndex] = angle;
    Serial.print("OK:S");
    Serial.print(servoIndex + 1);
    Serial.print(":");
    Serial.println(angle);
    return;
  }

  // -------------------------------------------------------------
  // 6. MULTI-SERVO COMMAND: ALL:<s1>,<s2>,<s3>,<s4>,<s5>,<s6>
  // -------------------------------------------------------------
  if (strncmp(cmd, "ALL:", 4) == 0) {
    lastPingTime = millis();
    char* token = strtok(cmd + 4, ",");
    int i = 0;
    while (token != NULL && i < NUM_SERVOS) {
      int angle = atoi(token);
      if (angle < MIN_ANGLES[i]) angle = MIN_ANGLES[i];
      if (angle > MAX_ANGLES[i]) angle = MAX_ANGLES[i];
      targetAngles[i] = angle;
      token = strtok(NULL, ",");
      i++;
    }
    if (i == NUM_SERVOS) {
      Serial.println("OK:ALL");
    } else {
      Serial.println("ERR:INCOMPLETE_ALL_ANGLES");
    }
    return;
  }

  // -------------------------------------------------------------
  // 7. GRIPPER SHORTCUT: GRIP:1 (Open) / GRIP:0 (Close)
  // -------------------------------------------------------------
  if (strncmp(cmd, "GRIP:", 5) == 0) {
    lastPingTime = millis();
    int state = atoi(cmd + 5);
    int target = (state == 1) ? 90 : 10;
    targetAngles[5] = target;
    Serial.print("OK:GRIP:");
    Serial.println(state);
    return;
  }

  // Unrecognized command
  Serial.print("ERR:UNKNOWN_CMD:");
  Serial.println(cmd);
}

// Transmit current joint angles over serial
void reportStatus() {
  Serial.print("STATUS:");
  for (int i = 0; i < NUM_SERVOS; i++) {
    Serial.print(currentAngles[i]);
    if (i < NUM_SERVOS - 1) Serial.print(",");
  }
  Serial.println();
}
