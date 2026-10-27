package com.esp32.smartpomodoro.data.repository

import com.esp32.smartpomodoro.data.model.ApiResponse
import com.esp32.smartpomodoro.data.model.PomodoroStatus
import com.esp32.smartpomodoro.data.model.StripTarget
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class Esp32Repository {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Volatile
    var hostAddress: String = "192.168.4.1" // Default SoftAP address

    private val baseUrl: String
        get() {
            val trimmed = hostAddress.trim()
            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed.removeSuffix("/")
            } else {
                "http://$trimmed".removeSuffix("/")
            }
        }

    suspend fun fetchStatus(): Result<PomodoroStatus> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/status"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error HTTP ${response.code}"))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))
                val status = gson.fromJson(body, PomodoroStatus::class.java)
                Result.success(status)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPomodoroAction(action: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val httpUrl = "$baseUrl/api/pomodoro".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("action", action)
                ?.build() ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))

            val request = Request.Builder().url(httpUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendColorCommand(cmdVal: String, target: StripTarget): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val httpUrl = "$baseUrl/api/cmd".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("val", cmdVal)
                ?.addQueryParameter("strip", target.apiVal)
                ?.build() ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))

            val request = Request.Builder().url(httpUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setBrightness(level: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val httpUrl = "$baseUrl/api/cmd".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("val", "brightness")
                ?.addQueryParameter("level", level.coerceIn(10, 255).toString())
                ?.build() ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))

            val request = Request.Builder().url(httpUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setFadeConfig(speed: Int?, palette: Int?, target: StripTarget): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val builder = "$baseUrl/api/cmd".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("val", "fade_config")
                ?.addQueryParameter("strip", target.apiVal) ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))
            if (speed != null) builder.addQueryParameter("speed", speed.toString())
            if (palette != null) builder.addQueryParameter("palette", palette.toString())
            val request = Request.Builder().url(builder.build()).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setSmoothConfig(speed: Int?, breath: Int?, palette: Int?, target: StripTarget): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val builder = "$baseUrl/api/cmd".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("val", "smooth_config")
                ?.addQueryParameter("strip", target.apiVal) ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))
            if (speed != null) builder.addQueryParameter("speed", speed.toString())
            if (breath != null) builder.addQueryParameter("breath", breath.toString())
            if (palette != null) builder.addQueryParameter("palette", palette.toString())
            val request = Request.Builder().url(builder.build()).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setStrobeMode(mode: String, target: StripTarget): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val builder = "$baseUrl/api/cmd".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("val", "strobe_mode")
                ?.addQueryParameter("mode", mode)
                ?.addQueryParameter("strip", target.apiVal) ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))
            val request = Request.Builder().url(builder.build()).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setAutoPause(enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val httpUrl = "$baseUrl/api/settings".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("autopause", if (enabled) "1" else "0")
                ?.build() ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))

            val request = Request.Builder().url(httpUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calibratePins(strip: String, action: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val httpUrl = "$baseUrl/api/calibrate".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("strip", strip)
                ?.addQueryParameter("action", action)
                ?.build() ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))

            val request = Request.Builder().url(httpUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("Failed with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(testHost: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val host = if (testHost.startsWith("http://") || testHost.startsWith("https://")) {
                testHost.removeSuffix("/")
            } else {
                "http://$testHost".removeSuffix("/")
            }
            val request = Request.Builder().url("$host/api/status").get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) Result.success(true)
                else Result.failure(IOException("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
