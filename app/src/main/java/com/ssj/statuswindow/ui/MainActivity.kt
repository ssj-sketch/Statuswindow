package com.ssj.statuswindow.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
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
import com.ssj.statuswindow.hud.*
import com.ssj.statuswindow.model.CardTransaction
import com.ssj.statuswindow.util.SmsParser as CardSmsParser
import com.ssj.statuswindow.ui.adapter.CardTransactionAdapter
import com.ssj.statuswindow.ui.CardEventActivity
import com.ssj.statuswindow.repo.CardEventRepository
import com.ssj.statuswindow.repo.NotificationLogRepository
import com.ssj.statuswindow.ui.adapter.*
import com.ssj.statuswindow.util.NotificationExportPreferences
import com.ssj.statuswindow.util.NotificationSheetsExporter
import com.ssj.statuswindow.util.SettingsPreferences
import com.ssj.statuswindow.util.SheetsShareConfig
import com.ssj.statuswindow.util.SmsParser
import com.ssj.statuswindow.viewmodel.MainViewModel
import com.ssj.statuswindow.viewmodel.MainViewModelFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private val vm: MainViewModel by viewModels {
        MainViewModelFactory(CardEventRepository.instance(this))
    }
    private val notificationRepo by lazy { NotificationLogRepository.instance(this) }
    private val exportPrefs by lazy { NotificationExportPreferences(this) }
    private val sheetsExporter by lazy { NotificationSheetsExporter(notificationRepo) }
    private val scope = MainScope()
    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
    
    // HUD 관련 컴포넌트
    private val hudEngine = HudEngine()
    private var currentSnapshot: HudSnapshot? = null
    
    // 어댑터들
    private val modifierAdapter = ModifierAdapter()
    private val timelineAdapter = TimelineAdapter()
    private val suggestionAdapter = CoachingSuggestionAdapter()
    private val cardTransactionAdapter = CardTransactionAdapter()
    
    // 카드 거래 내역 저장소
    private val cardTransactions = mutableListOf<CardTransaction>()
    
    // 설정 관리
    private lateinit var settingsPreferences: SettingsPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        setupRecyclerViews()

        // 설정 초기화
        initializeSettings()

        // FAB: 붙여넣기 입력 → 파싱 → 저장
        binding.fabAdd.setOnClickListener { showPasteDialog() }

        // 알림 접근 상태 표시 & 설정 화면 이동
        binding.btnOpenNotificationAccess.setOnClickListener { openNotificationAccessSettings() }
        
        // 설정 버튼 클릭
        binding.btnSettings.setOnClickListener { openSettings() }
        updateNotificationAccessIndicator()
        
        // 상세보기 버튼 클릭 이벤트
        binding.btnViewDetails.setOnClickListener { 
            startActivity(Intent(this, CardEventActivity::class.java))
        }

        // HUD 초기화 및 업데이트 (임시 비활성화)
        // initializeHUD()
        
        // 수집 목록 구독 (기존 기능 유지)
        scope.launch {
            vm.events.collectLatest { list ->
                val total = list.sumOf { it.amount }
                // 기존 총액 표시는 숨기고 HUD에 통합
            }
        }
    }

    private fun setupRecyclerViews() {
        // RecyclerView 설정 (모든 어댑터 비활성화)
        // binding.recyclerViewModifiers.layoutManager = LinearLayoutManager(this)
        // binding.recyclerViewModifiers.adapter = modifierAdapter
        
        // binding.recyclerViewTimeline.layoutManager = LinearLayoutManager(this)
        // binding.recyclerViewTimeline.adapter = timelineAdapter
        
        // binding.recyclerViewSuggestions.layoutManager = LinearLayoutManager(this)
        // binding.recyclerViewSuggestions.adapter = suggestionAdapter
        
        updateMonthlyTotal()
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
                    updateMonthlyTotal()
                    
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
    }

    private fun initializeHUD() {
        // 더미 데이터로 초기 HUD 생성
        val dummySignals = listOf(
            ActivitySignal(
                timestamp = Instant.now().minusSeconds(3600),
                steps = 5000,
                activityDuration = java.time.Duration.ofMinutes(30),
                isRecovery = false
            ),
            FocusSessionSignal(
                timestamp = Instant.now().minusSeconds(1800),
                duration = java.time.Duration.ofMinutes(25),
                interruptions = 2,
                isUserInitiated = true
            ),
            FinancialSignal(
                timestamp = Instant.now().minusSeconds(900),
                type = FinancialSignalType.SAVE,
                amount = 100000.0
            )
        )
        
        val snapshot = hudEngine.computeSnapshot(
            previous = null,
            signals = dummySignals,
            now = Instant.now()
        )
        
        updateHUD(snapshot)
    }

    private fun updateHUD(snapshot: HudSnapshot) {
        currentSnapshot = snapshot
        
        // 레벨/EXP 업데이트
        binding.tvLevel.text = "Lv.${snapshot.exp.level}"
        binding.tvExp.text = "EXP: ${snapshot.exp.currentExp}/${snapshot.exp.expForNext}"
        
        // 스탯 게이지 업데이트
        updateStatGauge(binding.progressWealth, binding.tvWealthScore, snapshot.statScore(HudStatKind.WEALTH))
        updateStatGauge(binding.progressVital, binding.tvVitalScore, snapshot.statScore(HudStatKind.VITAL))
        updateStatGauge(binding.progressCognition, binding.tvCognitionScore, snapshot.statScore(HudStatKind.COGNITION))
        updateStatGauge(binding.progressBalance, binding.tvBalanceScore, snapshot.statScore(HudStatKind.BALANCE))
        
        // 버프/디버프 업데이트 (비활성화)
        // val activeModifiers = snapshot.stats.values.flatMap { it.modifiers }
        //     .filter { it.isActive(Instant.now()) }
        // modifierAdapter.submitList(activeModifiers)
        
        // 타임라인 업데이트 (비활성화)
        // timelineAdapter.submitList(snapshot.timeline)
        
        // 코칭 제안 업데이트 (비활성화)
        // suggestionAdapter.submitList(snapshot.suggestions)
    }

    private fun updateStatGauge(progressIndicator: com.google.android.material.progressindicator.CircularProgressIndicator, 
                               scoreText: android.widget.TextView, score: Double) {
        try {
            // CircularProgressIndicator 비활성화, TextView만 사용
            scoreText.text = score.toInt().toString()
        } catch (e: Exception) {
            // 기본값으로 설정
            scoreText.text = score.toInt().toString()
        }
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
        updateNotificationAccessIndicator()
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
        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun updateNotificationAccessIndicator() {
        val on = isNotificationAccessEnabled()
        binding.chipNotifyStatus.text =
            if (on) getString(R.string.notify_on) else getString(R.string.notify_off)
        binding.chipNotifyStatus.isChecked = on
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
}
