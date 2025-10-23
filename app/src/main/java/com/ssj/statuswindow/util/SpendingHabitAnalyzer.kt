package com.ssj.statuswindow.util

import com.ssj.statuswindow.model.CardEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

/**
 * 카드 결제/취소 내역을 바탕으로 간단한 소비 습관을 추정하는 도구.
 *
 * SmsParser 를 통해 파싱한 결과를 입력으로 받아 상점/카테고리/일자별 지출 통계와
 * 대표 지표(총 지출, 평균 결제 금액 등)를 계산한다.
 */
object SpendingHabitAnalyzer {

    private val cardEventFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun analyze(rawMessages: List<String>): SpendingHabitReport {
        if (rawMessages.isEmpty()) return SpendingHabitReport.EMPTY
        val events = rawMessages.flatMap { SmsParser.parse(it) }
        return analyzeEvents(events)
    }

    fun analyze(rawMessage: String): SpendingHabitReport {
        if (rawMessage.isBlank()) return SpendingHabitReport.EMPTY
        return analyze(listOf(rawMessage))
    }

    fun analyzeEvents(events: List<CardEvent>): SpendingHabitReport {
        if (events.isEmpty()) return SpendingHabitReport.EMPTY

        val nonZeroEvents = events.filter { it.amount != 0L }
        if (nonZeroEvents.isEmpty()) return SpendingHabitReport.EMPTY

        val spendingEvents = nonZeroEvents.filter { it.amount > 0 }
        val cancellationEvents = nonZeroEvents.filter { it.amount < 0 }

        val totalSpending = spendingEvents.sumOf { it.amount }
        val totalCancellations = cancellationEvents.sumOf { it.amount.absoluteValue }
        val netSpending = totalSpending - totalCancellations
        val averageSpendingPerTransaction = if (spendingEvents.isNotEmpty()) {
            totalSpending.toDouble() / spendingEvents.size
        } else {
            0.0
        }

        val dailySpending = spendingEvents
            .mapNotNull { event ->
                event.time.toLocalDateTimeOrNull()?.toLocalDate()?.let { it to event.amount }
            }
            .groupBy({ it.first }, { it.second })
            .map { (date, amounts) ->
                DailySpending(
                    date = date.toString(),
                    totalAmount = amounts.sum()
                )
            }
            .sortedBy { it.date }

        val averageDailySpending = if (dailySpending.isNotEmpty()) {
            totalSpending.toDouble() / dailySpending.size
        } else {
            0.0
        }

        val merchantSummaries = spendingEvents
            .groupBy { it.merchant }
            .map { (merchant, merchantEvents) ->
                MerchantSummary(
                    merchant = merchant,
                    totalAmount = merchantEvents.sumOf { it.amount },
                    count = merchantEvents.size
                )
            }
            .sortedByDescending { it.totalAmount }

        val categorySummaries = spendingEvents
            .mapNotNull { event -> event.category?.let { it to event.amount } }
            .groupBy({ it.first }, { it.second })
            .map { (category, amounts) ->
                CategorySummary(
                    category = category,
                    totalAmount = amounts.sum(),
                    count = amounts.size
                )
            }
            .sortedByDescending { it.totalAmount }

        return SpendingHabitReport(
            totalSpending = totalSpending,
            totalCancellations = totalCancellations,
            netSpending = netSpending,
            transactionCount = nonZeroEvents.size,
            averageSpendingPerTransaction = averageSpendingPerTransaction,
            averageDailySpending = averageDailySpending,
            topMerchants = merchantSummaries,
            topCategories = categorySummaries,
            dailySpending = dailySpending,
            events = nonZeroEvents.sortedBy { it.time }
        )
    }

    private fun String.toLocalDateTimeOrNull(): LocalDateTime? = runCatching {
        LocalDateTime.parse(this, cardEventFormatter)
    }.getOrNull()

}

data class SpendingHabitReport(
    val totalSpending: Long,
    val totalCancellations: Long,
    val netSpending: Long,
    val transactionCount: Int,
    val averageSpendingPerTransaction: Double,
    val averageDailySpending: Double,
    val topMerchants: List<MerchantSummary>,
    val topCategories: List<CategorySummary>,
    val dailySpending: List<DailySpending>,
    val events: List<CardEvent>
) {
    companion object {
        val EMPTY = SpendingHabitReport(
            totalSpending = 0L,
            totalCancellations = 0L,
            netSpending = 0L,
            transactionCount = 0,
            averageSpendingPerTransaction = 0.0,
            averageDailySpending = 0.0,
            topMerchants = emptyList(),
            topCategories = emptyList(),
            dailySpending = emptyList(),
            events = emptyList()
        )
    }
}

data class MerchantSummary(
    val merchant: String,
    val totalAmount: Long,
    val count: Int
)

data class CategorySummary(
    val category: String,
    val totalAmount: Long,
    val count: Int
)

data class DailySpending(
    val date: String,
    val totalAmount: Long
)
