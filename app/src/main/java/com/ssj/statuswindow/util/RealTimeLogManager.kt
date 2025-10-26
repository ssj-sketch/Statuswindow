package com.ssj.statuswindow.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 실시간 로그 수집 및 관리 매니저
 */
class RealTimeLogManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "RealTimeLogManager"
        
        @Volatile
        private var INSTANCE: RealTimeLogManager? = null
        
        fun getInstance(context: Context): RealTimeLogManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RealTimeLogManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val logScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    private var isLoggingActive = false
    private var logFile: File? = null
    
    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    )
    
    enum class LogLevel(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARN(Log.WARN),
        ERROR(Log.ERROR)
    }
    
    /**
     * 로그 수집 시작
     */
    fun startLogging() {
        if (isLoggingActive) return
        
        isLoggingActive = true
        logFile = createLogFile()
        
        Log.i(TAG, "실시간 로그 수집 시작")
        
        // 로그 큐 처리 코루틴 시작
        logScope.launch {
            processLogQueue()
        }
        
        // 시스템 로그 수집 시작
        logScope.launch {
            collectSystemLogs()
        }
    }
    
    /**
     * 로그 수집 중지
     */
    fun stopLogging() {
        if (!isLoggingActive) return
        
        isLoggingActive = false
        logScope.cancel()
        
        Log.i(TAG, "실시간 로그 수집 중지")
    }
    
    /**
     * 커스텀 로그 추가
     */
    fun addLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (!isLoggingActive) return
        
        val logEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        logQueue.offer(logEntry)
        
        // Android 로그에도 출력
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
    
    /**
     * 로그 큐 처리
     */
    private suspend fun processLogQueue() {
        while (isLoggingActive) {
            try {
                val logEntry = logQueue.poll()
                if (logEntry != null) {
                    writeLogToFile(logEntry)
                } else {
                    delay(100) // 큐가 비어있으면 잠시 대기
                }
            } catch (e: Exception) {
                Log.e(TAG, "로그 처리 중 오류: ${e.message}", e)
                delay(1000)
            }
        }
    }
    
    /**
     * 시스템 로그 수집
     */
    private suspend fun collectSystemLogs() {
        while (isLoggingActive) {
            try {
                // 앱 관련 로그만 수집
                val process = Runtime.getRuntime().exec("adb logcat -d -s StatusWindow:*")
                val inputStream = process.inputStream
                val reader = inputStream.bufferedReader()
                
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (isLoggingActive && line.isNotEmpty()) {
                            parseAndAddSystemLog(line)
                        }
                    }
                }
                
                delay(5000) // 5초마다 수집
            } catch (e: Exception) {
                Log.e(TAG, "시스템 로그 수집 중 오류: ${e.message}", e)
                delay(10000) // 오류 시 10초 대기
            }
        }
    }
    
    /**
     * 시스템 로그 파싱 및 추가
     */
    private fun parseAndAddSystemLog(logLine: String) {
        try {
            // logcat 형식: "MM-DD HH:mm:ss.fff PID TID LEVEL TAG: MESSAGE"
            val parts = logLine.split(" ", limit = 6)
            if (parts.size >= 6) {
                val levelStr = parts[4]
                val tagMessage = parts[5]
                val colonIndex = tagMessage.indexOf(':')
                
                if (colonIndex > 0) {
                    val tag = tagMessage.substring(0, colonIndex)
                    val message = tagMessage.substring(colonIndex + 1).trim()
                    
                    val level = when (levelStr) {
                        "V" -> LogLevel.VERBOSE
                        "D" -> LogLevel.DEBUG
                        "I" -> LogLevel.INFO
                        "W" -> LogLevel.WARN
                        "E" -> LogLevel.ERROR
                        else -> LogLevel.INFO
                    }
                    
                    addLog(level, tag, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그 파싱 오류: ${e.message}", e)
        }
    }
    
    /**
     * 로그 파일에 쓰기
     */
    private suspend fun writeLogToFile(logEntry: LogEntry) {
        try {
            logFile?.let { file ->
                val logLine = buildString {
                    append(dateFormat.format(Date(logEntry.timestamp)))
                    append(" [${logEntry.level.name}]")
                    append(" ${logEntry.tag}:")
                    append(" ${logEntry.message}")
                    if (logEntry.throwable != null) {
                        append("\n${logEntry.throwable.stackTraceToString()}")
                    }
                    append("\n")
                }
                
                file.appendText(logLine)
            }
        } catch (e: Exception) {
            Log.e(TAG, "로그 파일 쓰기 오류: ${e.message}", e)
        }
    }
    
    /**
     * 로그 파일 생성
     */
    private fun createLogFile(): File {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logFile = File(logDir, "realtime_log_$timestamp.txt")
        
        // 로그 파일 헤더 작성
        logFile.writeText(buildString {
            appendLine("=== StatusWindow 실시간 로그 ===")
            appendLine("시작 시간: ${dateFormat.format(Date())}")
            appendLine("=".repeat(50))
            appendLine()
        })
        
        return logFile
    }
    
    /**
     * 최근 로그 가져오기
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return logQueue.toList().takeLast(count)
    }
    
    /**
     * 로그 파일 경로 가져오기
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
    
    /**
     * 로그 파일 내용 읽기
     */
    fun readLogFile(): String? {
        return try {
            logFile?.readText()
        } catch (e: Exception) {
            Log.e(TAG, "로그 파일 읽기 오류: ${e.message}", e)
            null
        }
    }
    
    /**
     * 로그 파일 삭제
     */
    fun clearLogFile() {
        try {
            logFile?.delete()
            logQueue.clear()
            Log.i(TAG, "로그 파일 삭제 완료")
        } catch (e: Exception) {
            Log.e(TAG, "로그 파일 삭제 오류: ${e.message}", e)
        }
    }
    
    /**
     * 로그 수집 상태 확인
     */
    fun isLoggingActive(): Boolean = isLoggingActive
    
    /**
     * 로그 큐 크기 확인
     */
    fun getQueueSize(): Int = logQueue.size
}
