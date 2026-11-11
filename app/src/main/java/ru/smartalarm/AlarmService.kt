package ru.smartalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAll()
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotif())
        startSound()
        startVibrate()
        return START_STICKY
    }

    private fun startSound() {
        val prefs = AlarmPrefs(this)
        try {
            player?.release()
            player = MediaPlayer().apply {
                if (!prefs.soundUri.isNullOrBlank()) {
                    setDataSource(applicationContext, Uri.parse(prefs.soundUri))
                } else {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(applicationContext, uri)
                }
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setVolume(1f, 1f)
                prepare()
                start()
            }
        } catch (_: Exception) {
            try {
                player?.release()
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                player = MediaPlayer.create(this, uri)?.apply {
                    isLooping = true
                    start()
                }
            } catch (_: Exception) { }
        }
    }

    private fun startVibrate() {
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 400, 500, 400)
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAll() {
        try { player?.stop() } catch (_: Exception) {}
        player?.release()
        player = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAll()
        super.onDestroy()
    }

    private fun buildNotif(): Notification {
        val chId = "alarm_ch"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(chId, "Alarm", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, RingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, chId)
            .setContentTitle("Будильник")
            .setContentText("Тапни кружки, чтобы выключить")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIF_ID = 42
        const val ACTION_STOP = "ru.smartalarm.STOP"
        fun stop(ctx: android.content.Context) {
            val i = Intent(ctx, AlarmService::class.java).setAction(ACTION_STOP)
            ctx.startService(i)
        }
    }
}
