package com.ssj.statuswindow.util

import com.ssj.statuswindow.database.entity.BankBalanceEntity
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.database.entity.IncomeTransactionEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * SMS 데이터 파싱 유틸리티
 * 3가지 SMS 타입을 파싱하여 Room 엔티티로 변환
 */
object SmsDataParser {
    
    // 카드 거래 내역 패턴
    private val CARD_PATTERN = Pattern.compile(
        "([가-힣]+카드)\\(([0-9]+)\\)([승인|취소]+)\\s+([가-힣*]+)\\s+([0-9,]+)원\\(([가-힣]+)\\)([0-9/]+)\\s+([0-9:]+)\\s+([가-힣\\w]+)\\s+누적([0-9,]+)원"
    )
    
    // 수입 내역 패턴 (공백 처리 개선)
    private val INCOME_PATTERN = Pattern.compile(
        "([가-힣]+)\\s+([0-9/]+)\\s+([0-9:]+)\\s+([0-9-*]+)\\s+([가-힣]+)\\s+([가-힣]+)\\s+([0-9,]+)\\s+잔액\\s+([0-9,]+)\\s+([가-힣]+)"
    )
    
    // 은행 잔고 패턴 (수입 내역에서 잔액 정보 추출)
    private val BALANCE_PATTERN = Pattern.compile(
        "잔액\\s+([0-9,]+)"
    )
    
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
    
    /**
     * SMS 텍스트를 파싱하여 적절한 엔티티로 변환
     */
    fun parseSmsText(smsText: String): List<Any> {
        val entities = mutableListOf<Any>()
        
        // 카드 거래 내역 파싱
        val cardTransaction = parseCardTransaction(smsText)
        if (cardTransaction != null) {
            entities.add(cardTransaction)
        }
        
        // 수입 내역 파싱
        val incomeTransaction = parseIncomeTransaction(smsText)
        if (incomeTransaction != null) {
            entities.add(incomeTransaction)
            
            // 수입 내역에서 은행 잔고 정보도 추출
            val bankBalance = parseBankBalance(smsText, incomeTransaction)
            if (bankBalance != null) {
                entities.add(bankBalance)
            }
        }
        
        return entities
    }
    
    /**
     * 카드 거래 내역 파싱
     * 예: 신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원
     */
    private fun parseCardTransaction(smsText: String): CardTransactionEntity? {
        val matcher = CARD_PATTERN.matcher(smsText)
        
        if (matcher.find()) {
            try {
                val cardType = matcher.group(1) // 신한카드
                val cardNumber = matcher.group(2) // 1054
                val transactionType = matcher.group(3) // 승인
                val user = matcher.group(4) // 신*진
                val amount = matcher.group(5).replace(",", "").toLong() // 98700
                val installment = matcher.group(6) // 일시불
                val dateStr = matcher.group(7) // 10/13
                val timeStr = matcher.group(8) // 15:48
                val merchant = matcher.group(9) // 가톨릭대병원
                val cumulativeAmount = matcher.group(10).replace(",", "").toLong() // 1960854
                
                // 날짜/시간 파싱
                val currentYear = LocalDateTime.now().year
                val transactionDate = LocalDateTime.parse("$currentYear/$dateStr $timeStr", 
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
                
                return CardTransactionEntity(
                    cardType = cardType,
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
                android.util.Log.e("SmsDataParser", "카드 거래 파싱 오류: ${e.message}")
            }
        }
        
        return null
    }
    
    /**
     * 수입 내역 파싱
     * 예: 신한 07/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여
     */
    private fun parseIncomeTransaction(smsText: String): IncomeTransactionEntity? {
        val matcher = INCOME_PATTERN.matcher(smsText)
        
        if (matcher.find()) {
            try {
                val bankName = matcher.group(1) // 신한
                val dateStr = matcher.group(2) // 07/11
                val timeStr = matcher.group(3) // 21:54
                val accountNumber = matcher.group(4) // 100-***-159993
                val transactionType = matcher.group(5) // 입금
                val description = matcher.group(6) // 급여
                val amount = matcher.group(7).replace(",", "").toLong() // 2500000
                val balance = matcher.group(8).replace(",", "").toLong() // 3265147
                
                // 날짜/시간 파싱
                val currentYear = LocalDateTime.now().year
                val transactionDate = LocalDateTime.parse("$currentYear/$dateStr $timeStr", 
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
                
                return IncomeTransactionEntity(
                    bankName = bankName,
                    accountNumber = accountNumber,
                    transactionType = transactionType,
                    description = description,
                    amount = amount,
                    balance = balance,
                    transactionDate = transactionDate
                )
            } catch (e: Exception) {
                android.util.Log.e("SmsDataParser", "수입 내역 파싱 오류: ${e.message}")
            }
        }
        
        return null
    }
    
    /**
     * 은행 잔고 정보 파싱
     * 수입 내역에서 잔액 정보를 추출하여 BankBalanceEntity 생성
     */
    private fun parseBankBalance(smsText: String, incomeTransaction: IncomeTransactionEntity): BankBalanceEntity? {
        val matcher = BALANCE_PATTERN.matcher(smsText)
        
        if (matcher.find()) {
            try {
                val balance = matcher.group(1).replace(",", "").toLong()
                
                return BankBalanceEntity(
                    bankName = incomeTransaction.bankName,
                    accountNumber = incomeTransaction.accountNumber,
                    balance = balance,
                    lastTransactionDate = incomeTransaction.transactionDate
                )
            } catch (e: Exception) {
                android.util.Log.e("SmsDataParser", "은행 잔고 파싱 오류: ${e.message}")
            }
        }
        
        return null
    }
    
    /**
     * SMS 텍스트가 카드 거래인지 확인
     */
    fun isCardTransaction(smsText: String): Boolean {
        return CARD_PATTERN.matcher(smsText).find()
    }
    
    /**
     * SMS 텍스트가 수입 내역인지 확인
     */
    fun isIncomeTransaction(smsText: String): Boolean {
        return INCOME_PATTERN.matcher(smsText).find()
    }
}
