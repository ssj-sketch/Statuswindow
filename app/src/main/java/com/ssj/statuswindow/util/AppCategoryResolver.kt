package com.ssj.statuswindow.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.content.pm.ApplicationInfoCompat
import com.ssj.statuswindow.R

object AppCategoryResolver {

    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        if (applicationInfo == null) {
            return context.getString(R.string.category_app_unknown)
        }

        val fallbackLabel = context.getString(R.string.category_app_other)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return fallbackLabel
        }

        @SuppressLint("InlinedApi")
        val category = ApplicationInfoCompat.getCategory(applicationInfo)

        return when (category) {
            ApplicationInfo.CATEGORY_AUDIO -> context.getString(R.string.category_app_audio)
            ApplicationInfo.CATEGORY_GAME -> context.getString(R.string.category_app_game)
            ApplicationInfo.CATEGORY_IMAGE -> context.getString(R.string.category_app_image)
            ApplicationInfo.CATEGORY_MAPS -> context.getString(R.string.category_app_maps)
            ApplicationInfo.CATEGORY_NEWS -> context.getString(R.string.category_app_news)
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> context.getString(R.string.category_app_productivity)
            ApplicationInfo.CATEGORY_SOCIAL -> context.getString(R.string.category_app_social)
            ApplicationInfo.CATEGORY_VIDEO -> context.getString(R.string.category_app_video)
            else -> fallbackLabel
        }
    }
}
