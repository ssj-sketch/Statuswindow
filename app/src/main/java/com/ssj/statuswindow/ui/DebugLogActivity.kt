package com.ssj.statuswindow.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ssj.statuswindow.databinding.ActivityDebugLogBinding
import com.ssj.statuswindow.util.CrashLogger
import java.io.File

/**
 * 디버그 로그 확인 액티비티
 */
class DebugLogActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDebugLogBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        loadLogs()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "디버그 로그"
    }
    
    private fun loadLogs() {
        val logFiles = CrashLogger.getLogFiles(this)
        
        if (logFiles.isEmpty()) {
            binding.tvLogContent.text = "저장된 로그가 없습니다."
            return
        }
        
        val logContent = StringBuilder()
        logContent.append("=== 로그 파일 목록 ===\n\n")
        
        logFiles.sortedByDescending { it.lastModified() }.forEach { file ->
            logContent.append("파일: ${file.name}\n")
            logContent.append("크기: ${file.length()} bytes\n")
            logContent.append("수정일: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(file.lastModified())}\n")
            logContent.append("---\n")
        }
        
        // 가장 최근 크래시 로그 내용 표시
        val crashLogs = logFiles.filter { it.name.startsWith("crash_") }
        if (crashLogs.isNotEmpty()) {
            val latestCrashLog = crashLogs.maxByOrNull { it.lastModified() }
            latestCrashLog?.let { file ->
                logContent.append("\n=== 최근 크래시 로그 ===\n")
                try {
                    val content = file.readText()
                    logContent.append(content)
                } catch (e: Exception) {
                    logContent.append("로그 파일 읽기 실패: ${e.message}")
                }
            }
        }
        
        binding.tvLogContent.text = logContent.toString()
    }
    
    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            loadLogs()
        }
        
        binding.btnClearLogs.setOnClickListener {
            CrashLogger.clearLogs(this)
            loadLogs()
        }
        
        binding.btnExportLogs.setOnClickListener {
            exportLogs()
        }
    }
    
    private fun exportLogs() {
        try {
            val logFiles = CrashLogger.getLogFiles(this)
            if (logFiles.isEmpty()) {
                binding.tvLogContent.text = "내보낼 로그가 없습니다."
                return
            }
            
            // 로그 내용을 하나의 문자열로 합치기
            val allLogs = StringBuilder()
            logFiles.sortedByDescending { it.lastModified() }.forEach { file ->
                allLogs.append("=== ${file.name} ===\n")
                try {
                    allLogs.append(file.readText())
                } catch (e: Exception) {
                    allLogs.append("읽기 실패: ${e.message}")
                }
                allLogs.append("\n\n")
            }
            
            // 클립보드에 복사
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Debug Logs", allLogs.toString())
            clipboard.setPrimaryClip(clip)
            
            binding.tvLogContent.text = "로그가 클립보드에 복사되었습니다.\n\n$allLogs"
        } catch (e: Exception) {
            binding.tvLogContent.text = "로그 내보내기 실패: ${e.message}"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
