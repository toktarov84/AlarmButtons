package com.example.alarmbuttons

import android.content.SharedPreferences

internal object Bridge {
    lateinit var preferences: SharedPreferences
    const val KEY_SETTINGS = "settings"
    var settings = Settings()
}