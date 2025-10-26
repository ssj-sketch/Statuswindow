package com.ssj.statuswindow.util

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 화면별 수정사항 추적 및 알림 시스템
 */
class ScreenModificationTracker(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("screen_modifications", Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    /**
     * 화면 수정사항 등록
     */
    fun registerModification(screenName: String, modification: String, details: String = "") {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val key = "modification_${screenName}_${System.currentTimeMillis()}"
        
        val modificationData = ModificationData(
            screenName = screenName,
            modification = modification,
            details = details,
            timestamp = timestamp,
            isRead = false
        )
        
        prefs.edit()
            .putString(key, modificationData.toJson())
            .apply()
        
        // 최신 수정사항으로 업데이트
        updateLatestModification(screenName, modificationData)
    }
    
    /**
     * 화면별 최신 수정사항 조회
     */
    fun getLatestModification(screenName: String): ModificationData? {
        val json = prefs.getString("latest_${screenName}", null)
        return json?.let { ModificationData.fromJson(it) }
    }
    
    /**
     * 읽지 않은 수정사항 개수 조회
     */
    fun getUnreadCount(screenName: String): Int {
        val allKeys = prefs.all.keys
        return allKeys.count { 
            it.startsWith("modification_${screenName}_") && 
            !prefs.getBoolean("read_$it", false)
        }
    }
    
    /**
     * 수정사항 읽음 처리
     */
    fun markAsRead(screenName: String) {
        val allKeys = prefs.all.keys
        allKeys.filter { it.startsWith("modification_${screenName}_") }
            .forEach { key ->
                prefs.edit().putBoolean("read_$key", true).apply()
            }
    }
    
    private fun updateLatestModification(screenName: String, modificationData: ModificationData) {
        prefs.edit()
            .putString("latest_$screenName", modificationData.toJson())
            .apply()
    }
}

/**
 * 수정사항 데이터 클래스
 */
data class ModificationData(
    val screenName: String,
    val modification: String,
    val details: String,
    val timestamp: String,
    val isRead: Boolean
) {
    fun toJson(): String {
        return "$screenName|$modification|$details|$timestamp|$isRead"
    }
    
    companion object {
        fun fromJson(json: String): ModificationData {
            val parts = json.split("|")
            return ModificationData(
                screenName = parts[0],
                modification = parts[1],
                details = parts[2],
                timestamp = parts[3],
                isRead = parts[4].toBoolean()
            )
        }
    }
}

