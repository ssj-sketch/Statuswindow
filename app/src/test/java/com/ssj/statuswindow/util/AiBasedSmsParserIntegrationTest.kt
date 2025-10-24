package com.ssj.statuswindow.util

import com.ssj.statuswindow.database.entity.BankBalanceEntity
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.database.entity.IncomeTransactionEntity
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

/**
 * 실제 SMS 데이터를 기반으로 한 AiBasedSmsParser 통합 테스트
 */
class AiBasedSmsParserIntegrationTest {

    @Test
    fun `카드 거래 SMS 파싱 테스트`() {
        val smsText = "신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원"
        
        val result = AiBasedSmsParser.parseSmsText(smsText)
        
        assertTrue("파싱 결과가 있어야 함", result.isNotEmpty())
        
        val cardTransaction = result.find { it is CardTransactionEntity } as? CardTransactionEntity
        assertNotNull("카드 거래가 파싱되어야 함", cardTransaction)
        
        assertEquals("카드타입", "신한카드", cardTransaction!!.cardType)
        assertEquals("카드번호", "1054", cardTransaction.cardNumber)
        assertEquals("사용자", "신*진", cardTransaction.user)
        assertEquals("금액", 98700L, cardTransaction.amount)
        assertEquals("할부", "일시불", cardTransaction.installment)
        assertEquals("가맹점", "가톨릭대병원", cardTransaction.merchant)
        assertEquals("누적금액", 1960854L, cardTransaction.cumulativeAmount)
        
        println("카드 거래 파싱 성공: ${cardTransaction}")
    }

    @Test
    fun `수입 내역 SMS 파싱 테스트`() {
        val smsText = "신한 10/11 21:54  100-***-159993 입금  2,500,000 잔액  3,265,147 급여"
        
        val result = AiBasedSmsParser.parseSmsText(smsText)
        
        assertTrue("파싱 결과가 있어야 함", result.isNotEmpty())
        
        val incomeTransaction = result.find { it is IncomeTransactionEntity } as? IncomeTransactionEntity
        assertNotNull("수입 내역이 파싱되어야 함", incomeTransaction)
        
        assertEquals("은행명", "신한", incomeTransaction!!.bankName)
        assertEquals("계좌번호", "100-***-159993", incomeTransaction.accountNumber)
        assertEquals("거래유형", "입금", incomeTransaction.transactionType)
        assertEquals("설명", "급여", incomeTransaction.description)
        assertEquals("금액", 2500000L, incomeTransaction.amount)
        assertEquals("잔액", 3265147L, incomeTransaction.balance)
        
        println("수입 내역 파싱 성공: ${incomeTransaction}")
        
        // 은행 잔고도 함께 파싱되는지 확인
        val bankBalance = result.find { it is BankBalanceEntity } as? BankBalanceEntity
        assertNotNull("은행 잔고가 파싱되어야 함", bankBalance)
        assertEquals("잔액이 일치해야 함", incomeTransaction.balance, bankBalance!!.balance)
        
        println("은행 잔고 파싱 성공: ${bankBalance}")
    }

    @Test
    fun `금액 추출 정확성 테스트`() {
        val testCases = listOf(
            "신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원" to 98700L,
            "신한카드(1054)승인 신*진 68,700원(일시불)10/13 15:48 가톨릭대병원 누적1,860,854원" to 68700L,
            "신한카드(1054)승인 신*진 2,700원(일시불)10/13 15:48 스타벅스 누적1,860,854원" to 2700L,
            "신한카드(1054)승인 신*진 42,820원(일시불)10/20 14:59 주식회사 이마트 누적1,903,674원" to 42820L,
            "신한 10/11 21:54  100-***-159993 입금  2,500,000 잔액  3,265,147 급여" to 2500000L,
            "신한 10/11 21:55  100-***-159993 입금   1,000,000 잔액  4,265,147" to 1000000L
        )
        
        testCases.forEach { (smsText, expectedAmount) ->
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val entity = result.find { 
                it is CardTransactionEntity || it is IncomeTransactionEntity 
            }
            
            assertNotNull("SMS에서 엔티티가 파싱되어야 함: $smsText", entity)
            
            val actualAmount = when (entity) {
                is CardTransactionEntity -> entity.amount
                is IncomeTransactionEntity -> entity.amount
                else -> 0L
            }
            
            assertEquals("금액이 정확히 추출되어야 함: $smsText", expectedAmount, actualAmount)
            println("금액 추출 성공: ${String.format("%,d", expectedAmount)}원 - $smsText")
        }
    }

    @Test
    fun `잔액 추출 정확성 테스트`() {
        val testCases = listOf(
            "신한 10/11 21:54  100-***-159993 입금  2,500,000 잔액  3,265,147 급여" to 3265147L,
            "신한 10/11 21:55  100-***-159993 입금   1,000,000 잔액  4,265,147" to 4265147L,
            "신한 09/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  5,265,147 급여" to 5265147L,
            "신한 08/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  4,265,147 급여" to 4265147L
        )
        
        testCases.forEach { (smsText, expectedBalance) ->
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val bankBalance = result.find { it is BankBalanceEntity } as? BankBalanceEntity
            assertNotNull("SMS에서 은행 잔고가 파싱되어야 함: $smsText", bankBalance)
            
            assertEquals("잔액이 정확히 추출되어야 함: $smsText", expectedBalance, bankBalance!!.balance)
            println("잔액 추출 성공: ${String.format("%,d", expectedBalance)}원 - $smsText")
        }
    }

    @Test
    fun `누적금액 추출 정확성 테스트`() {
        val testCases = listOf(
            "신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원" to 1960854L,
            "신한카드(1054)승인 신*진 68,700원(일시불)10/13 15:48 가톨릭대병원 누적1,860,854원" to 1860854L,
            "신한카드(1054)승인 신*진 42,820원(일시불)10/20 14:59 주식회사 이마트 누적1,903,674원" to 1903674L
        )
        
        testCases.forEach { (smsText, expectedCumulative) ->
            val result = AiBasedSmsParser.parseSmsText(smsText)
            
            val cardTransaction = result.find { it is CardTransactionEntity } as? CardTransactionEntity
            assertNotNull("SMS에서 카드 거래가 파싱되어야 함: $smsText", cardTransaction)
            
            assertEquals("누적금액이 정확히 추출되어야 함: $smsText", expectedCumulative, cardTransaction!!.cumulativeAmount)
            println("누적금액 추출 성공: ${String.format("%,d", expectedCumulative)}원 - $smsText")
        }
    }

    @Test
    fun `전체 SMS 데이터 처리 테스트`() {
        val cardSmsList = listOf(
            "신한카드(1054)승인 신*진 98,700원(일시불)10/13 15:48 가톨릭대병원 누적1,960,854원",
            "신한카드(1054)승인 신*진 68,700원(일시불)10/13 15:48 가톨릭대병원 누적1,860,854원",
            "신한카드(1054)승인 신*진 12,700원(일시불)10/13 15:48 스타벅스 누적1,860,854원",
            "신한카드(1054)승인 신*진 2,700원(일시불)10/13 15:48 스타벅스 누적1,860,854원",
            "신한카드(1054)승인 신*진 42,820원(일시불)10/20 14:59 주식회사 이마트 누적1,903,674원"
        )

        val incomeSmsList = listOf(
            "신한 10/11 21:55  100-***-159993 입금   1,000,000 잔액  4,265,147",
            "신한 10/11 21:54  100-***-159993 입금  2,500,000 잔액  3,265,147 급여",
            "신한 09/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  5,265,147 급여",
            "신한 08/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  4,265,147 급여",
            "신한 07/11 21:54  100-***-159993 입금 급여  2,500,000 잔액  3,265,147 급여"
        )

        val allSmsList = cardSmsList + incomeSmsList
        val startTime = System.currentTimeMillis()
        
        var totalParsedEntities = 0
        var successfulParses = 0
        
        allSmsList.forEachIndexed { index, smsText ->
            try {
                val result = AiBasedSmsParser.parseSmsText(smsText)
                totalParsedEntities += result.size
                successfulParses++
                
                if (result.isEmpty()) {
                    println("경고: SMS ${index + 1}에서 파싱 결과가 없음 - $smsText")
                } else {
                    println("성공: SMS ${index + 1}에서 ${result.size}개 엔티티 파싱됨")
                }
            } catch (e: Exception) {
                println("오류: SMS ${index + 1} 파싱 실패 - $smsText, 오류: ${e.message}")
            }
        }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime
        
        println("=== 전체 SMS 데이터 처리 결과 ===")
        println("총 SMS 개수: ${allSmsList.size}")
        println("성공적으로 파싱된 SMS: $successfulParses")
        println("총 파싱된 엔티티 수: $totalParsedEntities")
        println("처리 시간: ${processingTime}ms")
        println("평균 처리 시간: ${processingTime.toDouble() / allSmsList.size}ms/SMS")
        
        assertTrue("모든 SMS가 성공적으로 파싱되어야 함", successfulParses == allSmsList.size)
        assertTrue("충분한 엔티티가 파싱되어야 함", totalParsedEntities > allSmsList.size)
    }
}
