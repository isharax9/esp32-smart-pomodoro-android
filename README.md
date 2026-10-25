# ESP32 Smart Pomodoro Android App 📱🍅

A modern, native Android companion app for the **ESP32 Smart Pomodoro & Desk Environment System**. Built with **Jetpack Compose**, **Material 3**, and Kotlin with minimum support for **Android 8.0 (API Level 26)**.

---

## 🌟 Features

- **🍅 Real-Time Pomodoro Focus Tracking:**
  - Live animated circular countdown dial synchronized with the ESP32 hardware timer.
  - Distinct visual states: **Focus Sprint (Red)**, **Recovery Break (Green)**, and **Idle**.
  - One-tap controls: Start, Pause, Resume, Skip, and Reset.
  - Cycle tracker ("Cycles: X").

- **🪑 Ultrasonic Desk Presence & Walkaway Detection:**
  - Live chair occupancy status ("At Desk" vs "Away") and distance gauge (cm).
  - Walkaway smart auto-pause toggle switch.

- **🌡️ Room Climate Telemetry:**
  - Live ambient room temperature (°C) and relative humidity (%) from the ESP32's DHT11 sensor.

- **💡 Virtual 24-Key RGB Light Remote:**
  - Dual strip control: Independent or simultaneous control for Strip 1 (Desk Backlight) and Strip 2 (Monitor Glow).
  - Power ON / OFF toggles.
  - Smooth brightness slider (10% - 100%).
  - Full 16-color tactile swatch matrix.
  - 4 Dynamic FX modes: **Flash**, **Strobe**, **Fade**, and **Smooth**.

- **🔔 Background Session Notifications:**
  - High-priority notification channels (Android 8.0 Oreo+).
  - Sends haptic vibration and chime alerts when focus sprints or recovery breaks complete.

- **⚙️ Connection & Calibration Management:**
  - Quick-switch presets: Connect via ESP32 Fallback SoftAP (`192.168.4.1`) or local network mDNS (`pomodoro.local`).
  - Built-in "Test Ping" tool for immediate connection verification.
  - On-screen hardware pin calibration (swap R-G, G-B, R-B) without editing firmware code.

---

## 📋 System Requirements

- **Minimum Android Version:** Android 8.0 (Oreo / API Level 26)
- **Target Android Version:** Android 14 (API Level 34)
- **Development Tool:** Android Studio Iguana / Jellyfish (or newer)
- **Language / UI Framework:** Kotlin 1.9.23 + Jetpack Compose (Material 3)

---

## 🚀 Getting Started

### 1. Opening in Android Studio
1. Clone or open this repository directory in **Android Studio**.
2. Android Studio will automatically recognize the Gradle project and download dependencies.
3. Connect your Android device via USB (or start an Android Emulator with API 26+).
4. Click **Run** (`Shift + F10`) to build and launch the app.

### 2. Connecting to the ESP32
Ensure your phone is connected to the same Wi-Fi network as the ESP32:
- **Option A (Direct Hotspot Mode):** Connect your phone's Wi-Fi to the ESP32's hotspot `Desk-Pomodoro` (Password: `12345678`). The app defaults to `192.168.4.1`.
- **Option B (Home Wi-Fi Network):** If your ESP32 is connected to your home router, tap the connection pill in the app header, enter your ESP32's local IP address or `pomodoro.local`, and tap **Test Ping** then **Save & Connect**.
