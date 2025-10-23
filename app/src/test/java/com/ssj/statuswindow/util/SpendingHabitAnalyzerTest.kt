package com.ssj.statuswindow.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpendingHabitAnalyzerTest {

    @Test
    fun `analyze single shinhan card message`() {
        val message = "신한카드(1054)승인 신*진 42,820원(일시불)10/20 14:59 주식회사 이마트 누적1,903,674"

        val report = SpendingHabitAnalyzer.analyze(message)

        assertEquals(1, report.transactionCount)
        assertEquals(42820, report.totalSpending)
        assertEquals(42820, report.netSpending)
        assertTrue(report.topMerchants.firstOrNull()?.merchant?.contains("이마트") == true)
    }

    @Test
    fun `analyze multiple messages with cancellation`() {
        val messages = listOf(
            "신한카드(1054)승인 신*진 10,000원(일시불)10/20 10:00 스타벅스",
            "신한카드(1054)취소 신*진 3,000원(일시불)10/20 11:00 스타벅스",
            "신한카드(1054)승인 신*진 15,000원(일시불)10/21 09:00 버거킹"
        )

        val report = SpendingHabitAnalyzer.analyze(messages)

        assertEquals(3, report.transactionCount)
        assertEquals(25000, report.totalSpending)
        assertEquals(3000, report.totalCancellations)
        assertEquals(22000, report.netSpending)
        assertEquals(2, report.topMerchants.size)
        assertTrue(report.dailySpending.any { it.totalAmount == 15000L })
    }
}
