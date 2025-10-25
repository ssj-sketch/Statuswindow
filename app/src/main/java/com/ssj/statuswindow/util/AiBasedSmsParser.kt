package com.ssj.statuswindow.util

import com.ssj.statuswindow.database.entity.BankBalanceEntity
import com.ssj.statuswindow.database.entity.BankTransactionEntity
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.database.entity.IncomeTransactionEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * AI 추론을 사용한 SMS 파싱 엔진
 */
object AiBasedSmsParser {
    
    /**
     * SMS 텍스트를 AI 추론으로 파싱하여 적절한 엔티티로 변환
     */
    fun parseSmsText(smsText: String): List<Any> {
        val entities = mutableListOf<Any>()
        
        android.util.Log.d("AiBasedSmsParser", "=== SMS 파싱 시작 ===")
        android.util.Log.d("AiBasedSmsParser", "입력 SMS: $smsText")
        
        try {
            // 1. SMS 타입 분류 (AI 추론)
            val smsType = classifySmsType(smsText)
            android.util.Log.d("AiBasedSmsParser", "분류된 SMS 타입: $smsType")
            
            when (smsType) {
                SmsType.CARD_TRANSACTION -> {
                    android.util.Log.d("AiBasedSmsParser", "카드 거래로 분류됨")
                    val cardTransaction = parseCardTransactionWithAi(smsText)
                    if (cardTransaction != null) {
                        android.util.Log.d("AiBasedSmsParser", "카드 거래 파싱 성공: $cardTransaction")
                        entities.add(cardTransaction)
                    } else {
                        android.util.Log.w("AiBasedSmsParser", "카드 거래 파싱 실패")
                    }
                }
                SmsType.INCOME_TRANSACTION -> {
                    android.util.Log.d("AiBasedSmsParser", "수입 내역으로 분류됨")
                    val incomeTransaction = parseIncomeTransactionWithAi(smsText)
                    if (incomeTransaction != null) {
                        android.util.Log.d("AiBasedSmsParser", "수입 내역 파싱 성공: $incomeTransaction")
                        entities.add(incomeTransaction)
                        
                        // 은행 잔고 정보도 추출
                        val bankBalance = extractBankBalanceFromIncome(smsText, incomeTransaction)
                        if (bankBalance != null) {
                            android.util.Log.d("AiBasedSmsParser", "은행 잔고 추출 성공: $bankBalance")
                            entities.add(bankBalance)
                        } else {
                            android.util.Log.w("AiBasedSmsParser", "은행 잔고 추출 실패")
                        }
                    } else {
                        android.util.Log.w("AiBasedSmsParser", "수입 내역 파싱 실패")
                    }
                }
                SmsType.BANK_BALANCE -> {
                    android.util.Log.d("AiBasedSmsParser", "은행 잔고로 분류됨")
                    val bankBalance = parseBankBalanceWithAi(smsText)
                    if (bankBalance != null) {
                        android.util.Log.d("AiBasedSmsParser", "은행 잔고 파싱 성공: $bankBalance")
                        entities.add(bankBalance)
                    } else {
                        android.util.Log.w("AiBasedSmsParser", "은행 잔고 파싱 실패")
                    }
                }
                SmsType.UNKNOWN -> {
                    android.util.Log.w("AiBasedSmsParser", "알 수 없는 SMS 형식: $smsText")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AiBasedSmsParser", "SMS 파싱 오류: ${e.message}", e)
        }
        
        android.util.Log.d("AiBasedSmsParser", "파싱 결과: ${entities.size}개 엔티티 생성")
        android.util.Log.d("AiBasedSmsParser", "=== SMS 파싱 완료 ===")
        
        return entities
    }
    
    /**
     * AI 추론으로 SMS 타입 분류
     */
    private fun classifySmsType(smsText: String): SmsType {
        val text = smsText.lowercase()
        android.util.Log.d("AiBasedSmsParser", "분류 대상 텍스트: $text")
        
        // 카드 거래 패턴 인식
        if (text.contains("카드") && (text.contains("승인") || text.contains("취소")) && 
            text.contains("원") && text.contains("누적")) {
            android.util.Log.d("AiBasedSmsParser", "카드 거래 패턴 매칭됨")
            return SmsType.CARD_TRANSACTION
        }
        
        // 수입 내역 패턴 인식 (입금이 있는 경우)
        if (text.contains("입금")) {
            android.util.Log.d("AiBasedSmsParser", "입금 패턴 매칭됨 - 수입 내역으로 분류")
            return SmsType.INCOME_TRANSACTION
        }
        
        // 출금 내역 패턴 인식
        if (text.contains("출금") || text.contains("atm") || text.contains("이체") || 
            text.contains("현금인출") || text.contains("연말정산")) {
            android.util.Log.d("AiBasedSmsParser", "출금 내역 패턴 매칭됨")
            return SmsType.INCOME_TRANSACTION // 출금도 거래 내역으로 처리
        }
        
        // 은행 잔고 패턴 인식 (잔액만 있는 경우)
        if (text.contains("잔액") && !text.contains("입금") && !text.contains("출금")) {
            android.util.Log.d("AiBasedSmsParser", "은행 잔고 패턴 매칭됨")
            return SmsType.BANK_BALANCE
        }
        
        android.util.Log.w("AiBasedSmsParser", "어떤 패턴도 매칭되지 않음")
        return SmsType.UNKNOWN
    }
    
    /**
     * AI 추론으로 카드 거래 파싱
     */
    private fun parseCardTransactionWithAi(smsText: String): CardTransactionEntity? {
        try {
            val parts = smsText.split(" ")
            
            // 은행명 추출
            val bankName = extractBankName(smsText)
            
            // 카드번호 추출 (괄호 안의 숫자)
            val cardNumber = extractCardNumber(smsText)
            
            // 거래타입 추출
            val transactionType = if (smsText.contains("승인")) "승인" else "취소"
            
            // 사용자명 추출
            val user = extractUserName(smsText)
            
            // 금액 추출 (취소 거래는 음수로 처리)
            val rawAmount = extractAmount(smsText)
            val amount = if (transactionType == "취소") -rawAmount else rawAmount
            
            // 할부 정보 추출
            val installment = extractInstallment(smsText)
            
            // 날짜/시간 추출
            val transactionDate = extractDateTime(smsText)
            
            // 가맹점명 추출
            val merchant = extractMerchant(smsText)
            
            // 누적금액 추출
            val cumulativeAmount = extractCumulativeAmount(smsText)
            
            return CardTransactionEntity(
                cardType = bankName,
                cardNumber = cardNumber,
                transactionType = transactionType,
                user = user,
                amount = amount,
                installment = installment,
                transactionDate = transactionDate,
                merchant = merchant,
                cumulativeAmount = cumulativeAmount,
                originalText = smsText
            )
            
        } catch (e: Exception) {
            android.util.Log.e("AiBasedSmsParser", "카드 거래 파싱 오류: ${e.message}", e)
            return null
        }
    }
    
    /**
     * AI 추론으로 수입 내역 파싱 (IncomeTransactionEntity와 BankTransactionEntity 모두 생성)
     */
    private fun parseIncomeTransactionWithAi(smsText: String): IncomeTransactionEntity? {
        try {
            // 은행명 추출
            val bankName = extractBankName(smsText)
            
            // 계좌번호 추출
            val accountNumber = extractAccountNumber(smsText)
            
            // 거래타입 추출
            val transactionType = if (smsText.contains("입금")) "입금" else if (smsText.contains("출금")) "출금" else "기타"
            
            // 설명 추출
            val description = extractDescription(smsText)
            
            // 금액 추출
            val amount = extractAmount(smsText)
            
            // 잔액 추출
            val balance = extractBalance(smsText)
            
            // 날짜/시간 추출
            val transactionDate = extractDateTime(smsText)
            
            // IncomeTransactionEntity 생성
            val incomeEntity = IncomeTransactionEntity(
                bankName = bankName,
                accountNumber = accountNumber,
                transactionType = transactionType,
                description = description,
                amount = amount,
                balance = balance,
                transactionDate = transactionDate
            )
            
            // BankTransactionEntity도 함께 생성하여 저장
            val bankEntity = BankTransactionEntity(
                bankName = bankName,
                accountNumber = accountNumber,
                accountType = "입출금",
                transactionType = transactionType,
                amount = amount,
                balance = balance,
                description = description,
                transactionDate = transactionDate,
                memo = "",
                originalText = smsText
            )
            
            // BankTransactionEntity를 데이터베이스에 저장
            // 이 부분은 SmsDataRepository에서 처리하도록 수정 필요
            
            android.util.Log.d("AiBasedSmsParser", "BankTransactionEntity 생성됨: $bankEntity")
            
            return incomeEntity
            
        } catch (e: Exception) {
            android.util.Log.e("AiBasedSmsParser", "수입 내역 파싱 오류: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 수입 내역에서 은행 잔고 추출
     */
    private fun extractBankBalanceFromIncome(smsText: String, incomeTransaction: IncomeTransactionEntity): BankBalanceEntity? {
        try {
            val balance = extractBalance(smsText)
            val transactionDate = extractDateTime(smsText)
            
            return BankBalanceEntity(
                bankName = incomeTransaction.bankName,
                accountNumber = incomeTransaction.accountNumber,
                balance = balance,
                lastTransactionDate = transactionDate
            )
            
        } catch (e: Exception) {
            android.util.Log.e("AiBasedSmsParser", "은행 잔고 추출 오류: ${e.message}", e)
            return null
        }
    }
    
    /**
     * AI 추론으로 은행 잔고 파싱
     */
    private fun parseBankBalanceWithAi(smsText: String): BankBalanceEntity? {
        try {
            val bankName = extractBankName(smsText)
            val accountNumber = extractAccountNumber(smsText)
            val balance = extractBalance(smsText)
            val transactionDate = extractDateTime(smsText)
            
            return BankBalanceEntity(
                bankName = bankName,
                accountNumber = accountNumber,
                balance = balance,
                lastTransactionDate = transactionDate
            )
            
        } catch (e: Exception) {
            android.util.Log.e("AiBasedSmsParser", "은행 잔고 파싱 오류: ${e.message}", e)
            return null
        }
    }
    
    // === AI 추론 헬퍼 메서드들 ===
    
    private fun extractBankName(text: String): String {
        val koreanBanks = listOf("신한", "삼성", "현대", "KB", "롯데", "하나", "우리", "국민", "농협", "기업")
        
        for (bank in koreanBanks) {
            if (text.contains(bank)) {
                return bank
            }
        }
        
        // 카드 거래에서 은행명 추출
        if (text.contains("신한카드")) return "신한"
        if (text.contains("삼성카드")) return "삼성"
        if (text.contains("현대카드")) return "현대"
        if (text.contains("KB카드")) return "KB"
        if (text.contains("롯데카드")) return "롯데"
        if (text.contains("하나카드")) return "하나"
        
        return "알수없음"
    }
    
    private fun extractCardNumber(text: String): String {
        val pattern = "\\(([0-9]+)\\)".toRegex()
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractUserName(text: String): String {
        // 신*진 형태의 사용자명 추출
        val pattern = "([가-힣]\\*[가-힣])".toRegex()
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractAmount(text: String): Long {
        android.util.Log.d("AiBasedSmsParser", "금액 추출 시도: $text")
        
        // 패턴 1: "원"이 있는 경우
        val patternWithWon = "([0-9,]+)원".toRegex()
        val matchesWithWon = patternWithWon.findAll(text)
        
        for (match in matchesWithWon) {
            val amountStr = match.groupValues[1].replace(",", "")
            val amount = amountStr.toLongOrNull()
            if (amount != null && amount > 0) {
                android.util.Log.d("AiBasedSmsParser", "금액 추출 성공 (원 포함): $amount")
                return amount
            }
        }
        
        // 패턴 2: "입금" 또는 "출금" 키워드 뒤의 금액 추출 (개선된 로직)
        if (text.contains("입금") || text.contains("출금")) {
            val transactionPattern = "(입금|출금)\\s+([0-9,]+)".toRegex()
            val transactionMatch = transactionPattern.find(text)
            if (transactionMatch != null) {
                val amountStr = transactionMatch.groupValues[2].replace(",", "")
                val amount = amountStr.toLongOrNull()
                if (amount != null && amount > 0) {
                    android.util.Log.d("AiBasedSmsParser", "금액 추출 성공 (거래 패턴): $amount")
                    return amount
                }
            }
        }
        
        // 패턴 3: "원"이 없는 경우 - 숫자만 추출
        val patternWithoutWon = "([0-9,]+)".toRegex()
        val matchesWithoutWon = patternWithoutWon.findAll(text)
        
        val amounts = mutableListOf<Long>()
        for (match in matchesWithoutWon) {
            val amountStr = match.groupValues[1].replace(",", "")
            val amount = amountStr.toLongOrNull()
            if (amount != null && amount > 0) {
                amounts.add(amount)
            }
        }
        
        android.util.Log.d("AiBasedSmsParser", "추출된 금액들: $amounts")
        
        if (amounts.isNotEmpty()) {
            // 입금/급여 관련 키워드 근처의 금액을 우선 선택
            val incomeKeywords = listOf("입금", "급여", "보너스", "용돈", "부업")
            
            for (keyword in incomeKeywords) {
                val keywordIndex = text.indexOf(keyword)
                if (keywordIndex >= 0) {
                    // 키워드 뒤에서 금액 찾기 (입금 뒤에 금액이 오는 경우가 많음)
                    val afterKeyword = text.substring(keywordIndex + keyword.length)
                    
                    // 키워드 뒤에서 가장 가까운 큰 금액 찾기
                    val afterPattern = "\\s+([0-9,]+)".toRegex()
                    val afterMatches = afterPattern.findAll(afterKeyword)
                    
                    for (afterMatch in afterMatches) {
                        val amountStr = afterMatch.groupValues[1].replace(",", "")
                        val amount = amountStr.toLongOrNull()
                        if (amount != null && amounts.contains(amount) && amount > 100000) { // 10만원 이상만 고려
                            android.util.Log.d("AiBasedSmsParser", "금액 추출 성공 (키워드 뒤): $amount")
                            return amount
                        }
                    }
                }
            }
            
            // 키워드 근처에서 찾지 못한 경우, 가장 큰 금액을 선택 (입금은 보통 큰 금액)
            val maxAmount = amounts.maxOrNull() ?: 0L
            android.util.Log.d("AiBasedSmsParser", "금액 추출 성공 (최대값): $maxAmount")
            return maxAmount
        }
        
        android.util.Log.w("AiBasedSmsParser", "금액 추출 실패")
        return 0L
    }
    
    private fun extractInstallment(text: String): String {
        val pattern = "\\(([가-힣]+)\\)".toRegex()
        val matches = pattern.findAll(text)
        
        for (match in matches) {
            val installment = match.groupValues[1]
            if (installment.contains("일시불") || installment.contains("할부")) {
                return installment
            }
        }
        
        return "일시불"
    }
    
    private fun extractDateTime(text: String): LocalDateTime {
        val pattern = "([0-9]{2}/[0-9]{2})\\s+([0-9]{2}:[0-9]{2})".toRegex()
        val match = pattern.find(text)
        
        if (match != null) {
            val dateStr = match.groupValues[1]
            val timeStr = match.groupValues[2]
            val currentYear = LocalDateTime.now().year
            
            try {
                return LocalDateTime.parse("$currentYear/$dateStr $timeStr", 
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
            } catch (e: DateTimeParseException) {
                android.util.Log.w("AiBasedSmsParser", "날짜 파싱 오류: $dateStr $timeStr")
            }
        }
        
        return LocalDateTime.now()
    }
    
    private fun extractMerchant(text: String): String {
        // 카드 거래에서 가맹점명 추출 (시간 이후, 누적 이전)
        val pattern = "[0-9]{2}:[0-9]{2}\\s+([가-힣\\w]+)\\s+누적".toRegex()
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractCumulativeAmount(text: String): Long {
        val pattern = "누적([0-9,]+)원".toRegex()
        val match = pattern.find(text)
        
        if (match != null) {
            val amountStr = match.groupValues[1].replace(",", "")
            return amountStr.toLongOrNull() ?: 0L
        }
        
        return 0L
    }
    
    private fun extractAccountNumber(text: String): String {
        val pattern = "([0-9]+-[0-9*-]+)".toRegex()
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractDescription(text: String): String {
        // 급여 관련
        if (text.contains("급여")) return "급여"
        if (text.contains("보너스")) return "보너스"
        
        // 타 소득
        if (text.contains("용돈")) return "용돈"
        if (text.contains("부업")) return "부업"
        if (text.contains("투자수익")) return "투자수익"
        if (text.contains("신년용돈")) return "신년용돈"
        if (text.contains("환급금")) return "환급금"
        if (text.contains("부업수입")) return "부업수입"
        
        // 출금 관련
        if (text.contains("출금")) {
            // 출금 유형별 분류
            if (text.contains("생활비")) return "생활비"
            if (text.contains("카드결제")) return "카드결제"
            if (text.contains("현금인출")) return "현금인출"
            if (text.contains("대출상환")) return "대출상환"
            if (text.contains("신한카드")) return "신한카드"
            return "출금"
        }
        
        // 기본값 (입금이 있지만 구체적인 설명이 없는 경우)
        if (text.contains("입금")) return "입금"
        return "기타"
    }
    
    private fun extractBalance(text: String): Long {
        val pattern = "잔액\\s+([0-9,]+)".toRegex()
        val match = pattern.find(text)
        
        if (match != null) {
            val balanceStr = match.groupValues[1].replace(",", "")
            return balanceStr.toLongOrNull() ?: 0L
        }
        
        return 0L
    }
}

/**
 * SMS 타입 열거형
 */
enum class SmsType {
    CARD_TRANSACTION,    // 카드 거래
    INCOME_TRANSACTION,  // 수입 내역
    BANK_BALANCE,        // 은행 잔고
    UNKNOWN              // 알 수 없음
}
