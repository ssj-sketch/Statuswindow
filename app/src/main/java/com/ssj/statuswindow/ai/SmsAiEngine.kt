package com.ssj.statuswindow.ai

import com.ssj.statuswindow.model.CardTransaction
import java.time.LocalDateTime

/**
 * SMS 텍스트에서 카드 거래 정보를 추출하는 AI 엔진 인터페이스
 */
interface SmsAiEngine {
    
    /**
     * SMS 텍스트를 분석하여 카드 거래 정보를 추출
     * @param smsText SMS 텍스트
     * @return 추출된 카드 거래 정보 또는 null
     */
    fun extractTransaction(smsText: String): CardTransaction?
    
    /**
     * 여러 SMS 텍스트를 배치로 처리
     * @param smsTexts SMS 텍스트 리스트
     * @return 추출된 카드 거래 정보 리스트
     */
    fun extractTransactions(smsTexts: List<String>): List<CardTransaction>
    
    /**
     * AI 모델 초기화
     */
    fun initialize()
    
    /**
     * AI 모델 정리
     */
    fun cleanup()
}

/**
 * 카드 거래 정보 추출을 위한 데이터 클래스
 */
data class TransactionExtraction(
    val cardType: String?,
    val cardNumber: String?,
    val transactionType: String?,
    val user: String?,
    val amount: Long?,
    val installment: String?,
    val transactionDate: LocalDateTime?,
    val merchant: String?,
    val cumulativeAmount: Long?,
    val confidence: Float = 0.0f
)

/**
 * SMS 텍스트 분석 결과
 */
data class SmsAnalysisResult(
    val isCardTransaction: Boolean,
    val confidence: Float,
    val extraction: TransactionExtraction?,
    val rawText: String
)
