package com.ssj.statuswindow.ai.income

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * 소득 파싱 AI 엔진
 * SMS 텍스트에서 소득 관련 정보를 AI로 추출
 */
class IncomeParsingAiEngine {
    
    companion object {
        private const val TAG = "IncomeParsingAi"
        
        // 소득 관련 키워드들
        private val INCOME_KEYWORDS = listOf(
            "입금", "급여", "월급", "연봉", "보너스", "상여", "수당", 
            "부업", "알바", "사업", "투자", "배당", "환급", "환불", "보상"
        )
        
        // 은행명 패턴
        private val BANK_PATTERN = Pattern.compile("(신한|국민|우리|하나|농협|기업|새마을|신협|씨티|SC제일|카카오|토스)")
        
        // 날짜 패턴 (MM/dd)
        private val DATE_PATTERN = Pattern.compile("(\\d{2}/\\d{2})")
        
        // 시간 패턴 (HH:mm)
        private val TIME_PATTERN = Pattern.compile("(\\d{2}:\\d{2})")
        
        // 계좌번호 패턴
        private val ACCOUNT_PATTERN = Pattern.compile("(\\d{3}-\\*\\*\\*-\\d{6})")
        
        // 금액 패턴 (콤마 포함)
        private val AMOUNT_PATTERN = Pattern.compile("([\\d,]+)")
        
        // 잔액 패턴
        private val BALANCE_PATTERN = Pattern.compile("잔액\\s+([\\d,]+)")
    }
    
    /**
     * SMS 텍스트에서 소득 정보 추출
     */
    fun extractIncomeTransaction(smsText: String): IncomeTransaction? {
        try {
            Log.d(TAG, "소득 AI 파싱 시작: $smsText")
            
            // 1. 소득 관련 키워드 확인
            if (!containsIncomeKeyword(smsText)) {
                Log.d(TAG, "소득 키워드 없음")
                return null
            }
            
            // 2. 은행명 추출
            val bankName = extractBankName(smsText)
            if (bankName.isNullOrBlank()) {
                Log.d(TAG, "은행명 추출 실패")
                return null
            }
            
            // 3. 날짜 추출
            val dateStr = extractDate(smsText)
            if (dateStr.isNullOrBlank()) {
                Log.d(TAG, "날짜 추출 실패")
                return null
            }
            
            // 4. 시간 추출
            val timeStr = extractTime(smsText)
            if (timeStr.isNullOrBlank()) {
                Log.d(TAG, "시간 추출 실패")
                return null
            }
            
            // 5. 계좌번호 추출
            val accountNumber = extractAccountNumber(smsText)
            if (accountNumber.isNullOrBlank()) {
                Log.d(TAG, "계좌번호 추출 실패")
                return null
            }
            
            // 6. 거래타입 확인 (입금인지)
            val transactionType = if (smsText.contains("입금")) "입금" else null
            if (transactionType.isNullOrBlank()) {
                Log.d(TAG, "입금 거래가 아님")
                return null
            }
            
            // 7. 금액 추출
            val amount = extractAmount(smsText)
            if (amount <= 0) {
                Log.d(TAG, "금액 추출 실패")
                return null
            }
            
            // 8. 잔액 추출
            val balance = extractBalance(smsText)
            if (balance <= 0) {
                Log.d(TAG, "잔액 추출 실패")
                return null
            }
            
            // 9. 거래내용 추출 (AI 추론)
            val description = extractDescription(smsText)
            
            // 10. 거래일시 생성
            val transactionDateTime = createTransactionDateTime(dateStr, timeStr)
            
            val transaction = IncomeTransaction(
                bankName = bankName,
                accountNumber = accountNumber,
                transactionType = transactionType,
                amount = amount,
                balance = balance,
                description = description,
                transactionDate = transactionDateTime,
                originalText = smsText
            )
            
            Log.d(TAG, "소득 AI 파싱 성공: $description - ${amount}원")
            return transaction
            
        } catch (e: Exception) {
            Log.e(TAG, "소득 AI 파싱 오류", e)
            return null
        }
    }
    
    /**
     * 소득 관련 키워드 포함 여부 확인
     */
    private fun containsIncomeKeyword(text: String): Boolean {
        return INCOME_KEYWORDS.any { keyword -> 
            text.contains(keyword) 
        }
    }
    
    /**
     * 은행명 추출
     */
    private fun extractBankName(text: String): String? {
        val matcher = BANK_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
    
    /**
     * 날짜 추출 (MM/dd)
     */
    private fun extractDate(text: String): String? {
        val matcher = DATE_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
    
    /**
     * 시간 추출 (HH:mm)
     */
    private fun extractTime(text: String): String? {
        val matcher = TIME_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
    
    /**
     * 계좌번호 추출
     */
    private fun extractAccountNumber(text: String): String? {
        val matcher = ACCOUNT_PATTERN.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else null
    }
    
    /**
     * 금액 추출 (첫 번째 금액)
     */
    private fun extractAmount(text: String): Long {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val amountStr = matcher.group(1)
            return amountStr.replace(",", "").toLongOrNull() ?: 0L
        }
        return 0L
    }
    
    /**
     * 잔액 추출
     */
    private fun extractBalance(text: String): Long {
        val matcher = BALANCE_PATTERN.matcher(text)
        if (matcher.find()) {
            val balanceStr = matcher.group(1)
            return balanceStr.replace(",", "").toLongOrNull() ?: 0L
        }
        return 0L
    }
    
    /**
     * 거래내용 추출 (AI 추론)
     */
    private fun extractDescription(text: String): String {
        // 소득 키워드 기반으로 거래내용 추론
        return when {
            text.contains("급여") || text.contains("월급") || text.contains("연봉") -> "급여"
            text.contains("보너스") || text.contains("상여") -> "보너스"
            text.contains("수당") -> "수당"
            text.contains("부업") || text.contains("알바") -> "부업수입"
            text.contains("사업") -> "사업수입"
            text.contains("투자") || text.contains("배당") -> "투자수익"
            text.contains("환급") || text.contains("환불") -> "환급"
            text.contains("보상") -> "기타수입"
            else -> "기타수입"
        }
    }
    
    /**
     * 거래일시 생성
     */
    private fun createTransactionDateTime(dateStr: String, timeStr: String): LocalDateTime {
        val now = LocalDateTime.now()
        val currentYear = now.year
        
        // 날짜 파싱 (MM/dd 형식)
        val dateParts = dateStr.split("/")
        val month = dateParts[0].toInt()
        val day = dateParts[1].toInt()
        
        // 시간 파싱 (HH:mm 형식)
        val timeParts = timeStr.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        return LocalDateTime.of(currentYear, month, day, hour, minute)
    }
}

/**
 * 소득 거래 데이터 클래스
 */
data class IncomeTransaction(
    val bankName: String,
    val accountNumber: String,
    val transactionType: String,
    val amount: Long,
    val balance: Long,
    val description: String,
    val transactionDate: LocalDateTime,
    val originalText: String
)
