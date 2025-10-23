package com.ssj.statuswindow.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 크래시 로그 수집 및 저장 유틸리티
 */
object CrashLogger {
    
    private const val TAG = "CrashLogger"
    private const val LOG_DIR = "crash_logs"
    private const val LOG_FILE_PREFIX = "crash_"
    
    /**
     * 크래시 로그를 파일로 저장
     */
    fun saveCrashLog(context: Context, throwable: Throwable, additionalInfo: String = "") {
        try {
            val logDir = File(context.filesDir, LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val logFile = File(logDir, "${LOG_FILE_PREFIX}${timestamp}.txt")
            
            FileWriter(logFile).use { writer ->
                PrintWriter(writer).use { printWriter ->
                    printWriter.println("=== CRASH LOG ===")
                    printWriter.println("Timestamp: $timestamp")
                    printWriter.println("App Version: ${getAppVersion(context)}")
                    printWriter.println("Android Version: ${android.os.Build.VERSION.RELEASE}")
                    printWriter.println("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    printWriter.println()
                    
                    if (additionalInfo.isNotEmpty()) {
                        printWriter.println("Additional Info:")
                        printWriter.println(additionalInfo)
                        printWriter.println()
                    }
                    
                    printWriter.println("Exception:")
                    throwable.printStackTrace(printWriter)
                    printWriter.println()
                    
                    printWriter.println("=== END CRASH LOG ===")
                }
            }
            
            Log.e(TAG, "Crash log saved to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }
    
    /**
     * 일반 로그를 파일로 저장
     */
    fun saveLog(context: Context, level: String, tag: String, message: String) {
        try {
            val logDir = File(context.filesDir, LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val logFile = File(logDir, "app_logs.txt")
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            FileWriter(logFile, true).use { writer ->
                writer.append("$timestamp [$level] $tag: $message\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log", e)
        }
    }
    
    /**
     * 앱 버전 정보 가져오기
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * 저장된 로그 파일 목록 가져오기
     */
    fun getLogFiles(context: Context): List<File> {
        val logDir = File(context.filesDir, LOG_DIR)
        return if (logDir.exists()) {
            logDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 로그 파일 삭제
     */
    fun clearLogs(context: Context) {
        try {
            val logDir = File(context.filesDir, LOG_DIR)
            if (logDir.exists()) {
                logDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
}
