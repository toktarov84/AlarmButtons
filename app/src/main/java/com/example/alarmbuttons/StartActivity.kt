package com.example.alarmbuttons

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.alarmbuttons.Bridge.KEY_SETTINGS
import com.example.alarmbuttons.Bridge.settings
import com.example.alarmbuttons.Bridge.preferences
import com.example.alarmbuttons.databinding.ActivityStartBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import org.jetbrains.anko.*

class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val degree = listOf("Низкий", "Средний", "Высокий")
    private lateinit var latitude: String
    private lateinit var longitude: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            loadSettings()
            setPermissions()
            loadLocation()
        } catch (e: Exception) { toast("Неизвестная ошибка!!!") }

        binding.apply {
            imageViewFire.setOnClickListener { sendAlert("Пожар", 2, settings.fireDepartment) }
            imageViewAssault.setOnClickListener { sendAlert("Нападение", 2, settings.police) }
            imageViewFight.setOnClickListener { sendAlert("Драка", 1) }
            imageViewTheft.setOnClickListener { sendAlert("Кража", 1) }
            imageViewAmbulance.setOnClickListener { sendAlert("Скорая", 2, settings.ambulance) }
            imageViewOther.setOnClickListener { sendAlert("Другое") }
        }
    }

    private fun loadSettings() {
        preferences = getSharedPreferences(KEY_SETTINGS, MODE_PRIVATE)
        val json = preferences.getString(KEY_SETTINGS, "")
        if (!json.isNullOrEmpty()) settings = Gson().fromJson(json, Settings::class.java)

        if (settings.name == "" || settings.lastName == "" || settings.grade == "" || settings.myNumber == "") {
            alert ("Для продолжения работы с\n" +
                    "приложением, необходимо\n" +
                    "ввести ваши имя, фамилию\n" +
                    "класс и номер телефона.", "Внимание!"
            ) {
                yesButton {
                    startActivity<SettingsActivity>()
                }
                noButton { finishAffinity() }
            }.show()
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun loadLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                latitude = it.latitude.toString()
                longitude = it.longitude.toString()
            }
        }
    }

    private fun setPermissions() {
        if (!isPermissionCallPhone() || !isPermissionSendSMS() || !isPermissionFineLocation() || !isPermissionCoarseLocation()) {
            alert ("Для применения разрешений необходимо перезапустить приложение.") {
                positiveButton("Перезапустить") { recreate() }
                noButton {  }
            }.show()
        }

        if (!isPermissionCallPhone() || !isPermissionSendSMS() || !isPermissionFineLocation() || !isPermissionCoarseLocation()) {
            alert ("Это приложение требует\n" +
                    "разрешения звонить,\n" +
                    "отправлять смс и доступ\n" +
                    "к координатам по GPS!", "Внимание!") {
                yesButton {
                    ActivityCompat.requestPermissions(this@StartActivity, arrayOf(Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 987)
                }
                noButton { finishAffinity() }
            }.show()
        }
    }

    private fun sendAlert(info: String, lvlArg: Int = 0, tel: String = settings.teacher) {
        var lvl = lvlArg

        alert ("Вы уверены? После согласия\n" +
                "информация поступит в\n" +
                "соответствующие службы!", "$info?") {
            positiveButton ("Да") {
                sendSMS(info, lvl, settings.principal)
                sendSMS(info, lvl, settings.teacher)
                sendSMS(info, lvl, settings.parent)

                try {
                    makeCall(tel)
                } catch (e: Exception) {  }

                settings.count++
            }
            noButton {  }
        }.show()

        if (lvl == 0) {
            selector("Выберите уровень опасности!", degree) { _, i ->
                lvl = i
            }
        }
    }

    private fun sendSMS(info: String, lvl: Int, tel: String) {
        try {
            SmsManager.getDefault().sendTextMessage(tel, null, smsText(info, lvl), null, null)
            if (latitude.isNotEmpty()) {
                SmsManager.getDefault().sendTextMessage(tel, null, "широта: $latitude долгота: $longitude", null, null)
            }
        } catch (e: Exception) { toast("Невозможно отправить СМС на номер $tel") }
    }

    private fun smsText(info: String, lvl: Int): String {
        return "${settings.myNumber.takeLast(3)} ${settings.count} $info ${degree[lvl]} ${settings.lastName} ${settings.name} ${settings.grade}"
    }

    private fun isPermissionCallPhone(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    private fun isPermissionSendSMS(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun isPermissionFineLocation(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    private fun isPermissionCoarseLocation(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSettings -> startActivity<SettingsActivity>()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        val json = Gson().toJson(settings)
        preferences.edit().putString(KEY_SETTINGS, json).apply()
    }
}