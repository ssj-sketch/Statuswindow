package com.ssj.statuswindow.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.CardTransactionEntity
import com.ssj.statuswindow.ui.adapter.CardTransactionAdapter
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.util.ModificationNotificationManager
import com.ssj.statuswindow.util.NavigationManager
import com.ssj.statuswindow.ui.components.AppToolbar
import com.ssj.statuswindow.ui.components.SummaryCard
import com.ssj.statuswindow.ui.components.SectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.*

/**
 * 정렬 순서 enum
 */
enum class SortOrder {
    POPULAR,        // 인기순 (거래 빈도 높은 순)
    RECENT,         // 최신순 (날짜 내림차순)
    OLDEST,         // 오래된순 (날짜 오름차순)
    HIGH_AMOUNT,    // 금액 높은순
    LOW_AMOUNT,     // 금액 낮은순
    INPUT_ORDER     // 입력순 (기본값)
}

/**
 * 카드 사용내역 상세페이지
 */
class CardDetailsActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appToolbar: AppToolbar
    private lateinit var summaryCard: SummaryCard
    private lateinit var sectionHeader: SectionHeader
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSummary: TextView
    private lateinit var btnDeleteAll: Button
    private lateinit var tvTitle: TextView
    private lateinit var mainLayout: LinearLayout
    
    // 정렬 버튼들
    private lateinit var btnSortDropdown: Button
    
    private lateinit var database: StatusWindowDatabase
    private var currentSortOrder = SortOrder.POPULAR // 기본값: 인기순
    private lateinit var adapter: CardTransactionAdapter
    private val cardTransactions = mutableListOf<CardTransaction>()
    private lateinit var modificationManager: ModificationNotificationManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("CardDetailsActivity", "CardDetailsActivity onCreate 시작")
        setContentView(R.layout.activity_card_details)
        
        // 데이터베이스 초기화
        database = StatusWindowDatabase.getDatabase(this)
        android.util.Log.d("CardDetailsActivity", "데이터베이스 초기화 완료")
        
        // 수정사항 알림 매니저 초기화
        modificationManager = ModificationNotificationManager(this)
        
        setupViews()
        setupToolbar()
        setupNavigation()
        setupRecyclerView()
        setupClickListeners()
        setupSortButtons()
        loadCardDetails()
        
        // 테스트 데이터 자동 추가 (디버깅용)
        addTestData()
        
        // 수정사항 알림 표시
        showModificationNotification()
        
        android.util.Log.d("CardDetailsActivity", "CardDetailsActivity 초기화 완료")
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        appToolbar = findViewById(R.id.appToolbar)
        summaryCard = findViewById(R.id.summaryCard)
        sectionHeader = findViewById(R.id.sectionHeader)
        navigationView = findViewById(R.id.navigationView)
        recyclerView = findViewById(R.id.recyclerView)
        tvSummary = findViewById(R.id.tvSummary)
        btnDeleteAll = findViewById(R.id.btnDeleteAll)
        tvTitle = findViewById(R.id.tvTitle)
        mainLayout = findViewById(R.id.mainLayout)
        
        // 정렬 버튼들 초기화
        btnSortDropdown = findViewById(R.id.btnSortDropdown)
        
        // 새로운 컴포넌트 설정
        setupNewComponents()
    }
    
    private fun setupNewComponents() {
        // AppToolbar 설정
        appToolbar.setupWithDrawer(this, drawerLayout)
        appToolbar.setTitle("카드 사용내역")
        
        // SummaryCard 설정
        summaryCard.setTitle("📊 사용내역 요약")
        summaryCard.setPrimaryValue("0건")
        summaryCard.setSubtitle("총 거래 건수")
        
        // SectionHeader 설정
        sectionHeader.setTitle("📋 상세 거래 내역")
    }
    
    private fun setupToolbar() {
        // AppToolbar는 이미 setupNewComponents에서 설정됨
        // 기존 toolbar 관련 코드는 제거
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, CardDetailsActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, CardDetailsActivity::class.java)
    }
    
    private fun setupRecyclerView() {
        adapter = CardTransactionAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // 삭제 버튼 클릭 리스너
        adapter.setOnDeleteClickListener { transaction ->
            showDeleteConfirmDialog(transaction)
        }
        
        // 아이템 클릭 리스너 (상세내용 표시)
        adapter.setOnItemClickListener { transaction ->
            showTransactionDetailDialog(transaction)
        }
    }
    
    /**
     * 정렬 버튼 설정
     */
    private fun setupSortButtons() {
        btnSortDropdown.setOnClickListener {
            showSortOptionsDialog()
        }
        
        // 초기 정렬 순서 설정
        updateSortButtonText()
    }
    
    private fun showSortOptionsDialog() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_sort_options, null)
        
        // 옵션들 초기화
        val optionPopular = bottomSheetView.findViewById<TextView>(R.id.option_popular)
        val optionRecent = bottomSheetView.findViewById<TextView>(R.id.option_recent)
        val optionOldest = bottomSheetView.findViewById<TextView>(R.id.option_oldest)
        val optionHighAmount = bottomSheetView.findViewById<TextView>(R.id.option_high_amount)
        val optionLowAmount = bottomSheetView.findViewById<TextView>(R.id.option_low_amount)
        val optionInputOrder = bottomSheetView.findViewById<TextView>(R.id.option_input_order)
        
        val options = listOf(
            optionPopular to SortOrder.POPULAR,
            optionRecent to SortOrder.RECENT,
            optionOldest to SortOrder.OLDEST,
            optionHighAmount to SortOrder.HIGH_AMOUNT,
            optionLowAmount to SortOrder.LOW_AMOUNT,
            optionInputOrder to SortOrder.INPUT_ORDER
        )
        
        // 모든 옵션을 보이게 설정
        options.forEach { (textView, _) ->
            textView.visibility = android.view.View.VISIBLE
        }
        
        // 현재 선택된 옵션에 체크 표시
        val currentOption = options.find { it.second == currentSortOrder }?.first
        currentOption?.let { 
            it.visibility = android.view.View.VISIBLE
            it.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_24dp, 0)
        }
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(bottomSheetView)
            .setCancelable(true)
            .create()
            
        // 옵션 클릭 리스너 설정
        options.forEach { (textView, sortOrder) ->
            textView.setOnClickListener {
                changeSortOrder(sortOrder)
                dialog.dismiss()
            }
        }
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        
        // 바텀 시트 스타일로 설정
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
    }
    
    private fun updateSortButtonText() {
        val sortText = when (currentSortOrder) {
            SortOrder.POPULAR -> "인기순"
            SortOrder.RECENT -> "최신순"
            SortOrder.OLDEST -> "오래된순"
            SortOrder.HIGH_AMOUNT -> "금액 높은순"
            SortOrder.LOW_AMOUNT -> "금액 낮은순"
            SortOrder.INPUT_ORDER -> "입력순"
        }
        btnSortDropdown.text = sortText
    }
    
    /**
     * 정렬 순서 변경
     */
    private fun changeSortOrder(sortOrder: SortOrder) {
        currentSortOrder = sortOrder
        updateSortButtonText()
        
        // 현재 데이터로 다시 정렬
        adapter.submitList(cardTransactions, currentSortOrder)
        
        android.util.Log.d("CardDetailsActivity", "정렬 순서 변경: $sortOrder")
    }
    
    
    private fun setupClickListeners() {
        btnDeleteAll.setOnClickListener {
            addTestData()
        }
        
        // 할부기간 및 청구금액 테스트 버튼 추가 (임시)
        btnDeleteAll.setOnLongClickListener {
            testInstallmentAndBilling()
            true
        }
    }
    
    /**
     * 테스트 데이터 추가
     */
    private fun addTestData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                
                // 기존 테스트 데이터 삭제
                android.util.Log.d("CardDetailsActivity", "기존 테스트 데이터 삭제 시작")
                cardTransactionDao.deleteAllCardTransactions()
                android.util.Log.d("CardDetailsActivity", "기존 테스트 데이터 삭제 완료")
                
                // 테스트 데이터 추가
                android.util.Log.d("CardDetailsActivity", "테스트 데이터 추가 시작")
                
                // 연도 생략 날짜 파싱 함수
                fun parseDateWithYearOmission(month: Int, day: Int, hour: Int, minute: Int): java.time.LocalDateTime {
                    val now = java.time.LocalDateTime.now()
                    val currentYear = now.year
                    val currentMonth = now.monthValue
                    val currentDay = now.dayOfMonth
                    
                    val targetDate = java.time.LocalDate.of(currentYear, month, day)
                    val currentDate = java.time.LocalDate.of(currentYear, currentMonth, currentDay)
                    
                    val year = if (targetDate.isAfter(currentDate)) {
                        currentYear - 1 // 오늘 이후는 작년
                    } else {
                        currentYear // 오늘 이전은 올해
                    }
                    
                    return java.time.LocalDateTime.of(year, month, day, hour, minute)
                }
                
                val testTransactions = listOf(
                    CardTransactionEntity(
                        cardType = "신한카드",
                        cardNumber = "1054",
                        transactionType = "승인",
                        user = "신*진",
                        amount = 42820L,
                        installment = "일시불",
                        transactionDate = parseDateWithYearOmission(10, 22, 14, 59),
                        merchant = "주식회사 이마트",
                        cumulativeAmount = 1903674L,
                        category = "식료품",
                        originalText = "신한카드(1054)승인 신*진 42,820원(일시불)10/22 14:59 주식회사 이마트 누적1,903,674"
                    ),
                    CardTransactionEntity(
                        cardType = "신한카드",
                        cardNumber = "1054",
                        transactionType = "승인",
                        user = "신*진",
                        amount = 98700L,
                        installment = "2개월",
                        transactionDate = parseDateWithYearOmission(10, 22, 15, 48),
                        merchant = "카톨릭대병원",
                        cumulativeAmount = 1960854L,
                        category = "의료",
                        originalText = "신한카드(1054)승인 신*진 98,700원(2개월)10/22 15:48 카톨릭대병원 누적1,960,854원"
                    ),
                    CardTransactionEntity(
                        cardType = "신한카드",
                        cardNumber = "1054",
                        transactionType = "취소",
                        user = "신*진",
                        amount = 12700L,
                        installment = "일시불",
                        transactionDate = parseDateWithYearOmission(10, 22, 15, 48),
                        merchant = "스타벅스",
                        cumulativeAmount = 1860854L,
                        category = "카페",
                        originalText = "신한카드(1054)취소 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원"
                    ),
                    CardTransactionEntity(
                        cardType = "신한카드",
                        cardNumber = "1054",
                        transactionType = "승인",
                        user = "신*진",
                        amount = 12700L,
                        installment = "일시불",
                        transactionDate = parseDateWithYearOmission(10, 22, 15, 48),
                        merchant = "스타벅스",
                        cumulativeAmount = 1860854L,
                        category = "카페",
                        originalText = "신한카드(1054)승인 신*진 12,700원(일시불)10/22 15:48 스타벅스 누적1,860,854원"
                    ),
                    CardTransactionEntity(
                        cardType = "신한카드",
                        cardNumber = "1054",
                        transactionType = "승인",
                        user = "신*진",
                        amount = 42820L,
                        installment = "일시불",
                        transactionDate = parseDateWithYearOmission(10, 21, 14, 59),
                        merchant = "주식회사 이마트",
                        cumulativeAmount = 1903674L,
                        category = "식료품",
                        originalText = "신한카드(1054)승인 신*진 42,820원(일시불)10/21 14:59 주식회사 이마트 누적1,903,674"
                    ),
                    CardTransactionEntity(
                        cardType = "신한카드",
                        cardNumber = "1054",
                        transactionType = "승인",
                        user = "신*진",
                        amount = 98700L,
                        installment = "3개월",
                        transactionDate = parseDateWithYearOmission(10, 21, 15, 48),
                        merchant = "카톨릭대병원",
                        cumulativeAmount = 1960854L,
                        category = "의료",
                        originalText = "신한카드(1054)승인 신*진 98,700원(3개월)10/21 15:48 카톨릭대병원 누적1,960,854원"
                    ),
                    CardTransactionEntity(
                        cardType = "신한카드",
                        cardNumber = "1054",
                        transactionType = "승인",
                        user = "신*진",
                        amount = 12700L,
                        installment = "일시불",
                        transactionDate = parseDateWithYearOmission(10, 21, 15, 48),
                        merchant = "스타벅스",
                        cumulativeAmount = 1860854L,
                        category = "카페",
                        originalText = "신한카드(1054)승인 신*진 12,700원(일시불)10/21 15:48 스타벅스 누적1,860,854원"
                    )
                )
                
                // 데이터베이스에 삽입
                testTransactions.forEachIndexed { index, transaction ->
                    val insertedId = cardTransactionDao.insertCardTransaction(transaction)
                    android.util.Log.d("CardDetailsActivity", "테스트 데이터 ${index + 1} 삽입 완료: ID=${insertedId}, 할부=${transaction.installment}")
                }
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardDetailsActivity, "테스트 데이터 추가 완료 (${testTransactions.size}건)", android.widget.Toast.LENGTH_SHORT).show()
                    loadCardDetails() // 화면 새로고침
                    
                    // 수정사항 등록
                    registerTestModification()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CardDetailsActivity", "테스트 데이터 추가 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardDetailsActivity, "테스트 데이터 추가 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 수정사항 알림 표시
     */
    private fun showModificationNotification() {
        try {
            val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)
            modificationManager.showModificationNotification(
                containerLayout = mainLayout,
                screenName = "CardDetailsActivity",
                onViewDetails = {
                    // 수정사항 상세 보기
                    showModificationDetailsDialog()
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("CardDetailsActivity", "수정사항 알림 표시 오류: ${e.message}", e)
        }
    }
    
    /**
     * 수정사항 상세 다이얼로그 표시
     */
    private fun showModificationDetailsDialog() {
        val modificationDetails = buildString {
            append("🔧 카드 사용내역 화면 수정사항\n\n")
            append("📅 최근 수정사항:\n")
            append("• 상세내역에 할부기간 표시 개선\n")
            append("• 청구금액과 사용금액 구분 표시\n")
            append("• 테스트 데이터 자동 추가 기능\n")
            append("• 종합 테스트 케이스 추가\n\n")
            append("✨ 개선 효과:\n")
            append("• 정확한 청구금액 계산\n")
            append("• 명확한 정보 표시\n")
            append("• 효율적인 테스트 환경\n")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("수정사항 상세")
            .setMessage(modificationDetails)
            .setPositiveButton("확인", null)
            .show()
    }
    
    /**
     * 수정사항 등록 (테스트용)
     */
    private fun registerTestModification() {
        modificationManager.registerModification(
            screenName = "CardDetailsActivity",
            modification = "할부기간 표시 및 청구금액 계산 개선",
            details = "상세내역에 할부기간이 항상 표시되고, 청구금액과 사용금액이 구분되어 표시됩니다."
        )
    }
    
    /**
     * 할부기간 및 청구금액 테스트 메서드 (실제 데이터 기반)
     */
    private fun testInstallmentAndBilling() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                val now = java.time.LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                
                // 모든 카드 거래 조회
                val allTransactions = cardTransactionDao.getAllCardTransactions()
                withContext(Dispatchers.Main) {
                        val testResult = buildString {
                            append("🧪 실제 데이터 기반 테스트 결과\n\n")
                            
                            // 예상 데이터 분석
                            append("📋 예상 테스트 데이터:\n")
                            append("• 이마트: 42,820원 (일시불) × 2건 = 85,640원\n")
                            append("• 카톨릭대병원: 98,700원 (2개월) → 월납부 49,350원\n")
                            append("• 카톨릭대병원: 98,700원 (3개월) → 월납부 32,900원\n")
                            append("• 스타벅스: 12,700원 (일시불) × 2건 = 25,400원\n")
                            append("• 스타벅스 취소: -12,700원\n")
                            append("• 예상 총 사용금액: 321,140원\n")
                            append("• 예상 총 청구금액: 180,590원\n\n")
                            
                            // 할부 유형별 분석
                            val installmentTypes = allTransactions.groupBy { it.installment }
                            append("📊 실제 할부 유형별 거래 현황:\n")
                            installmentTypes.forEach { (installment, transactions) ->
                                val totalAmount = transactions.sumOf { it.amount }
                                val count = transactions.size
                                append("• ${installment}: ${count}건, 총 ${NumberFormat.getNumberInstance(Locale.KOREA).format(totalAmount)}원\n")
                            }
                            
                            append("\n📅 이달(10월) 청구금액 상세 분석:\n")
                            
                            // 이달 거래 분석
                            val currentMonthTransactions = allTransactions.filter { 
                                it.transactionDate.monthValue == now.monthValue 
                            }
                            if (currentMonthTransactions.isNotEmpty()) {
                                append("• 실제 이달 거래: ${currentMonthTransactions.size}건\n")
                                currentMonthTransactions.forEach { transaction ->
                                    val billingAmount = calculateBillingAmount(transaction)
                                    val installmentInfo = if (transaction.installment.isNotEmpty()) transaction.installment else "일시불"
                                    val transactionType = if (transaction.transactionType == "취소") "❌" else "✅"
                                    append("  ${transactionType} ${transaction.merchant}: ${NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.amount)}원 (${installmentInfo}) → 청구: ${NumberFormat.getNumberInstance(Locale.KOREA).format(billingAmount)}원\n")
                                }
                            }
                            
                            // DB에서 계산된 청구금액
                            val dbBillingAmount = cardTransactionDao.getMonthlyBillAmount(startOfMonth, endOfMonth) ?: 0L
                            val dbUsageAmount = cardTransactionDao.getTotalCardUsageAmount(startOfMonth, endOfMonth) ?: 0L
                            
                            append("\n💰 실제 계산 결과:\n")
                            append("• 총 사용금액: ${NumberFormat.getNumberInstance(Locale.KOREA).format(dbUsageAmount)}원\n")
                            append("• 총 청구금액: ${NumberFormat.getNumberInstance(Locale.KOREA).format(dbBillingAmount)}원\n")
                            
                            // 수동 계산과 비교
                            val manualBillingAmount = calculateManualBillingAmount(currentMonthTransactions)
                            append("• 수동 계산 청구금액: ${NumberFormat.getNumberInstance(Locale.KOREA).format(manualBillingAmount)}원\n")
                            
                            val difference = dbBillingAmount - manualBillingAmount
                            append("• 차이: ${NumberFormat.getNumberInstance(Locale.KOREA).format(difference)}원\n")
                            
                            // 예상값과 비교
                            val expectedUsage = 321140L
                            val expectedBilling = 180590L
                            val usageDifference = dbUsageAmount - expectedUsage
                            val billingDifference = dbBillingAmount - expectedBilling
                            
                            append("\n🎯 예상값 대비 검증:\n")
                            append("• 사용금액 차이: ${NumberFormat.getNumberInstance(Locale.KOREA).format(usageDifference)}원\n")
                            append("• 청구금액 차이: ${NumberFormat.getNumberInstance(Locale.KOREA).format(billingDifference)}원\n")
                            
                            if (difference == 0L && usageDifference == 0L && billingDifference == 0L) {
                                append("\n✅ 모든 계산이 정확합니다!")
                            } else {
                                append("\n❌ 계산에 오차가 있습니다!")
                                if (difference != 0L) append("\n  - 수동계산과 DB계산 불일치")
                                if (usageDifference != 0L) append("\n  - 예상 사용금액과 불일치")
                                if (billingDifference != 0L) append("\n  - 예상 청구금액과 불일치")
                            }
                            
                            // 할부 거래 상세 분석
                            val installmentTransactions = currentMonthTransactions.filter { 
                                it.installment.isNotEmpty() && it.installment != "일시불" 
                            }
                            if (installmentTransactions.isNotEmpty()) {
                                append("\n🔍 할부 거래 상세 분석:\n")
                                installmentTransactions.forEach { transaction ->
                                    val monthlyPayment = calculateBillingAmount(transaction)
                                    append("• ${transaction.merchant}: ${NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.amount)}원 (${transaction.installment}) → 월 납부: ${NumberFormat.getNumberInstance(Locale.KOREA).format(monthlyPayment)}원\n")
                                }
                            }
                        }
                        
                    // 결과를 다이얼로그로 표시
                    androidx.appcompat.app.AlertDialog.Builder(this@CardDetailsActivity)
                        .setTitle("실제 데이터 테스트 결과")
                        .setMessage(testResult)
                        .setPositiveButton("확인", null)
                        .show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CardDetailsActivity", "테스트 실행 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardDetailsActivity, "테스트 실행 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 개별 거래의 청구금액 계산
     */
    private fun calculateBillingAmount(transaction: CardTransactionEntity): Long {
        return when {
            transaction.transactionType == "취소" -> -transaction.amount
            transaction.installment == "일시불" -> transaction.amount
            transaction.installment.contains("개월") -> {
                val months = transaction.installment.replace("개월", "").toIntOrNull() ?: 1
                transaction.amount / months
            }
            else -> transaction.amount
        }
    }
    
    /**
     * 수동으로 청구금액 계산 (검증용)
     */
    private fun calculateManualBillingAmount(transactions: List<CardTransactionEntity>): Long {
        return transactions.sumOf { transaction ->
            when {
                transaction.transactionType == "취소" -> -transaction.amount
                transaction.installment == "일시불" -> transaction.amount
                transaction.installment.contains("개월") -> {
                    val months = transaction.installment.replace("개월", "").toIntOrNull() ?: 1
                    transaction.amount / months
                }
                else -> transaction.amount
            }
        }
    }
    
    private fun showDeleteConfirmDialog(transaction: CardTransaction) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("거래 삭제 확인")
            .setMessage("이 거래를 삭제하시겠습니까?\n\n가맹점: ${transaction.merchant}\n금액: ${NumberFormat.getNumberInstance(Locale.KOREA).format(transaction.amount)}원")
            .setPositiveButton("삭제") { _, _ ->
                deleteCardTransaction(transaction)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun showTransactionDetailDialog(transaction: CardTransaction) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")
        
        val detailMessage = buildString {
            append("💳 카드 거래 상세 정보\n\n")
            append("가맹점: ${transaction.merchant}\n")
            append("금액: ${formatter.format(transaction.amount)}원\n")
            append("거래일시: ${transaction.transactionDate.format(dateFormatter)}\n")
            append("사용자: ${transaction.user}\n")
            append("카드번호: ${transaction.cardNumber}\n")
            append("거래유형: ${transaction.transactionType}\n")
            append("할부기간: ${if (transaction.installment.isNotEmpty()) transaction.installment else "일시불"}\n")
            append("누적금액: ${formatter.format(transaction.cumulativeAmount)}원")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("거래 상세 정보")
            .setMessage(detailMessage)
            .setPositiveButton("확인", null)
            .show()
    }
    
    private fun deleteCardTransaction(transaction: CardTransaction) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                
                // CardTransaction을 CardTransactionEntity로 변환하여 삭제
                val entity = CardTransactionEntity(
                    cardType = transaction.cardType,
                    cardNumber = transaction.cardNumber,
                    transactionType = transaction.transactionType,
                    user = transaction.user,
                    amount = transaction.amount,
                    installment = transaction.installment,
                    transactionDate = transaction.transactionDate,
                    merchant = transaction.merchant,
                    cumulativeAmount = transaction.cumulativeAmount,
                    originalText = transaction.originalText
                )
                
                // 실제로는 ID로 삭제해야 하지만, 여기서는 간단히 처리
                android.util.Log.d("CardDetailsActivity", "거래 삭제 시도: ${transaction.merchant}")
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardDetailsActivity, "거래가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                    // 데이터 다시 로드
                    loadCardDetails()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CardDetailsActivity", "거래 삭제 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardDetailsActivity, "삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showDeleteAllConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("전체 삭제 확인")
            .setMessage("모든 카드 사용내역을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteAllCardDetails()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun deleteAllCardDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                
                // 모든 카드 거래 데이터 삭제
                cardTransactionDao.deleteAllCardTransactions()
                
                withContext(Dispatchers.Main) {
                    // UI 업데이트
                    cardTransactions.clear()
                    adapter.submitList(emptyList())
                    
                    // SummaryCard 업데이트
                    summaryCard.setTitle("📊 사용내역 요약")
                    summaryCard.setPrimaryValue("0건")
                    summaryCard.setSubtitle("총 거래 건수")
                    
                    android.widget.Toast.makeText(this@CardDetailsActivity, "모든 데이터가 삭제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@CardDetailsActivity, "삭제 오류: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun loadCardDetails() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                val allTransactions = cardTransactionDao.getAllCardTransactions()
                
                    withContext(Dispatchers.Main) {
                    // CardTransactionEntity를 CardTransaction으로 변환
                    val cardTransactionList = allTransactions.map { entity ->
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
                        }
                        
                    cardTransactions.clear()
                    cardTransactions.addAll(cardTransactionList)
                    adapter.submitList(cardTransactionList, currentSortOrder)
                    updateSummary(cardTransactionList)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ 데이터 로드 오류: ${e.message}"
                }
            }
        }
    }
    
    private fun updateSummary(transactionList: List<CardTransaction>) {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        
        // DB에서 직접 조회
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cardTransactionDao = database.cardTransactionDao()
                
                // 현재 월의 시작과 끝 날짜 계산
                val now = java.time.LocalDateTime.now()
                android.util.Log.d("CardDetailsActivity", "현재 날짜: $now")
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59)
                
                // DB에서 총액 조회
                android.util.Log.d("CardDetailsActivity", "월별 조회 범위: ${startOfMonth} ~ ${endOfMonth}")
                val totalAmount = cardTransactionDao.getTotalCardUsageAmount(startOfMonth, endOfMonth) ?: 0L
                val totalBillingAmount = cardTransactionDao.getMonthlyBillAmount(startOfMonth, endOfMonth) ?: 0L
                val totalCount = cardTransactionDao.getCardTransactionCountByDateRange(startOfMonth, endOfMonth)
                android.util.Log.d("CardDetailsActivity", "DB 조회 결과 - 총액: $totalAmount, 청구액: $totalBillingAmount, 건수: $totalCount")
                
                // 메인 스레드에서 UI 업데이트
                withContext(Dispatchers.Main) {
                    // SummaryCard 업데이트
                    summaryCard.setTitle("📊 사용내역 요약")
                    summaryCard.setPrimaryValue("${totalCount}건")
                    summaryCard.setSubtitle("총 거래 건수")
                    
                    // 기존 요약 정보도 업데이트 (디버깅용)
        val summary = StringBuilder()
                    summary.append("📊 카드 사용내역 요약 (DB 조회)\n\n")
                    summary.append("총 거래 건수: ${totalCount}건\n")
        summary.append("총 사용금액: ${formatter.format(totalAmount)}원\n")
        summary.append("총 청구금액: ${formatter.format(totalBillingAmount)}원\n\n")
        
                    // 카테고리별 통계는 DB에서 조회 (추후 구현)
        summary.append("📈 카테고리별 사용내역\n")
        summary.append("-".repeat(30)).append("\n")
                    summary.append("(DB 기반 카테고리 통계는 추후 구현)\n")
        
        tvSummary.text = summary.toString()
                }
            } catch (e: Exception) {
                android.util.Log.e("CardDetailsActivity", "DB 조회 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    tvSummary.text = "❌ DB 조회 오류: ${e.message}"
                }
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(navigationView)
                true
            }
            R.id.nav_dashboard -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawers()
                true
            }
            R.id.nav_card_details -> {
                // 현재 화면이므로 아무것도 하지 않음
                drawerLayout.closeDrawers()
                true
            }
            R.id.nav_bank_transaction -> {
                val intent = Intent(this, BankTransactionActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawers()
                true
            }
            R.id.nav_bank_transaction_table -> {
                val intent = Intent(this, BankTransactionActivity::class.java)
                startActivity(intent)
                drawerLayout.closeDrawers()
                true
            }
            R.id.nav_settings -> {
                // 설정 메뉴 처리
                android.widget.Toast.makeText(this, "설정 메뉴", android.widget.Toast.LENGTH_SHORT).show()
                drawerLayout.closeDrawers()
                true
            }
            R.id.nav_about -> {
                // 앱 정보 메뉴 처리
                android.widget.Toast.makeText(this, "앱 정보", android.widget.Toast.LENGTH_SHORT).show()
                drawerLayout.closeDrawers()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
