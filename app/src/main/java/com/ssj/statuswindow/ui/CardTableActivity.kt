package com.ssj.statuswindow.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.util.ExcelExportManager
import com.ssj.statuswindow.util.NavigationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.io.File
import java.io.FileOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.*
import android.os.Environment
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/**
 * 카드사용내역 테이블 화면 (엑셀 형태)
 */
class CardTableActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    
    // 조회기간 선택
    private lateinit var spinnerPeriod: Spinner
    private lateinit var tvTotalCount: TextView
    private lateinit var btnExportExcel: Button
    
    // 엑셀 형태 그리드뷰
    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var tableLayout: TableLayout
    
    private lateinit var database: StatusWindowDatabase
    private val cardTransactions = mutableListOf<CardTransaction>()
    private lateinit var excelExportManager: ExcelExportManager
    
    // 조회기간 enum
    enum class PeriodType {
        THIS_MONTH,    // 이번달
        LAST_MONTH,    // 저번달
        THREE_MONTHS,  // 3개월
        ALL            // 전체
    }
    
    private var currentPeriod = PeriodType.THIS_MONTH
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
        setContentView(R.layout.activity_card_table)
        
        // 데이터베이스 초기화
        database = StatusWindowDatabase.getDatabase(this)
        
        // 엑셀 내보내기 매니저 초기화
        excelExportManager = ExcelExportManager(this)
        
        setupViews()
        setupToolbar()
        setupNavigation()
            setupPeriodSpinner()
        setupClickListeners()
        loadCardTable()
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("CardTableActivity", "초기화 오류: ${e.message}", e)
            // 폴백: 간단한 TextView
            val textView = android.widget.TextView(this)
            textView.text = "카드 테이블 로딩 오류: ${e.message}"
            setContentView(textView)
        }
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        navigationView = findViewById(R.id.navigationView)
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        btnExportExcel = findViewById(R.id.btnExportExcel)
        horizontalScrollView = findViewById(R.id.horizontalScrollView)
        tableLayout = findViewById(R.id.tableLayout)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        supportActionBar?.title = "카드사용내역 테이블"
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, CardTableActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, CardTableActivity::class.java)
    }
    
    private fun setupPeriodSpinner() {
        val periodOptions = arrayOf("이번달", "저번달", "3개월", "전체")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periodOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter
        
        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                currentPeriod = when (position) {
                    0 -> PeriodType.THIS_MONTH
                    1 -> PeriodType.LAST_MONTH
                    2 -> PeriodType.THREE_MONTHS
                    3 -> PeriodType.ALL
                    else -> PeriodType.THIS_MONTH
                }
                loadCardTable()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupClickListeners() {
        btnExportExcel.setOnClickListener {
            exportToExcel()
        }
    }
    
    private fun loadCardTable() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                val dateRange = getDateRange(currentPeriod)
                
                val transactions = if (dateRange != null) {
                    cardTransactionDao.getCardTransactionsByDateRange(dateRange.first, dateRange.second)
                } else {
                    cardTransactionDao.getAllCardTransactions()
                }
                
                withContext(Dispatchers.Main) {
                    cardTransactions.clear()
                    cardTransactions.addAll(transactions.map { entity ->
                        CardTransaction(
                            cardType = entity.cardType,
                            cardNumber = entity.cardNumber,
                            transactionType = entity.transactionType,
                            user = entity.user,
                            amount = entity.amount,
                            installment = entity.installment,
                            transactionDate = entity.transactionDate,
                            merchant = entity.merchant,
                            cumulativeAmount = entity.cumulativeAmount,
                            category = entity.category,
                            memo = entity.memo,
                            originalText = entity.originalText
                        )
                    })
                    
                    updateTable()
                    updateSummary()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardTableActivity, "데이터 로드 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun getDateRange(period: PeriodType): Pair<LocalDateTime, LocalDateTime>? {
        val now = LocalDateTime.now()
        return when (period) {
            PeriodType.THIS_MONTH -> {
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                Pair(startOfMonth, endOfMonth)
            }
            PeriodType.LAST_MONTH -> {
                val lastMonth = now.minusMonths(1)
                val startOfLastMonth = lastMonth.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfLastMonth = lastMonth.withDayOfMonth(lastMonth.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                Pair(startOfLastMonth, endOfLastMonth)
            }
            PeriodType.THREE_MONTHS -> {
                val startOfThreeMonths = now.minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfCurrentMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                Pair(startOfThreeMonths, endOfCurrentMonth)
            }
            PeriodType.ALL -> null
        }
    }
    
    private fun updateTable() {
        tableLayout.removeAllViews()
        
        // 헤더 행 생성
        createHeaderRow()
        
        // 데이터 행들 생성
        cardTransactions.forEach { transaction ->
            createDataRow(transaction)
        }
    }
    
    private fun createHeaderRow() {
        val headerRow = TableRow(this)
        headerRow.setBackgroundColor(getColor(R.color.primary_color))
        
        val headers = arrayOf("거래일시", "카드종류", "카드번호", "거래구분", "사용자", "금액", "할부", "가맹점", "카테고리", "누적금액")
        
        headers.forEach { headerText ->
            val textView = TextView(this)
            textView.text = headerText
            textView.setTextColor(getColor(android.R.color.white))
            textView.textSize = 12f
            textView.setPadding(8, 8, 8, 8)
            textView.setBackgroundColor(getColor(R.color.primary_color))
            textView.minWidth = 200
            textView.gravity = android.view.Gravity.CENTER
            
            headerRow.addView(textView)
        }
        
        tableLayout.addView(headerRow)
    }
    
    private fun createDataRow(transaction: CardTransaction) {
        val dataRow = TableRow(this)
        dataRow.setBackgroundColor(getColor(android.R.color.white))
        
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        
        val rowData = arrayOf(
            transaction.transactionDate.format(dateFormatter),
            transaction.cardType,
            transaction.cardNumber,
            transaction.transactionType,
            transaction.user,
            "${formatter.format(transaction.amount)}원",
            transaction.installment.ifEmpty { "일시불" },
            transaction.merchant,
            transaction.category ?: "",
            "${formatter.format(transaction.cumulativeAmount)}원"
        )
        
        rowData.forEach { data ->
            val textView = TextView(this)
            textView.text = data
            textView.setTextColor(getColor(android.R.color.black))
            textView.textSize = 11f
            textView.setPadding(8, 8, 8, 8)
            textView.minWidth = 200
            textView.gravity = android.view.Gravity.CENTER
            textView.setBackgroundColor(getColor(android.R.color.white))
            
            dataRow.addView(textView)
        }
        
        tableLayout.addView(dataRow)
    }
    
    private fun updateSummary() {
        val totalCount = cardTransactions.size
        tvTotalCount.text = "총 ${totalCount}건"
    }
    
    private fun exportToExcel() {
        android.util.Log.d("CardTableActivity", "엑셀 내보내기 시작")
        
        // 데이터 검증
        if (cardTransactions.isEmpty()) {
            android.util.Log.w("CardTableActivity", "엑셀 내보낼 데이터가 없습니다")
            android.widget.Toast.makeText(this@CardTableActivity, "내보낼 데이터가 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // 헤더 정의
        val headers = arrayOf("거래일시", "카드사", "거래처", "금액", "할부", "승인/취소", "누적금액")
        
        // 데이터 변환
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        
        val dataRows = cardTransactions.map { transaction ->
            arrayOf(
                transaction.transactionDate.format(formatter),
                transaction.cardType ?: "",
                transaction.merchant ?: "",
                transaction.amount.toString(),
                transaction.installment ?: "일시불",
                if (transaction.transactionType.contains("취소")) "취소" else "승인",
                transaction.cumulativeAmount.toString()
            )
        }
        
        // ExcelExportManager를 사용하여 내보내기
        excelExportManager.exportToExcel(
            fileName = "카드사용내역",
            headers = headers,
            dataRows = dataRows,
            onSuccess = { filePath ->
                android.util.Log.d("CardTableActivity", "엑셀 내보내기 성공: $filePath")
            },
            onError = { error ->
                android.util.Log.e("CardTableActivity", "엑셀 내보내기 실패: $error")
            }
        )
    }
    
    private fun showStoragePermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("저장소 권한 필요")
            .setMessage("엑셀 파일을 다운로드 폴더에 저장하기 위해 저장소 접근 권한이 필요합니다.\n\n" +
                       "• 파일 저장 위치: /Download/\n" +
                       "• 파일 형식: .xlsx (Excel)\n" +
                       "• 파일명: 카드사용내역_날짜시간.xlsx")
            .setPositiveButton("권한 허용") { _, _ ->
                ActivityCompat.requestPermissions(this, 
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                android.widget.Toast.makeText(this, "엑셀 내보내기가 취소되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun performExcelExport() {
        android.util.Log.d("CardTableActivity", "performExcelExport 시작 - 데이터 개수: ${cardTransactions.size}")
        
        // 데이터 검증
        if (cardTransactions.isEmpty()) {
            android.util.Log.w("CardTableActivity", "엑셀 내보낼 데이터가 없습니다")
            android.widget.Toast.makeText(this@CardTableActivity, "내보낼 데이터가 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch(Dispatchers.IO) {
            var workbook: XSSFWorkbook? = null
            try {
                android.util.Log.d("CardTableActivity", "Apache POI 워크북 생성 시작")
                workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("카드사용내역")
                
                // 헤더 스타일 생성
                val headerStyle = workbook.createCellStyle()
                val headerFont = workbook.createFont()
                headerFont.bold = true
                headerFont.fontHeightInPoints = 12
                headerStyle.setFont(headerFont)
                headerStyle.fillForegroundColor = IndexedColors.BLUE.index
                headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
                headerStyle.borderTop = BorderStyle.THIN
                headerStyle.borderBottom = BorderStyle.THIN
                headerStyle.borderLeft = BorderStyle.THIN
                headerStyle.borderRight = BorderStyle.THIN
                headerStyle.alignment = HorizontalAlignment.CENTER
                
                // 데이터 스타일 생성
                val dataStyle = workbook.createCellStyle()
                dataStyle.borderTop = BorderStyle.THIN
                dataStyle.borderBottom = BorderStyle.THIN
                dataStyle.borderLeft = BorderStyle.THIN
                dataStyle.borderRight = BorderStyle.THIN
                dataStyle.alignment = HorizontalAlignment.CENTER
                
                android.util.Log.d("CardTableActivity", "헤더 행 생성 시작")
                // 헤더 행 생성
                val headerRow = sheet.createRow(0)
                val headers = arrayOf("거래일시", "카드종류", "카드번호", "거래구분", "사용자", "금액", "할부", "가맹점", "카테고리", "누적금액")
                
                headers.forEachIndexed { index, headerText ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(headerText)
                    cell.cellStyle = headerStyle
                }
                
                android.util.Log.d("CardTableActivity", "데이터 행 생성 시작 - ${cardTransactions.size}개 거래")
                // 데이터 행들 생성
                val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                
                cardTransactions.forEachIndexed { rowIndex, transaction ->
                    val dataRow = sheet.createRow(rowIndex + 1)
                    
                    val rowData = arrayOf(
                        transaction.transactionDate.format(dateFormatter),
                        transaction.cardType,
                        transaction.cardNumber,
                        transaction.transactionType,
                        transaction.user,
                        "${formatter.format(transaction.amount)}원",
                        transaction.installment.ifEmpty { "일시불" },
                        transaction.merchant,
                        transaction.category ?: "",
                        "${formatter.format(transaction.cumulativeAmount)}원"
                    )
                    
                    rowData.forEachIndexed { cellIndex, data ->
                        val cell = dataRow.createCell(cellIndex)
                        cell.setCellValue(data)
                        cell.cellStyle = dataStyle
                    }
                }
                
                android.util.Log.d("CardTableActivity", "컬럼 너비 설정")
                // 컬럼 너비 설정 (autoSizeColumn 대신 고정 너비 사용)
                headers.forEachIndexed { index, _ ->
                    try {
                        // 각 컬럼에 적절한 고정 너비 설정
                        val columnWidth = when (index) {
                            0 -> 4000  // 거래일시
                            1 -> 2000  // 카드종류
                            2 -> 3000  // 카드번호
                            3 -> 2000  // 거래구분
                            4 -> 2000  // 사용자
                            5 -> 3000  // 금액
                            6 -> 2000  // 할부
                            7 -> 4000  // 가맹점
                            8 -> 2000  // 카테고리
                            9 -> 3000  // 누적금액
                            else -> 3000
                        }
                        sheet.setColumnWidth(index, columnWidth)
                        android.util.Log.d("CardTableActivity", "컬럼 $index 너비 설정: $columnWidth")
            } catch (e: Exception) {
                        android.util.Log.w("CardTableActivity", "컬럼 $index 너비 설정 실패: ${e.message}")
                        sheet.setColumnWidth(index, 3000)
                    }
                }
                
                android.util.Log.d("CardTableActivity", "파일 저장 시작")
                // 파일 저장 (Android 버전별 처리)
                val fileName = "카드사용내역_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.xlsx"
                
                val file = try {
                    // 다운로드 폴더 우선 사용
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    android.util.Log.d("CardTableActivity", "다운로드 폴더 사용: ${downloadsDir.absolutePath}")
                    
                    // 다운로드 폴더가 존재하지 않으면 생성
                    if (!downloadsDir.exists()) {
                        android.util.Log.d("CardTableActivity", "다운로드 폴더 생성 시도")
                        downloadsDir.mkdirs()
                    }
                    
                    File(downloadsDir, fileName)
                } catch (e: Exception) {
                    android.util.Log.e("CardTableActivity", "다운로드 폴더 접근 실패: ${e.message}", e)
                    try {
                        // 폴백 1: 앱별 다운로드 폴더 사용
                        val appDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        android.util.Log.d("CardTableActivity", "앱별 다운로드 폴더 사용: ${appDir?.absolutePath}")
                        File(appDir, fileName)
                    } catch (e2: Exception) {
                        android.util.Log.e("CardTableActivity", "앱별 다운로드 폴더도 실패: ${e2.message}", e2)
                        // 폴백 2: 앱 내부 저장소 사용
                        android.util.Log.d("CardTableActivity", "앱 내부 저장소 사용")
                        File(filesDir, fileName)
                    }
                }
                
                android.util.Log.d("CardTableActivity", "파일 저장 경로: ${file.absolutePath}")
                
                // 파일 저장
                try {
                    FileOutputStream(file).use { outputStream ->
                        workbook?.write(outputStream)
                        android.util.Log.d("CardTableActivity", "파일 쓰기 완료")
                    }
                    
                    workbook?.close()
                    workbook = null
                    android.util.Log.d("CardTableActivity", "워크북 정리 완료")
                } catch (e: Exception) {
                    android.util.Log.e("CardTableActivity", "파일 저장 실패: ${e.message}", e)
                    throw e
                }
                
                android.util.Log.d("CardTableActivity", "엑셀 파일 생성 완료: ${file.absolutePath}")
                
                    withContext(Dispatchers.Main) {
                    val message = if (file.absolutePath.contains("Download")) {
                        "엑셀 파일이 다운로드 폴더에 저장되었습니다.\n파일명: $fileName\n경로: ${file.absolutePath}"
                    } else if (file.absolutePath.contains("files")) {
                        "엑셀 파일이 앱별 저장소에 저장되었습니다.\n파일명: $fileName\n경로: ${file.absolutePath}"
                    } else {
                        "엑셀 파일이 저장되었습니다.\n파일명: $fileName\n경로: ${file.absolutePath}"
                    }
                    android.widget.Toast.makeText(
                        this@CardTableActivity, 
                        message, 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CardTableActivity", "엑셀 파일 생성 오류: ${e.message}", e)
                e.printStackTrace()
                
                // 워크북 정리
                try {
                    workbook?.close()
                } catch (closeException: Exception) {
                    android.util.Log.w("CardTableActivity", "워크북 닫기 실패: ${closeException.message}")
                }
                
                withContext(Dispatchers.Main) {
                    val errorMessage = when {
                        e.message?.contains("autoSizeColumn") == true -> "엑셀 컬럼 크기 조정 오류가 발생했습니다."
                        e.message?.contains("FileOutputStream") == true -> "파일 저장 권한 오류가 발생했습니다."
                        e.message?.contains("XSSFWorkbook") == true -> "엑셀 파일 생성 오류가 발생했습니다."
                        else -> "엑셀 파일 생성 오류: ${e.message}"
                    }
                    
                    android.widget.Toast.makeText(
                        this@CardTableActivity, 
                        errorMessage, 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        excelExportManager.onRequestPermissionsResult(requestCode, grantResults)
    }
    
    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("저장소 권한 거부됨")
            .setMessage("엑셀 파일을 저장하기 위해 저장소 권한이 필요합니다.\n\n" +
                       "설정에서 권한을 허용하시겠습니까?")
            .setPositiveButton("설정으로 이동") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("나중에") { dialog, _ ->
                dialog.dismiss()
                android.widget.Toast.makeText(this, "엑셀 내보내기를 위해 저장소 권한이 필요합니다.", android.widget.Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openAppSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "설정 화면을 열 수 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(navigationView)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}