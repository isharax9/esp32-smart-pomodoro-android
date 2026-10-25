package com.esp32.smartpomodoro

import android.app.Application
import com.esp32.smartpomodoro.data.repository.Esp32Repository
import com.esp32.smartpomodoro.data.stopwatch.StopwatchEngine
import com.esp32.smartpomodoro.notification.NotificationHelper
import com.esp32.smartpomodoro.notification.StopwatchFeedbackHelper

class SmartPomodoroApp : Application() {

    lateinit var repository: Esp32Repository
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    lateinit var stopwatchEngine: StopwatchEngine
        private set

    lateinit var stopwatchFeedbackHelper: StopwatchFeedbackHelper
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Esp32Repository()
        notificationHelper = NotificationHelper(this)
        stopwatchEngine = StopwatchEngine()
        stopwatchFeedbackHelper = StopwatchFeedbackHelper(this)
    }
}
