package com.ssj.statuswindow.util

import com.ssj.statuswindow.database.entity.BankBalanceEntity
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.database.entity.IncomeTransactionEntity
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

/**
 * AiBasedSmsParser 단위 테스트
 */
class AiBasedSmsParserTest {

    @Test
    fun `수입 내역 SMS 파싱 테스트 - 급여`() {
        val smsText = "신한 10/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여"
        
        val result = AiBasedSmsParser.parseSmsText(smsText)
        
        assertEquals("파싱 결과는 2개여야 함 (수입내역 + 은행잔고)", 2, result.size)
        
        val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
        assertNotNull("수입 내역이 파싱되어야 함", incomeTransaction)
        
        assertEquals("은행명", "신한", incomeTransaction!!.bankName)
        assertEquals("계좌번호", "100-***-159993", incomeTransaction.accountNumber)
        assertEquals("거래유형", "입금", incomeTransaction.transactionType)
        assertEquals("설명", "급여", incomeTransaction.description)
        assertEquals("금액", 2500000L, incomeTransaction.amount)
        assertEquals("잔액", 3265147L, incomeTransaction.balance)
        
        val bankBalance = result.find { it is BankBalanceEntity } as? BankBalanceEntity
        assertNotNull("은행 잔고가 파싱되어야 함", bankBalance)
        assertEquals("잔액", 3265147L, bankBalance!!.balance)
    }

    @Test
    fun `수입 내역 SMS 파싱 테스트 - 급여 없이`() {
        val smsText = "신한 10/11 21:55  100-***-159993 입금   1,000,000 잔액  4,265,147"
        
        val result = AiBasedSmsParser.parseSmsText(smsText)
        
        assertEquals("파싱 결과는 2개여야 함 (수입내역 + 은행잔고)", 2, result.size)
        
        val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
        assertNotNull("수입 내역이 파싱되어야 함", incomeTransaction)
        
        assertEquals("금액", 1000000L, incomeTransaction!!.amount)
        assertEquals("설명", "입금", incomeTransaction.description)
    }

    @Test
    fun `카드 거래 SMS 파싱 테스트`() {
        val smsText = "신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원"
        
        val result = AiBasedSmsParser.parseSmsText(smsText)
        
        assertEquals("파싱 결과는 1개여야 함 (카드거래)", 1, result.size)
        
        val cardTransaction = result.find { it is CardTransactionEntity } as? CardTransactionEntity
        assertNotNull("카드 거래가 파싱되어야 함", cardTransaction)
        
        assertEquals("카드타입", "신한카드", cardTransaction!!.cardType)
        assertEquals("카드번호", "1054", cardTransaction.cardNumber)
        assertEquals("사용자", "신*진", cardTransaction.user)
        assertEquals("금액", 98700L, cardTransaction.amount)
        assertEquals("할부", "일시불", cardTransaction.installment)
        assertEquals("가맹점", "가톨릭대병원", cardTransaction.merchant)
        assertEquals("누적금액", 1960854L, cardTransaction.cumulativeAmount)
    }

    @Test
    fun `여러 줄 SMS 파싱 테스트`() {
        val multiLineSms = """
            신한 10/11 21:55  100-***-159993 입금   1,000,000 잔액  4,265,147  
            신한 10/11 21:55  100-***-159993 입금   1,000,000 잔액  4,265,147 
            신한 10/11 21:54  100-***-159993 입금  2,500,000 잔액  3,265,147 급여
        """.trimIndent()
        
        // 각 줄을 개별적으로 파싱
        val lines = multiLineSms.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        assertEquals("3줄이 있어야 함", 3, lines.size)
        
        lines.forEachIndexed { index, line ->
            val result = AiBasedSmsParser.parseSmsText(line)
            assertTrue("줄 ${index + 1}에서 파싱 결과가 있어야 함", result.isNotEmpty())
            
            val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
            assertNotNull("줄 ${index + 1}에서 수입 내역이 파싱되어야 함", incomeTransaction)
            
            if (line.contains("급여")) {
                assertEquals("급여 금액", 2500000L, incomeTransaction!!.amount)
                assertEquals("급여 설명", "급여", incomeTransaction.description)
            } else {
                assertEquals("일반 입금 금액", 1000000L, incomeTransaction!!.amount)
                assertEquals("일반 입금 설명", "입금", incomeTransaction.description)
            }
        }
    }

    @Test
    fun `금액 추출 테스트 - 다양한 형식`() {
        val testCases = listOf(
            "입금 2,500,000 잔액" to 2500000L,
            "입금 1000000 잔액" to 1000000L,
            "입금 500,000원 잔액" to 500000L,
            "입금 1,000,000원 잔액" to 1000000L,
            "급여 3,000,000 잔액" to 3000000L,
            "보너스 500,000 잔액" to 500000L
        )
        
        testCases.forEach { (text, expectedAmount) ->
            val smsText = "신한 10/11 21:54  100-***-159993 $text  3,265,147"
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
            assertNotNull("$text 에서 수입 내역이 파싱되어야 함", incomeTransaction)
            assertEquals("$text 에서 금액이 올바르게 추출되어야 함", expectedAmount, incomeTransaction!!.amount)
        }
    }

    @Test
    fun `잔액 추출 테스트`() {
        val testCases = listOf(
            "잔액 3,265,147" to 3265147L,
            "잔액 1000000" to 1000000L,
            "잔액 5,000,000" to 5000000L
        )
        
        testCases.forEach { (text, expectedBalance) ->
            val smsText = "신한 10/11 21:54  100-***-159993 입금 2,500,000 $text"
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val bankBalance = result.find { it is BankBalanceEntity } as? BankBalanceEntity
            assertNotNull("$text 에서 은행 잔고가 파싱되어야 함", bankBalance)
            assertEquals("$text 에서 잔액이 올바르게 추출되어야 함", expectedBalance, bankBalance!!.balance)
        }
    }

    @Test
    fun `은행명 추출 테스트`() {
        val testCases = listOf(
            "신한 10/11" to "신한",
            "KB 10/11" to "KB",
            "하나 10/11" to "하나",
            "우리 10/11" to "우리",
            "국민 10/11" to "국민",
            "농협 10/11" to "농협",
            "기업 10/11" to "기업"
        )
        
        testCases.forEach { (text, expectedBank) ->
            val smsText = "$text 21:54  100-***-159993 입금 2,500,000 잔액 3,265,147"
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
            assertNotNull("$text 에서 수입 내역이 파싱되어야 함", incomeTransaction)
            assertEquals("$text 에서 은행명이 올바르게 추출되어야 함", expectedBank, incomeTransaction!!.bankName)
        }
    }

    @Test
    fun `계좌번호 추출 테스트`() {
        val testCases = listOf(
            "100-***-159993" to "100-***-159993",
            "123-456-789012" to "123-456-789012",
            "999-***-888888" to "999-***-888888"
        )
        
        testCases.forEach { (accountNumber, expectedAccount) ->
            val smsText = "신한 10/11 21:54  $accountNumber 입금 2,500,000 잔액 3,265,147"
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
            assertNotNull("$accountNumber 에서 수입 내역이 파싱되어야 함", incomeTransaction)
            assertEquals("$accountNumber 에서 계좌번호가 올바르게 추출되어야 함", expectedAccount, incomeTransaction!!.accountNumber)
        }
    }

    @Test
    fun `날짜시간 추출 테스트`() {
        val testCases = listOf(
            "10/11 21:54" to LocalDateTime.of(2025, 10, 11, 21, 54),
            "12/25 15:30" to LocalDateTime.of(2025, 12, 25, 15, 30),
            "01/01 00:00" to LocalDateTime.of(2025, 1, 1, 0, 0)
        )
        
        testCases.forEach { (dateTimeStr, expectedDateTime) ->
            val smsText = "신한 $dateTimeStr  100-***-159993 입금 2,500,000 잔액 3,265,147"
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
            assertNotNull("$dateTimeStr 에서 수입 내역이 파싱되어야 함", incomeTransaction)
            assertEquals("$dateTimeStr 에서 날짜시간이 올바르게 추출되어야 함", expectedDateTime, incomeTransaction!!.transactionDate)
        }
    }

    @Test
    fun `설명 추출 테스트`() {
        val testCases = listOf(
            "급여" to "급여",
            "보너스" to "보너스",
            "용돈" to "용돈",
            "부업" to "부업",
            "투자수익" to "투자수익",
            "출금" to "출금",
            "입금" to "입금"
        )
        
        testCases.forEach { (description, expectedDescription) ->
            val smsText = "신한 10/11 21:54  100-***-159993 $description 2,500,000 잔액 3,265,147"
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
            assertNotNull("$description 에서 수입 내역이 파싱되어야 함", incomeTransaction)
            assertEquals("$description 에서 설명이 올바르게 추출되어야 함", expectedDescription, incomeTransaction!!.description)
        }
    }

    @Test
    fun `잘못된 형식 SMS 처리 테스트`() {
        val invalidSmsList = listOf(
            "잘못된 형식의 SMS",
            "123456789",
            "은행명만 있는 SMS",
            "",
            "   ",
            "특수문자만 !@#$%^&*()"
        )
        
        invalidSmsList.forEach { invalidSms ->
            val result = AiBasedSmsParser.parseSmsText(invalidSms)
            // 잘못된 형식의 SMS는 빈 결과를 반환하거나 기본값을 가진 엔티티를 반환해야 함
            assertTrue("잘못된 형식 '$invalidSms' 에서도 오류 없이 처리되어야 함", result.isEmpty() || result.all { entity ->
                when (entity) {
                    is IncomeTransactionEntity -> entity.amount >= 0
                    is BankBalanceEntity -> entity.balance >= 0
                    is CardTransactionEntity -> entity.amount >= 0
                    else -> true
                }
            })
        }
    }

    @Test
    fun `경계값 테스트`() {
        // 매우 큰 금액
        val largeAmountSms = "신한 10/11 21:54  100-***-159993 입금 999,999,999 잔액 1,000,000,000"
        val largeResult = AiBasedSmsParser.parseSmsText(largeAmountSms)
        val largeIncome = largeResult.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
        assertEquals("큰 금액 처리", 999999999L, largeIncome?.amount)
        
        // 매우 작은 금액
        val smallAmountSms = "신한 10/11 21:54  100-***-159993 입금 1 잔액 100"
        val smallResult = AiBasedSmsParser.parseSmsText(smallAmountSms)
        val smallIncome = smallResult.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
        assertEquals("작은 금액 처리", 1L, smallIncome?.amount)
    }
}