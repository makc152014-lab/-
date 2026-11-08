package com.clawd.pet
import android.content.Context
import org.json.*
import java.net.*

object Brain {
    const val SYS = "Ти Clawd — милий піксельний пітомець, що живе поверх екрану телефону. Відповідай мовою користувача, коротко (1-2 речення), тепло, з емоціями. Завжди починай відповідь одним тегом настрою: [happy], [dance], [sad] або [calm]."
    private val hist = ArrayList<Pair<String, String>>()

    fun ask(c: Context, text: String): String {
        val p = c.getSharedPreferences("p", 0)
        val key = p.getString("key", "") ?: ""
        if (key.isBlank()) return "[sad] Встав API-ключ у налаштуваннях"
        hist.add("user" to text)
        while (hist.size > 10) hist.removeAt(0)
        while (hist.first().first != "user") hist.removeAt(0)
        val out = try { if (p.getString("prov", "gemini") == "claude") claude(key) else gemini(key) }
        catch (e: Exception) { hist.removeAt(hist.size - 1); throw e }
        hist.add("assistant" to out)
        return out
    }

    private fun gemini(key: String): String {
        val contents = JSONArray()
        hist.forEach { (r, t) -> contents.put(JSONObject().put("role", if (r == "user") "user" else "model")
            .put("parts", JSONArray().put(JSONObject().put("text", t)))) }
        val body = JSONObject().put("contents", contents).put("systemInstruction",
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYS))))
        val r = post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            mapOf("x-goog-api-key" to key), body)
        return JSONObject(r).getJSONArray("candidates").getJSONObject(0).getJSONObject("content")
            .getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun claude(key: String): String {
        val msgs = JSONArray()
        hist.forEach { (r, t) -> msgs.put(JSONObject().put("role", r).put("content", t)) }
        val body = JSONObject().put("model", "claude-sonnet-5-5").put("max_tokens", 300).put("system", SYS).put("messages", msgs)
        val r = post("https://api.anthropic.com/v1/messages",
            mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01"), body)
        return JSONObject(r).getJSONArray("content").getJSONObject(0).getString("text")
    }

    private fun post(url: String, hdr: Map<String, String>, body: JSONObject): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("Content-Type", "application/json")
        hdr.forEach { (k, v) -> c.setRequestProperty(k, v) }
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val txt = (if (code < 400) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code >= 400) throw Exception("HTTP $code: ${txt.take(120)}")
        return txt
    }
}

object Bus { var music: ((String) -> Unit)? = null; var msg: ((String) -> Unit)? = null }
