package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.util.RealTimeLogManager
import com.ssj.statuswindow.util.NavigationManager
import com.ssj.statuswindow.ui.components.AppToolbar
import kotlinx.coroutines.*

/**
 * 실시간 로그 모니터링 화면
 */
class RealTimeLogActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appToolbar: AppToolbar
    private lateinit var navigationView: NavigationView
    private lateinit var btnStartLogging: Button
    private lateinit var btnStopLogging: Button
    private lateinit var btnClearLogs: Button
    private lateinit var btnExportLogs: Button
    private lateinit var btnRefreshLogs: Button
    private lateinit var tvLogStatus: TextView
    private lateinit var tvQueueSize: TextView
    private lateinit var spinnerLogLevel: Spinner
    private lateinit var tvRealtimeLogs: TextView
    
    private lateinit var logManager: RealTimeLogManager
    private val logScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentLogLevel = RealTimeLogManager.LogLevel.VERBOSE
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime_log)
        
        setupViews()
        setupToolbar()
        setupNavigation()
        setupLogManager()
        setupSpinner()
        setupClickListeners()
        startLogMonitoring()
    }
    
    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        appToolbar = findViewById(R.id.appToolbar)
        
        // AppToolbar 설정
        appToolbar.setupWithDrawer(this, drawerLayout)
        appToolbar.setTitle("실시간 로그")
        navigationView = findViewById(R.id.navigationView)
        btnStartLogging = findViewById(R.id.btnStartLogging)
        btnStopLogging = findViewById(R.id.btnStopLogging)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        btnExportLogs = findViewById(R.id.btnExportLogs)
        btnRefreshLogs = findViewById(R.id.btnRefreshLogs)
        tvLogStatus = findViewById(R.id.tvLogStatus)
        tvQueueSize = findViewById(R.id.tvQueueSize)
        spinnerLogLevel = findViewById(R.id.spinnerLogLevel)
        tvRealtimeLogs = findViewById(R.id.tvRealtimeLogs)
    }
    
    private fun setupToolbar() {
        // AppToolbar는 이미 setupViews에서 설정됨
        // 기존 toolbar 관련 코드는 제거
    }
    
    private fun setupNavigation() {
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, RealTimeLogActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, RealTimeLogActivity::class.java)
    }
    
    private fun setupLogManager() {
        logManager = RealTimeLogManager.getInstance(this)
        updateStatus()
    }
    
    private fun setupSpinner() {
        val logLevels = listOf(
            "모든 로그" to RealTimeLogManager.LogLevel.VERBOSE,
            "DEBUG 이상" to RealTimeLogManager.LogLevel.DEBUG,
            "INFO 이상" to RealTimeLogManager.LogLevel.INFO,
            "WARN 이상" to RealTimeLogManager.LogLevel.WARN,
            "ERROR만" to RealTimeLogManager.LogLevel.ERROR
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, logLevels.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLogLevel.adapter = adapter
        
        spinnerLogLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                currentLogLevel = logLevels[position].second
                refreshLogs()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupClickListeners() {
        btnStartLogging.setOnClickListener {
            startLogging()
        }
        
        btnStopLogging.setOnClickListener {
            stopLogging()
        }
        
        btnClearLogs.setOnClickListener {
            clearLogs()
        }
        
        btnExportLogs.setOnClickListener {
            exportLogs()
        }
        
        btnRefreshLogs.setOnClickListener {
            refreshLogs()
        }
    }
    
    /**
     * 로그 수집 시작
     */
    private fun startLogging() {
        logManager.startLogging()
        updateStatus()
        Toast.makeText(this, "로그 수집이 시작되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 로그 수집 중지
     */
    private fun stopLogging() {
        logManager.stopLogging()
        updateStatus()
        Toast.makeText(this, "로그 수집이 중지되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 로그 삭제
     */
    private fun clearLogs() {
        logManager.clearLogFile()
        tvRealtimeLogs.text = "로그가 삭제되었습니다."
        updateStatus()
        Toast.makeText(this, "로그가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 로그 내보내기
     */
    private fun exportLogs() {
        val logContent = logManager.readLogFile()
        if (logContent != null) {
            // 클립보드에 복사
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("로그", logContent)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(this, "로그가 클립보드에 복사되었습니다.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "내보낼 로그가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 로그 새로고침
     */
    private fun refreshLogs() {
        val logContent = logManager.readLogFile()
        if (logContent != null) {
            val filteredContent = filterLogsByLevel(logContent)
            tvRealtimeLogs.text = filteredContent
        } else {
            tvRealtimeLogs.text = "로그가 없습니다."
        }
    }
    
    /**
     * 로그 레벨별 필터링
     */
    private fun filterLogsByLevel(logContent: String): String {
        val lines = logContent.split("\n")
        val filteredLines = lines.filter { line ->
            when (currentLogLevel) {
                RealTimeLogManager.LogLevel.VERBOSE -> true
                RealTimeLogManager.LogLevel.DEBUG -> line.contains("[DEBUG]") || line.contains("[INFO]") || line.contains("[WARN]") || line.contains("[ERROR]")
                RealTimeLogManager.LogLevel.INFO -> line.contains("[INFO]") || line.contains("[WARN]") || line.contains("[ERROR]")
                RealTimeLogManager.LogLevel.WARN -> line.contains("[WARN]") || line.contains("[ERROR]")
                RealTimeLogManager.LogLevel.ERROR -> line.contains("[ERROR]")
            }
        }
        return filteredLines.joinToString("\n")
    }
    
    /**
     * 상태 업데이트
     */
    private fun updateStatus() {
        val isActive = logManager.isLoggingActive()
        tvLogStatus.text = if (isActive) "상태: 수집 중" else "상태: 중지됨"
        tvQueueSize.text = "큐: ${logManager.getQueueSize()}"
        
        btnStartLogging.isEnabled = !isActive
        btnStopLogging.isEnabled = isActive
    }
    
    /**
     * 로그 모니터링 시작
     */
    private fun startLogMonitoring() {
        logScope.launch {
            while (isActive) {
                try {
                    updateStatus()
                    
                    // 실시간 로그 업데이트
                    if (logManager.isLoggingActive()) {
                        val recentLogs = logManager.getRecentLogs(50)
                        if (recentLogs.isNotEmpty()) {
                            val logText = recentLogs.joinToString("\n") { logEntry ->
                                "${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(logEntry.timestamp))} [${logEntry.level.name}] ${logEntry.tag}: ${logEntry.message}"
                            }
                            tvRealtimeLogs.text = logText
                        }
                    }
                    
                    delay(1000) // 1초마다 업데이트
                } catch (e: Exception) {
                    android.util.Log.e("RealTimeLogActivity", "모니터링 오류: ${e.message}", e)
                    delay(5000)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        logScope.cancel()
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
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
