package com.ssj.statuswindow.test

import com.ssj.statuswindow.util.AiBasedSmsParser

/**
 * SMS 파서 직접 테스트
 */
object DirectSmsParserTest {
    
    fun testIncomeSmsParsing() {
        val testSms = "신한 10/11 21:54 100-***-159993 입금 2,500,000 잔액 3,700,000 급여"
        
        println("=== 직접 SMS 파서 테스트 ===")
        println("입력 SMS: $testSms")
        
        val entities = AiBasedSmsParser.parseSmsText(testSms)
        
        println("파싱 결과:")
        entities.forEach { entity ->
            println("- $entity")
        }
        
        println("총 ${entities.size}개 엔티티 생성됨")
    }
    
    fun testWithdrawalSmsParsing() {
        val testSms = "신한 10/11 21:54 100-***-159993 출금 3,500,000 잔액 1,200,000 신한카드"
        
        println("=== 출금 SMS 파서 테스트 ===")
        println("입력 SMS: $testSms")
        
        val entities = AiBasedSmsParser.parseSmsText(testSms)
        
        println("파싱 결과:")
        entities.forEach { entity ->
            println("- $entity")
        }
        
        println("총 ${entities.size}개 엔티티 생성됨")
    }
}



