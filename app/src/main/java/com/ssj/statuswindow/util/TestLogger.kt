package com.ssj.statuswindow.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 테스트 전용 로깅 시스템
 * 테스트 케이스별로 세밀한 로그를 수집하고 분석할 수 있도록 설계
 */
object TestLogger {
    
    private const val TAG = "TestLogger"
    private const val LOG_DIR = "test_logs"
    private const val LOG_FILE_PREFIX = "test_"
    
    // 로그 레벨 정의
    enum class LogLevel(val value: String) {
        DEBUG("DEBUG"),
        INFO("INFO"),
        WARN("WARN"),
        ERROR("ERROR"),
        TEST_START("TEST_START"),
        TEST_END("TEST_END"),
        TEST_STEP("TEST_STEP"),
        TEST_RESULT("TEST_RESULT"),
        DATA_VERIFICATION("DATA_VERIFICATION"),
        PERFORMANCE("PERFORMANCE")
    }
    
    /**
     * 테스트 케이스 시작 로그
     */
    fun startTestCase(context: Context, testCaseName: String, description: String = "") {
        val message = buildString {
            append("=== 테스트 케이스 시작 ===")
            append("\n테스트명: $testCaseName")
            if (description.isNotEmpty()) {
                append("\n설명: $description")
            }
            append("\n시작시간: ${getCurrentTimestamp()}")
            append("\n" + "=".repeat(50))
        }
        
        logToFile(context, LogLevel.TEST_START, "TestCase", message)
        Log.i(TAG, "테스트 시작: $testCaseName")
    }
    
    /**
     * 테스트 케이스 종료 로그
     */
    fun endTestCase(context: Context, testCaseName: String, result: TestResult) {
        val message = buildString {
            append("=== 테스트 케이스 종료 ===")
            append("\n테스트명: $testCaseName")
            append("\n결과: ${result.status}")
            append("\n소요시간: ${result.durationMs}ms")
            if (result.message.isNotEmpty()) {
                append("\n메시지: ${result.message}")
            }
            if (result.data.isNotEmpty()) {
                append("\n데이터: ${result.data}")
            }
            append("\n종료시간: ${getCurrentTimestamp()}")
            append("\n" + "=".repeat(50))
        }
        
        logToFile(context, LogLevel.TEST_END, "TestCase", message)
        Log.i(TAG, "테스트 종료: $testCaseName - ${result.status}")
    }
    
    /**
     * 테스트 단계 로그
     */
    fun logTestStep(context: Context, testCaseName: String, stepNumber: Int, stepName: String, 
                   details: String = "", data: Map<String, Any> = emptyMap()) {
        val message = buildString {
            append("단계 $stepNumber: $stepName")
            if (details.isNotEmpty()) {
                append("\n상세: $details")
            }
            if (data.isNotEmpty()) {
                append("\n데이터: ${formatDataMap(data)}")
            }
            append("\n시간: ${getCurrentTimestamp()}")
        }
        
        logToFile(context, LogLevel.TEST_STEP, testCaseName, message)
        Log.d(TAG, "[$testCaseName] 단계 $stepNumber: $stepName")
    }
    
    /**
     * 데이터 검증 로그
     */
    fun logDataVerification(context: Context, testCaseName: String, verificationType: String,
                           expected: Any, actual: Any, isMatch: Boolean, details: String = "") {
        val message = buildString {
            append("데이터 검증: $verificationType")
            append("\n예상값: $expected")
            append("\n실제값: $actual")
            append("\n일치여부: ${if (isMatch) "✅ 성공" else "❌ 실패"}")
            if (details.isNotEmpty()) {
                append("\n상세: $details")
            }
            append("\n시간: ${getCurrentTimestamp()}")
        }
        
        logToFile(context, LogLevel.DATA_VERIFICATION, testCaseName, message)
        if (isMatch) {
            Log.i(TAG, "[$testCaseName] 검증: $verificationType - 성공")
        } else {
            Log.e(TAG, "[$testCaseName] 검증: $verificationType - 실패")
        }
    }
    
    /**
     * 성능 측정 로그
     */
    fun logPerformance(context: Context, testCaseName: String, operation: String, 
                      durationMs: Long, additionalInfo: Map<String, Any> = emptyMap()) {
        val message = buildString {
            append("성능 측정: $operation")
            append("\n소요시간: ${durationMs}ms")
            if (additionalInfo.isNotEmpty()) {
                append("\n추가정보: ${formatDataMap(additionalInfo)}")
            }
            append("\n시간: ${getCurrentTimestamp()}")
        }
        
        logToFile(context, LogLevel.PERFORMANCE, testCaseName, message)
        Log.d(TAG, "[$testCaseName] 성능: $operation - ${durationMs}ms")
    }
    
    /**
     * 데이터베이스 상태 로그
     */
    fun logDatabaseState(context: Context, testCaseName: String, tableName: String, 
                        recordCount: Int, additionalInfo: Map<String, Any> = emptyMap()) {
        val message = buildString {
            append("DB 상태: $tableName")
            append("\n레코드 수: $recordCount")
            if (additionalInfo.isNotEmpty()) {
                append("\n추가정보: ${formatDataMap(additionalInfo)}")
            }
            append("\n시간: ${getCurrentTimestamp()}")
        }
        
        logToFile(context, LogLevel.DATA_VERIFICATION, testCaseName, message)
        Log.d(TAG, "[$testCaseName] DB 상태: $tableName - ${recordCount}건")
    }
    
    /**
     * SMS 파싱 결과 로그
     */
    fun logSmsParsingResult(context: Context, testCaseName: String, smsText: String,
                           parsingResult: Map<String, Any>, isSuccess: Boolean) {
        val message = buildString {
            append("SMS 파싱 결과")
            append("\nSMS: $smsText")
            append("\n성공여부: ${if (isSuccess) "✅ 성공" else "❌ 실패"}")
            append("\n파싱결과: ${formatDataMap(parsingResult)}")
            append("\n시간: ${getCurrentTimestamp()}")
        }
        
        logToFile(context, LogLevel.TEST_RESULT, testCaseName, message)
        if (isSuccess) {
            Log.i(TAG, "[$testCaseName] SMS 파싱: 성공")
        } else {
            Log.e(TAG, "[$testCaseName] SMS 파싱: 실패")
        }
    }
    
    /**
     * 일반 로그 (기존 Log.d, Log.i 등과 동일하지만 파일에도 저장)
     */
    fun log(context: Context, level: LogLevel, tag: String, message: String) {
        logToFile(context, level, tag, message)
        
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
            else -> Log.i(tag, message)
        }
    }
    
    /**
     * 파일에 로그 저장
     */
    private fun logToFile(context: Context, level: LogLevel, tag: String, message: String) {
        try {
            val logDir = File(context.filesDir, LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val logFile = File(logDir, "${LOG_FILE_PREFIX}${timestamp}.txt")
            
            FileWriter(logFile, true).use { writer ->
                val logEntry = buildString {
                    append("${getCurrentTimestamp()} [${level.value}] $tag: ")
                    append(message.replace("\n", "\n${" ".repeat(30)}"))
                    append("\n")
                }
                writer.append(logEntry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log to file", e)
        }
    }
    
    /**
     * 테스트 결과 데이터 클래스
     */
    data class TestResult(
        val status: TestStatus,
        val durationMs: Long,
        val message: String = "",
        val data: Map<String, Any> = emptyMap()
    )
    
    enum class TestStatus {
        SUCCESS, FAILED, PARTIAL_SUCCESS, SKIPPED
    }
    
    /**
     * 유틸리티 함수들
     */
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }
    
    private fun formatDataMap(data: Map<String, Any>): String {
        return data.entries.joinToString(", ") { "${it.key}=${it.value}" }
    }
    
    /**
     * 테스트 로그 파일 목록 가져오기
     */
    fun getTestLogFiles(context: Context): List<File> {
        val logDir = File(context.filesDir, LOG_DIR)
        return if (logDir.exists()) {
            logDir.listFiles()?.filter { it.name.startsWith(LOG_FILE_PREFIX) }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * 테스트 로그 파일 삭제
     */
    fun clearTestLogs(context: Context) {
        try {
            val logDir = File(context.filesDir, LOG_DIR)
            if (logDir.exists()) {
                logDir.listFiles()?.filter { it.name.startsWith(LOG_FILE_PREFIX) }?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear test logs", e)
        }
    }
    
    /**
     * 특정 테스트 케이스의 로그만 추출
     */
    fun getTestCaseLogs(context: Context, testCaseName: String): List<String> {
        val logFiles = getTestLogFiles(context)
        val logs = mutableListOf<String>()
        
        logFiles.forEach { file ->
            try {
                file.readLines().forEach { line ->
                    if (line.contains(testCaseName)) {
                        logs.add(line)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read log file: ${file.name}", e)
            }
        }
        
        return logs
    }
}
