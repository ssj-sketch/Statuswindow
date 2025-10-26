package com.ssj.statuswindow.test

import com.ssj.statuswindow.util.AiBasedSmsParser

/**
 * SMS 파서 테스트
 */
object SmsParserTest {
    
    fun testIncomeParsing() {
        val testSms = "신한 10/11 21:54 100-***-159993 입금 2,500,000 잔액 3,700,000 급여"
        
        println("=== SMS 파서 테스트 ===")
        println("입력 SMS: $testSms")
        
        val entities = AiBasedSmsParser.parseSmsText(testSms)
        
        println("파싱 결과:")
        entities.forEach { entity ->
            println("- $entity")
        }
    }
}



