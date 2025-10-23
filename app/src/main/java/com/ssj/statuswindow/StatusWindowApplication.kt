package com.ssj.statuswindow

import android.app.Application
import android.util.Log
import com.ssj.statuswindow.util.CrashLogger

/**
 * 애플리케이션 클래스 - 전역 예외 처리 및 초기화
 */
class StatusWindowApplication : Application() {
    
    companion object {
        private const val TAG = "StatusWindowApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 전역 예외 처리기 설정
        setupGlobalExceptionHandler()
        
        Log.d(TAG, "Application started")
    }
    
    /**
     * 전역 예외 처리기 설정
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // 크래시 로그 저장
                CrashLogger.saveCrashLog(
                    this,
                    throwable,
                    "Thread: ${thread.name}\n" +
                    "Thread ID: ${thread.id}\n" +
                    "Stack trace: ${throwable.stackTraceToString()}"
                )
                
                Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
                
                // 기본 핸들러 호출
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Error in exception handler", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
