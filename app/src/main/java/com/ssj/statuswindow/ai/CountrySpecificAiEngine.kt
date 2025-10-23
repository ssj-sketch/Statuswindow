package com.ssj.statuswindow.ai

import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.BankBalance

/**
 * 국가별 AI 추론 엔진 인터페이스
 */
interface CountrySpecificAiEngine {
    
    /**
     * 국가 코드 반환
     */
    fun getCountryCode(): String
    
    /**
     * 국가명 반환
     */
    fun getCountryName(): String
    
    /**
     * 엔진 초기화
     */
    fun initialize()
    
    /**
     * 엔진 정리
     */
    fun cleanup()
    
    /**
     * SMS에서 카드 거래 정보 추출
     */
    fun extractCardTransaction(smsText: String): CardTransaction?
    
    /**
     * SMS에서 은행 잔고 정보 추출
     */
    fun extractBankBalance(smsText: String): BankBalance?
    
    /**
     * SMS에서 급여 정보 추출
     */
    fun extractSalaryInfo(smsText: String): SalaryInfo?
    
    /**
     * SMS에서 자동이체 정보 추출
     */
    fun extractAutoTransferInfo(smsText: String): AutoTransferInfo?
    
    /**
     * 신뢰도 점수 반환 (0.0 ~ 1.0)
     */
    fun getConfidenceScore(smsText: String): Float
}

/**
 * 급여 정보 데이터 클래스
 */
data class SalaryInfo(
    val amount: Long,
    val bankName: String,
    val accountNumber: String,
    val salaryDate: java.time.LocalDateTime,
    val description: String = "급여"
)

/**
 * 자동이체 정보 데이터 클래스
 */
data class AutoTransferInfo(
    val amount: Long,
    val fromBank: String,
    val toBank: String,
    val transferDate: java.time.LocalDateTime,
    val description: String,
    val purpose: String = "자동이체"
)
