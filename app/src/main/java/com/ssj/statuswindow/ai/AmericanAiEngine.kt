package com.ssj.statuswindow.ai

import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.BankBalance
import com.ssj.statuswindow.util.CountryDateFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * 미국 SMS 패턴을 위한 AI 추론 엔진
 */
class AmericanAiEngine : CountrySpecificAiEngine {
    
    override fun getCountryCode(): String = "US"
    override fun getCountryName(): String = "United States"
    
    // 미국 카드사 키워드 매핑
    private val americanCardTypes = mapOf(
        "Visa" to "Visa",
        "Mastercard" to "Mastercard",
        "American Express" to "American Express",
        "Discover" to "Discover",
        "Chase" to "Chase",
        "Bank of America" to "Bank of America",
        "Wells Fargo" to "Wells Fargo",
        "Capital One" to "Capital One",
        "Citi" to "Citibank"
    )
    
    // 미국 은행 키워드 매핑
    private val americanBanks = mapOf(
        "Chase" to "Chase Bank",
        "Bank of America" to "Bank of America",
        "Wells Fargo" to "Wells Fargo",
        "Capital One" to "Capital One",
        "Citi" to "Citibank",
        "US Bank" to "US Bank",
        "PNC" to "PNC Bank",
        "TD Bank" to "TD Bank"
    )
    
    // 미국 카드 거래 SMS 패턴들
    private val americanCardPatterns = listOf(
        // 기본 패턴: Chase Card ending in 1234: $98.70 at STARBUCKS on 10/13 at 3:48 PM
        Pattern.compile("([A-Za-z\\s]+)\\s+Card\\s+ending\\s+in\\s+(\\d{4}):\\s+\\$([\\d,]+\\.[\\d]{2})\\s+at\\s+([A-Za-z\\s]+)\\s+on\\s+(\\d{1,2}/\\d{1,2})\\s+at\\s+(\\d{1,2}:\\d{2})\\s+(AM|PM)"),
        
        // 승인 패턴: APPROVED: $50.00 at TARGET on 11/15 at 9:30 AM
        Pattern.compile("APPROVED:\\s+\\$([\\d,]+\\.[\\d]{2})\\s+at\\s+([A-Za-z\\s]+)\\s+on\\s+(\\d{1,2}/\\d{1,2})\\s+at\\s+(\\d{1,2}:\\d{2})\\s+(AM|PM)"),
        
        // 거부 패턴: DECLINED: $100.00 at WALMART on 11/15 at 2:30 PM
        Pattern.compile("DECLINED:\\s+\\$([\\d,]+\\.[\\d]{2})\\s+at\\s+([A-Za-z\\s]+)\\s+on\\s+(\\d{1,2}/\\d{1,2})\\s+at\\s+(\\d{1,2}:\\d{2})\\s+(AM|PM)")
    )
    
    // 미국 은행 SMS 패턴들
    private val americanBankPatterns = listOf(
        // 기본 패턴: Chase: Deposit of $2,500.00 to account ending in 9993 on 01/11 at 9:54 PM. Balance: $3,265.15
        Pattern.compile("([A-Za-z\\s]+):\\s+(Deposit|Withdrawal)\\s+of\\s+\\$([\\d,]+\\.[\\d]{2})\\s+to\\s+account\\s+ending\\s+in\\s+(\\d{4})\\s+on\\s+(\\d{1,2}/\\d{1,2})\\s+at\\s+(\\d{1,2}:\\d{2})\\s+(AM|PM)\\.\\s+Balance:\\s+\\$([\\d,]+\\.[\\d]{2})"),
        
        // 간소화된 패턴: Chase: $2,500.00 deposited. Balance: $3,265.15
        Pattern.compile("([A-Za-z\\s]+):\\s+\\$([\\d,]+\\.[\\d]{2})\\s+(deposited|withdrawn)\\.\\s+Balance:\\s+\\$([\\d,]+\\.[\\d]{2})"),
        
        // 급여 패턴: Chase: Direct deposit of $3,000.00. Balance: $5,265.15
        Pattern.compile("([A-Za-z\\s]+):\\s+Direct\\s+deposit\\s+of\\s+\\$([\\d,]+\\.[\\d]{2})\\.\\s+Balance:\\s+\\$([\\d,]+\\.[\\d]{2})")
    )
    
    // 미국 급여 SMS 패턴들
    private val americanSalaryPatterns = listOf(
        // 급여 패턴: Direct deposit of $3,000.00 from COMPANY NAME
        Pattern.compile("Direct\\s+deposit\\s+of\\s+\\$([\\d,]+\\.[\\d]{2})\\s+from\\s+([A-Za-z\\s]+)"),
        
        // 월급 패턴: Payroll deposit: $3,000.00
        Pattern.compile("Payroll\\s+deposit:\\s+\\$([\\d,]+\\.[\\d]{2})"),
        
        // 급여 패턴: Salary deposit: $3,000.00
        Pattern.compile("Salary\\s+deposit:\\s+\\$([\\d,]+\\.[\\d]{2})")
    )
    
    // 미국 자동이체 SMS 패턴들
    private val americanAutoTransferPatterns = listOf(
        // 자동이체 패턴: Auto transfer: $100.00 to SAVINGS
        Pattern.compile("Auto\\s+transfer:\\s+\\$([\\d,]+\\.[\\d]{2})\\s+to\\s+([A-Za-z\\s]+)"),
        
        // 정기이체 패턴: Scheduled transfer: $50.00 to CHECKING
        Pattern.compile("Scheduled\\s+transfer:\\s+\\$([\\d,]+\\.[\\d]{2})\\s+to\\s+([A-Za-z\\s]+)"),
        
        // 자동출금 패턴: Auto payment: $30.00 to NETFLIX
        Pattern.compile("Auto\\s+payment:\\s+\\$([\\d,]+\\.[\\d]{2})\\s+to\\s+([A-Za-z\\s]+)")
    )
    
    // 날짜 파싱을 위한 미국 포맷
    private val americanDateFormats = listOf(
        DateTimeFormatter.ofPattern("MM/dd h:mm a"),
        DateTimeFormatter.ofPattern("M/d h:mm a"),
        DateTimeFormatter.ofPattern("MM/d h:mm a"),
        DateTimeFormatter.ofPattern("M/dd h:mm a")
    )
    
    override fun initialize() {
        // 미국 엔진 초기화 로직
    }
    
    override fun cleanup() {
        // 미국 엔진 정리 로직
    }
    
    override fun extractCardTransaction(smsText: String): CardTransaction? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 미국 카드 패턴에 대해 시도
        for ((index, pattern) in americanCardPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseAmericanCardWithPattern(matcher, index, trimmedText)
                if (extraction != null && extraction.confidence > 0.7f) {
                    return convertToCardTransaction(extraction, trimmedText)
                }
            }
        }
        
        // 휴리스틱 분석 시도
        val heuristicResult = analyzeAmericanCardWithHeuristics(trimmedText)
        if (heuristicResult != null && heuristicResult.confidence > 0.5f) {
            return convertToCardTransaction(heuristicResult, trimmedText)
        }
        
        return null
    }
    
    override fun extractBankBalance(smsText: String): BankBalance? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 미국 은행 패턴에 대해 시도
        for ((index, pattern) in americanBankPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseAmericanBankWithPattern(matcher, index, trimmedText)
                if (extraction != null && extraction.confidence > 0.7f) {
                    return convertToBankBalance(extraction, trimmedText)
                }
            }
        }
        
        // 휴리스틱 분석 시도
        val heuristicResult = analyzeAmericanBankWithHeuristics(trimmedText)
        if (heuristicResult != null && heuristicResult.confidence > 0.5f) {
            return convertToBankBalance(heuristicResult, trimmedText)
        }
        
        return null
    }
    
    override fun extractSalaryInfo(smsText: String): SalaryInfo? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 미국 급여 패턴에 대해 시도
        for ((index, pattern) in americanSalaryPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseAmericanSalaryWithPattern(matcher, index, trimmedText)
                if (extraction != null && extraction.confidence > 0.7f) {
                    return convertToSalaryInfo(extraction, trimmedText)
                }
            }
        }
        
        return null
    }
    
    override fun extractAutoTransferInfo(smsText: String): AutoTransferInfo? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 미국 자동이체 패턴에 대해 시도
        for ((index, pattern) in americanAutoTransferPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseAmericanAutoTransferWithPattern(matcher, index, trimmedText)
                if (extraction != null && extraction.confidence > 0.7f) {
                    return convertToAutoTransferInfo(extraction, trimmedText)
                }
            }
        }
        
        return null
    }
    
    override fun getConfidenceScore(smsText: String): Float {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return 0.0f
        
        // 영어 키워드가 포함되어 있는지 확인
        val englishKeywords = listOf("Card", "Deposit", "Withdrawal", "Balance", "Approved", "Declined", "Direct deposit", "Auto transfer")
        val hasEnglishKeywords = englishKeywords.any { trimmedText.contains(it, ignoreCase = true) }
        
        if (!hasEnglishKeywords) return 0.0f
        
        // 미국 패턴 매칭 점수 계산
        var maxScore = 0.0f
        
        // 카드 패턴 점수
        for (pattern in americanCardPatterns) {
            if (pattern.matcher(trimmedText).find()) {
                maxScore = maxOf(maxScore, 0.9f)
            }
        }
        
        // 은행 패턴 점수
        for (pattern in americanBankPatterns) {
            if (pattern.matcher(trimmedText).find()) {
                maxScore = maxOf(maxScore, 0.8f)
            }
        }
        
        // 급여 패턴 점수
        for (pattern in americanSalaryPatterns) {
            if (pattern.matcher(trimmedText).find()) {
                maxScore = maxOf(maxScore, 0.7f)
            }
        }
        
        return maxScore
    }
    
    // 날짜/시간 파싱 메서드 (미국 형식: MM/dd HH:mm)
    private fun parseDateTime(dateStr: String, timeStr: String): LocalDateTime {
        return CountryDateFormatter.parseDateByCountry("US", dateStr, timeStr)
    }
    
    // 미국 카드 거래 파싱 메서드들
    private fun parseAmericanCardWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): AmericanCardExtraction? {
        // 구현 생략
        return null
    }
    
    private fun analyzeAmericanCardWithHeuristics(text: String): AmericanCardExtraction? {
        // 구현 생략
        return null
    }
    
    // 미국 은행 잔고 파싱 메서드들
    private fun parseAmericanBankWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): AmericanBankExtraction? {
        // 구현 생략
        return null
    }
    
    private fun analyzeAmericanBankWithHeuristics(text: String): AmericanBankExtraction? {
        // 구현 생략
        return null
    }
    
    // 미국 급여 파싱 메서드들
    private fun parseAmericanSalaryWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): AmericanSalaryExtraction? {
        // 구현 생략
        return null
    }
    
    // 미국 자동이체 파싱 메서드들
    private fun parseAmericanAutoTransferWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): AmericanAutoTransferExtraction? {
        // 구현 생략
        return null
    }
    
    // 변환 메서드들
    private fun convertToCardTransaction(extraction: AmericanCardExtraction, originalText: String): CardTransaction {
        // 구현 생략
        return CardTransaction(
            cardType = "Visa",
            cardNumber = "****",
            transactionType = "승인",
            user = "***",
            amount = 0L,
            installment = "일시불",
            transactionDate = LocalDateTime.now(),
            merchant = "알수없음",
            cumulativeAmount = 0L,
            category = null,
            memo = "",
            originalText = originalText
        )
    }
    
    private fun convertToBankBalance(extraction: AmericanBankExtraction, originalText: String): BankBalance {
        // 구현 생략
        return BankBalance(
            bankName = "Chase Bank",
            accountNumber = "***-****-****",
            balance = 0L,
            accountType = "Checking",
            lastUpdated = LocalDateTime.now(),
            memo = ""
        )
    }
    
    private fun convertToSalaryInfo(extraction: AmericanSalaryExtraction, originalText: String): SalaryInfo {
        // 구현 생략
        return SalaryInfo(
            amount = 0L,
            bankName = "Chase Bank",
            accountNumber = "***-****-****",
            salaryDate = LocalDateTime.now(),
            description = "Salary"
        )
    }
    
    private fun convertToAutoTransferInfo(extraction: AmericanAutoTransferExtraction, originalText: String): AutoTransferInfo {
        // 구현 생략
        return AutoTransferInfo(
            amount = 0L,
            fromBank = "Chase Bank",
            toBank = "알수없음",
            transferDate = LocalDateTime.now(),
            description = "Auto Transfer",
            purpose = "Auto Transfer"
        )
    }
}

// 미국 전용 데이터 클래스들
data class AmericanCardExtraction(
    val cardType: String?,
    val cardNumber: String?,
    val transactionType: String?,
    val user: String?,
    val amount: Long?,
    val installment: String?,
    val transactionDate: LocalDateTime?,
    val merchant: String?,
    val cumulativeAmount: Long?,
    val confidence: Float
)

data class AmericanBankExtraction(
    val bankName: String?,
    val accountNumber: String?,
    val balance: Long?,
    val transactionType: String?,
    val description: String?,
    val amount: Long?,
    val transactionDate: LocalDateTime?,
    val memo: String?,
    val confidence: Float
)

data class AmericanSalaryExtraction(
    val amount: Long?,
    val bankName: String?,
    val accountNumber: String?,
    val salaryDate: LocalDateTime?,
    val description: String?,
    val confidence: Float
)

data class AmericanAutoTransferExtraction(
    val amount: Long?,
    val fromBank: String?,
    val toBank: String?,
    val transferDate: LocalDateTime?,
    val description: String?,
    val purpose: String?,
    val confidence: Float
)
