package com.ssj.statuswindow.ai

import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.BankBalance
import com.ssj.statuswindow.util.CountryDateFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

/**
 * 한국 SMS 패턴을 위한 AI 추론 엔진
 */
class KoreanAiEngine : CountrySpecificAiEngine {
    
    override fun getCountryCode(): String = "KR"
    override fun getCountryName(): String = "대한민국"
    
    // 한국 카드사 키워드 매핑
    private val koreanCardTypes = mapOf(
        "신한카드" to "신한카드",
        "삼성카드" to "삼성카드",
        "현대카드" to "현대카드",
        "KB카드" to "KB카드",
        "롯데카드" to "롯데카드",
        "하나카드" to "하나카드",
        "BC카드" to "BC카드",
        "우리카드" to "우리카드",
        "농협카드" to "농협카드"
    )
    
    // 한국 은행 키워드 매핑
    private val koreanBanks = mapOf(
        "신한" to "신한은행",
        "삼성" to "삼성은행", 
        "현대" to "현대은행",
        "KB" to "KB국민은행",
        "롯데" to "롯데은행",
        "하나" to "하나은행",
        "우리" to "우리은행",
        "국민" to "KB국민은행",
        "농협" to "농협은행",
        "기업" to "기업은행",
        "새마을" to "새마을금고",
        "신협" to "신협",
        "우체국" to "우체국"
    )
    
    // 한국 카드 거래 SMS 패턴들
    private val koreanCardPatterns = listOf(
        // 기본 패턴: 신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원
        Pattern.compile("([가-힣]+카드)\\((\\d+)\\)(승인|취소)\\s+([가-힣*]+)\\s+(\\d{1,3}(?:,\\d{3})*)원\\((일시불|\\d+개월)\\)(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})\\s+([가-힣\\s]+)\\s+누적(\\d{1,3}(?:,\\d{3})*)(?:원)?"),
        
        // 공백 없는 패턴: 신한카드(1054)승인신*진98,700원(일시불)10/13 15:48가톨릭대병원누적1,960,854원
        Pattern.compile("([가-힣]+카드)\\((\\d+)\\)(승인|취소)([가-힣*]+)(\\d{1,3}(?:,\\d{3})*)원\\((일시불|\\d+개월)\\)(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})([가-힣\\s]+)누적(\\d{1,3}(?:,\\d{3})*)(?:원)?"),
        
        // 간소화된 패턴: 신한카드 승인 12,700원 스타벅스 10/13 15:48
        Pattern.compile("([가-힣]+카드)\\s+(승인|취소)\\s+(\\d{1,3}(?:,\\d{3})*)원\\s+([가-힣\\s]+)\\s+(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})"),
        
        // 할부 정보가 없는 패턴: 신한카드(1054)승인 신*진 98,700원 10/13 15:48 가톨릭대병원
        Pattern.compile("([가-힣]+카드)\\((\\d+)\\)(승인|취소)\\s+([가-힣*]+)\\s+(\\d{1,3}(?:,\\d{3})*)원\\s+(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})\\s+([가-힣\\s]+)")
    )
    
    // 한국 은행 SMS 패턴들
    private val koreanBankPatterns = listOf(
        // 기본 패턴: 신한 01/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여
        Pattern.compile("([가-힣]+)\\s+(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(\\d{3}-\\*{3}-\\d{6})\\s+(입금|출금)\\s+([가-힣]+)\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)\\s+([가-힣]+)"),
        
        // 간소화된 패턴: 신한 01/11 21:54 입금 급여 2,500,000 잔액 3,265,147
        Pattern.compile("([가-힣]+)\\s+(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(입금|출금)\\s+([가-힣]+)\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)"),
        
        // 계좌번호 없는 패턴: 신한은행 입금 급여 2,500,000 잔액 3,265,147
        Pattern.compile("([가-힣]+은행)\\s+(입금|출금)\\s+([가-힣]+)\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)"),
        
        // 다른 형식: [신한은행] 입금 2,500,000원 잔액 3,265,147원
        Pattern.compile("\\[([가-힣]+은행)\\]\\s+(입금|출금)\\s+(\\d{1,3}(?:,\\d{3})*)원\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)원")
    )
    
    // 한국 급여 SMS 패턴들
    private val koreanSalaryPatterns = listOf(
        // 기본 급여 패턴: 신한 01/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여
        Pattern.compile("([가-힣]+)\\s+(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(\\d{3}-\\*{3}-\\d{6})\\s+입금\\s+급여\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액"),
        
        // 간소화된 급여 패턴: 급여 입금 2,500,000원
        Pattern.compile("급여\\s+입금\\s+(\\d{1,3}(?:,\\d{3})*)원"),
        
        // 월급 패턴: 월급 입금 2,500,000원
        Pattern.compile("월급\\s+입금\\s+(\\d{1,3}(?:,\\d{3})*)원")
    )
    
    // 한국 자동이체 SMS 패턴들
    private val koreanAutoTransferPatterns = listOf(
        // 자동이체 패턴: 자동이체 출금 100,000원
        Pattern.compile("자동이체\\s+출금\\s+(\\d{1,3}(?:,\\d{3})*)원"),
        
        // 정기이체 패턴: 정기이체 출금 50,000원
        Pattern.compile("정기이체\\s+출금\\s+(\\d{1,3}(?:,\\d{3})*)원"),
        
        // 자동출금 패턴: 자동출금 30,000원
        Pattern.compile("자동출금\\s+(\\d{1,3}(?:,\\d{3})*)원")
    )
    
    // 날짜 파싱을 위한 한국 포맷
    private val koreanDateFormats = listOf(
        DateTimeFormatter.ofPattern("MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("M/d HH:mm"),
        DateTimeFormatter.ofPattern("MM/d HH:mm"),
        DateTimeFormatter.ofPattern("M/dd HH:mm")
    )
    
    override fun initialize() {
        // 한국 엔진 초기화 로직
    }
    
    override fun cleanup() {
        // 한국 엔진 정리 로직
    }
    
    override fun extractCardTransaction(smsText: String): CardTransaction? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 한국 카드 패턴에 대해 시도
        for ((index, pattern) in koreanCardPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseKoreanCardWithPattern(matcher, index, trimmedText)
                if (extraction != null && extraction.confidence > 0.7f) {
                    return convertToCardTransaction(extraction, trimmedText)
                }
            }
        }
        
        // 휴리스틱 분석 시도
        val heuristicResult = analyzeKoreanCardWithHeuristics(trimmedText)
        if (heuristicResult != null && heuristicResult.confidence > 0.5f) {
            return convertToCardTransaction(heuristicResult, trimmedText)
        }
        
        return null
    }
    
    override fun extractBankBalance(smsText: String): BankBalance? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 한국 은행 패턴에 대해 시도
        for ((index, pattern) in koreanBankPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseKoreanBankWithPattern(matcher, index, trimmedText)
                if (extraction != null && extraction.confidence > 0.7f) {
                    return convertToBankBalance(extraction, trimmedText)
                }
            }
        }
        
        // 휴리스틱 분석 시도
        val heuristicResult = analyzeKoreanBankWithHeuristics(trimmedText)
        if (heuristicResult != null && heuristicResult.confidence > 0.5f) {
            return convertToBankBalance(heuristicResult, trimmedText)
        }
        
        return null
    }
    
    override fun extractSalaryInfo(smsText: String): SalaryInfo? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 한국 급여 패턴에 대해 시도
        for ((index, pattern) in koreanSalaryPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseKoreanSalaryWithPattern(matcher, index, trimmedText)
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
        
        // 한국 자동이체 패턴에 대해 시도
        for ((index, pattern) in koreanAutoTransferPatterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseKoreanAutoTransferWithPattern(matcher, index, trimmedText)
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
        
        // 한국어 키워드가 포함되어 있는지 확인
        val koreanKeywords = listOf("카드", "은행", "입금", "출금", "승인", "취소", "잔액", "급여", "자동이체")
        val hasKoreanKeywords = koreanKeywords.any { trimmedText.contains(it) }
        
        if (!hasKoreanKeywords) return 0.0f
        
        // 한국 패턴 매칭 점수 계산
        var maxScore = 0.0f
        
        // 카드 패턴 점수
        for (pattern in koreanCardPatterns) {
            if (pattern.matcher(trimmedText).find()) {
                maxScore = maxOf(maxScore, 0.9f)
            }
        }
        
        // 은행 패턴 점수
        for (pattern in koreanBankPatterns) {
            if (pattern.matcher(trimmedText).find()) {
                maxScore = maxOf(maxScore, 0.8f)
            }
        }
        
        // 급여 패턴 점수
        for (pattern in koreanSalaryPatterns) {
            if (pattern.matcher(trimmedText).find()) {
                maxScore = maxOf(maxScore, 0.7f)
            }
        }
        
        return maxScore
    }
    
    // 한국 카드 거래 파싱 메서드들
    private fun parseKoreanCardWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): KoreanCardExtraction? {
        // 구현 생략 - 기존 로직과 유사
        return null
    }
    
    private fun analyzeKoreanCardWithHeuristics(text: String): KoreanCardExtraction? {
        // 구현 생략 - 기존 로직과 유사
        return null
    }
    
    // 한국 은행 잔고 파싱 메서드들
    private fun parseKoreanBankWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): KoreanBankExtraction? {
        return try {
            when (patternIndex) {
                0 -> {
                    // 기본 패턴: 신한 01/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여
                    val bankName = matcher.group(1) ?: ""
                    val dateStr = matcher.group(2) ?: ""
                    val timeStr = matcher.group(3) ?: ""
                    val accountNumber = matcher.group(4) ?: ""
                    val transactionType = matcher.group(5) ?: ""
                    val description = matcher.group(6) ?: ""
                    val amountStr = matcher.group(7) ?: ""
                    val balanceStr = matcher.group(8) ?: ""
                    
                    val amount = amountStr.replace(",", "").toLongOrNull() ?: 0L
                    val balance = balanceStr.replace(",", "").toLongOrNull() ?: 0L
                    val dateTime = parseDateTime(dateStr, timeStr)
                    
                    KoreanBankExtraction(
                        bankName = koreanBanks[bankName] ?: bankName,
                        accountNumber = accountNumber,
                        balance = balance,
                        transactionType = transactionType,
                        amount = amount,
                        description = description,
                        dateTime = dateTime,
                        memo = "",
                        confidence = 0.9f
                    )
                }
                1 -> {
                    // 간소화된 패턴: 신한 01/11 21:54 입금 급여 2,500,000 잔액 3,265,147
                    val bankName = matcher.group(1) ?: ""
                    val dateStr = matcher.group(2) ?: ""
                    val timeStr = matcher.group(3) ?: ""
                    val transactionType = matcher.group(4) ?: ""
                    val description = matcher.group(5) ?: ""
                    val amountStr = matcher.group(6) ?: ""
                    val balanceStr = matcher.group(7) ?: ""
                    
                    val amount = amountStr.replace(",", "").toLongOrNull() ?: 0L
                    val balance = balanceStr.replace(",", "").toLongOrNull() ?: 0L
                    val dateTime = parseDateTime(dateStr, timeStr)
                    
                    KoreanBankExtraction(
                        bankName = koreanBanks[bankName] ?: bankName,
                        accountNumber = "",
                        balance = balance,
                        transactionType = transactionType,
                        amount = amount,
                        description = description,
                        dateTime = dateTime,
                        memo = "",
                        confidence = 0.8f
                    )
                }
                2 -> {
                    // 계좌번호 없는 패턴: 신한은행 입금 급여 2,500,000 잔액 3,265,147
                    val bankName = matcher.group(1) ?: ""
                    val transactionType = matcher.group(2) ?: ""
                    val description = matcher.group(3) ?: ""
                    val amountStr = matcher.group(4) ?: ""
                    val balanceStr = matcher.group(5) ?: ""
                    
                    val amount = amountStr.replace(",", "").toLongOrNull() ?: 0L
                    val balance = balanceStr.replace(",", "").toLongOrNull() ?: 0L
                    
                    KoreanBankExtraction(
                        bankName = bankName,
                        accountNumber = "",
                        balance = balance,
                        transactionType = transactionType,
                        amount = amount,
                        description = description,
                        dateTime = LocalDateTime.now(),
                        memo = "",
                        confidence = 0.7f
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("KoreanAiEngine", "은행 잔고 파싱 오류", e)
            null
        }
    }
    
    private fun analyzeKoreanBankWithHeuristics(text: String): KoreanBankExtraction? {
        // 구현 생략 - 기존 로직과 유사
        return null
    }
    
    // 날짜/시간 파싱 메서드 (한국 형식: MM/dd HH:mm)
    private fun parseDateTime(dateStr: String, timeStr: String): LocalDateTime {
        return CountryDateFormatter.parseDateByCountry("KR", dateStr, timeStr)
    }
    
    // 한국 급여 파싱 메서드들
    private fun parseKoreanSalaryWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): KoreanSalaryExtraction? {
        return try {
            when (patternIndex) {
                0 -> {
                    // 기본 급여 패턴: 신한 01/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여
                    val bankName = matcher.group(1) ?: ""
                    val dateStr = matcher.group(2) ?: ""
                    val timeStr = matcher.group(3) ?: ""
                    val accountNumber = matcher.group(4) ?: ""
                    val amountStr = matcher.group(5) ?: ""
                    
                    val amount = amountStr.replace(",", "").toLongOrNull() ?: 0L
                    val dateTime = parseDateTime(dateStr, timeStr)
                    
                    KoreanSalaryExtraction(
                        amount = amount,
                        bankName = koreanBanks[bankName] ?: bankName,
                        accountNumber = accountNumber,
                        salaryDate = dateTime,
                        description = "급여",
                        confidence = 0.9f
                    )
                }
                1, 2 -> {
                    // 간소화된 급여/월급 패턴: 급여 입금 2,500,000원 또는 월급 입금 2,500,000원
                    val amountStr = matcher.group(1) ?: ""
                    val amount = amountStr.replace(",", "").toLongOrNull() ?: 0L
                    
                    KoreanSalaryExtraction(
                        amount = amount,
                        bankName = "알수없음",
                        accountNumber = "",
                        salaryDate = LocalDateTime.now(),
                        description = "급여",
                        confidence = 0.7f
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // 한국 자동이체 파싱 메서드들
    private fun parseKoreanAutoTransferWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): KoreanAutoTransferExtraction? {
        // 구현 생략
        return null
    }
    
    // 변환 메서드들
    private fun convertToCardTransaction(extraction: KoreanCardExtraction, originalText: String): CardTransaction {
        // 구현 생략
        return CardTransaction(
            cardType = "신한카드",
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
    
    private fun convertToBankBalance(extraction: KoreanBankExtraction, originalText: String): BankBalance {
        return BankBalance(
            bankName = extraction.bankName ?: "알수없음",
            accountNumber = extraction.accountNumber ?: "",
            balance = extraction.balance ?: 0L,
            accountType = "입출금",
            lastUpdated = extraction.dateTime ?: LocalDateTime.now(),
            memo = "${extraction.transactionType ?: ""} ${extraction.description ?: ""}"
        )
    }
    
    private fun convertToSalaryInfo(extraction: KoreanSalaryExtraction, originalText: String): SalaryInfo {
        return SalaryInfo(
            amount = extraction.amount ?: 0L,
            bankName = extraction.bankName ?: "알수없음",
            accountNumber = extraction.accountNumber ?: "",
            salaryDate = extraction.salaryDate ?: LocalDateTime.now(),
            description = extraction.description ?: "급여"
        )
    }
    
    private fun convertToAutoTransferInfo(extraction: KoreanAutoTransferExtraction, originalText: String): AutoTransferInfo {
        // 구현 생략
        return AutoTransferInfo(
            amount = 0L,
            fromBank = "신한은행",
            toBank = "알수없음",
            transferDate = LocalDateTime.now(),
            description = "자동이체",
            purpose = "자동이체"
        )
    }
}

// 한국 전용 데이터 클래스들
data class KoreanCardExtraction(
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

data class KoreanBankExtraction(
    val bankName: String?,
    val accountNumber: String?,
    val balance: Long?,
    val transactionType: String?,
    val description: String?,
    val amount: Long?,
    val dateTime: LocalDateTime?,
    val memo: String?,
    val confidence: Float
)

data class KoreanSalaryExtraction(
    val amount: Long?,
    val bankName: String?,
    val accountNumber: String?,
    val salaryDate: LocalDateTime?,
    val description: String?,
    val confidence: Float
)

data class KoreanAutoTransferExtraction(
    val amount: Long?,
    val fromBank: String?,
    val toBank: String?,
    val transferDate: LocalDateTime?,
    val description: String?,
    val purpose: String?,
    val confidence: Float
)
