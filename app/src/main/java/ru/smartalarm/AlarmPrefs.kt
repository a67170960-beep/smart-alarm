package ru.smartalarm

import android.content.Context

class AlarmPrefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("smart_alarm", Context.MODE_PRIVATE)

    var hour: Int
        get() = p.getInt("hour", 7)
        set(v) = p.edit().putInt("hour", v).apply()

    var minute: Int
        get() = p.getInt("minute", 30)
        set(v) = p.edit().putInt("minute", v).apply()

    var enabled: Boolean
        get() = p.getBoolean("enabled", false)
        set(v) = p.edit().putBoolean("enabled", v).apply()

    var soundUri: String?
        get() = p.getString("sound_uri", null)
        set(v) = p.edit().putString("sound_uri", v).apply()

    var soundName: String
        get() = p.getString("sound_name", "Системный") ?: "Системный"
        set(v) = p.edit().putString("sound_name", v).apply()

    /** number of circle taps required to dismiss, 3..10 */
    var tapsNeeded: Int
        get() = p.getInt("taps", 5).coerceIn(3, 10)
        set(v) = p.edit().putInt("taps", v.coerceIn(3, 10)).apply()
}
