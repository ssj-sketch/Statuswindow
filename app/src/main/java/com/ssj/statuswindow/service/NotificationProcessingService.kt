package com.ssj.statuswindow.service

import android.content.Context
import android.util.Log
import com.ssj.statuswindow.model.*
import com.ssj.statuswindow.util.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * 알림 처리 서비스
 * 신뢰도 점수 계산, 중복 제거, 파싱 결과 관리
 */
class NotificationProcessingService(private val context: Context) {
    
    companion object {
        private const val TAG = "NotificationProcessing"
        private const val DUPLICATE_DETECTION_WINDOW_MINUTES = 5
        private const val MIN_CONFIDENCE_THRESHOLD = 0.7f
        private const val MAX_PROCESSING_HISTORY = 1000
    }
    
    // 처리된 알림의 해시를 저장 (중복 검사용)
    private val processedHashes = ConcurrentHashMap<String, LocalDateTime>()
    
    // 최근 처리 결과 저장 (성능 최적화용)
    private val recentResults = ConcurrentHashMap<String, NotificationProcessingResult>()
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * 알림을 처리하고 결과를 반환
     */
    fun processNotification(
        packageName: String,
        appName: String,
        content: String,
        postedAt: LocalDateTime
    ): NotificationProcessingResult {
        
        val notificationId = generateNotificationId(packageName, content, postedAt)
        
        // 1. 중복 검사
        if (isDuplicateNotification(content, postedAt)) {
            return NotificationProcessingResult(
                notificationId = notificationId,
                packageName = packageName,
                appName = appName,
                content = content,
                processedAt = LocalDateTime.now(),
                confidenceScore = 0.0f,
                processingStatus = ProcessingStatus.DUPLICATE
            )
        }
        
        // 2. 파싱 시도
        val parsingResult = parseNotificationContent(content, packageName)
        
        // 3. 신뢰도 점수 계산
        val confidenceScore = calculateConfidenceScore(parsingResult, content, packageName)
        
        // 4. 처리 상태 결정
        val processingStatus = determineProcessingStatus(confidenceScore, parsingResult)
        
        // 5. 결과 생성
        val result = NotificationProcessingResult(
            notificationId = notificationId,
            packageName = packageName,
            appName = appName,
            content = content,
            processedAt = LocalDateTime.now(),
            confidenceScore = confidenceScore,
            processingStatus = processingStatus,
            extractedData = if (confidenceScore >= MIN_CONFIDENCE_THRESHOLD) parsingResult else null,
            errorMessage = if (confidenceScore < MIN_CONFIDENCE_THRESHOLD) "Low confidence parsing" else null
        )
        
        // 6. 결과 저장 및 캐시 업데이트
        saveProcessingResult(result)
        
        Log.d(TAG, "Processed notification: $appName, confidence: $confidenceScore, status: $processingStatus")
        
        return result
    }
    
    /**
     * 알림 내용을 파싱하여 금융 데이터 추출
     */
    private fun parseNotificationContent(content: String, packageName: String): ExtractedFinancialData? {
        return try {
            // SMS 파서를 사용하여 파싱
            val parsedTransactions = SmsParser.parseSmsText(content)
            
            if (parsedTransactions.isNotEmpty()) {
                val transaction = parsedTransactions.first()
                
                ExtractedFinancialData(
                    transactionType = determineTransactionType(content, packageName),
                    amount = transaction.amount,
                    currency = "KRW",
                    merchant = transaction.merchant,
                    accountInfo = "${transaction.cardType} ${transaction.cardNumber}",
                    timestamp = transaction.transactionDate,
                    rawText = content
                )
            } else {
                // 파싱 실패 시 null 반환
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification content", e)
            null
        }
    }
    
    /**
     * 신뢰도 점수 계산
     */
    private fun calculateConfidenceScore(
        extractedData: ExtractedFinancialData?,
        content: String,
        packageName: String
    ): Float {
        var score = 0.0f
        
        // 1. 파싱 성공 여부 (40%)
        if (extractedData != null) {
            score += 0.4f
        }
        
        // 2. 금액 정보 존재 여부 (20%)
        if (extractedData?.amount != null && extractedData.amount > 0) {
            score += 0.2f
        }
        
        // 3. 앱 신뢰도 (20%)
        val appTrustScore = calculateAppTrustScore(packageName)
        score += appTrustScore * 0.2f
        
        // 4. 텍스트 품질 (20%)
        val textQualityScore = calculateTextQualityScore(content)
        score += textQualityScore * 0.2f
        
        return min(1.0f, max(0.0f, score))
    }
    
    /**
     * 앱 신뢰도 점수 계산
     */
    private fun calculateAppTrustScore(packageName: String): Float {
        return when {
            packageName.contains("bank", ignoreCase = true) -> 1.0f
            packageName.contains("card", ignoreCase = true) -> 0.9f
            packageName.contains("securities", ignoreCase = true) -> 0.8f
            packageName.contains("sms", ignoreCase = true) -> 0.7f
            else -> 0.5f
        }
    }
    
    /**
     * 텍스트 품질 점수 계산
     */
    private fun calculateTextQualityScore(content: String): Float {
        var score = 0.0f
        
        // 금액 패턴 포함 여부
        if (content.contains(Regex("\\d{1,3}(?:,\\d{3})*원"))) {
            score += 0.3f
        }
        
        // 거래 키워드 포함 여부
        val transactionKeywords = listOf("승인", "취소", "입금", "출금", "결제", "매수", "매도")
        val keywordCount = transactionKeywords.count { content.contains(it) }
        score += (keywordCount.toFloat() / transactionKeywords.size) * 0.4f
        
        // 날짜/시간 패턴 포함 여부
        if (content.contains(Regex("\\d{1,2}/\\d{1,2}"))) {
            score += 0.2f
        }
        
        // 텍스트 길이 적절성
        if (content.length in 20..200) {
            score += 0.1f
        }
        
        return min(1.0f, score)
    }
    
    /**
     * 거래 유형 결정
     */
    private fun determineTransactionType(content: String, packageName: String): TransactionType {
        return when {
            content.contains("승인") -> TransactionType.CARD_PAYMENT
            content.contains("취소") -> TransactionType.CARD_CANCELLATION
            content.contains("입금") -> TransactionType.BANK_DEPOSIT
            content.contains("출금") -> TransactionType.BANK_WITHDRAWAL
            content.contains("급여") || content.contains("월급") -> TransactionType.SALARY
            content.contains("자동이체") -> TransactionType.AUTO_TRANSFER
            content.contains("매수") || content.contains("매도") -> TransactionType.INVESTMENT
            else -> TransactionType.UNKNOWN
        }
    }
    
    /**
     * 처리 상태 결정
     */
    private fun determineProcessingStatus(
        confidenceScore: Float,
        extractedData: ExtractedFinancialData?
    ): ProcessingStatus {
        return when {
            confidenceScore >= MIN_CONFIDENCE_THRESHOLD && extractedData != null -> ProcessingStatus.SUCCESS
            confidenceScore >= 0.5f && extractedData != null -> ProcessingStatus.PARTIAL_SUCCESS
            extractedData == null -> ProcessingStatus.FAILED
            else -> ProcessingStatus.INVALID_FORMAT
        }
    }
    
    /**
     * 중복 알림 검사
     */
    private fun isDuplicateNotification(content: String, postedAt: LocalDateTime): Boolean {
        val contentHash = content.hashCode().toString()
        val windowStart = postedAt.minusMinutes(DUPLICATE_DETECTION_WINDOW_MINUTES.toLong())
        
        return processedHashes.any { (hash, timestamp) ->
            hash == contentHash && timestamp.isAfter(windowStart)
        }
    }
    
    /**
     * 알림 ID 생성
     */
    private fun generateNotificationId(packageName: String, content: String, postedAt: LocalDateTime): String {
        return "${packageName}_${content.hashCode()}_${postedAt.hashCode()}"
    }
    
    /**
     * 처리 결과 저장
     */
    private fun saveProcessingResult(result: NotificationProcessingResult) {
        val contentHash = result.content.hashCode().toString()
        processedHashes[contentHash] = result.processedAt
        
        // 최근 결과 캐시 업데이트
        recentResults[result.notificationId] = result
        
        // 캐시 크기 제한
        if (recentResults.size > MAX_PROCESSING_HISTORY) {
            val oldestKey = recentResults.keys.first()
            recentResults.remove(oldestKey)
        }
        
        // 오래된 해시 정리
        cleanupOldHashes()
    }
    
    /**
     * 오래된 해시 정리
     */
    private fun cleanupOldHashes() {
        val cutoffTime = LocalDateTime.now().minusHours(24)
        processedHashes.entries.removeIf { it.value.isBefore(cutoffTime) }
    }
    
    /**
     * 최근 처리 결과 조회
     */
    fun getRecentResults(limit: Int = 50): List<NotificationProcessingResult> {
        return recentResults.values
            .sortedByDescending { it.processedAt }
            .take(limit)
    }
    
    /**
     * 신뢰도가 낮은 결과 조회 (사용자 확인 필요)
     */
    fun getLowConfidenceResults(): List<NotificationProcessingResult> {
        return recentResults.values
            .filter { it.confidenceScore < MIN_CONFIDENCE_THRESHOLD && it.processingStatus != ProcessingStatus.DUPLICATE }
            .sortedByDescending { it.processedAt }
    }
    
    /**
     * 처리 통계 조회
     */
    fun getProcessingStats(): ProcessingStats {
        val results = recentResults.values
        val totalCount = results.size
        val successCount = results.count { it.processingStatus == ProcessingStatus.SUCCESS }
        val partialCount = results.count { it.processingStatus == ProcessingStatus.PARTIAL_SUCCESS }
        val failedCount = results.count { it.processingStatus == ProcessingStatus.FAILED }
        val duplicateCount = results.count { it.processingStatus == ProcessingStatus.DUPLICATE }
        
        return ProcessingStats(
            totalProcessed = totalCount,
            successCount = successCount,
            partialSuccessCount = partialCount,
            failedCount = failedCount,
            duplicateCount = duplicateCount,
            averageConfidenceScore = if (totalCount > 0) results.map { it.confidenceScore }.average().toFloat() else 0.0f
        )
    }
}

/**
 * 처리 통계 데이터 클래스
 */
data class ProcessingStats(
    val totalProcessed: Int,
    val successCount: Int,
    val partialSuccessCount: Int,
    val failedCount: Int,
    val duplicateCount: Int,
    val averageConfidenceScore: Float
)
