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
import com.ssj.statuswindow.database.entity.LoanEntity
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
 * 대출관리 테이블 화면
 */
class LoanTableActivity : AppCompatActivity() {

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
    private val loans = mutableListOf<LoanEntity>()
    private lateinit var excelExportManager: ExcelExportManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loan_table)
        
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
            loadLoans()
            
        } catch (e: Exception) {
            android.util.Log.e("LoanTableActivity", "초기화 오류: ${e.message}", e)
            // 오류 발생 시 기본 텍스트뷰 표시
            val errorView = TextView(this).apply {
                text = "대출관리 테이블 로딩 오류: ${e.message}"
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
        appToolbar.setTitle("대출관리")
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
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, LoanTableActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, LoanTableActivity::class.java)
    }
    
    private fun setupPeriodSpinner() {
        val periods = arrayOf("이번달", "저번달", "3개월", "전체")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriod.adapter = adapter
        
        spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                android.util.Log.d("LoanTableActivity", "조회기간 선택: ${periods[position]}")
                loadLoans()
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
        android.util.Log.d("LoanTableActivity", "엑셀 내보내기 시작")
        
        // 데이터 검증
        if (loans.isEmpty()) {
            android.util.Log.w("LoanTableActivity", "엑셀 내보낼 데이터가 없습니다")
            android.widget.Toast.makeText(this@LoanTableActivity, "내보낼 데이터가 없습니다.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // 헤더 정의
        val headers = arrayOf("은행명", "대출명", "대출유형", "계좌번호", "대출잔액", "이자율(%)", "상태", "다음상환일", "월이자납입금액")
        
        // 데이터 변환
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        
        val dataRows = loans.map { loan ->
            arrayOf(
                loan.bankName,
                loan.loanName,
                loan.loanType,
                loan.accountNumber,
                numberFormat.format(loan.remainingPrincipal),
                String.format("%.2f", loan.interestRate),
                loan.status,
                loan.nextPaymentDate?.format(formatter) ?: "",
                numberFormat.format(loan.monthlyInterestPayment)
            )
        }
        
        // ExcelExportManager를 사용하여 내보내기
        excelExportManager.exportToExcel(
            fileName = "대출관리",
            headers = headers,
            dataRows = dataRows,
            onSuccess = { filePath ->
                android.util.Log.d("LoanTableActivity", "엑셀 내보내기 성공: $filePath")
            },
            onError = { error ->
                android.util.Log.e("LoanTableActivity", "엑셀 내보내기 실패: $error")
            }
        )
    }
    
    private fun loadLoans() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loansFlow = database.loanDao().getAllLoans()
                
                loansFlow.collect { loansList ->
                    withContext(Dispatchers.Main) {
                        loans.clear()
                        loans.addAll(loansList)
                        updateTable()
                        updateTotalCount()
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("LoanTableActivity", "대출 로딩 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@LoanTableActivity, "대출 로딩 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        
        loans.forEach { loan ->
            createDataRow(loan, formatter, numberFormat)
        }
    }
    
    private fun createHeaderRow() {
        val headerRow = TableRow(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@LoanTableActivity, android.R.color.holo_blue_bright))
        }
        
        val headers = arrayOf("은행명", "대출명", "대출유형", "계좌번호", "대출잔액", "이자율(%)", "상태", "다음상환일", "월이자납입금액")
        
        headers.forEach { header ->
            val textView = TextView(this).apply {
                text = header
                setTextColor(ContextCompat.getColor(this@LoanTableActivity, android.R.color.white))
                setPadding(16, 12, 16, 12)
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            headerRow.addView(textView)
        }
        
        tableLayout.addView(headerRow)
    }
    
    private fun createDataRow(loan: LoanEntity, formatter: DateTimeFormatter, numberFormat: NumberFormat) {
        val row = TableRow(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@LoanTableActivity, android.R.color.white))
        }
        
        val data = arrayOf(
            loan.bankName,
            loan.loanName,
            loan.loanType,
            loan.accountNumber,
            numberFormat.format(loan.remainingPrincipal),
            String.format("%.2f", loan.interestRate),
            loan.status,
            loan.nextPaymentDate?.format(formatter) ?: "",
            numberFormat.format(loan.monthlyInterestPayment)
        )
        
        data.forEach { cellData ->
            val textView = TextView(this).apply {
                text = cellData
                setTextColor(ContextCompat.getColor(this@LoanTableActivity, android.R.color.black))
                setPadding(16, 12, 16, 12)
                textSize = 12f
                setBackgroundColor(ContextCompat.getColor(this@LoanTableActivity, android.R.color.white))
            }
            row.addView(textView)
        }
        
        tableLayout.addView(row)
    }
    
    private fun updateTotalCount() {
        tvTotalCount.text = "총 ${loans.size}건"
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
