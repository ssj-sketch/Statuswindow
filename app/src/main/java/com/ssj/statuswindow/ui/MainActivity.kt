package com.ssj.statuswindow.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivityMainBinding
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.util.SmsParser as CardSmsParser
import com.ssj.statuswindow.ui.CardEventActivity
import com.ssj.statuswindow.repo.CardEventRepository
import com.ssj.statuswindow.repo.IncomeRepository
import com.ssj.statuswindow.model.IncomeInfo
import com.ssj.statuswindow.repo.NotificationLogRepository
import com.ssj.statuswindow.util.NotificationExportPreferences
import com.ssj.statuswindow.util.NotificationSheetsExporter
import com.ssj.statuswindow.util.SettingsPreferences
import com.ssj.statuswindow.util.SheetsShareConfig
import com.ssj.statuswindow.util.ExcelPreviewDialog
import com.ssj.statuswindow.util.NotificationHistoryPermissionManager
import com.ssj.statuswindow.viewmodel.MainViewModel
import com.ssj.statuswindow.viewmodel.MainViewModelFactory
import com.ssj.statuswindow.service.MonthlySummaryService
import com.ssj.statuswindow.service.ExcelExportService
import com.ssj.statuswindow.model.MonthlyCardSummary
import com.ssj.statuswindow.service.RetirementCalculationService
import com.ssj.statuswindow.util.FinancialNotificationAnalyzer
import com.ssj.statuswindow.model.RetirementPlan
import com.ssj.statuswindow.model.SalaryInfo
import com.ssj.statuswindow.model.AutoTransferInfo
import com.ssj.statuswindow.model.DynamicRetirementAsset
import com.ssj.statuswindow.repo.AssetRepository
import com.ssj.statuswindow.model.RetirementSettings
import com.ssj.statuswindow.model.RetirementAssetEstimate
import com.ssj.statuswindow.model.RefinedPensionCalculationResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale
import android.text.TextWatcher
import android.text.Editable

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val vm: MainViewModel by viewModels {
        MainViewModelFactory(CardEventRepository.instance(this))
    }
    private val notificationRepo by lazy { NotificationLogRepository.instance(this) }
    private val exportPrefs by lazy { NotificationExportPreferences(this) }
    private val sheetsExporter by lazy { NotificationSheetsExporter(notificationRepo) }
    private val monthlySummaryService by lazy { MonthlySummaryService() }
    private val excelExportService by lazy { ExcelExportService(this) }
    private val retirementService = RetirementCalculationService
    private val incomeRepo by lazy { IncomeRepository(this) }
    private val assetRepo = AssetRepository.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
    
    // 은퇴설정 (기본값)
    private var retirementSettings = RetirementSettings(
        birthDate = LocalDate.of(1989, 1, 1),
        pensionSubscriptionMonths = 420, // 30세부터 현재까지 가정
        desiredRetirementAge = 60
    )
    
    // 카드 거래 내역 저장소
    private val cardTransactions = mutableListOf<CardTransaction>()
    
    // 설정 관리
    private lateinit var settingsPreferences: SettingsPreferences
    
    // 은퇴 계획 관련 변수
    private var currentRetirementPlan: RetirementPlan? = null
    private var detectedSalary: SalaryInfo? = null
    private var autoTransfers = mutableListOf<AutoTransferInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // CrashLogger.saveLog(this, "INFO", "MainActivity", "onCreate started")
            
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // CrashLogger.saveLog(this, "INFO", "MainActivity", "UI binding completed")
        } catch (e: Exception) {
            // CrashLogger.saveCrashLog(this, e, "MainActivity.onCreate() - Initial setup failed")
            throw e
        }

        // Toolbar + drawer
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_life_rpg_hud)

        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.nav_drawer_open,
            R.string.nav_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        binding.navigationView.setCheckedItem(R.id.nav_card_events)
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_card_events -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, CardEventActivity::class.java))
                    true
                }
                R.id.nav_income_detail -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, IncomeDetailActivity::class.java))
                    true
                }
                R.id.nav_asset_management -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, AssetManagementActivity::class.java))
                    true
                }
                R.id.nav_notification_log -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    openNotificationLog()
                    true
                }
                R.id.nav_export_sheets -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    showExportDialog()
                    true
                }
                else -> false
            }
        }

        // RecyclerView 설정
        try {
            setupRecyclerViews()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "setupRecyclerViews 오류", e)
            // 기본값으로 설정
            binding.tvMonthlyTotal.text = "0원"
            binding.tvMonthlyCount.text = "0건"
            binding.tvMonthlyIncome.text = "0원"
        }

        // 설정 초기화
        initializeSettings()
        
        // SMS 파서 초기화 (국가별 AI 엔진 설정)
        try {
            CardSmsParser.initialize(this)
        } catch (e: Exception) {
            // SMS 파서 초기화 실패 시 기본값 사용
            android.util.Log.e("MainActivity", "SMS Parser initialization failed", e)
        }

        // FAB: 붙여넣기 입력 → 파싱 → 저장
        binding.fabAdd.setOnClickListener { showPasteDialog() }

        // 알림 접근 상태 표시 & 설정 화면 이동
        binding.btnOpenNotificationAccess.setOnClickListener { openNotificationAccessSettings() }
        
        // 설정 버튼 클릭
        binding.btnSettings.setOnClickListener { openSettings() }
        updateNotificationAccessIndicator()
        
        // 수입 상세보기 링크
        binding.tvIncomeDetailLink.setOnClickListener {
            startActivity(Intent(this, IncomeDetailActivity::class.java))
        }
        
                // 상세보기 버튼 클릭 이벤트
                binding.btnViewDetails.setOnClickListener {
                    startActivity(Intent(this, CardEventActivity::class.java))
                }

                // 엑셀 내보내기 버튼 클릭 이벤트
                binding.btnExportExcel.setOnClickListener {
                    showExportExcelDialog()
                }

                // 은퇴설정 버튼 클릭 이벤트
                binding.btnRetirementSettings.setOnClickListener {
                    showRetirementSettingsDialog()
                }

        // HUD 초기화 및 업데이트 (임시 비활성화)
        // initializeHUD()
        
        // 수집 목록 구독 (기존 기능 유지)
        scope.launch {
            vm.events.collect { list ->
                val total = list.sumOf { it.amount }
                // 기존 총액 표시는 숨기고 HUD에 통합
            }
        }
        
        // 은퇴설정 UI 초기화
        updateRetirementSettingsUI()
        
        // 은퇴자산 추정 초기화
        updateRetirementAssetEstimate()
    }

    private fun setupRecyclerViews() {
        updateMonthlyTotal()
        updateMonthlyIncome()
    }
    
    private fun updateMonthlyTotal() {
        val currentMonth = java.time.LocalDate.now().monthValue
        val currentYear = java.time.LocalDate.now().year

        val monthlyTransactions = cardTransactions.filter { transaction ->
            transaction.transactionDate.monthValue == currentMonth &&
            transaction.transactionDate.year == currentYear
        }

        val totalAmount = monthlyTransactions.sumOf { it.amount }
        val transactionCount = monthlyTransactions.size

        binding.tvMonthlyTotal.text = "${String.format("%,d", totalAmount)}원"
        binding.tvMonthlyCount.text = "${transactionCount}건"
        
        // 결제 예상액 계산 및 표시
        updatePaymentForecast(monthlyTransactions)
        
        // 소비가 변경되면 은퇴자산도 재계산
        updateRetirementPlan()
    }
    
    private fun updateMonthlyIncome() {
        val monthlyIncome = incomeRepo.getCurrentMonthIncome()
        val currentText = binding.tvMonthlyIncome.text.toString()
        val newText = "${String.format("%,d", monthlyIncome)}원"
        
        // 애니메이션으로 금액 변경
        if (currentText != newText) {
            animateIncomeChange(currentText, newText)
        } else {
            binding.tvMonthlyIncome.text = newText
        }
        
        // 수입이 변경되면 은퇴자산도 재계산
        updateRetirementPlan()
    }
    
    private fun animateIncomeChange(oldText: String, newText: String) {
        // 숫자 추출
        val oldAmount = oldText.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
        val newAmount = newText.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
        
        if (oldAmount != newAmount) {
            val animator = android.animation.ValueAnimator.ofInt(oldAmount.toInt(), newAmount.toInt())
            animator.duration = 1000 // 1초 애니메이션
            animator.addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                binding.tvMonthlyIncome.text = "${String.format("%,d", animatedValue)}원"
            }
            
            // 애니메이션 시작 시 색상 변경
            binding.tvMonthlyIncome.setTextColor(getColor(R.color.income_color))
            animator.start()
            
            // 애니메이션 완료 후 원래 색상으로 복원
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.tvMonthlyIncome.setTextColor(getColor(R.color.income_color))
                }
            })
        } else {
            binding.tvMonthlyIncome.text = newText
        }
    }
    
    /**
     * 결제 예상액 업데이트
     */
    private fun updatePaymentForecast(monthlyTransactions: List<CardTransaction>) {
        val forecast = monthlySummaryService.calculatePaymentForecast(monthlyTransactions)
        
        binding.tvEstimatedTotal.text = "${String.format("%,d", forecast.actualBillingAmount)}원"
        
        val forecastInfo = "승인: +${String.format("%,d", forecast.approvedAmount)}원 | " +
                         "취소: -${String.format("%,d", forecast.cancelledAmount)}원 | " +
                         "실제청구: ${String.format("%,d", forecast.actualBillingAmount)}원"
        binding.tvForecastInfo.text = forecastInfo
    }
    
    /**
     * 은퇴 계획 계산 및 업데이트
     */
    private fun updateRetirementPlan() {
        try {
            // 기본값으로 계산 (설정에서 가져올 예정)
            val currentAge = 35
            val retirementAge = 60
            val currentSalary = detectedSalary?.amount ?: 3000000L // 기본 급여 300만원
            val monthlyIncome = incomeRepo.getCurrentMonthIncome()
            val monthlyExpense = getCurrentMonthlyExpense()
            
            // 동적 은퇴자산 계산
            val dynamicAsset = retirementService.calculateDynamicRetirementAsset(
                currentAge = currentAge,
                retirementAge = retirementAge,
                currentSalary = currentSalary,
                monthlyIncome = monthlyIncome,
                monthlySpending = monthlyExpense,
                currentAsset = 0L // 현재 자산은 0으로 가정
            )
            
            // 기존 RetirementPlan도 업데이트
            currentRetirementPlan = RetirementPlan(
                currentAge = currentAge,
                retirementAge = retirementAge,
                currentSalary = currentSalary,
                monthlyExpense = monthlyExpense,
                targetRetirementAsset = dynamicAsset.projectedAsset,
                monthlyRetirementExpense = dynamicAsset.monthlyRetirementExpense,
                pensionAmount = dynamicAsset.currentPensionValue
            )
            
            updateRetirementPlanUI(dynamicAsset)
            updateAssetImpactAnimation()
        } catch (e: Exception) {
            // 초기화 실패 시 기본값으로 설정
            e.printStackTrace()
        }
    }
    
    /**
     * 은퇴 계획 UI 업데이트 (동적 데이터)
     */
    private fun updateRetirementPlanUI(dynamicAsset: DynamicRetirementAsset? = null) {
        try {
            val plan = currentRetirementPlan ?: return
            
            // 동적 데이터가 있으면 사용, 없으면 기존 데이터 사용
            val assetAmount = dynamicAsset?.projectedAsset ?: plan.targetRetirementAsset
            val monthlyExpense = dynamicAsset?.monthlyRetirementExpense ?: plan.monthlyRetirementExpense
            val pensionAmount = dynamicAsset?.currentPensionValue ?: plan.pensionAmount
            
            // 큰 글씨로 은퇴자산 표시
            binding.tvTargetRetirementAsset.text = "${nf.format(assetAmount)}원"
            binding.tvMonthlyRetirementExpense.text = "${nf.format(monthlyExpense)}원"
            binding.tvPensionAmount.text = "${nf.format(pensionAmount)}원"
            
            // 애니메이션 효과 추가
            animateRetirementAssetChange(assetAmount)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 은퇴자산 변경 애니메이션
     */
    private fun animateRetirementAssetChange(newAmount: Long) {
        val assetText = binding.tvTargetRetirementAsset
        val progressBar = binding.progressAssetImpact
        
        // 텍스트 색상 애니메이션
        assetText.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                assetText.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()
        
        // 프로그레스 바 애니메이션
        val maxAsset = 1000000000L // 10억
        val progress = ((newAmount.toFloat() / maxAsset) * 100).toInt().coerceIn(0, 100)
        
        ValueAnimator.ofInt(progressBar.progress, progress).apply {
            duration = 1000
            addUpdateListener { animator ->
                progressBar.progress = animator.animatedValue as Int
            }
            start()
        }
    }
    private fun getCurrentMonthlyExpense(): Long {
        return try {
            val transactions = vm.getAllTransactions()
            val currentMonth = java.time.LocalDate.now()
            
            val monthlyTransactions = transactions.filter { transaction ->
                transaction.transactionDate.year == currentMonth.year &&
                transaction.transactionDate.monthValue == currentMonth.monthValue
            }
            
            monthlyTransactions.sumOf { it.amount }
        } catch (e: Exception) {
            // 오류 발생 시 기본값 반환
            e.printStackTrace()
            0L
        }
    }
    
    /**
     * 자산 영향 애니메이션 업데이트
     */
    private fun updateAssetImpactAnimation() {
        try {
            val plan = currentRetirementPlan ?: return
            val currentSpending = getCurrentMonthlyExpense()
            
            // 현재 소비가 미래 자산에 미치는 영향 계산
            val assetImpact = RetirementCalculationService.calculateSpendingImpact(
                additionalSpending = currentSpending,
                currentAge = plan.currentAge,
                retirementAge = plan.retirementAge
            )
            
            // UI 업데이트
            binding.tvAssetImpact.text = "이번 달 소비: ${nf.format(currentSpending)}원 → 미래 자산 감소: ${nf.format(assetImpact)}원"
            
            // 프로그레스 바 업데이트 (최대 1000만원 기준)
            val maxImpact = 10000000L
            val progress = ((assetImpact.toFloat() / maxImpact) * 100).toInt().coerceIn(0, 100)
            binding.progressAssetImpact.progress = progress
            
            // 드라마틱한 애니메이션 효과
            animateAssetImpact(assetImpact)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 자산 영향 드라마틱 애니메이션
     */
    private fun animateAssetImpact(assetImpact: Long) {
        val impactText = binding.tvAssetImpact
        val progressBar = binding.progressAssetImpact
        
        // 텍스트 색상 애니메이션
        impactText.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(300)
            .withEndAction {
                impactText.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .start()
            }
            .start()
        
        // 프로그레스 바 애니메이션
        progressBar.animate()
            .alpha(0.7f)
            .setDuration(200)
            .withEndAction {
                progressBar.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
    
    /**
     * 은행잔고 정보 감지 및 저장
     */
    private fun detectAndSaveBankBalance(smsText: String) {
        try {
            val bankBalance = CardSmsParser.parseBankBalance(smsText)
            if (bankBalance != null) {
                // AssetRepository에 은행잔고 추가
                assetRepo.addBankBalance(bankBalance)
                
                android.util.Log.d("MainActivity", "Bank balance detected and saved: ${bankBalance.bankName} ${bankBalance.accountNumber} - ${nf.format(bankBalance.balance)}원")
                
                // 사용자에게 알림
                Snackbar.make(binding.root, 
                    "은행잔고가 자동으로 추가되었습니다!\n" +
                    "${bankBalance.bankName} ${bankBalance.accountNumber}\n" +
                    "잔고: ${nf.format(bankBalance.balance)}원", 
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error detecting bank balance", e)
        }
    }
    
    /**
     * 급여 정보 감지 및 저장
     */
    private fun detectAndSaveSalary(notificationText: String, appName: String) {
        val salaryInfo = FinancialNotificationAnalyzer.extractSalaryInfo(notificationText, appName)
        if (salaryInfo != null) {
            detectedSalary = salaryInfo
            
            // 실수령액 기반 국민연금 납입액 역계산
            val pensionInfo = retirementService.calculatePensionContributionFromNetSalary(salaryInfo.amount)
            
            // 수입 정보로 저장 (실수령액 기준으로 수정)
            val incomeInfo = IncomeInfo(
                amount = salaryInfo.amount, // 실수령액으로 저장 (품질 개선)
                source = "급여",
                bankName = salaryInfo.bankName,
                transactionDate = salaryInfo.salaryDate,
                description = "월급 (실수령액: ${nf.format(salaryInfo.amount)}원, 총급여: ${nf.format(pensionInfo.grossSalary)}원, 국민연금: ${nf.format(pensionInfo.pensionContribution)}원)",
                isRecurring = true
            )
            incomeRepo.addIncome(incomeInfo)
            
            updateRetirementPlan()

            // 급여 감지 알림 (국민연금 정보 포함)
            Snackbar.make(binding.root, 
                "급여 정보가 감지되었습니다!\n" +
                "실수령액: ${nf.format(salaryInfo.amount)}원\n" +
                "총급여: ${nf.format(pensionInfo.grossSalary)}원\n" +
                "국민연금 납입액: ${nf.format(pensionInfo.pensionContribution)}원", 
                Snackbar.LENGTH_LONG).show()
        }
    }
    
    /**
     * 자동이체 정보 감지 및 저장
     */
    private fun detectAndSaveAutoTransfer(notificationText: String, appName: String) {
        val autoTransferInfo = FinancialNotificationAnalyzer.extractAutoTransferInfo(notificationText, appName)
        if (autoTransferInfo != null) {
            autoTransfers.add(autoTransferInfo)
            
            // 자동이체 감지 알림
            Snackbar.make(binding.root, "자동이체 감지: ${autoTransferInfo.transferType} ${nf.format(autoTransferInfo.amount)}원", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 엑셀 내보내기 다이얼로그 표시
     */
    private fun showExportExcelDialog() {
        val options = arrayOf(
            "거래내역 미리보기",
            "월별 요약 미리보기", 
            "통합 리포트 미리보기"
        )
        
        AlertDialog.Builder(this)
            .setTitle("엑셀 내보내기")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTransactionPreview()
                    1 -> showMonthlySummaryPreview()
                    2 -> showComprehensiveReportPreview()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    /**
     * 현재 타임스탬프 생성
     */
    private fun getCurrentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    
    /**
     * 거래내역 미리보기 표시
     */
    private fun showTransactionPreview() {
        val transactions = vm.getAllTransactions()
        val fileName = "card_transactions_${getCurrentTimestamp()}.csv"
        
        ExcelPreviewDialog.showTransactionPreview(
            context = this,
            transactions = transactions,
            fileName = fileName,
            onDownload = { exportCardTransactions() },
            onCancel = { }
        )
    }
    
    /**
     * 월별 요약 미리보기 표시
     */
    private fun showMonthlySummaryPreview() {
        val transactions = vm.getAllTransactions()
        val summary = monthlySummaryService.calculateMonthlySummary(transactions)
        val fileName = "monthly_summary_${getCurrentTimestamp()}.csv"
        
        ExcelPreviewDialog.showMonthlySummaryPreview(
            context = this,
            summary = summary,
            fileName = fileName,
            onDownload = { exportMonthlySummary() },
            onCancel = { }
        )
    }
    
    /**
     * 통합 리포트 미리보기 표시
     */
    private fun showComprehensiveReportPreview() {
        val transactions = vm.getAllTransactions()
        val summary = monthlySummaryService.calculateMonthlySummary(transactions)
        val forecast = monthlySummaryService.calculatePaymentForecast(transactions)
        val fileName = "comprehensive_report_${getCurrentTimestamp()}.csv"
        
        ExcelPreviewDialog.showComprehensiveReportPreview(
            context = this,
            transactions = transactions,
            summary = summary,
            forecast = forecast,
            fileName = fileName,
            onDownload = { exportComprehensiveReport() },
            onCancel = { }
        )
    }
    
    /**
     * 카드 거래내역만 내보내기
     */
    private fun exportCardTransactions() {
        scope.launch {
            try {
                val transactions = vm.getAllTransactions()
                val file = excelExportService.exportCardTransactions(transactions)
                
                if (file != null) {
                    Snackbar.make(binding.root, "거래내역이 내보내기되었습니다: ${file.name}", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(binding.root, "내보내기 실패", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "내보내기 오류: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * 월별 요약만 내보내기
     */
    private fun exportMonthlySummary() {
        scope.launch {
            try {
                val transactions = vm.getAllTransactions()
                val summary = monthlySummaryService.calculateMonthlySummary(transactions)
                val file = excelExportService.exportMonthlySummary(summary)
                
                if (file != null) {
                    Snackbar.make(binding.root, "월별 요약이 내보내기되었습니다: ${file.name}", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(binding.root, "내보내기 실패", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "내보내기 오류: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * 통합 리포트 내보내기
     */
    private fun exportComprehensiveReport() {
        scope.launch {
            try {
                val transactions = vm.getAllTransactions()
                val summary = monthlySummaryService.calculateMonthlySummary(transactions)
                val forecast = monthlySummaryService.calculatePaymentForecast(transactions)
                val file = excelExportService.exportComprehensiveReport(transactions, summary, forecast)
                
                if (file != null) {
                    Snackbar.make(binding.root, "통합 리포트가 내보내기되었습니다: ${file.name}", Snackbar.LENGTH_LONG).show()
                } else {
                    Snackbar.make(binding.root, "내보내기 실패", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "내보내기 오류: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }
    
    private fun initializeSettings() {
        settingsPreferences = SettingsPreferences.getInstance(this)
        updateDuplicateSettingsDisplay()
    }
    
    private fun updateDuplicateSettingsDisplay() {
        val minutes = settingsPreferences.getDuplicateDetectionMinutes()
        binding.tvDuplicateSettings.text = "중복 거래 인식: ${minutes}분"
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    
    
    
    private fun parseAndAddSmsTransactions(smsText: String) {
        try {
            val duplicateDetectionMinutes = settingsPreferences.getDuplicateDetectionMinutes()
            val newTransactions = CardSmsParser.parseSmsText(smsText, duplicateDetectionMinutes)
            
            // 각 줄마다 개별적으로 급여 정보와 은행잔고 정보 감지
            val lines = smsText.trim().split("\n")
            for (line in lines) {
                if (line.isBlank()) continue
                
                // 급여 정보 감지 (각 줄마다)
                detectAndSaveSalary(line, "SMS 입력")
                
                // 은행잔고 정보 감지 (각 줄마다)
                detectAndSaveBankBalance(line)
            }
            
            if (newTransactions.isNotEmpty()) {
                // 기존 거래와 중복 제거
                val existingKeys = cardTransactions.map { transaction ->
                    "${transaction.cardNumber}_${transaction.amount}_${transaction.transactionDate}_${transaction.merchant}"
                }.toSet()
                
                val uniqueNewTransactions = newTransactions.filter { transaction ->
                    val uniqueKey = "${transaction.cardNumber}_${transaction.amount}_${transaction.transactionDate}_${transaction.merchant}"
                    !existingKeys.contains(uniqueKey)
                }
                
                if (uniqueNewTransactions.isNotEmpty()) {
                    cardTransactions.addAll(uniqueNewTransactions)
                    // Repository에도 저장
                    vm.addAllTransactions(uniqueNewTransactions)
                    updateMonthlyTotal()
                    updateMonthlyIncome() // 수입 정보도 업데이트
                    
                    Snackbar.make(
                        binding.root,
                        getString(R.string.msg_imported_n, uniqueNewTransactions.size),
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        "모든 거래가 이미 존재합니다",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } else {
                // 거래는 없지만 급여 정보가 있을 수 있으므로 수입 정보 업데이트
                updateMonthlyIncome()
                Snackbar.make(
                    binding.root,
                    R.string.msg_no_parsable,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(
                binding.root,
                "SMS 파싱 중 오류가 발생했습니다: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
        
        // 은퇴자산 추정 업데이트
        updateRetirementAssetEstimate()
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (drawerToggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        updateMonthlyTotal()
        updateMonthlyIncome()
        updateNotificationAccessIndicator()
        updateRetirementAssetEstimate() // 은퇴자산 추정 업데이트
        // 설정 화면에서 돌아왔을 때 설정 표시 업데이트
        updateDuplicateSettingsDisplay()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        )
        return enabled?.split(":")?.any { it.contains(packageName, ignoreCase = true) } == true
    }

    private fun openNotificationAccessSettings() {
        NotificationHistoryPermissionManager.requestNotificationAccessPermission(this)
    }

    private fun updateNotificationAccessIndicator() {
        val permissionStatus = NotificationHistoryPermissionManager.getPermissionStatus(this)
        binding.chipNotifyStatus.isChecked = permissionStatus.hasNotificationAccess
        binding.chipNotifyStatus.text = if (permissionStatus.hasNotificationAccess) getString(R.string.notify_on) else getString(R.string.notify_off)
    }

    private fun showPasteDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_paste, null)
        val et = view.findViewById<EditText>(R.id.etPaste)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_paste_sms))
            .setView(view)
            .setPositiveButton(R.string.action_import) { d, _ ->
                val text = et.text?.toString().orEmpty()
                parseAndAddSmsTransactions(text)
                d.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun openNotificationLog() {
        startActivity(Intent(this, NotificationLogActivity::class.java))
    }

    private fun showExportDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_export_sheets, null)
        val etUrl = view.findViewById<EditText>(R.id.etScriptUrl)
        val etSheet = view.findViewById<EditText>(R.id.etSheetName)
        etUrl.setText(exportPrefs.endpointUrl)
        etSheet.setText(exportPrefs.sheetName.ifBlank { getString(R.string.default_sheet_name) })

        AlertDialog.Builder(this)
            .setTitle(R.string.title_export_to_sheets)
            .setView(view)
            .setPositiveButton(R.string.menu_export_sheets) { dialog, _ ->
                val url = etUrl.text?.toString().orEmpty()
                val sheet = etSheet.text?.toString().orEmpty()
                if (url.isBlank()) {
                    Snackbar.make(binding.root, R.string.msg_export_requires_url, Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                exportPrefs.endpointUrl = url
                exportPrefs.sheetName = sheet.ifBlank { getString(R.string.default_sheet_name) }
                exportNotifications(url, sheet)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun exportNotifications(url: String, sheetName: String) {
        val snackbar = Snackbar.make(binding.root, R.string.msg_export_progress, Snackbar.LENGTH_INDEFINITE)
        scope.launch {
            snackbar.show()
            val result = sheetsExporter.export(
                SheetsShareConfig(
                    endpointUrl = url,
                    sheetName = sheetName.ifBlank { getString(R.string.default_sheet_name) }
                )
            )
            snackbar.dismiss()
            result
                .onSuccess { count ->
                    if (count <= 0) {
                        Snackbar.make(
                            binding.root,
                            R.string.msg_export_empty,
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.msg_export_success, count),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
                .onFailure { t ->
                    val message = t.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.msg_export_failure_unknown)
                    Snackbar.make(
                        binding.root,
                        getString(R.string.msg_export_failure, message),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
        }
    }
    
    /**
     * 은퇴설정 다이얼로그 표시
     */
    private fun showRetirementSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_retirement_settings, null)
        
        val etBirthDate = dialogView.findViewById<EditText>(R.id.etBirthDate)
        val etPensionMonths = dialogView.findViewById<EditText>(R.id.etPensionMonths)
        val etRetirementAge = dialogView.findViewById<EditText>(R.id.etRetirementAge)
        val etNetMonthlyIncome = dialogView.findViewById<EditText>(R.id.etNetMonthlyIncome)
        val tvCalculationPreview = dialogView.findViewById<TextView>(R.id.tvCalculationPreview)
        
        // 현재 설정값으로 초기화
        etBirthDate.setText("${retirementSettings.birthDate.year}-${retirementSettings.birthDate.monthValue.toString().padStart(2, '0')}")
        etPensionMonths.setText(retirementSettings.pensionSubscriptionMonths.toString())
        etRetirementAge.setText(retirementSettings.desiredRetirementAge.toString())
        etNetMonthlyIncome.setText("3000000") // 기본값
        
        // 계산 미리보기 업데이트
        updateCalculationPreview(etNetMonthlyIncome.text.toString().toLongOrNull() ?: 3000000L, tvCalculationPreview)
        
        // 실시간 계산 미리보기
        etNetMonthlyIncome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val income = s.toString().toLongOrNull() ?: 0L
                if (income > 0) {
                    updateCalculationPreview(income, tvCalculationPreview)
                }
            }
        })
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("저장") { _, _ ->
                saveRetirementSettings(
                    etBirthDate.text.toString(),
                    etPensionMonths.text.toString().toIntOrNull() ?: 420,
                    etRetirementAge.text.toString().toIntOrNull() ?: 60
                )
            }
            .setNegativeButton("취소", null)
            .create()
        
        dialog.show()
    }
    
    /**
     * 계산 미리보기 업데이트
     */
    private fun updateCalculationPreview(netMonthlyIncome: Long, tvPreview: TextView) {
        try {
            val result = retirementService.calculatePensionWithNewFormula(
                birthDate = "${retirementSettings.birthDate.year}-${retirementSettings.birthDate.monthValue.toString().padStart(2, '0')}",
                netMonthlyIncome = netMonthlyIncome,
                retirementAge = retirementSettings.desiredRetirementAge,
                subscriptionMonths = retirementSettings.pensionSubscriptionMonths
            )
            
            tvPreview.text = "${result.calculationResult.baseIncomeAmount}\n${result.calculationResult.expectedPensionAmount}"
        } catch (e: Exception) {
            tvPreview.text = "계산 오류"
        }
    }
    
    /**
     * 은퇴설정 저장
     */
    private fun saveRetirementSettings(birthDateStr: String, pensionMonths: Int, retirementAge: Int) {
        try {
            val parts = birthDateStr.split("-")
            if (parts.size == 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                
                retirementSettings = RetirementSettings(
                    birthDate = LocalDate.of(year, month, 1),
                    pensionSubscriptionMonths = pensionMonths,
                    desiredRetirementAge = retirementAge
                )
                
                // UI 업데이트
                updateRetirementSettingsUI()
                
                Snackbar.make(binding.root, "은퇴설정이 저장되었습니다", Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "설정 저장 중 오류가 발생했습니다", Snackbar.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 은퇴자산 추정 업데이트
     */
    private fun updateRetirementAssetEstimate() {
        scope.launch {
            try {
                // 현재 월 수입 및 지출 계산
                val currentMonthIncome = incomeRepo.getCurrentMonthIncome()
                val currentMonthExpense = vm.events.value.sumOf { it.amount }
                val currentAssets = assetRepo.getTotalAssetValue()
                
                // 은퇴자산 추정 계산
                val estimate = retirementService.calculateRetirementAssetEstimate(
                    retirementSettings = retirementSettings,
                    currentAssets = currentAssets,
                    currentMonthlyIncome = currentMonthIncome,
                    currentMonthlyExpense = currentMonthExpense,
                    countryCode = "KR"
                )
                
                // UI 업데이트
                updateRetirementAssetUI(estimate)
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error updating retirement asset estimate", e)
            }
        }
    }
    
    /**
     * 은퇴자산 UI 업데이트
     */
    private fun updateRetirementAssetUI(estimate: RetirementAssetEstimate) {
        // 목표 은퇴자산 표시
        binding.tvTargetRetirementAsset.text = formatLargeNumber(estimate.estimatedRetirementAssets)
        
        // 자산 영향 텍스트 업데이트
        val impactText = "이번 달 소비: ${nf.format(estimate.estimatedMonthlyExpense)}원 → 미래 자산 감소: ${nf.format(estimate.estimatedMonthlyExpense * estimate.yearsToRetirement * 12)}원"
        binding.tvAssetImpact.text = impactText
        
        // 자산 영향 프로그레스 바 업데이트 (소비 대비 저축 비율)
        val savingsRatio = if (estimate.estimatedMonthlyIncome > 0) {
            (estimate.monthlySavings.toFloat() / estimate.estimatedMonthlyIncome.toFloat() * 100).toInt()
        } else {
            0
        }
        binding.progressAssetImpact.progress = savingsRatio
        
        // 은퇴 후 월 생활비 업데이트
        binding.tvMonthlyRetirementExpense.text = "${nf.format(estimate.monthlyLivingExpenseAfterRetirement)}원"
        
        // 정교화된 국민연금 계산 및 표시
        updateRefinedPensionDisplay(estimate.estimatedMonthlyIncome)
        
        android.util.Log.d("MainActivity", "Retirement asset estimate updated: ${estimate.estimatedRetirementAssets}")
    }
    
    /**
     * 정교화된 국민연금 표시 업데이트
     */
    private fun updateRefinedPensionDisplay(currentMonthlyIncome: Long) {
        val refinedPensionResult = retirementService.calculateRefinedPension(
            retirementSettings = retirementSettings,
            currentMonthlySalary = currentMonthlyIncome,
            countryCode = "KR"
        )
        
        if (refinedPensionResult != null) {
            // 실제 급여가 있을 때만 국민연금 표시
            binding.tvPensionAmount.text = "${nf.format(refinedPensionResult.estimatedMonthlyPension)}원"
            binding.tvPensionAmount.visibility = android.view.View.VISIBLE
            
            android.util.Log.d("MainActivity", "Refined pension calculated: ${refinedPensionResult.estimatedMonthlyPension}원 (${refinedPensionResult.calculationMethod})")
        } else {
            // 실제 급여가 없을 때는 국민연금 숨김
            binding.tvPensionAmount.visibility = android.view.View.GONE
            
            android.util.Log.d("MainActivity", "No actual salary - pension display hidden")
        }
    }
    
    /**
     * 큰 숫자를 한국식으로 포맷팅 (억, 만 단위)
     */
    private fun formatLargeNumber(amount: Long): String {
        return when {
            amount >= 100_000_000 -> {
                val eok = amount / 100_000_000
                val man = (amount % 100_000_000) / 10_000
                if (man > 0) "${eok}억 ${man}만원" else "${eok}억원"
            }
            amount >= 10_000 -> {
                val man = amount / 10_000
                "${man}만원"
            }
            else -> "${nf.format(amount)}원"
        }
    }
    
    /**
     * 은퇴설정 UI 업데이트
     */
    private fun updateRetirementSettingsUI() {
        binding.tvBirthDate.text = "${retirementSettings.birthDate.year}-${retirementSettings.birthDate.monthValue.toString().padStart(2, '0')}"
        binding.tvPensionMonths.text = "${retirementSettings.pensionSubscriptionMonths}개월"
        binding.tvRetirementAge.text = "${retirementSettings.desiredRetirementAge}세"
    }
}
