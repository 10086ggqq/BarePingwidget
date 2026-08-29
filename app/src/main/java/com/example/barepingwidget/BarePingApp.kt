package com.example.barepingwidget

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.barepingwidget.data.SiteRepository
import com.example.barepingwidget.monitor.MonitorScheduler
import com.example.barepingwidget.notify.NotificationHelper

class BarePingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        MonitorScheduler.schedule(this)
        applyTheme(SiteRepository.getInstance(this).themeMode)
    }

    companion object {
        fun applyTheme(mode: Int) {
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }
}
