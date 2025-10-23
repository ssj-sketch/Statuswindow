package com.ssj.statuswindow.util

import com.ssj.statuswindow.data.model.AppNotificationLog
import com.ssj.statuswindow.repo.NotificationLogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SheetsShareConfig(
    val endpointUrl: String,
    val sheetName: String
)

class NotificationSheetsExporter(
    private val repository: NotificationLogRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun export(config: SheetsShareConfig): Result<Int> {
        if (config.endpointUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("endpointUrl is empty"))
        }
        val sheetName = config.sheetName.ifBlank { NotificationExportPreferences.DEFAULT_SHEET_NAME }
        val data = repository.snapshot()
        if (data.isEmpty()) {
            return Result.success(0)
        }
        return withContext(dispatcher) {
            try {
                val payload = buildPayload(sheetName, data)
                val responseCode = postJson(config.endpointUrl, payload)
                if (responseCode in 200..299) {
                    Result.success(data.size)
                } else {
                    Result.failure(IllegalStateException("HTTP $responseCode"))
                }
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
    }

    private fun buildPayload(sheetName: String, entries: List<AppNotificationLog>): String {
        val rows = JSONArray()
        for (entry in entries) {
            val row = JSONArray()
            row.put(entry.postedAtIso)
            row.put(entry.appName)
            row.put(entry.appCategory)
            row.put(entry.content)
            row.put(entry.notificationCategory.orEmpty())
            row.put(entry.packageName)
            rows.put(row)
        }
        val root = JSONObject()
            .put("sheet", sheetName)
            .put("headers", JSONArray(listOf("Timestamp", "App", "App Category", "Content", "Notification Category", "Package")))
            .put("rows", rows)
        return root.toString()
    }

    private fun postJson(endpoint: String, payload: String): Int {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 15_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        conn.outputStream.use { os ->
            OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                writer.write(payload)
            }
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.errorStream?.close()
        } else {
            conn.inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    while (reader.readLine() != null) {
                        // drain input
                    }
                }
            }
        }
        conn.disconnect()
        return code
    }
}
