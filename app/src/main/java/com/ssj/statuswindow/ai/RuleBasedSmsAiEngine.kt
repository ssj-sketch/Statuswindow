package com.ssj.statuswindow.ai

import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.BankBalance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

/**
 * 규칙 기반 AI 엔진 - 다양한 SMS 패턴을 처리하는 지능적인 파서
 */
class RuleBasedSmsAiEngine : SmsAiEngine {
    
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
    
    // 다양한 SMS 패턴들을 정의
    private val patterns = listOf(
        // 기본 패턴: 신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원
        Pattern.compile("([가-힣]+카드)\\((\\d+)\\)(승인|취소)\\s+([가-힣*]+)\\s+(\\d{1,3}(?:,\\d{3})*)원\\((일시불|\\d+개월)\\)(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})\\s+([가-힣\\s]+)\\s+누적(\\d{1,3}(?:,\\d{3})*)(?:원)?"),
        
        // 공백 없는 패턴: 신한카드(1054)승인신*진98,700원(일시불)10/13 15:48가톨릭대병원누적1,960,854원
        Pattern.compile("([가-힣]+카드)\\((\\d+)\\)(승인|취소)([가-힣*]+)(\\d{1,3}(?:,\\d{3})*)원\\((일시불|\\d+개월)\\)(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})([가-힣\\s]+)누적(\\d{1,3}(?:,\\d{3})*)(?:원)?"),
        
        // 다른 카드사 패턴: 삼성카드(1234)승인 김*수 50,000원(일시불)11/15 09:30 스타벅스 누적500,000원
        Pattern.compile("([가-힣]+카드)\\((\\d+)\\)(승인|취소)\\s+([가-힣*]+)\\s+(\\d{1,3}(?:,\\d{3})*)원\\((일시불|\\d+개월)\\)(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})\\s+([가-힣\\s]+)\\s+누적(\\d{1,3}(?:,\\d{3})*)(?:원)?"),
        
        // 간소화된 패턴: 신한카드 승인 12,700원 스타벅스 10/13 15:48
        Pattern.compile("([가-힣]+카드)\\s+(승인|취소)\\s+(\\d{1,3}(?:,\\d{3})*)원\\s+([가-힣\\s]+)\\s+(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})"),
        
        // 할부 정보가 없는 패턴: 신한카드(1054)승인 신*진 98,700원 10/13 15:48 가톨릭대병원
        Pattern.compile("([가-힣]+카드)\\((\\d+)\\)(승인|취소)\\s+([가-힣*]+)\\s+(\\d{1,3}(?:,\\d{3})*)원\\s+(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})\\s+([가-힣\\s]+)")
    )
    
    // 날짜 파싱을 위한 다양한 포맷
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("M/d HH:mm"),
        DateTimeFormatter.ofPattern("MM/d HH:mm"),
        DateTimeFormatter.ofPattern("M/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    )
    
    // 은행 키워드 매핑
    private val bankMapping = mapOf(
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
    
    // 은행 SMS 패턴들
    private val bankPatterns = listOf(
        // 기본 패턴: 신한 01/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여
        Pattern.compile("([가-힣]+)\\s+(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(\\d{3}-\\*{3}-\\d{6})\\s+(입금|출금)\\s+([가-힣]+)\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)\\s+([가-힣]+)"),
        
        // 간소화된 패턴: 신한 01/11 21:54 입금 급여 2,500,000 잔액 3,265,147
        Pattern.compile("([가-힣]+)\\s+(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}:\\d{2})\\s+(입금|출금)\\s+([가-힣]+)\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)"),
        
        // 계좌번호 없는 패턴: 신한은행 입금 급여 2,500,000 잔액 3,265,147
        Pattern.compile("([가-힣]+은행)\\s+(입금|출금)\\s+([가-힣]+)\\s+(\\d{1,3}(?:,\\d{3})*)\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)"),
        
        // 다른 형식: [신한은행] 입금 2,500,000원 잔액 3,265,147원
        Pattern.compile("\\[([가-힣]+은행)\\]\\s+(입금|출금)\\s+(\\d{1,3}(?:,\\d{3})*)원\\s+잔액\\s+(\\d{1,3}(?:,\\d{3})*)원")
    )
    
    // 가맹점 정리 규칙
    private val merchantCleanupRules = listOf(
        "주식회사\\s*" to "",
        "㈜" to "",
        "\\(주\\)" to "",
        "\\s+" to " " // 여러 공백을 하나로
    )
    
    override fun initialize() {
        // 규칙 기반 엔진은 초기화가 필요 없음
    }
    
    override fun cleanup() {
        // 규칙 기반 엔진은 정리가 필요 없음
    }
    
    override fun extractTransaction(smsText: String): CardTransaction? {
        val trimmedText = smsText.trim()
        if (trimmedText.isBlank()) return null
        
        // 각 패턴에 대해 시도
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(trimmedText)
            if (matcher.find()) {
                val extraction = parseWithPattern(matcher, index, trimmedText)
                if (extraction != null && extraction.confidence > 0.7f) {
                    return convertToCardTransaction(extraction, trimmedText)
                }
            }
        }
        
        // 패턴 매칭이 실패한 경우 휴리스틱 분석 시도
        val heuristicResult = analyzeWithHeuristics(trimmedText)
        if (heuristicResult != null && heuristicResult.confidence > 0.5f) {
            return convertToCardTransaction(heuristicResult, trimmedText)
        }
        
        return null
    }
    
    override fun extractTransactions(smsTexts: List<String>): List<CardTransaction> {
        return smsTexts.mapNotNull { extractTransaction(it) }
    }
    
    /**
     * 패턴 매칭 결과를 파싱
     */
    private fun parseWithPattern(matcher: java.util.regex.Matcher, patternIndex: Int, originalText: String): TransactionExtraction? {
        return try {
            when (patternIndex) {
                0, 1, 2 -> { // 기본 패턴들
                    val cardType = matcher.group(1) ?: return null
                    val cardNumber = matcher.group(2) ?: return null
                    val transactionType = matcher.group(3) ?: return null
                    val user = matcher.group(4) ?: return null
                    val amountStr = matcher.group(5)?.replace(",", "") ?: return null
                    val installment = matcher.group(6) ?: return null
                    val dateStr = matcher.group(7) ?: return null
                    val merchant = matcher.group(8)?.trim() ?: return null
                    val cumulativeStr = matcher.group(9)?.replace(",", "") ?: return null
                    
                    TransactionExtraction(
                        cardType = cardType,
                        cardNumber = cardNumber,
                        transactionType = transactionType,
                        user = user,
                        amount = amountStr.toLongOrNull(),
                        installment = installment,
                        transactionDate = parseDate(dateStr),
                        merchant = cleanMerchant(merchant),
                        cumulativeAmount = cumulativeStr.toLongOrNull(),
                        confidence = 0.9f
                    )
                }
                3 -> { // 간소화된 패턴
                    val cardType = matcher.group(1) ?: return null
                    val transactionType = matcher.group(2) ?: return null
                    val amountStr = matcher.group(3)?.replace(",", "") ?: return null
                    val merchant = matcher.group(4)?.trim() ?: return null
                    val dateStr = matcher.group(5) ?: return null
                    
                    TransactionExtraction(
                        cardType = cardType,
                        cardNumber = "****", // 카드번호 없음
                        transactionType = transactionType,
                        user = "***", // 사용자명 없음
                        amount = amountStr.toLongOrNull(),
                        installment = "일시불", // 기본값
                        transactionDate = parseDate(dateStr),
                        merchant = cleanMerchant(merchant),
                        cumulativeAmount = null,
                        confidence = 0.7f
                    )
                }
                4 -> { // 할부 정보 없는 패턴
                    val cardType = matcher.group(1) ?: return null
                    val cardNumber = matcher.group(2) ?: return null
                    val transactionType = matcher.group(3) ?: return null
                    val user = matcher.group(4) ?: return null
                    val amountStr = matcher.group(5)?.replace(",", "") ?: return null
                    val dateStr = matcher.group(6) ?: return null
                    val merchant = matcher.group(7)?.trim() ?: return null
                    
                    TransactionExtraction(
                        cardType = cardType,
                        cardNumber = cardNumber,
                        transactionType = transactionType,
                        user = user,
                        amount = amountStr.toLongOrNull(),
                        installment = "일시불", // 기본값
                        transactionDate = parseDate(dateStr),
                        merchant = cleanMerchant(merchant),
                        cumulativeAmount = null,
                        confidence = 0.8f
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 휴리스틱 분석으로 거래 정보 추출
     */
    private fun analyzeWithHeuristics(text: String): TransactionExtraction? {
        // 카드사 키워드 찾기
        val cardType = koreanCardTypes.keys.find { text.contains(it) }
        if (cardType == null) return null
        
        // 금액 패턴 찾기 (숫자,숫자원 또는 숫자원)
        val amountPattern = Pattern.compile("(\\d{1,3}(?:,\\d{3})*)원")
        val amountMatcher = amountPattern.matcher(text)
        val amounts = mutableListOf<Long>()
        while (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)?.replace(",", "")
            amountStr?.toLongOrNull()?.let { amounts.add(it) }
        }
        
        if (amounts.isEmpty()) return null
        
        // 승인/취소 키워드 찾기
        val transactionType = when {
            text.contains("승인") -> "승인"
            text.contains("취소") -> "취소"
            else -> "승인" // 기본값
        }
        
        // 날짜 패턴 찾기
        val datePattern = Pattern.compile("(\\d{1,2}/\\d{1,2}\\s*\\d{1,2}:\\d{2})")
        val dateMatcher = datePattern.matcher(text)
        val dateStr = if (dateMatcher.find()) dateMatcher.group(1) else null
        
        // 가맹점명 추출 (간단한 휴리스틱)
        val merchant = extractMerchantName(text)
        
        return TransactionExtraction(
            cardType = cardType,
            cardNumber = "****",
            transactionType = transactionType,
            user = "***",
            amount = amounts.firstOrNull(),
            installment = "일시불",
            transactionDate = dateStr?.let { parseDate(it) },
            merchant = merchant,
            cumulativeAmount = null,
            confidence = 0.6f
        )
    }
    
    /**
     * 가맹점명 추출
     */
    private fun extractMerchantName(text: String): String? {
        // 간단한 휴리스틱: 금액과 시간 사이의 텍스트를 가맹점으로 추정
        val parts = text.split("원")
        if (parts.size >= 2) {
            val afterAmount = parts[1]
            val merchantPart = afterAmount.split("\\d{1,2}/\\d{1,2}".toRegex()).firstOrNull()
            return merchantPart?.trim()?.takeIf { it.isNotBlank() }
        }
        return null
    }
    
    /**
     * 날짜 파싱
     */
    private fun parseDate(dateStr: String): LocalDateTime? {
        val currentYear = LocalDateTime.now().year
        
        for (format in dateFormats) {
            try {
                val parsed = LocalDateTime.parse("$currentYear-$dateStr", format)
                return parsed
            } catch (e: DateTimeParseException) {
                // 다음 포맷 시도
            }
        }
        
        return null
    }
    
    /**
     * 가맹점명 정리
     */
    private fun cleanMerchant(merchant: String): String {
        var cleaned = merchant
        for ((pattern, replacement) in merchantCleanupRules) {
            cleaned = cleaned.replace(pattern.toRegex(), replacement)
        }
        return cleaned.trim()
    }
    
    /**
     * TransactionExtraction을 CardTransaction으로 변환
     */
    private fun convertToCardTransaction(extraction: TransactionExtraction, originalText: String): CardTransaction {
        return CardTransaction(
            cardType = extraction.cardType ?: "알수없음",
            cardNumber = extraction.cardNumber ?: "****",
            transactionType = extraction.transactionType ?: "승인",
            user = extraction.user ?: "***",
            amount = extraction.amount ?: 0L,
            installment = extraction.installment ?: "일시불",
            transactionDate = extraction.transactionDate ?: LocalDateTime.now(),
            merchant = extraction.merchant ?: "알수없음",
            cumulativeAmount = extraction.cumulativeAmount ?: 0L,
            category = null,
            memo = "",
            originalText = originalText
        )
    }
}
