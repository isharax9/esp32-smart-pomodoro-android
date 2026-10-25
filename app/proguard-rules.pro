# Proguard rules for ESP32 Smart Pomodoro
-keep class com.esp32.smartpomodoro.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
