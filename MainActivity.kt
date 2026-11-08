package com.clawd.pet
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        val sp = getSharedPreferences("p", 0); val d = resources.displayMetrics.density
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((20 * d).toInt(), (40 * d).toInt(), (20 * d).toInt(), 20) }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }.also { col.addView(it) }
        col.addView(TextView(this).apply { text = "Clawd Pet"; textSize = 26f; gravity = Gravity.CENTER })
        val rg = RadioGroup(this); val g = RadioButton(this).apply { text = "Gemini (безкоштовно)"; id = 1 }
        val c = RadioButton(this).apply { text = "Claude"; id = 2 }; rg.addView(g); rg.addView(c)
        if (sp.getString("prov", "gemini") == "claude") c.isChecked = true else g.isChecked = true
        col.addView(rg)
        val key = EditText(this).apply { hint = "API-ключ"; setText(sp.getString("key", "")); setSingleLine() }; col.addView(key)
        btn("Зберегти") { sp.edit().putString("key", key.text.toString().trim()).putString("prov", if (c.isChecked) "claude" else "gemini").apply()
            Toast.makeText(this, "Збережено", Toast.LENGTH_SHORT).show() }
        btn("1. Дозвіл «поверх екрану»") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
        btn("2. Доступ до сповіщень (музика/чати)") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        btn("3. Запустити Clawd") { if (Settings.canDrawOverlays(this)) startForegroundService(Intent(this, OverlayService::class.java))
            else Toast.makeText(this, "Спершу дай дозвіл №1", Toast.LENGTH_LONG).show() }
        btn("Зупинити") { stopService(Intent(this, OverlayService::class.java)) }
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
