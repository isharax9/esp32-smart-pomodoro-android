package com.esp32.smartpomodoro

import android.app.Application
import com.esp32.smartpomodoro.data.repository.Esp32Repository
import com.esp32.smartpomodoro.notification.NotificationHelper

class SmartPomodoroApp : Application() {

    lateinit var repository: Esp32Repository
        private set

    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Esp32Repository()
        notificationHelper = NotificationHelper(this)
    }
}
