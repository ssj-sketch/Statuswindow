package com.ssj.statuswindow.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.content.pm.ApplicationInfoCompat
import com.ssj.statuswindow.R

object AppCategoryResolver {

    @SuppressLint("InlinedApi")
    private val CATEGORY_LABELS: Map<Int, Int> = mapOf(
        ApplicationInfo.CATEGORY_AUDIO to R.string.category_app_audio,
        ApplicationInfo.CATEGORY_GAME to R.string.category_app_game,
        ApplicationInfo.CATEGORY_IMAGE to R.string.category_app_image,
        ApplicationInfo.CATEGORY_MAPS to R.string.category_app_maps,
        ApplicationInfo.CATEGORY_NEWS to R.string.category_app_news,
        ApplicationInfo.CATEGORY_PRODUCTIVITY to R.string.category_app_productivity,
        ApplicationInfo.CATEGORY_SOCIAL to R.string.category_app_social,
        ApplicationInfo.CATEGORY_VIDEO to R.string.category_app_video
    )

    @SuppressLint("InlinedApi")
    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        if (applicationInfo == null) {
            return context.getString(R.string.category_app_unknown)
        }

        val fallbackResId = R.string.category_app_other
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.getString(fallbackResId)
        }

        val labelResId = CATEGORY_LABELS[ApplicationInfoCompat.getCategory(applicationInfo)]
            ?: fallbackResId

        return context.getString(labelResId)
    }
}
