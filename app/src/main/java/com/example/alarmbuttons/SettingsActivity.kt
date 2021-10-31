package com.example.alarmbuttons

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.example.alarmbuttons.Bridge.KEY_SETTINGS
import com.example.alarmbuttons.Bridge.settings
import com.example.alarmbuttons.Bridge.preferences
import com.example.alarmbuttons.databinding.ActivitySettingsBinding
import com.google.gson.Gson
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton

// Экран настроек
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Добавляем на actionBar стрелочку назад

        // Из загруженных настроек заполняем поля на экране
        binding.apply {
            editTextName.setText(settings.name)
            editTextLastName.setText(settings.lastName)
            editTextGrade.setText(settings.grade)
            editTextMyNumber.setText(settings.myNumber)
            editTextTeacher.setText(settings.teacher)
            editTextPrincipal.setText(settings.principal)
            editTextParent.setText(settings.parent)
        }
    }

    // Переназначаем аппаратную кнопку "назад"
    override fun onBackPressed() {
        onPause()
        // Если не заполнены поля имя, фамилия, класс и ваш номер телефона
        if (settings.name == "" || settings.lastName == "" || settings.grade == "" || settings.myNumber == "") {
            // Создаётся предупреждение
            alert(
                "Для продолжения работы с\n" +
                        "приложением, необходимо\n" +
                        "ввести ваши имя, фамилию\n" +
                        "класс и номер телефона.", "Внимание!"
            ) {
                yesButton { } // "Ok" ничего не делает
                noButton { finishAffinity() } // "Отмена" завершает приложение
            }.show()
        } else super.onBackPressed() // Иначе исходная функция аппаратной кнопки "назад"(возрващенние на предыдущий экран)
    }

    // Если нажата стрелочка "назад" на actionBar, то вызывается функция аппаратной кнопки "назад"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    // Сохраняем настройки приложения в состоянии паузы
    override fun onPause() {
        super.onPause()

        // Сохраняем все настройки в объект settings
        binding.apply {
            settings.name = editTextName.text.toString()
            settings.lastName = editTextLastName.text.toString()
            settings.grade = editTextGrade.text.toString()
            settings.myNumber = editTextMyNumber.text.toString()
            settings.teacher = editTextTeacher.text.toString()
            settings.principal = editTextPrincipal.text.toString()
            settings.parent = editTextParent.text.toString()
        }

        val json = Gson().toJson(settings) // Сохранем объект settings в json строку
        preferences.edit().putString(KEY_SETTINGS, json).apply() // Сохраняем json в файл настроек приложения
    }
}
