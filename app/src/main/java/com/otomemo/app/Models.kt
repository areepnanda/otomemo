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
    val odometer: Long,
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
        put("odometer", odometer)
        put("items", items)
        put("cost", cost)
        put("notes", notes)
    }

    companion object {
        fun fromJson(obj: JSONObject): ServiceRecord = ServiceRecord(
            obj.optLong("id"),
            obj.optString("vehicleName"),
            obj.optString("plateNumber"),
            obj.optString("date"),
            obj.optString("workshop"),
            obj.optLong("odometer"),
            obj.optString("items"),
            obj.optLong("cost"),
            obj.optString("notes")
        )
    }
}

data class Vehicle(val id: Long, var brand: String, var model: String, var year: String, var plate: String) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("brand", brand)
        put("model", model)
        put("year", year)
        put("plate", plate)
    }

    companion object {
        fun fromJson(obj: JSONObject) = Vehicle(
            obj.optLong("id"),
            obj.optString("brand", obj.optString("name")),
            obj.optString("model"),
            obj.optString("year"),
            obj.optString("plate")
        )
    }
}

data class Workshop(val id: Long, var name: String) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
    }

    companion object {
        fun fromJson(obj: JSONObject) = Workshop(obj.optLong("id"), obj.optString("name"))
    }
}

data class ServiceItem(val id: Long, var name: String) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
    }

    companion object {
        fun fromJson(obj: JSONObject) = ServiceItem(obj.optLong("id"), obj.optString("name"))
    }
}

data class MasterRow(val id: Long, val title: String, val subtitle: String)

object Db {
    private const val PREFS = "otomemo"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun <T> load(context: Context, key: String, parse: (JSONObject) -> T): MutableList<T> {
        val raw = prefs(context).getString(key, null) ?: return mutableListOf()
        val arr = JSONArray(raw)
        val list = mutableListOf<T>()
        for (i in 0 until arr.length()) list.add(parse(arr.getJSONObject(i)))
        return list
    }

    private fun <T> save(context: Context, key: String, list: List<T>, toJson: (T) -> JSONObject) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs(context).edit().putString(key, arr.toString()).apply()
    }

    fun loadRecords(context: Context) = load(context, "records", ServiceRecord::fromJson)
    fun saveRecords(context: Context, list: List<ServiceRecord>) = save(context, "records", list) { it.toJson() }

    fun loadVehicles(context: Context) = load(context, "vehicles", Vehicle::fromJson)
    fun saveVehicles(context: Context, list: List<Vehicle>) = save(context, "vehicles", list) { it.toJson() }

    fun loadWorkshops(context: Context) = load(context, "workshops", Workshop::fromJson)
    fun saveWorkshops(context: Context, list: List<Workshop>) = save(context, "workshops", list) { it.toJson() }

    fun loadItems(context: Context) = load(context, "items", ServiceItem::fromJson)
    fun saveItems(context: Context, list: List<ServiceItem>) = save(context, "items", list) { it.toJson() }
}
