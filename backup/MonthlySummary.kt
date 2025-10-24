package com.ssj.statuswindow.model

import java.time.LocalDate

/**
 * 월별 카드 사용 요약 정보
 */
data class MonthlyCardSummary(
    val year: Int,
    val month: Int,
    val totalAmount: Long,
    val transactionCount: Int,
    val averageAmount: Double,
    val topMerchants: List<MerchantSummary>,
    val cardBreakdown: List<CardSummary>,
    val dailyBreakdown: List<DailySummary>,
    val categoryBreakdown: List<CategorySummary>
)

/**
 * 가맹점별 요약
 */
data class MerchantSummary(
    val merchant: String,
    val amount: Long,
    val count: Int,
    val percentage: Double
)

/**
 * 카드별 요약
 */
data class CardSummary(
    val cardType: String,
    val cardNumber: String,
    val amount: Long,
    val count: Int,
    val percentage: Double
)

/**
 * 일별 요약
 */
data class DailySummary(
    val date: LocalDate,
    val amount: Long,
    val count: Int
)

/**
 * 카테고리별 요약
 */
data class CategorySummary(
    val category: String,
    val amount: Long,
    val count: Int,
    val percentage: Double
)

/**
 * 결제 예상액 계산을 위한 데이터
 */
data class PaymentForecast(
    val currentMonthTotal: Long,
    val estimatedMonthlyTotal: Long,
    val daysRemaining: Int,
    val averageDailySpending: Double,
    val projectedSpending: Long,
    val confidence: Double,
    val actualBillingAmount: Long, // 실제 청구될 금액 (할부 반영)
    val approvedAmount: Long, // 승인 금액 합계
    val cancelledAmount: Long // 취소 금액 합계
)
