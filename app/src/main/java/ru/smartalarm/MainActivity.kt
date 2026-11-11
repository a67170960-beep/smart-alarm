package ru.smartalarm

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: AlarmPrefs
    private lateinit var timePicker: TimePicker
    private lateinit var soundName: TextView
    private lateinit var difficultyLabel: TextView
    private lateinit var status: TextView

    private val pickSound = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { }
        prefs.soundUri = uri.toString()
        prefs.soundName = uri.lastPathSegment?.substringAfterLast('/') ?: "Свой файл"
        soundName.text = prefs.soundName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = AlarmPrefs(this)

        timePicker = findViewById(R.id.timePicker)
        soundName = findViewById(R.id.soundName)
        difficultyLabel = findViewById(R.id.difficultyLabel)
        status = findViewById(R.id.status)
        val switchEnabled = findViewById<SwitchMaterial>(R.id.switchEnabled)
        val difficulty = findViewById<SeekBar>(R.id.difficulty)
        val timeLabel = findViewById<TextView>(R.id.timeLabel)

        timePicker.setIs24HourView(true)
        if (Build.VERSION.SDK_INT >= 23) {
            timePicker.hour = prefs.hour
            timePicker.minute = prefs.minute
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour = prefs.hour
            @Suppress("DEPRECATION")
            timePicker.currentMinute = prefs.minute
        }
        timeLabel.text = fmt(prefs.hour, prefs.minute)
        soundName.text = prefs.soundName
        switchEnabled.isChecked = prefs.enabled
        difficulty.progress = (prefs.tapsNeeded - 3).coerceIn(0, 7)
        difficultyLabel.text = "${prefs.tapsNeeded} кружков"

        timePicker.setOnTimeChangedListener { _, h, m ->
            timeLabel.text = fmt(h, m)
        }
        difficulty.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val n = progress + 3
                difficultyLabel.text = "$n кружков"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnPickSound).setOnClickListener {
            ensureMediaPermission()
            pickSound.launch(arrayOf("audio/*", "application/ogg", "*/*"))
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            save(switchEnabled.isChecked, difficulty.progress + 3)
        }
        findViewById<Button>(R.id.btnTest).setOnClickListener {
            save(true, difficulty.progress + 3)
            startActivity(Intent(this, RingActivity::class.java))
            startForegroundService(Intent(this, AlarmService::class.java))
        }

        requestNotifPermission()
        ensureExactAlarm()
        refreshStatus()
    }

    private fun save(enabled: Boolean, taps: Int) {
        val h = if (Build.VERSION.SDK_INT >= 23) timePicker.hour else @Suppress("DEPRECATION") timePicker.currentHour
        val m = if (Build.VERSION.SDK_INT >= 23) timePicker.minute else @Suppress("DEPRECATION") timePicker.currentMinute
        prefs.hour = h
        prefs.minute = m
        prefs.tapsNeeded = taps
        prefs.enabled = enabled
        if (enabled) {
            AlarmScheduler.schedule(this, h, m)
            status.text = "Включён на ${fmt(h, m)} · ${taps} кружков"
        } else {
            AlarmScheduler.cancel(this)
            status.text = "Выключен"
        }
    }

    private fun refreshStatus() {
        status.text = if (prefs.enabled)
            "Включён на ${fmt(prefs.hour, prefs.minute)} · ${prefs.tapsNeeded} кружков"
        else "Выключен"
    }

    private fun fmt(h: Int, m: Int) = "%02d:%02d".format(h, m)

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun ensureMediaPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 2)
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
        }
    }

    private fun ensureExactAlarm() {
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (_: Exception) { }
            }
        }
    }
}
