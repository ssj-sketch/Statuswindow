package com.ssj.statuswindow.util

import android.content.Context
import android.util.Log
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * 로그를 DB에 저장하는 매니저
 */
class LogManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: LogManager? = null
        
        fun getInstance(): LogManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogManager().also { INSTANCE = it }
            }
        }
    }
    
    private var database: StatusWindowDatabase? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    fun initialize(context: Context) {
        database = StatusWindowDatabase.getDatabase(context)
    }
    
    /**
     * DEBUG 레벨 로그 저장
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        Log.d(tag, message, throwable)
        saveLog("DEBUG", tag, message, throwable)
    }
    
    /**
     * INFO 레벨 로그 저장
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        Log.i(tag, message, throwable)
        saveLog("INFO", tag, message, throwable)
    }
    
    /**
     * WARN 레벨 로그 저장
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        saveLog("WARN", tag, message, throwable)
    }
    
    /**
     * ERROR 레벨 로그 저장
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        saveLog("ERROR", tag, message, throwable)
    }
    
    /**
     * 로그를 DB에 저장
     */
    private fun saveLog(level: String, tag: String, message: String, throwable: Throwable? = null) {
        coroutineScope.launch {
            try {
                val logEntity = LogEntity(
                    level = level,
                    tag = tag,
                    message = message,
                    timestamp = LocalDateTime.now(),
                    stackTrace = throwable?.let { 
                        android.util.Log.getStackTraceString(it)
                    },
                    extra = null
                )
                
                database?.logDao()?.insertLog(logEntity)
            } catch (e: Exception) {
                // 로그 저장 실패 시 시스템 로그에만 기록
                Log.e("LogManager", "로그 저장 실패: ${e.message}", e)
            }
        }
    }
    
    /**
     * 커스텀 로그 저장
     */
    fun log(level: String, tag: String, message: String, extra: String? = null) {
        Log.d(tag, message)
        coroutineScope.launch {
            try {
                val logEntity = LogEntity(
                    level = level,
                    tag = tag,
                    message = message,
                    timestamp = LocalDateTime.now(),
                    stackTrace = null,
                    extra = extra
                )
                
                database?.logDao()?.insertLog(logEntity)
            } catch (e: Exception) {
                Log.e("LogManager", "커스텀 로그 저장 실패: ${e.message}", e)
            }
        }
    }
    
    /**
     * 오래된 로그 삭제 (7일 이전)
     */
    fun cleanupOldLogs() {
        coroutineScope.launch {
            try {
                val cutoffTime = LocalDateTime.now().minusDays(7)
                database?.logDao()?.deleteOldLogs(cutoffTime)
                Log.d("LogManager", "오래된 로그 정리 완료")
            } catch (e: Exception) {
                Log.e("LogManager", "로그 정리 실패: ${e.message}", e)
            }
        }
    }
    
    /**
     * 모든 로그 삭제
     */
    fun clearAllLogs() {
        coroutineScope.launch {
            try {
                database?.logDao()?.deleteAllLogs()
                Log.d("LogManager", "모든 로그 삭제 완료")
            } catch (e: Exception) {
                Log.e("LogManager", "로그 삭제 실패: ${e.message}", e)
            }
        }
    }
}

