package com.ssj.statuswindow.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ssj.statuswindow.R
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.model.MonthlyCardSummary
import com.ssj.statuswindow.model.PaymentForecast
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 엑셀 미리보기 다이얼로그 유틸리티
 */
object ExcelPreviewDialog {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    
    /**
     * 거래내역 미리보기 다이얼로그 표시
     */
    fun showTransactionPreview(
        context: Context,
        transactions: List<CardTransaction>,
        fileName: String,
        onDownload: () -> Unit,
        onCancel: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_excel_preview)
        
        // 파일 정보 설정
        dialog.findViewById<TextView>(R.id.tvFileName).text = fileName
        dialog.findViewById<TextView>(R.id.tvDataCount).text = "${transactions.size}건"
        
        // 테이블 생성
        val tableLayout = dialog.findViewById<TableLayout>(R.id.tablePreview)
        createTransactionTable(context, tableLayout, transactions)
        
        // 버튼 이벤트
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownload).setOnClickListener {
            onDownload()
            dialog.dismiss()
        }
        
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            onCancel()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * 월별 요약 미리보기 다이얼로그 표시
     */
    fun showMonthlySummaryPreview(
        context: Context,
        summary: MonthlyCardSummary,
        fileName: String,
        onDownload: () -> Unit,
        onCancel: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_excel_preview)
        
        // 파일 정보 설정
        dialog.findViewById<TextView>(R.id.tvFileName).text = fileName
        dialog.findViewById<TextView>(R.id.tvDataCount).text = "요약 데이터"
        
        // 테이블 생성
        val tableLayout = dialog.findViewById<TableLayout>(R.id.tablePreview)
        createMonthlySummaryTable(context, tableLayout, summary)
        
        // 버튼 이벤트
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownload).setOnClickListener {
            onDownload()
            dialog.dismiss()
        }
        
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            onCancel()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * 통합 리포트 미리보기 다이얼로그 표시
     */
    fun showComprehensiveReportPreview(
        context: Context,
        transactions: List<CardTransaction>,
        summary: MonthlyCardSummary,
        forecast: PaymentForecast,
        fileName: String,
        onDownload: () -> Unit,
        onCancel: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_excel_preview)
        
        // 파일 정보 설정
        dialog.findViewById<TextView>(R.id.tvFileName).text = fileName
        dialog.findViewById<TextView>(R.id.tvDataCount).text = "${transactions.size}건 + 요약"
        
        // 테이블 생성
        val tableLayout = dialog.findViewById<TableLayout>(R.id.tablePreview)
        createComprehensiveReportTable(context, tableLayout, transactions, summary, forecast)
        
        // 버튼 이벤트
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownload).setOnClickListener {
            onDownload()
            dialog.dismiss()
        }
        
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel).setOnClickListener {
            onCancel()
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * 거래내역 테이블 생성
     */
    private fun createTransactionTable(context: Context, tableLayout: TableLayout, transactions: List<CardTransaction>) {
        // 헤더 행
        val headerRow = TableRow(context)
        val headers = arrayOf("날짜", "시간", "카드사", "금액", "가맹점", "거래유형")
        
        headers.forEach { header ->
            val textView = createHeaderCell(context, header)
            headerRow.addView(textView)
        }
        tableLayout.addView(headerRow)
        
        // 데이터 행 (최대 10건)
        val previewTransactions = transactions.distinctBy { 
            "${it.cardNumber}_${it.amount}_${it.merchant}_${it.transactionDate}" 
        }.sortedByDescending { it.transactionDate }.take(10)
        
        previewTransactions.forEach { transaction ->
            val dataRow = TableRow(context)
            
            val amountDisplay = if (transaction.transactionType == "승인") {
                "+${numberFormat.format(transaction.amount)}"
            } else {
                "-${numberFormat.format(transaction.amount)}"
            }
            
            val data = arrayOf(
                dateFormat.format(Date.from(transaction.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant())),
                timeFormat.format(Date.from(transaction.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant())),
                transaction.cardType,
                amountDisplay,
                transaction.merchant.take(10) + if (transaction.merchant.length > 10) "..." else "",
                transaction.transactionType
            )
            
            data.forEach { cellData ->
                val textView = createDataCell(context, cellData)
                dataRow.addView(textView)
            }
            tableLayout.addView(dataRow)
        }
    }
    
    /**
     * 월별 요약 테이블 생성
     */
    private fun createMonthlySummaryTable(context: Context, tableLayout: TableLayout, summary: MonthlyCardSummary) {
        // 기본 요약 정보
        val summaryRow = TableRow(context)
        val summaryText = createHeaderCell(context, "월별 요약")
        summaryText.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 6f)
        summaryRow.addView(summaryText)
        tableLayout.addView(summaryRow)
        
        // 요약 데이터
        val dataRows = arrayOf(
            arrayOf("년도", summary.year.toString()),
            arrayOf("월", summary.month.toString()),
            arrayOf("총 사용액", "${numberFormat.format(summary.totalAmount)}원"),
            arrayOf("거래 건수", "${summary.transactionCount}건"),
            arrayOf("평균 거래액", "${numberFormat.format(summary.averageAmount.toLong())}원")
        )
        
        dataRows.forEach { rowData ->
            val dataRow = TableRow(context)
            rowData.forEach { cellData ->
                val textView = createDataCell(context, cellData)
                dataRow.addView(textView)
            }
            tableLayout.addView(dataRow)
        }
        
        // 상위 가맹점 헤더
        val merchantHeaderRow = TableRow(context)
        val merchantHeaderText = createHeaderCell(context, "상위 가맹점")
        merchantHeaderText.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 6f)
        merchantHeaderRow.addView(merchantHeaderText)
        tableLayout.addView(merchantHeaderRow)
        
        // 상위 가맹점 데이터 (최대 5개)
        summary.topMerchants.take(5).forEach { merchant ->
            val merchantRow = TableRow(context)
            val merchantData = arrayOf(
                merchant.merchant.take(15) + if (merchant.merchant.length > 15) "..." else "",
                "${numberFormat.format(merchant.amount)}원",
                "${merchant.count}건",
                "${String.format("%.1f", merchant.percentage)}%"
            )
            
            merchantData.forEach { cellData ->
                val textView = createDataCell(context, cellData)
                merchantRow.addView(textView)
            }
            tableLayout.addView(merchantRow)
        }
    }
    
    /**
     * 통합 리포트 테이블 생성
     */
    private fun createComprehensiveReportTable(
        context: Context,
        tableLayout: TableLayout, 
        transactions: List<CardTransaction>,
        summary: MonthlyCardSummary,
        forecast: PaymentForecast
    ) {
        // 결제 예상액 섹션
        val forecastHeaderRow = TableRow(context)
        val forecastHeaderText = createHeaderCell(context, "결제 예상액 (할부 반영)")
        forecastHeaderText.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 6f)
        forecastHeaderRow.addView(forecastHeaderText)
        tableLayout.addView(forecastHeaderRow)
        
        val forecastDataRows = arrayOf(
            arrayOf("승인 금액 합계", "+${numberFormat.format(forecast.approvedAmount)}원"),
            arrayOf("취소 금액 합계", "-${numberFormat.format(forecast.cancelledAmount)}원"),
            arrayOf("실제 청구될 금액", "${numberFormat.format(forecast.actualBillingAmount)}원"),
            arrayOf("예상 월 총액", "${numberFormat.format(forecast.estimatedMonthlyTotal)}원"),
            arrayOf("신뢰도", "${String.format("%.1f", forecast.confidence * 100)}%")
        )
        
        forecastDataRows.forEach { rowData ->
            val dataRow = TableRow(context)
            rowData.forEach { cellData ->
                val textView = createDataCell(context, cellData)
                dataRow.addView(textView)
            }
            tableLayout.addView(dataRow)
        }
        
        // 거래내역 미리보기 헤더
        val transactionHeaderRow = TableRow(context)
        val transactionHeaderText = createHeaderCell(context, "거래내역 미리보기 (최대 5건)")
        transactionHeaderText.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 6f)
        transactionHeaderRow.addView(transactionHeaderText)
        tableLayout.addView(transactionHeaderRow)
        
        // 거래내역 데이터 (최대 5건)
        val previewTransactions = transactions.distinctBy { 
            "${it.cardNumber}_${it.amount}_${it.merchant}_${it.transactionDate}" 
        }.sortedByDescending { it.transactionDate }.take(5)
        
        previewTransactions.forEach { transaction ->
            val dataRow = TableRow(context)
            
            val amountDisplay = if (transaction.transactionType == "승인") {
                "+${numberFormat.format(transaction.amount)}"
            } else {
                "-${numberFormat.format(transaction.amount)}"
            }
            
            val data = arrayOf(
                dateFormat.format(Date.from(transaction.transactionDate.atZone(java.time.ZoneId.systemDefault()).toInstant())),
                amountDisplay,
                transaction.merchant.take(12) + if (transaction.merchant.length > 12) "..." else "",
                transaction.transactionType
            )
            
            data.forEach { cellData ->
                val textView = createDataCell(context, cellData)
                dataRow.addView(textView)
            }
            tableLayout.addView(dataRow)
        }
    }
    
    /**
     * 헤더 셀 생성
     */
    private fun createHeaderCell(context: Context?, text: String): TextView {
        val safeContext = context ?: throw IllegalArgumentException("Context cannot be null")
        return TextView(safeContext).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(ContextCompat.getColor(safeContext, R.color.primary_color))
            setPadding(8, 8, 8, 8)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            textSize = 12f
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }
    
    /**
     * 데이터 셀 생성
     */
    private fun createDataCell(context: Context?, text: String): TextView {
        val safeContext = context ?: throw IllegalArgumentException("Context cannot be null")
        return TextView(safeContext).apply {
            this.text = text
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER
            textSize = 11f
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }
}
