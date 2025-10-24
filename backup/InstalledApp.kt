package com.ssj.statuswindow.model

import android.graphics.drawable.Drawable

/**
 * 설치된 앱 정보를 나타내는 데이터 클래스
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSelected: Boolean = false
)
