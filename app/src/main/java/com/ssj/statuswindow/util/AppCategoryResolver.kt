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
        ApplicationInfo.CATEGORY_VIDEO to R.string.category_app_video
    )

    /**
     * Translates the category of the app that sent the notification into a localized string.
     * Before API 26, category metadata is not available, so it always returns the "Other" label.
     */
    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        // If applicationInfo is null, return an "unknown" label.
        if (applicationInfo == null) {
            return context.getString(R.string.category_app_unknown)
        }

        // For Android versions before Oreo (API 26), app categories are not available.
        // Fall back to a generic "other" category.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.getString(R.string.category_app_other)
        }

        // Get the category from ApplicationInfo and find the corresponding string resource ID.
        // If the category is not in our map, or is UNDEFINED, use the "other" label as a fallback.
        val labelResId = categoryLabelResIds[applicationInfo.category] ?: R.string.category_app_other

        return context.getString(labelResId)
    }
}
