package com.ssj.statuswindow.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.ssj.statuswindow.R

/**
 * Resolves user-facing labels for notification posting apps. The platform only started exposing
 * category metadata in API 26, so older devices always fall back to the generic "other" label.
 */
object AppCategoryResolver {

    @SuppressLint("InlinedApi")
    private val categoryLabelResIds: Map<Int, Int> = mapOf(
        ApplicationInfo.CATEGORY_AUDIO to R.string.category_app_audio,
        ApplicationInfo.CATEGORY_GAME to R.string.category_app_game,
        ApplicationInfo.CATEGORY_IMAGE to R.string.category_app_image,
        ApplicationInfo.CATEGORY_MAPS to R.string.category_app_maps,
        ApplicationInfo.CATEGORY_NEWS to R.string.category_app_news,
        ApplicationInfo.CATEGORY_PRODUCTIVITY to R.string.category_app_productivity,
        ApplicationInfo.CATEGORY_SOCIAL to R.string.category_app_social,
        ApplicationInfo.CATEGORY_VIDEO to R.string.category_app_video,
        ApplicationInfo.CATEGORY_UNDEFINED to R.string.category_app_other
    )

    @SuppressLint("InlinedApi")
    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        val fallback = context.getString(R.string.category_app_other)

        if (applicationInfo == null) {
            return context.getString(R.string.category_app_unknown)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return fallback
        }

        val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationInfo.category
        } else {
            ApplicationInfo.CATEGORY_UNDEFINED
        }
        val labelResId = categoryLabelResIds[category] ?: R.string.category_app_other

        return context.getString(labelResId)
    }
}
