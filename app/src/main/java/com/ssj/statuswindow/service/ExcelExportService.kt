package com.ssj.statuswindow.service

import android.content.Context
import android.os.Environment
import com.ssj.statuswindow.model.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 엑셀 형태로 데이터를 내보내는 서비스
 */
class ExcelExportService(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * 카드 거래 내역을 CSV 형태로 내보내기 (엑셀에서 열 수 있음)
     */
    fun exportCardTransactions(transactions: List<CardTransaction>, fileName: String? = null): File? {
        return try {
            val file = createExportFile(fileName ?: "card_transactions_${getCurrentTimestamp()}.csv")
            val writer = FileWriter(file)
            
            // CSV 헤더
            writer.appendLine("날짜,시간,카드사,카드번호,거래유형,사용자,금액,할부,가맹점,누적금액,카테고리,메모")
            
            // 거래 데이터 (중복 제거)
            val uniqueTransactions = transactions.distinctBy { 
                "${it.cardNumber}_${it.amount}_${it.merchant}_${it.transactionDate}" 
            }.sortedByDescending { it.transactionDate }
            
            uniqueTransactions.forEach { transaction ->
                val amountDisplay = if (transaction.transactionType == "승인") {
                    "+${transaction.amount}"
                } else {
                    "-${transaction.amount}"
                }
                
                writer.appendLine(
                    "${dateFormat.format(Date.from(transaction.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))}," +
                    "${timeFormat.format(Date.from(transaction.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))}," +
                    "${transaction.cardType}," +
                    "${transaction.cardNumber}," +
                    "${transaction.transactionType}," +
                    "${transaction.user}," +
                    "${amountDisplay}," +
                    "${transaction.installment}," +
                    "\"${transaction.merchant}\"," +
                    "${transaction.cumulativeAmount}," +
                    "${transaction.category ?: ""}," +
                    "\"${transaction.memo}\""
                )
            }
            
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 월별 요약을 CSV 형태로 내보내기
     */
    fun exportMonthlySummary(summary: MonthlyCardSummary, fileName: String? = null): File? {
        return try {
            val file = createExportFile(fileName ?: "monthly_summary_${summary.year}_${summary.month}.csv")
            val writer = FileWriter(file)
            
            // 기본 요약 정보
            writer.appendLine("월별 카드 사용 요약")
            writer.appendLine("년도,${summary.year}")
            writer.appendLine("월,${summary.month}")
            writer.appendLine("총 사용액,${summary.totalAmount}")
            writer.appendLine("거래 건수,${summary.transactionCount}")
            writer.appendLine("평균 거래액,${String.format("%.2f", summary.averageAmount)}")
            writer.appendLine("")
            
            // 상위 가맹점
            writer.appendLine("상위 가맹점")
            writer.appendLine("가맹점명,사용액,거래건수,비율(%)")
            summary.topMerchants.forEach { merchant ->
                writer.appendLine("\"${merchant.merchant}\",${merchant.amount},${merchant.count},${String.format("%.2f", merchant.percentage)}")
            }
            writer.appendLine("")
            
            // 카드별 요약
            writer.appendLine("카드별 요약")
            writer.appendLine("카드사,카드번호,사용액,거래건수,비율(%)")
            summary.cardBreakdown.forEach { card ->
                writer.appendLine("\"${card.cardType}\",${card.cardNumber},${card.amount},${card.count},${String.format("%.2f", card.percentage)}")
            }
            writer.appendLine("")
            
            // 일별 요약
            writer.appendLine("일별 요약")
            writer.appendLine("날짜,사용액,거래건수")
            summary.dailyBreakdown.forEach { daily ->
                writer.appendLine("${dateFormat.format(Date.from(daily.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()))},${daily.amount},${daily.count}")
            }
            writer.appendLine("")
            
            // 카테고리별 요약
            writer.appendLine("카테고리별 요약")
            writer.appendLine("카테고리,사용액,거래건수,비율(%)")
            summary.categoryBreakdown.forEach { category ->
                writer.appendLine("\"${category.category}\",${category.amount},${category.count},${String.format("%.2f", category.percentage)}")
            }
            
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 결제 예상액 리포트를 CSV 형태로 내보내기
     */
    fun exportPaymentForecast(forecast: PaymentForecast, fileName: String? = null): File? {
        return try {
            val file = createExportFile(fileName ?: "payment_forecast_${getCurrentTimestamp()}.csv")
            val writer = FileWriter(file)
            
            writer.appendLine("결제 예상액 리포트")
            writer.appendLine("항목,값")
            writer.appendLine("현재 월 사용액,${forecast.currentMonthTotal}")
            writer.appendLine("예상 월 총액,${forecast.estimatedMonthlyTotal}")
            writer.appendLine("남은 일수,${forecast.daysRemaining}")
            writer.appendLine("일평균 소비액,${String.format("%.2f", forecast.averageDailySpending)}")
            writer.appendLine("예상 추가 소비액,${forecast.projectedSpending}")
            writer.appendLine("신뢰도,${String.format("%.2f", forecast.confidence * 100)}%")
            
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 통합 리포트 내보내기 (거래내역 + 월별요약 + 예상액)
     */
    fun exportComprehensiveReport(
        transactions: List<CardTransaction>,
        summary: MonthlyCardSummary,
        forecast: PaymentForecast,
        fileName: String? = null
    ): File? {
        return try {
            val file = createExportFile(fileName ?: "comprehensive_report_${getCurrentTimestamp()}.csv")
            val writer = FileWriter(file)
            
            // 1. 기본 요약
            writer.appendLine("=== 월별 카드 사용 요약 ===")
            writer.appendLine("년도,${summary.year}")
            writer.appendLine("월,${summary.month}")
            writer.appendLine("총 사용액,${summary.totalAmount}")
            writer.appendLine("거래 건수,${summary.transactionCount}")
            writer.appendLine("평균 거래액,${String.format("%.2f", summary.averageAmount)}")
            writer.appendLine("")
            
            // 2. 결제 예상액 (할부 반영)
            writer.appendLine("=== 결제 예상액 (할부 반영) ===")
            writer.appendLine("승인 금액 합계,+${forecast.approvedAmount}")
            writer.appendLine("취소 금액 합계,-${forecast.cancelledAmount}")
            writer.appendLine("실제 청구될 금액,${forecast.actualBillingAmount}")
            writer.appendLine("예상 월 총액,${forecast.estimatedMonthlyTotal}")
            writer.appendLine("남은 일수,${forecast.daysRemaining}")
            writer.appendLine("일평균 소비액,${String.format("%.2f", forecast.averageDailySpending)}")
            writer.appendLine("예상 추가 소비액,${forecast.projectedSpending}")
            writer.appendLine("신뢰도,${String.format("%.2f", forecast.confidence * 100)}%")
            writer.appendLine("")
            
            // 3. 상위 가맹점
            writer.appendLine("=== 상위 가맹점 ===")
            writer.appendLine("순위,가맹점명,사용액,거래건수,비율(%)")
            summary.topMerchants.forEachIndexed { index, merchant ->
                writer.appendLine("${index + 1},\"${merchant.merchant}\",${merchant.amount},${merchant.count},${String.format("%.2f", merchant.percentage)}")
            }
            writer.appendLine("")
            
            // 4. 카드별 요약
            writer.appendLine("=== 카드별 요약 ===")
            writer.appendLine("카드사,카드번호,사용액,거래건수,비율(%)")
            summary.cardBreakdown.forEach { card ->
                writer.appendLine("\"${card.cardType}\",${card.cardNumber},${card.amount},${card.count},${String.format("%.2f", card.percentage)}")
            }
            writer.appendLine("")
            
            // 5. 상세 거래 내역 (중복 제거, 승인/취소 표시)
            writer.appendLine("=== 상세 거래 내역 ===")
            writer.appendLine("날짜,시간,카드사,카드번호,거래유형,사용자,금액,할부,가맹점,누적금액,카테고리,메모")
            
            val uniqueTransactions = transactions.distinctBy { 
                "${it.cardNumber}_${it.amount}_${it.merchant}_${it.transactionDate}" 
            }.sortedByDescending { it.transactionDate }
            
            uniqueTransactions.forEach { transaction ->
                val amountDisplay = if (transaction.transactionType == "승인") {
                    "+${transaction.amount}"
                } else {
                    "-${transaction.amount}"
                }
                
                writer.appendLine(
                    "${dateFormat.format(Date.from(transaction.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))}," +
                    "${timeFormat.format(Date.from(transaction.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant()))}," +
                    "${transaction.cardType}," +
                    "${transaction.cardNumber}," +
                    "${transaction.transactionType}," +
                    "${transaction.user}," +
                    "${amountDisplay}," +
                    "${transaction.installment}," +
                    "\"${transaction.merchant}\"," +
                    "${transaction.cumulativeAmount}," +
                    "${transaction.category ?: ""}," +
                    "\"${transaction.memo}\""
                )
            }
            
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 내보내기 파일 생성
     */
    private fun createExportFile(fileName: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val statusWindowDir = File(downloadsDir, "StatusWindow")
        
        if (!statusWindowDir.exists()) {
            statusWindowDir.mkdirs()
        }
        
        return File(statusWindowDir, fileName)
    }
    
    /**
     * 현재 타임스탬프 생성
     */
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}
