package com.ssj.statuswindow.util

import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.CardEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * SMS 텍스트를 파싱하여 카드 거래 내역을 추출하는 유틸리티
 */
object SmsParser {
    
    // SMS 패턴을 정규식으로 정의 (더 유연한 패턴)
    private val SMS_PATTERN = Pattern.compile(
        "([가-힣]+카드)\\((\\d+)\\)([승인|취소]+)\\s+([가-힣*]+)\\s+(\\d{1,3}(?:,\\d{3})*)원\\((일시불|\\d+개월)\\)(\\d{1,2}/\\d{1,2}\\s+\\d{1,2}:\\d{2})\\s+([가-힣\\s]+)\\s+누적(\\d{1,3}(?:,\\d{3})*)(?:원)?"
    )
    
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm")
    
    // 기존 거래 내역을 저장 (중복 검사용)
    private val existingTransactions = mutableListOf<CardTransaction>()
    
    /**
     * SMS 텍스트를 파싱하여 카드 거래 내역 리스트를 반환 (시간 기반 중복 제거 포함)
     */
    fun parseSmsText(smsText: String, duplicateDetectionMinutes: Int = 5): List<CardTransaction> {
        val transactions = mutableListOf<CardTransaction>()
        val lines = smsText.trim().split("\n")
        
        for (line in lines) {
            if (line.isBlank()) continue
            
            try {
                val transaction = parseSingleSms(line)
                if (transaction != null && !isDuplicateTransaction(transaction, duplicateDetectionMinutes)) {
                    transactions.add(transaction)
                    existingTransactions.add(transaction)
                }
            } catch (e: Exception) {
                // 파싱 실패한 라인은 무시
                e.printStackTrace()
            }
        }
        
        return transactions
    }
    
    /**
     * 시간 기반 중복 거래 검사
     */
    private fun isDuplicateTransaction(newTransaction: CardTransaction, duplicateDetectionMinutes: Int): Boolean {
        val currentTime = newTransaction.transactionDate
        val timeThreshold = java.time.Duration.ofMinutes(duplicateDetectionMinutes.toLong())
        
        return existingTransactions.any { existing ->
            // 같은 카드, 같은 금액, 같은 가맹점인지 확인
            val isSameTransaction = existing.cardNumber == newTransaction.cardNumber &&
                    existing.amount == newTransaction.amount &&
                    existing.merchant == newTransaction.merchant
            
            if (isSameTransaction) {
                // 시간 차이 확인
                val timeDifference = java.time.Duration.between(existing.transactionDate, currentTime)
                timeDifference <= timeThreshold
            } else {
                false
            }
        }
    }
    
    /**
     * 기존 거래 내역 초기화 (테스트용)
     */
    fun clearExistingTransactions() {
        existingTransactions.clear()
    }
    
    /**
     * 기존 호환성을 위한 parse 메서드 (CardEvent 반환)
     */
    fun parse(smsText: String): List<CardEvent> {
        val transactions = parseSmsText(smsText)
        return transactions.map { transaction ->
            CardEvent(
                id = "sms_${transaction.transactionDate.hashCode()}",
                time = transaction.transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                merchant = transaction.merchant,
                amount = transaction.amount,
                sourceApp = "sms_parser",
                raw = transaction.originalText,
                cardBrand = transaction.cardType,
                cardLast4 = transaction.cardNumber,
                cumulativeAmount = transaction.cumulativeAmount
            )
        }
    }
    
    /**
     * 단일 SMS 라인을 파싱하여 카드 거래 내역을 반환
     */
    private fun parseSingleSms(smsLine: String): CardTransaction? {
        val matcher = SMS_PATTERN.matcher(smsLine.trim())
        
        if (!matcher.find()) {
            return null
        }
        
        return try {
            val cardType = matcher.group(1) ?: return null // 신한카드
            val cardNumber = matcher.group(2) ?: return null // 1054
            val transactionType = matcher.group(3) ?: return null // 승인
            val user = matcher.group(4) ?: return null // 신*진
            val amountStr = matcher.group(5)?.replace(",", "") ?: return null // 42820
            val installment = matcher.group(6) ?: return null // 일시불
            val dateStr = matcher.group(7) ?: return null // 10/21 14:59
            val merchant = matcher.group(8)?.trim() ?: return null // 주식회사 이마트
            val cumulativeStr = matcher.group(9)?.replace(",", "") ?: return null // 1903674
            
            // 금액 파싱
            val amount = amountStr.toLong()
            val cumulativeAmount = cumulativeStr.toLong()
            
            // 날짜 파싱 (현재 년도 기준)
            val currentYear = LocalDateTime.now().year
            val dateTime = LocalDateTime.parse("$currentYear-$dateStr", 
                DateTimeFormatter.ofPattern("yyyy-MM/dd HH:mm"))
            
            // 가맹점명 정리 (주식회사 제거)
            val cleanMerchant = merchant.replace("주식회사\\s*".toRegex(), "").trim()
            
            CardTransaction(
                cardType = cardType,
                cardNumber = cardNumber,
                transactionType = transactionType,
                user = user,
                amount = amount,
                installment = installment,
                transactionDate = dateTime,
                merchant = cleanMerchant,
                cumulativeAmount = cumulativeAmount,
                category = null, // 머신러닝으로 자동분류 예정
                originalText = smsLine
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}