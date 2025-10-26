package com.ssj.statuswindow.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.BankTransactionEntity
import com.ssj.statuswindow.util.ExcelExportManager
import com.ssj.statuswindow.util.NavigationManager
import com.ssj.statuswindow.ui.components.AppToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 입출금내역 테이블 화면 (BankTransactionActivity와 구분)
 */
class BankTransactionTableActivity : AppCompatActivity() {

    // UI 컴포넌트
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appToolbar: AppToolbar
    private lateinit var navigationView: NavigationView
    private lateinit var spinnerPeriod: Spinner
    private lateinit var tvTotalCount: TextView
    private lateinit var btnExportExcel: Button
    private lateinit var horizontalScrollView: HorizontalScrollView
    private lateinit var scrollView: ScrollView
    private lateinit var tableLayout: TableLayout
    
    private lateinit var database: StatusWindowDatabase
    private val bankTransactions = mutableListOf<BankTransactionEntity>()
    private lateinit var excelExportManager: ExcelExportManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_transaction_table)
        
        try {
            // 데이터베이스 초기화
            database = StatusWindowDatabase.getDatabase(this)
            
            // 엑셀 내보내기 매니저 초기화
            excelExportManager = ExcelExportManager(this)
            
            setupViews()
            setupToolbar()
            setupNavigation()
            setupPeriodSpinner()
            setupExcelExportButton()
            loadBankTransactions()
            
        } catch (e: Exception) {
            android.util.Log.e("BankTransactionTableActivity", "초기화 오류: ${e.message}", e)
            // 오류 발생 시 기본 텍스트뷰 표시
            val errorView = TextView(this).apply {
                text = "입출금내역 테이블 로딩 오류: ${e.message}"
                setPadding(16, 16, 16, 16)
            }
            setContentView(errorView)
        }
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        appToolbar = findViewById(R.id.appToolbar)
        
        // AppToolbar 설정
        appToolbar.setupWithDrawer(this, drawerLayout)
        appToolbar.setTitle("입출금 테이블")
        navigationView = findViewById(R.id.navigationView)
        spinnerPeriod = findViewById(R.id.spinnerPeriod)
        tvTotalCount = findViewById(R.id.tvTotalCount)
        btnExportExcel = findViewById(R.id.btnExportExcel)
        horizontalScrollView = findViewById(R.id.horizontalScrollView)
        scrollView = findViewById(R.id.scrollView)
        tableLayout = findViewById(R.id.tableLayout)
    }
    
    private fun setupToolbar() {
        // AppToolbar는 이미 setupViews에서 설정됨
        // 기존 toolbar 관련 코드는 제거
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, BankTransactionTableActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, BankTransactionTableActivity::class.java)
    }
    
    private fun setupPeriodSpinner() {
        val periods = arrayOf("이번달", "저번달", "3개월", "전체")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter
        
        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                android.util.Log.d("BankTransactionTableActivity", "조회기간 선택: ${periods[position]}")
                loadBankTransactions()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupExcelExportButton() {
        btnExportExcel.setOnClickListener {
            exportToExcel()
        }
    }
    
    private fun exportToExcel() {
        android.util.Log.d("BankTransactionTableActivity", "엑셀 내보내기 시작")
        
        // 데이터 검증
        if (bankTransactions.isEmpty()) {
            android.util.Log.w("BankTransactionTableActivity", "엑셀 내보낼 데이터가 없습니다")
            android.widget.Toast.makeText(this@BankTransactionTableActivity, "내보낼 데이터가 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // 헤더 정의
        val headers = arrayOf("거래일시", "거래구분", "금액", "잔액", "거래처", "메모")
        
        // 데이터 변환
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        
        val dataRows = bankTransactions.map { transaction ->
            arrayOf(
                transaction.transactionDate.format(formatter),
                transaction.transactionType ?: "",
                transaction.amount.toString(),
                transaction.balance.toString(),
                transaction.description,
                transaction.memo ?: ""
            )
        }
        
        // ExcelExportManager를 사용하여 내보내기
        excelExportManager.exportToExcel(
            fileName = "입출금내역테이블",
            headers = headers,
            dataRows = dataRows,
            onSuccess = { filePath ->
                android.util.Log.d("BankTransactionTableActivity", "엑셀 내보내기 성공: $filePath")
            },
            onError = { error ->
                android.util.Log.e("BankTransactionTableActivity", "엑셀 내보내기 실패: $error")
            }
        )
    }
    
    private fun loadBankTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val transactionsFlow = database.bankTransactionDao().getAllBankTransactions()
                
                transactionsFlow.collect { transactions ->
                    withContext(Dispatchers.Main) {
                        bankTransactions.clear()
                        bankTransactions.addAll(transactions)
                        updateTable()
                        updateTotalCount()
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BankTransactionTableActivity", "입출금내역 로딩 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@BankTransactionTableActivity, "입출금내역 로딩 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateTable() {
        // 기존 테이블 내용 제거
        tableLayout.removeAllViews()
        
        // 헤더 행 생성
        createHeaderRow()
        
        // 데이터 행 생성
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        
        bankTransactions.forEach { transaction ->
            createDataRow(transaction, formatter, numberFormat)
        }
    }
    
    private fun createHeaderRow() {
        val headerRow = TableRow(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@BankTransactionTableActivity, android.R.color.holo_blue_bright))
        }
        
        val headers = arrayOf("거래일시", "거래구분", "금액", "잔액", "거래처", "메모")
        
        headers.forEach { header ->
            val textView = TextView(this).apply {
                text = header
                setTextColor(ContextCompat.getColor(this@BankTransactionTableActivity, android.R.color.white))
                setPadding(16, 12, 16, 12)
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            headerRow.addView(textView)
        }
        
        tableLayout.addView(headerRow)
    }
    
    private fun createDataRow(transaction: BankTransactionEntity, formatter: DateTimeFormatter, numberFormat: NumberFormat) {
        val row = TableRow(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@BankTransactionTableActivity, android.R.color.white))
        }
        
        val data = arrayOf(
            transaction.transactionDate.format(formatter),
            transaction.transactionType ?: "",
            numberFormat.format(transaction.amount),
            numberFormat.format(transaction.balance),
            transaction.description,
            transaction.memo ?: ""
        )
        
        data.forEach { cellData ->
            val textView = TextView(this).apply {
                text = cellData
                setTextColor(ContextCompat.getColor(this@BankTransactionTableActivity, android.R.color.black))
                setPadding(16, 12, 16, 12)
                textSize = 12f
                setBackgroundColor(ContextCompat.getColor(this@BankTransactionTableActivity, android.R.color.white))
            }
            row.addView(textView)
        }
        
        tableLayout.addView(row)
    }
    
    private fun updateTotalCount() {
        tvTotalCount.text = "총 ${bankTransactions.size}건"
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        excelExportManager.onRequestPermissionsResult(requestCode, grantResults)
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
