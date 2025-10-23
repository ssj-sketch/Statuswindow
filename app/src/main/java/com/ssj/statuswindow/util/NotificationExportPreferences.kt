package com.ssj.statuswindow.util

import android.content.Context

class NotificationExportPreferences(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("notification_export", Context.MODE_PRIVATE)

    var endpointUrl: String
        get() = prefs.getString(KEY_ENDPOINT_URL, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_ENDPOINT_URL, value).apply()
        }

    var sheetName: String
        get() = prefs.getString(KEY_SHEET_NAME, DEFAULT_SHEET_NAME).orEmpty()
        set(value) {
            prefs.edit().putString(KEY_SHEET_NAME, value).apply()
        }

    companion object {
        private const val KEY_ENDPOINT_URL = "endpointUrl"
        private const val KEY_SHEET_NAME = "sheetName"
        const val DEFAULT_SHEET_NAME = "Notifications"
    }
}
