package com.example.alarmbuttons

// Дата класс объекта с настройками
internal data class Settings(
    var count: Int = 0,
    var name: String = "",
    var lastName: String = "",
    var grade: String = "",
    var myNumber: String = "",
    var teacher: String = "",
    var principal: String = "",
    var parent: String = "",
    val fireDepartment: String = "101",
    val police: String = "102",
    val ambulance: String = "103",
    val emergency: String = "112"
)
