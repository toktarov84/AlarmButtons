package com.example.alarmbuttons

import android.content.SharedPreferences

// Синглтон для прозрачной передачи данных между экранами
internal object Bridge {
    lateinit var preferences: SharedPreferences // Создаём переменную файла настроек
    const val KEY_SETTINGS = "settings" // Создаём константу для настроек
    var settings = Settings() // Создаём объект настроек
}