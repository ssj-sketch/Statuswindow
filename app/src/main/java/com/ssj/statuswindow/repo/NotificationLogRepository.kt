package com.ssj.statuswindow.repo

import android.content.Context
import android.content.SharedPreferences
import com.ssj.statuswindow.R
import com.ssj.statuswindow.model.AppNotificationLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class NotificationLogRepository private constructor(ctx: Context) {

    private val context = ctx.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_logs", Context.MODE_PRIVATE)

class NotificationLogRepository private constructor(ctx: Context) {

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("notification_logs", Context.MODE_PRIVATE)

    private val _logs = MutableStateFlow<List<AppNotificationLog>>(emptyList())
    val logs: StateFlow<List<AppNotificationLog>> get() = _logs

    init {
        load()
    }

    fun add(entry: AppNotificationLog) {
        addAll(listOf(entry))
    }

    fun addAll(entries: List<AppNotificationLog>) {
        if (entries.isEmpty()) return
        val merged = linkedMapOf<String, AppNotificationLog>()
        _logs.value.forEach { merged[it.id] = it }
        for (entry in entries) {
            merged[entry.id] = entry
        }
        val sorted = merged.values
            .sortedByDescending { it.postedAtEpochMillis }
            .take(MAX_ENTRIES)
        _logs.value = sorted
        save()
    }

    fun snapshot(): List<AppNotificationLog> = _logs.value.toList()

    fun clear() {
        _logs.value = emptyList()
        save()
    }

    private fun load() {
        val json = prefs.getString("logs", "[]") ?: "[]"
        val parsed = mutableListOf<AppNotificationLog>()

        runCatching { JSONArray(json) }
            .onFailure { err ->
                Timber.w(err, "NotificationLogRepository: stored data corrupted, clearing cache")
                prefs.edit().remove("logs").apply()
            }
            .onSuccess { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val packageName = obj.optString("packageName")
                    val postedAtIso = obj.optString("postedAtIso")
                    if (id.isBlank() || packageName.isBlank() || postedAtIso.isBlank()) {
                        Timber.w("NotificationLogRepository: skip malformed entry at %d", i)
                        continue
                    }

                    val entry = AppNotificationLog(
                        id = id,
                        packageName = packageName,
                        appName = obj.optString("appName", packageName).ifBlank { packageName },
                        appCategory = obj.optString("appCategory", context.getString(R.string.category_app_other)),
                        postedAtIso = postedAtIso,
                        postedAtEpochMillis = obj.optLong("postedAtEpochMillis", 0L),
                        notificationCategory = obj.optStringOrNull("notificationCategory"),
                        content = obj.optString("content", "")
                    )

                    parsed.add(entry)
                }
            }

        parsed.sortByDescending { it.postedAtEpochMillis }
        _logs.value = parsed.take(MAX_ENTRIES)
        val arr = JSONArray(json)
        val list = mutableListOf<AppNotificationLog>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val entry = AppNotificationLog(
                id = o.getString("id"),
                packageName = o.getString("packageName"),
                appName = o.optString("appName", o.getString("packageName")),
                appCategory = o.optString("appCategory", "Uncategorized"),
                postedAtIso = o.getString("postedAtIso"),
                postedAtEpochMillis = o.optLong("postedAtEpochMillis", 0L),
                notificationCategory = o.optStringOrNull("notificationCategory"),
                content = o.optString("content", "")
            )
            list.add(entry)
        }
        list.sortByDescending { it.postedAtEpochMillis }
        _logs.value = list.take(MAX_ENTRIES)
    }

    private fun save() {
        val arr = JSONArray()
        _logs.value.forEach { entry ->
            val o = JSONObject()
            o.put("id", entry.id)
            o.put("packageName", entry.packageName)
            o.put("appName", entry.appName)
            o.put("appCategory", entry.appCategory)
            o.put("postedAtIso", entry.postedAtIso)
            o.put("postedAtEpochMillis", entry.postedAtEpochMillis)
            entry.notificationCategory?.let { o.put("notificationCategory", it) }
            o.put("content", entry.content)
            arr.put(o)
        }
        prefs.edit().putString("logs", arr.toString()).apply()
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    companion object {
        private const val MAX_ENTRIES = 500
        @Volatile private var INSTANCE: NotificationLogRepository? = null

        fun instance(ctx: Context): NotificationLogRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationLogRepository(ctx.applicationContext).also { INSTANCE = it }
            }
    }
}
