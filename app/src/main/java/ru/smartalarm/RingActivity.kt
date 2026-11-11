package ru.smartalarm

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class RingActivity : AppCompatActivity() {
    private var needed = 5
    private var done = 0
    private lateinit var root: FrameLayout
    private lateinit var hint: TextView
    private val circleSizeDp = 72

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContentView(R.layout.activity_ring)
        root = findViewById(R.id.ringRoot)
        hint = findViewById(R.id.ringHint)
        val prefs = AlarmPrefs(this)
        needed = prefs.tapsNeeded
        findViewById<TextView>(R.id.ringTime).text =
            "%02d:%02d".format(prefs.hour, prefs.minute)
        updateHint()
        root.post { spawnCircle() }
    }

    private fun updateHint() {
        hint.text = "Тапни по кружкам: $done / $needed"
    }

    private fun spawnCircle() {
        // remove old floating targets (keep header views)
        val toRemove = mutableListOf<View>()
        for (i in 0 until root.childCount) {
            val v = root.getChildAt(i)
            if (v.tag == "circle") toRemove.add(v)
        }
        toRemove.forEach { root.removeView(it) }

        if (done >= needed) {
            dismissAlarm()
            return
        }

        val size = (circleSizeDp * resources.displayMetrics.density).toInt()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val w = root.width
        val h = root.height
        if (w <= 0 || h <= 0) {
            root.postDelayed({ spawnCircle() }, 50)
            return
        }
        val maxX = (w - size - pad).coerceAtLeast(pad)
        val maxY = (h - size - pad).coerceAtLeast(pad)
        // keep away from top header roughly
        val minY = (160 * resources.displayMetrics.density).toInt()
        val x = Random.nextInt(pad, maxX)
        val y = Random.nextInt(minY.coerceAtMost(maxY), maxY)

        val circle = View(this).apply {
            tag = "circle"
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                leftMargin = x
                topMargin = y
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF2C2C2E.toInt())
                setStroke((3 * resources.displayMetrics.density).toInt(), 0xFFFFFFFF.toInt())
            }
            isClickable = true
            setOnClickListener {
                done++
                updateHint()
                // brief flash
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFFFFFFFF.toInt())
                }
                postDelayed({ spawnCircle() }, 120)
            }
        }
        root.addView(circle)
    }

    private fun dismissAlarm() {
        AlarmService.stop(this)
        // reschedule next day if still enabled
        val prefs = AlarmPrefs(this)
        if (prefs.enabled) {
            AlarmScheduler.schedule(this, prefs.hour, prefs.minute)
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // block back — must complete circles
    }
}
