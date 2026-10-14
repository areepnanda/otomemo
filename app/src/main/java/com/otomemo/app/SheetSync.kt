package com.otomemo.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object SheetSync {

    private const val PREFS = "otomemo_sync"
    private const val KEY_URL = "url"
    private const val KEY_TOKEN = "token"
    private const val KEY_QUEUE = "queue"
    private const val KEY_LAST_SYNC = "last_sync"

    private const val TBL_VEHICLES = "vehicles"
    private const val TBL_ITEMS = "items"
    private const val TBL_WORKSHOPS = "workshops"
    private const val TBL_RECORDS = "records"

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun url(context: Context) = prefs(context).getString(KEY_URL, "").orEmpty()

    fun token(context: Context) = prefs(context).getString(KEY_TOKEN, "").orEmpty()

    fun isConfigured(context: Context) = url(context).isNotEmpty() && token(context).isNotEmpty()

    fun saveConfig(context: Context, url: String, token: String) {
        prefs(context).edit()
            .putString(KEY_URL, url.trim())
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }

    fun lastSyncLabel(context: Context): String {
        val t = prefs(context).getLong(KEY_LAST_SYNC, 0L)
        if (t == 0L) return "Belum pernah sinkron"
        val fmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return "Terakhir sinkron: ${fmt.format(Date(t))}"
    }

    fun upsert(context: Context, table: String, row: JSONObject) {
        if (!isConfigured(context)) return
        val payload = JSONObject()
            .put("token", token(context))
            .put("action", "upsert")
            .put("table", table)
            .put("row", row)
        sendAsync(context, payload)
    }

    fun delete(context: Context, table: String, id: Long) {
        if (!isConfigured(context)) return
        val payload = JSONObject()
            .put("token", token(context))
            .put("action", "delete")
            .put("table", table)
            .put("id", id)
        sendAsync(context, payload)
    }

    fun replaceAll(context: Context, onDone: ((Boolean) -> Unit)? = null) {
        if (!isConfigured(context)) {
            onDone?.invoke(false)
            return
        }
        val data = JSONObject()
            .put(TBL_VEHICLES, JSONArray(Db.loadVehicles(context).map { it.toJson() }))
            .put(TBL_ITEMS, JSONArray(Db.loadItems(context).map { it.toJson() }))
            .put(TBL_WORKSHOPS, JSONArray(Db.loadWorkshops(context).map { it.toJson() }))
            .put(TBL_RECORDS, JSONArray(Db.loadRecords(context).map { it.toJson() }))
        val payload = JSONObject()
            .put("token", token(context))
            .put("action", "replaceAll")
            .put("data", data)
        writeQueue(context, mutableListOf())
        sendAsync(context, payload, onDone)
    }

    fun flushPending(context: Context) {
        if (!isConfigured(context)) return
        val appCtx = context.applicationContext
        executor.execute { flush(appCtx) }
    }

    private fun sendAsync(
        context: Context,
        payload: JSONObject,
        onDone: ((Boolean) -> Unit)? = null
    ) {
        val appCtx = context.applicationContext
        executor.execute {
            val ok = send(appCtx, payload)
            if (ok) flush(appCtx) else queue(appCtx, payload)
            onDone?.let { cb -> main.post { cb(ok) } }
        }
    }

    private fun flush(context: Context): Boolean {
        val pending = readQueue(context)
        if (pending.isEmpty()) return true
        for (i in pending.indices) {
            if (!send(context, pending[i])) {
                writeQueue(context, pending.subList(i, pending.size))
                return false
            }
        }
        writeQueue(context, mutableListOf())
        return true
    }

    private fun send(context: Context, payload: JSONObject): Boolean = try {
        post(url(context), payload.toString())
        prefs(context).edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
        true
    } catch (_: Exception) {
        false
    }

    private fun readQueue(context: Context): MutableList<JSONObject> {
        val raw = prefs(context).getString(KEY_QUEUE, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapTo(mutableListOf()) { arr.getJSONObject(it) }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun writeQueue(context: Context, ops: List<JSONObject>) {
        val arr = JSONArray()
        ops.forEach { arr.put(it) }
        prefs(context).edit().putString(KEY_QUEUE, arr.toString()).apply()
    }

    private fun queue(context: Context, op: JSONObject) {
        val ops = readQueue(context)
        ops.add(op)
        writeQueue(context, ops)
    }

    private fun post(urlStr: String, body: String): String {
        var current = urlStr
        var redirects = 0
        while (true) {
            val conn = URL(current).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                        ?: throw IOException("Redirect tanpa tujuan (HTTP $code)")
                    current = URL(URL(current), loc).toString()
                    if (++redirects > 5) throw IOException("Terlalu banyak redirect")
                    continue
                }
                if (code !in 200..299) {
                    val err = try {
                        conn.errorStream?.bufferedReader()?.readText()?.take(200)
                    } catch (_: Exception) {
                        ""
                    }
                    throw IOException("HTTP $code${if (err.isEmpty()) "" else ": $err"}")
                }
                return conn.inputStream.bufferedReader().readText()
            } finally {
                conn.disconnect()
            }
        }
    }
}
