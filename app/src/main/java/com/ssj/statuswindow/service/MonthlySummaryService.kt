package com.ssj.statuswindow.service

import com.ssj.statuswindow.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 월별 카드 사용 요약을 계산하는 서비스
 */
class MonthlySummaryService {
    
    /**
     * 현재 월의 카드 사용 요약을 계산
     */
    fun calculateMonthlySummary(transactions: List<CardTransaction>): MonthlyCardSummary {
        val currentDate = LocalDate.now()
        val currentMonth = currentDate.monthValue
        val currentYear = currentDate.year
        
        // 이번달 거래만 필터링
        val monthlyTransactions = transactions.filter { transaction ->
            transaction.transactionDate.year == currentYear &&
            transaction.transactionDate.monthValue == currentMonth
        }
        
        val totalAmount = monthlyTransactions.sumOf { it.amount }
        val transactionCount = monthlyTransactions.size
        val averageAmount = if (transactionCount > 0) totalAmount.toDouble() / transactionCount else 0.0
        
        // 상위 가맹점 계산
        val topMerchants = calculateTopMerchants(monthlyTransactions, totalAmount)
        
        // 카드별 요약 계산
        val cardBreakdown = calculateCardBreakdown(monthlyTransactions, totalAmount)
        
        // 일별 요약 계산
        val dailyBreakdown = calculateDailyBreakdown(monthlyTransactions)
        
        // 카테고리별 요약 계산 (현재는 기본 카테고리)
        val categoryBreakdown = calculateCategoryBreakdown(monthlyTransactions, totalAmount)
        
        return MonthlyCardSummary(
            year = currentYear,
            month = currentMonth,
            totalAmount = totalAmount,
            transactionCount = transactionCount,
            averageAmount = averageAmount,
            topMerchants = topMerchants,
            cardBreakdown = cardBreakdown,
            dailyBreakdown = dailyBreakdown,
            categoryBreakdown = categoryBreakdown
        )
    }
    
    /**
     * 결제 예상액 계산 (할부 반영한 실제 청구 금액)
     */
    fun calculatePaymentForecast(transactions: List<CardTransaction>): PaymentForecast {
        val currentDate = LocalDate.now()
        val currentMonth = currentDate.monthValue
        val currentYear = currentDate.year
        
        // 이번달 거래만 필터링 (중복 제거)
        val monthlyTransactions = transactions.filter { transaction ->
            transaction.transactionDate.year == currentYear &&
            transaction.transactionDate.monthValue == currentMonth
        }.distinctBy { "${it.cardNumber}_${it.amount}_${it.merchant}_${it.transactionDate}" }
        
        // 승인/취소 금액 분리 계산
        val approvedTransactions = monthlyTransactions.filter { it.transactionType == "승인" }
        val cancelledTransactions = monthlyTransactions.filter { it.transactionType == "취소" }
        
        val approvedAmount = approvedTransactions.sumOf { it.amount }
        val cancelledAmount = cancelledTransactions.sumOf { it.amount }
        val currentMonthTotal = approvedAmount - cancelledAmount
        
        // 할부를 반영한 실제 청구 금액 계산
        val actualBillingAmount = calculateActualBillingAmount(monthlyTransactions)
        
        // 월말까지 남은 일수 계산
        val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
        val daysRemaining = ChronoUnit.DAYS.between(currentDate, lastDayOfMonth).toInt() + 1
        
        // 일평균 소비액 계산 (승인 금액 기준)
        val daysPassed = currentDate.dayOfMonth
        val averageDailySpending = if (daysPassed > 0) approvedAmount.toDouble() / daysPassed else 0.0
        
        // 예상 월 총액 계산 (승인 금액 기준)
        val projectedSpending = (averageDailySpending * daysRemaining).roundToInt().toLong()
        val estimatedMonthlyTotal = approvedAmount + projectedSpending
        
        // 신뢰도 계산 (거래 수와 일수 기반)
        val confidence = calculateConfidence(monthlyTransactions.size, daysPassed)
        
        return PaymentForecast(
            currentMonthTotal = currentMonthTotal,
            estimatedMonthlyTotal = estimatedMonthlyTotal,
            daysRemaining = daysRemaining,
            averageDailySpending = averageDailySpending,
            projectedSpending = projectedSpending,
            confidence = confidence,
            actualBillingAmount = actualBillingAmount,
            approvedAmount = approvedAmount,
            cancelledAmount = cancelledAmount
        )
    }
    
    /**
     * 할부를 반영한 실제 청구 금액 계산
     */
    private fun calculateActualBillingAmount(transactions: List<CardTransaction>): Long {
        val currentDate = LocalDate.now()
        val currentMonth = currentDate.monthValue
        val currentYear = currentDate.year
        
        // 이번달에 청구될 할부 금액 계산
        var billingAmount = 0L
        
        transactions.forEach { transaction ->
            when (transaction.installment) {
                "일시불" -> {
                    // 일시불은 당월 청구
                    if (transaction.transactionType == "승인") {
                        billingAmount += transaction.amount
                    } else if (transaction.transactionType == "취소") {
                        billingAmount -= transaction.amount
                    }
                }
                else -> {
                    // 할부는 첫 달에만 청구 (예: "3개월" -> 3개월 할부)
                    val installmentMonths = transaction.installment.replace("개월", "").toIntOrNull() ?: 1
                    val transactionMonth = transaction.transactionDate.monthValue
                    val transactionYear = transaction.transactionDate.year
                    
                    // 할부 첫 달이 현재 월과 같은 경우에만 청구
                    if (transactionYear == currentYear && transactionMonth == currentMonth) {
                        if (transaction.transactionType == "승인") {
                            billingAmount += transaction.amount
                        } else if (transaction.transactionType == "취소") {
                            billingAmount -= transaction.amount
                        }
                    }
                }
            }
        }
        
        return billingAmount
    }
    
    /**
     * 상위 가맹점 계산
     */
    private fun calculateTopMerchants(transactions: List<CardTransaction>, totalAmount: Long): List<MerchantSummary> {
        val merchantGroups = transactions.groupBy { it.merchant }
        
        return merchantGroups.map { (merchant, merchantTransactions) ->
            val amount = merchantTransactions.sumOf { it.amount }
            val count = merchantTransactions.size
            val percentage = if (totalAmount > 0) (amount.toDouble() / totalAmount) * 100 else 0.0
            
            MerchantSummary(
                merchant = merchant,
                amount = amount,
                count = count,
                percentage = percentage
            )
        }.sortedByDescending { it.amount }.take(10)
    }
    
    /**
     * 카드별 요약 계산
     */
    private fun calculateCardBreakdown(transactions: List<CardTransaction>, totalAmount: Long): List<CardSummary> {
        val cardGroups = transactions.groupBy { "${it.cardType}(${it.cardNumber})" }
        
        return cardGroups.map { (cardInfo, cardTransactions) ->
            val amount = cardTransactions.sumOf { it.amount }
            val count = cardTransactions.size
            val percentage = if (totalAmount > 0) (amount.toDouble() / totalAmount) * 100 else 0.0
            
            val cardType = cardTransactions.first().cardType
            val cardNumber = cardTransactions.first().cardNumber
            
            CardSummary(
                cardType = cardType,
                cardNumber = cardNumber,
                amount = amount,
                count = count,
                percentage = percentage
            )
        }.sortedByDescending { it.amount }
    }
    
    /**
     * 일별 요약 계산
     */
    private fun calculateDailyBreakdown(transactions: List<CardTransaction>): List<DailySummary> {
        val dailyGroups = transactions.groupBy { it.transactionDate.toLocalDate() }
        
        return dailyGroups.map { (date, dailyTransactions) ->
            val amount = dailyTransactions.sumOf { it.amount }
            val count = dailyTransactions.size
            
            DailySummary(
                date = date,
                amount = amount,
                count = count
            )
        }.sortedBy { it.date }
    }
    
    /**
     * 카테고리별 요약 계산 (기본 카테고리)
     */
    private fun calculateCategoryBreakdown(transactions: List<CardTransaction>, totalAmount: Long): List<CategorySummary> {
        // 기본 카테고리 매핑
        val categoryMapping = mapOf(
            "스타벅스" to "카페/음료",
            "이마트" to "마트/쇼핑",
            "가톨릭대병원" to "의료/건강",
            "올리브영" to "화장품/뷰티",
            "맥도날드" to "외식/배달",
            "배달의민족" to "외식/배달",
            "요기요" to "외식/배달",
            "쿠팡" to "온라인쇼핑",
            "네이버" to "온라인쇼핑",
            "아마존" to "온라인쇼핑"
        )
        
        val categoryGroups = transactions.groupBy { transaction ->
            categoryMapping.entries.find { (key, _) ->
                transaction.merchant.contains(key)
            }?.value ?: "기타"
        }
        
        return categoryGroups.map { (category, categoryTransactions) ->
            val amount = categoryTransactions.sumOf { it.amount }
            val count = categoryTransactions.size
            val percentage = if (totalAmount > 0) (amount.toDouble() / totalAmount) * 100 else 0.0
            
            CategorySummary(
                category = category,
                amount = amount,
                count = count,
                percentage = percentage
            )
        }.sortedByDescending { it.amount }
    }
    
    /**
     * 신뢰도 계산
     */
    private fun calculateConfidence(transactionCount: Int, daysPassed: Int): Double {
        // 거래 수가 많고, 충분한 일수가 지났을수록 신뢰도가 높음
        val transactionFactor = minOf(transactionCount / 10.0, 1.0) // 최대 1.0
        val dayFactor = minOf(daysPassed / 15.0, 1.0) // 최대 1.0
        
        return (transactionFactor + dayFactor) / 2.0
    }
}
