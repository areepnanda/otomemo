package com.otomemo.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ServiceRecord(
    val id: Long,
    val vehicleName: String,
    val plateNumber: String,
    val date: String,
    val workshop: String,
    val items: String,
    val cost: Long,
    val notes: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("vehicleName", vehicleName)
        put("plateNumber", plateNumber)
        put("date", date)
        put("workshop", workshop)
        put("items", items)
        put("cost", cost)
        put("notes", notes)
    }

    companion object {
        fun fromJson(obj: JSONObject): ServiceRecord = ServiceRecord(
            id = obj.optLong("id"),
            vehicleName = obj.optString("vehicleName"),
            plateNumber = obj.optString("plateNumber"),
            date = obj.optString("date"),
            workshop = obj.optString("workshop"),
            items = obj.optString("items"),
            cost = obj.optLong("cost"),
            notes = obj.optString("notes")
        )
    }
}

object RecordStore {
    private const val PREFS = "otomemo"
    private const val KEY = "records"

    fun load(context: Context): MutableList<ServiceRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
        val list = mutableListOf<ServiceRecord>()
        if (raw != null) {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) list.add(ServiceRecord.fromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun save(context: Context, records: List<ServiceRecord>) {
        val arr = JSONArray()
        records.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
