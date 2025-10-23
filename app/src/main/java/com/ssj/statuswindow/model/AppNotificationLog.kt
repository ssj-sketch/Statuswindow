package com.ssj.statuswindow.model

/**
 * Generic notification log entry collected from the device.
 *
 * @param id Stable unique identifier for the notification instance.
 * @param packageName Android package name that posted the notification.
 * @param appName User facing app label resolved via PackageManager.
 * @param appCategory Human readable category of the posting application.
 * @param postedAtIso Timestamp string formatted as "yyyy-MM-dd HH:mm:ss".
 * @param postedAtEpochMillis Milliseconds since epoch for ordering/export.
 * @param notificationCategory Optional notification category from framework.
 * @param content Aggregated textual content shown to the user.
 */
data class AppNotificationLog(
    val id: String,
    val packageName: String,
    val appName: String,
    val appCategory: String,
    val postedAtIso: String,
    val postedAtEpochMillis: Long,
    val notificationCategory: String? = null,
    val content: String
)
