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
        val fallbackLabel = context.getString(R.string.category_app_other)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return fallbackLabel
        }

        @SuppressLint("InlinedApi")
        val category = ApplicationInfoCompat.getCategory(applicationInfo)

        return when (category) {
        val fallbackResId = R.string.category_app_other

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.getString(fallbackResId)
        }

        val labelResId = when (ApplicationInfoCompat.getCategory(applicationInfo)) {
        val category = ApplicationInfoCompat.getCategory(applicationInfo)
        val labelResId = when (category) {
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
    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        if (applicationInfo == null) return context.getString(R.string.category_app_unknown)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.getString(R.string.category_app_unknown)
        }
        val fallback = context.getString(R.string.category_app_other)
        return when (ApplicationInfoCompat.getCategory(applicationInfo)) {
            ApplicationInfo.CATEGORY_AUDIO -> context.getString(R.string.category_app_audio)
            ApplicationInfo.CATEGORY_GAME -> context.getString(R.string.category_app_game)
            ApplicationInfo.CATEGORY_IMAGE -> context.getString(R.string.category_app_image)
            ApplicationInfo.CATEGORY_MAPS -> context.getString(R.string.category_app_maps)
            ApplicationInfo.CATEGORY_NEWS -> context.getString(R.string.category_app_news)
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> context.getString(R.string.category_app_productivity)
            ApplicationInfo.CATEGORY_SOCIAL -> context.getString(R.string.category_app_social)
            ApplicationInfo.CATEGORY_VIDEO -> context.getString(R.string.category_app_video)
            else -> fallbackLabel
            ApplicationInfo.CATEGORY_UNDEFINED -> fallback
            else -> fallback
            ApplicationInfo.CATEGORY_UNDEFINED -> context.getString(R.string.category_app_other)
            else -> context.getString(R.string.category_app_other)
            ApplicationInfo.CATEGORY_UNDEFINED, ApplicationInfo.CATEGORY_OTHER ->
                context.getString(R.string.category_app_other)
            else -> context.getString(R.string.category_app_other)
            ApplicationInfo.CATEGORY_UNDEFINED, ApplicationInfo.CATEGORY_OTHER, else ->
                context.getString(R.string.category_app_other)
        }
    }
}
