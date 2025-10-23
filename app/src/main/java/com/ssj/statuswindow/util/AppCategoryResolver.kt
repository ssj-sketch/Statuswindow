package com.ssj.statuswindow.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.ssj.statuswindow.R

/**
 * 알림을 보낸 앱의 카테고리를 로컬라이즈된 문자열로 변환합니다.
 * API 26 이전에는 카테고리 메타데이터가 제공되지 않으므로 항상 "기타" 라벨을 반환합니다.
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

    @SuppressLint("InlinedApi")
    fun resolve(context: Context, applicationInfo: ApplicationInfo?): String {
        val unknownLabel = context.getString(R.string.category_app_unknown)
        val fallbackLabelResId = R.string.category_app_other

        val info = applicationInfo ?: return unknownLabel

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.getString(fallbackLabelResId)
        }

        val labelResId = categoryLabelResIds[info.category] ?: fallbackLabelResId
        return context.getString(labelResId)
    }
}
