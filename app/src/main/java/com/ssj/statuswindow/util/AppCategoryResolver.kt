package com.ssj.statuswindow.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.ssj.statuswindow.R

object AppCategoryResolver {

    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        if (applicationInfo == null) return context.getString(R.string.category_app_unknown)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.getString(R.string.category_app_unknown)
        }
        return when (applicationInfo.category) {
            ApplicationInfo.CATEGORY_AUDIO -> context.getString(R.string.category_app_audio)
            ApplicationInfo.CATEGORY_GAME -> context.getString(R.string.category_app_game)
            ApplicationInfo.CATEGORY_IMAGE -> context.getString(R.string.category_app_image)
            ApplicationInfo.CATEGORY_MAPS -> context.getString(R.string.category_app_maps)
            ApplicationInfo.CATEGORY_NEWS -> context.getString(R.string.category_app_news)
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> context.getString(R.string.category_app_productivity)
            ApplicationInfo.CATEGORY_SOCIAL -> context.getString(R.string.category_app_social)
            ApplicationInfo.CATEGORY_VIDEO -> context.getString(R.string.category_app_video)
            ApplicationInfo.CATEGORY_UNDEFINED, ApplicationInfo.CATEGORY_OTHER ->
                context.getString(R.string.category_app_other)
            else -> context.getString(R.string.category_app_other)
            ApplicationInfo.CATEGORY_UNDEFINED, ApplicationInfo.CATEGORY_OTHER, else ->
                context.getString(R.string.category_app_other)
        }
    }
}
