package com.ssj.statuswindow.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.content.pm.ApplicationInfoCompat
import com.ssj.statuswindow.R

object AppCategoryResolver {

    @SuppressLint("InlinedApi")
    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        if (applicationInfo == null) {
            return context.getString(R.string.category_app_unknown)
        }

        val fallbackResId = R.string.category_app_other
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.getString(fallbackResId)
        }

        val labelResId = when (ApplicationInfoCompat.getCategory(applicationInfo)) {
            ApplicationInfo.CATEGORY_AUDIO -> R.string.category_app_audio
            ApplicationInfo.CATEGORY_GAME -> R.string.category_app_game
            ApplicationInfo.CATEGORY_IMAGE -> R.string.category_app_image
            ApplicationInfo.CATEGORY_MAPS -> R.string.category_app_maps
            ApplicationInfo.CATEGORY_NEWS -> R.string.category_app_news
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> R.string.category_app_productivity
            ApplicationInfo.CATEGORY_SOCIAL -> R.string.category_app_social
            ApplicationInfo.CATEGORY_VIDEO -> R.string.category_app_video
            ApplicationInfo.CATEGORY_UNDEFINED,
            ApplicationInfo.CATEGORY_OTHER -> fallbackResId
            else -> fallbackResId
        }

        return context.getString(labelResId)
    }
}
