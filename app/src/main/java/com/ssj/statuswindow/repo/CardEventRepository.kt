package com.ssj.statuswindow.repo

import android.content.Context
import android.content.SharedPreferences
import com.ssj.statuswindow.model.CardEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * 간단 로컬 저장(SharedPreferences) + 메모리 캐시.
 * - 중복제거: (time + merchant + amount) 정규화 키 사용(부호 포함)
 * - 같은 앱에서 여러번 수집되어도, 키가 다르면 모두 보임
 * - 부가필드(cardBrand 등)도 JSON으로 함께 저장/복원
 */
class CardEventRepository private constructor(ctx: Context) {

    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("card_events", Context.MODE_PRIVATE)

    private val _events = MutableStateFlow<List<CardEvent>>(emptyList())
    val events: StateFlow<List<CardEvent>> get() = _events

    private val keys = ConcurrentHashMap.newKeySet<String>()

    init {
        load()
    }

    fun addAll(list: List<CardEvent>) {
        if (list.isEmpty()) return
        val cur = _events.value.toMutableList()
        for (e in list) {
            val key = normalizedKey(e)
            if (keys.add(key)) {
                cur.add(e)
            }
        }
        cur.sortByDescending { it.time }
        _events.value = cur
        save()
    }

    fun add(e: CardEvent) = addAll(listOf(e))

    private fun normalizedKey(e: CardEvent): String {
        fun norm(s: String) = s.trim().replace("\\s+".toRegex(), " ").lowercase()
        return "${norm(e.time)}|${norm(e.merchant)}|${e.amount}"
    }

    private fun load() {
        val json = prefs.getString("events", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<CardEvent>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val e = CardEvent(
                id = o.getString("id"),
                time = o.getString("time"),
                merchant = o.getString("merchant"),
                amount = o.getLong("amount"),
                sourceApp = o.getString("sourceApp"),
                raw = o.optString("raw", ""),
                cardBrand = o.optStringOrNull("cardBrand"),
                cardLast4 = o.optStringOrNull("cardLast4"),
                installmentMonths = o.optIntOrNull("installmentMonths"),
                category = o.optStringOrNull("category"),
                cumulativeAmount = o.optLongOrNull("cumulativeAmount"),
                holderMasked = o.optStringOrNull("holderMasked")
            )
            list.add(e)
            keys.add(normalizedKey(e))
        }
        list.sortByDescending { it.time }
        _events.value = list
    }

    private fun save() {
        val arr = JSONArray()
        _events.value.forEach { e ->
            val o = JSONObject()
            o.put("id", e.id)
            o.put("time", e.time)
            o.put("merchant", e.merchant)
            o.put("amount", e.amount)
            o.put("sourceApp", e.sourceApp)
            o.put("raw", e.raw)

            // optional fields
            e.cardBrand?.let { o.put("cardBrand", it) }
            e.cardLast4?.let { o.put("cardLast4", it) }
            e.installmentMonths?.let { o.put("installmentMonths", it) }
            e.category?.let { o.put("category", it) }
            e.cumulativeAmount?.let { o.put("cumulativeAmount", it) }
            e.holderMasked?.let { o.put("holderMasked", it) }

            arr.put(o)
        }
        prefs.edit().putString("events", arr.toString()).apply()
    }

    // ---- JSONObject helpers ----

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) {
            // 숫자/문자 상관없이 Long으로 시도
            when (val v = get(key)) {
                is Number -> v.toLong()
                is String -> v.toLongOrNull()
                else -> null
            }
        } else null

    companion object {
        @Volatile private var INSTANCE: CardEventRepository? = null
        fun instance(ctx: Context): CardEventRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CardEventRepository(ctx.applicationContext).also { INSTANCE = it }
            }
    }
}
