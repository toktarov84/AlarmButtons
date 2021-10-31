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

// Основной экран программы
class StartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStartBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val degree = listOf("Низкий", "Средний", "Высокий") // список с уровнями опасности
    private lateinit var latitude: String // широта
    private lateinit var longitude: String // долгота

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            loadSettings() // Загружает и проверяет настройки
            setPermissions() // Проверяет и запрашивает разрешения
            loadLocation() // Загружает координаты по GPS
        } catch (e: Exception) { toast("Неизвестная ошибка!!!") } // Обрабатываем возможные ошибки

        // Назначает слушателей на кнопки
        binding.apply {
            imageViewFire.setOnClickListener { sendAlert("Пожар", 2, settings.fireDepartment) }
            imageViewAssault.setOnClickListener { sendAlert("Нападение", 2, settings.police) }
            imageViewFight.setOnClickListener { sendAlert("Драка", 1) }
            imageViewTheft.setOnClickListener { sendAlert("Кража", 1) }
            imageViewAmbulance.setOnClickListener { sendAlert("Скорая", 2, settings.ambulance) }
            imageViewOther.setOnClickListener { sendAlert("Другое") }
        }
    }

    // Загружает и проверяет настройки
    private fun loadSettings() {
        preferences = getSharedPreferences(KEY_SETTINGS, MODE_PRIVATE) // открываем файл настроек приложения для чтения и записи
        val json = preferences.getString(KEY_SETTINGS, "") // Загружаем настройки из файла настроек приложения в json строку
        if (!json.isNullOrEmpty()) settings = Gson().fromJson(json, Settings::class.java) // Если настройки не пусты, получаем объект класса Settings из json

        // Если приложение запущенно первый раз или не заполнены поля name, lastName и myNumber
        if (settings.name == "" || settings.lastName == "" || settings.grade == "" || settings.myNumber == "") {
            // Предупреждает о необходимости полей для работы приложения
            alert ("Для продолжения работы с\n" +
                    "приложением, необходимо\n" +
                    "ввести ваши имя, фамилию\n" +
                    "класс и номер телефона.", "Внимание!"
            ) {
                yesButton {
                    startActivity<SettingsActivity>() // Если "Ok", то перемещаемся на экран настроек
                }
                noButton { finishAffinity() } // Если "Отмена", завершаем приложение
            }.show()
        }
    }

    // Загружает координаты по GPS
    @SuppressLint("MissingPermission")
    private fun loadLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) // создаём службу геолокации
        // Загружаем текущее местоположение и сохранёем широту и долготу
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                latitude = it.latitude.toString()
                longitude = it.longitude.toString()
            }
        }
    }

    // Проверяет и запрашивает разрешения (последовательные диалоги накладываются друг на друга, поэтому идут в обратном порядке)
    private fun setPermissions() {
        // Если приложению запрещены звонки, отправка СМС и геолокация, выдаём диалог о том, что требуется перезапуск, он нужен для определения координат (2й диалог)
        if (!isPermissionCallPhone() || !isPermissionSendSMS() || !isPermissionFineLocation() || !isPermissionCoarseLocation()) {
            alert ("Для применения разрешений необходимо перезапустить приложение.") {
                positiveButton("Перезапустить") { recreate() } // Если "Перезапустить", то перезапускаем приложение
                noButton {  } // Если "Отмена", продолжаем работу с приложением, на случай отсутствие на устройстве Google сервисов
            }.show()
        }

        // Если приложению запрещены звонки, отправка СМС и геолокация, выдаём диалог предупреждения (1й диалог)
        if (!isPermissionCallPhone() || !isPermissionSendSMS() || !isPermissionFineLocation() || !isPermissionCoarseLocation()) {
            alert ("Это приложение требует\n" +
                    "разрешения звонить,\n" +
                    "отправлять смс и доступ\n" +
                    "к координатам по GPS!", "Внимание!") {
                // Если "Ok", запрашиваем разрешения
                yesButton {
                    ActivityCompat.requestPermissions(this@StartActivity, arrayOf(Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 987)
                }
                noButton { finishAffinity() } // Если "Отмена", завершаем приложение
            }.show()
        }
    }

    private fun sendAlert(info: String, lvlArg: Int = 0, tel: String = settings.teacher) {
        // аргумент метода защищён от записи, поэтому создаём локальную переменную
        var lvl = lvlArg

        // Диалог подтверждения
        alert ("Вы уверены? После согласия\n" +
                "информация поступит в\n" +
                "соответствующие службы!", "$info?") {
            // Если "Да", то отправляем СМС и делаем звонок
            positiveButton ("Да") {
                sendSMS(info, lvl, settings.principal)
                sendSMS(info, lvl, settings.teacher)
                sendSMS(info, lvl, settings.parent)

                try {
                    makeCall(tel)
                } catch (e: Exception) {  }

                // увеличиваем счётчик использования "тревожной кнопки", это поможет от заведомо ложных оповещений
                settings.count++

                toast(smsText(info, lvl)) // метод отладки смс (не будем же мы по настоящему звонить в полицию?)
            }
            noButton {  }
        }.show()

        // Если в оргументах метода не укзан уровень угрозы или указан 0(низкий), то запрашиваем и изменяем уровень угрозы
        if (lvl == 0) {
            selector("Выберите уровень опасности!", degree) { _, i ->
                lvl = i
            }
        }
    }

    private fun sendSMS(info: String, lvl: Int, tel: String) {
        try {
            SmsManager.getDefault().sendTextMessage(tel, null, smsText(info, lvl), null, null)
            // Если не удалось получить координаты, то отправляет их во втором СМС сообщении
            if (latitude.isNotEmpty()) {
                SmsManager.getDefault().sendTextMessage(tel, null, "широта: $latitude долгота: $longitude", null, null)
            }
        } catch (e: Exception) { toast("Невозможно отправить СМС на номер $tel") }// На случай неправильных номеров и невозможности отправить СМС
    }

    // Возвращает строку с СМС сообщением:
    // "3 последний цифры номера" + "количество нажатий тревожной кнопки" + "уровень опасности" + "что произошло" + "фамилия" + "имя" + "класс"
    // Последние цифры номера, это защита от дурака, если номер не совпадает с реальным, то высока вероятность ложной тревоги
    // Длинна смс кирилицей сильно ограниченна, поэтому сообщенние такое "компактное"
    private fun smsText(info: String, lvl: Int): String {
        return "${settings.myNumber.takeLast(3)} ${settings.count} $info ${degree[lvl]} ${settings.lastName} ${settings.name} ${settings.grade}"
    }

    // Проверяем разрешение на звонки
    private fun isPermissionCallPhone(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    // Проверяем разрешение на отправку СМС
    private fun isPermissionSendSMS(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    // Проверяем разрешени на геолокацию
    private fun isPermissionFineLocation(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    private fun isPermissionCoarseLocation(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Создаём меню в actionBar из файла R.menu.menu_main.xml
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // При нажатии на пункт меню "Настройки", переходим на экран с настройками
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSettings -> startActivity<SettingsActivity>()
        }
        return super.onOptionsItemSelected(item)
    }

    // Сохраняем настройки приложения в состоянии паузы (на этом экране количество нажатий "тревожная кнопка")
    override fun onPause() {
        super.onPause()

        val json = Gson().toJson(settings) // Сохраняем объект settings(настройки) в json строку
        preferences.edit().putString(KEY_SETTINGS, json).apply() // Сохраняем json в файл настроек приложения
    }
}