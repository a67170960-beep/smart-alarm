package ru.smartalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val i = Intent(context, AlarmService::class.java)
        ContextCompat.startForegroundService(context, i)
        val ring = Intent(context, RingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(ring)
    }
}
