package com.ssj.statuswindow.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 앱 설정을 관리하는 유틸리티 클래스
 */
class SettingsPreferences private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "status_window_settings"
        private const val KEY_DUPLICATE_DETECTION_MINUTES = "duplicate_detection_minutes"
        
        // 기본값: 5분
        private const val DEFAULT_DUPLICATE_DETECTION_MINUTES = 5
        
        @Volatile
        private var INSTANCE: SettingsPreferences? = null
        
        fun getInstance(context: Context): SettingsPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 중복 거래 인식 시간(분) 설정
     */
    fun setDuplicateDetectionMinutes(minutes: Int) {
        prefs.edit()
            .putInt(KEY_DUPLICATE_DETECTION_MINUTES, minutes)
            .apply()
    }
    
    /**
     * 중복 거래 인식 시간(분) 가져오기
     */
    fun getDuplicateDetectionMinutes(): Int {
        return prefs.getInt(KEY_DUPLICATE_DETECTION_MINUTES, DEFAULT_DUPLICATE_DETECTION_MINUTES)
    }
    
    /**
     * 중복 거래 인식 시간(초) 가져오기
     */
    fun getDuplicateDetectionSeconds(): Int {
        return getDuplicateDetectionMinutes() * 60
    }
}
