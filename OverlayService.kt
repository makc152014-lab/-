package com.clawd.pet
import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.view.inputmethod.*
import android.widget.*

class PetView(c: Context) : View(c) {
    var mood = "walk"; var t = 0
    private val p = Paint()
    private val rows = listOf("#########", "##E###E##", "#########")
    override fun onDraw(cv: Canvas) {
        val s = width / 9f; var dy = 0f; var rot = 0f
        val w = Math.sin(t / 3.0).toFloat()
        when (mood) {
            "dance" -> { dy = -Math.abs(w) * s * 1.5f; rot = w * 12f }
            "happy" -> dy = -Math.abs(Math.sin(t / 2.0)).toFloat() * s * 2.5f
            "sad" -> dy = s * 0.5f
        }
        cv.save(); cv.rotate(rot, width / 2f, height / 2f); cv.translate(0f, dy + s * 3)
        val legs = if (mood == "walk" && (t / 4) % 2 == 1) "..#...#.." else ".#.....#."
        (rows + legs).forEachIndexed { y, r -> r.forEachIndexed { x, ch ->
            if (ch != '.') { p.color = if (ch == 'E') 0xFF2B1B14.toInt() else 0xFFD97757.toInt()
                cv.drawRect(x * s, y * s, (x + 1) * s, (y + 1) * s, p) } } }
        cv.restore()
    }
}

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var root: LinearLayout
    private lateinit var pet: PetView
    private lateinit var bubble: TextView
    private lateinit var input: EditText
    private val h = Handler(Looper.getMainLooper())
    private var dir = 4; private var dragging = false; private var moodUntil = 0L; private var lastAuto = 0L
    private val hideBubble = Runnable { bubble.visibility = View.GONE }
    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("clawd", "Clawd", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "clawd").setContentTitle("Clawd живе на екрані").setSmallIcon(android.R.drawable.star_on).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(1, n)

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        val bg = { GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(16).toFloat() } }
        bubble = TextView(this).apply { setTextColor(Color.BLACK); textSize = 14f; setPadding(dp(12), dp(8), dp(12), dp(8))
            maxWidth = dp(220); background = bg(); visibility = View.GONE }
        input = EditText(this).apply { hint = "Напиши Clawd…"; setTextColor(Color.BLACK); setHintTextColor(Color.GRAY)
            textSize = 14f; maxWidth = dp(220); minWidth = dp(180); setSingleLine(); imeOptions = EditorInfo.IME_ACTION_SEND
            background = bg(); setPadding(dp(12), dp(8), dp(12), dp(8)); visibility = View.GONE
            setOnEditorActionListener { v, _, _ -> val t = v.text.toString().trim(); closeInput(); if (t.isNotEmpty()) ask(t); true } }
        pet = PetView(this)
        root.addView(bubble); root.addView(input); root.addView(pet, LinearLayout.LayoutParams(dp(96), dp(96)))

        lp = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT)
        lp.gravity = Gravity.TOP or Gravity.START; lp.x = dp(40); lp.y = dp(400)
        wm.addView(root, lp)

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var downAt = 0L
        pet.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; dragging = false; downAt = System.currentTimeMillis() }
                MotionEvent.ACTION_MOVE -> if (Math.abs(e.rawX - sx) > dp(8) || Math.abs(e.rawY - sy) > dp(8)) {
                    dragging = true; lp.x = ox + (e.rawX - sx).toInt(); lp.y = oy + (e.rawY - sy).toInt(); wm.updateViewLayout(root, lp) }
                MotionEvent.ACTION_UP -> { if (!dragging) { if (System.currentTimeMillis() - downAt > 500) { mood("happy", 3000); say("Мурр~ 💛") } else openInput() }
                    dragging = false }
            }
            true
        }

        Bus.music = { track -> mood("dance", 25000); if (System.currentTimeMillis() - lastAuto > 60000) { lastAuto = System.currentTimeMillis()
            ask("Зараз грає: $track. Коротко прокоментуй.", true) } }
        Bus.msg = { m -> val l = m.lowercase()
            when { Regex("привіт|привет|hello").containsMatchIn(l) -> { mood("happy", 3000); say("Привіт! 👋", 3000) }
                Regex("люблю|love").containsMatchIn(l) -> { mood("happy", 4000); say("Ой, любов! 💛", 3000) }
                Regex("ахах|хаха|haha").containsMatchIn(l) -> { mood("happy", 3000); say("Хіхі 😄", 3000) }
                else -> say("Нове повідомлення!", 2500) } }
        tick()
    }

    private fun tick() {
        pet.t++
        if (System.currentTimeMillis() > moodUntil && pet.mood != "walk") pet.mood = "walk"
        if (!dragging && pet.mood == "walk" && input.visibility == View.GONE) {
            val maxX = resources.displayMetrics.widthPixels - root.width
            lp.x += dir; if (lp.x <= 0 || lp.x >= maxX) { dir = -dir; lp.x = lp.x.coerceIn(0, maxOf(0, maxX)) }
            wm.updateViewLayout(root, lp)
        }
        pet.invalidate(); h.postDelayed({ tick() }, 60)
    }

    private fun mood(m: String, ms: Long) { pet.mood = m; moodUntil = System.currentTimeMillis() + ms }
    private fun say(t: String, ms: Long = 7000) { bubble.text = t; bubble.visibility = View.VISIBLE; h.removeCallbacks(hideBubble); h.postDelayed(hideBubble, ms) }

    private fun openInput() {
        input.visibility = View.VISIBLE; lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        wm.updateViewLayout(root, lp); input.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, 0)
    }
    private fun closeInput() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(input.windowToken, 0)
        input.setText(""); input.visibility = View.GONE; lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        wm.updateViewLayout(root, lp)
    }

    private fun ask(text: String, auto: Boolean = false) {
        if (!auto) say("…", 30000)
        Thread {
            val r = try { Brain.ask(this, text) } catch (e: Exception) { "[sad] ${e.message}" }
            h.post {
                val m = Regex("^\\[(happy|dance|sad|calm)]\\s*").find(r)
                if (m != null) mood(if (m.groupValues[1] == "calm") "walk" else m.groupValues[1], 6000)
                say(r.removePrefix(m?.value ?: ""))
            }
        }.start()
    }

    override fun onDestroy() { Bus.music = null; Bus.msg = null; h.removeCallbacksAndMessages(null); wm.removeView(root); super.onDestroy() }
}
