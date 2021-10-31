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

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

    override fun onBackPressed() {
        onPause()
        if (settings.name == "" || settings.lastName == "" || settings.grade == "" || settings.myNumber == "") {
            alert(
                "Для продолжения работы с\n" +
                        "приложением, необходимо\n" +
                        "ввести ваши имя, фамилию\n" +
                        "класс и номер телефона.", "Внимание!"
            ) {
                yesButton { }
                noButton { finishAffinity() }
            }.show()
        } else super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        binding.apply {
            settings.name = editTextName.text.toString()
            settings.lastName = editTextLastName.text.toString()
            settings.grade = editTextGrade.text.toString()
            settings.myNumber = editTextMyNumber.text.toString()
            settings.teacher = editTextTeacher.text.toString()
            settings.principal = editTextPrincipal.text.toString()
            settings.parent = editTextParent.text.toString()
        }

        val json = Gson().toJson(settings)
        preferences.edit().putString(KEY_SETTINGS, json).apply()
    }
}
