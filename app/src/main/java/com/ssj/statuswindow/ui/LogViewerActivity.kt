package com.ssj.statuswindow.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.StatusWindowDatabase
import com.ssj.statuswindow.database.entity.LogEntity
import com.ssj.statuswindow.ui.adapter.LogAdapter
import com.ssj.statuswindow.ui.components.AppToolbar
import com.ssj.statuswindow.util.LogManager
import com.ssj.statuswindow.util.NavigationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * 로그 확인 액티비티
 */
class LogViewerActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var appToolbar: AppToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var logAdapter: LogAdapter
    private lateinit var database: StatusWindowDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)
        
        // 데이터베이스 초기화
        database = StatusWindowDatabase.getDatabase(this)
        
        // LogManager 초기화 확인
        LogManager.getInstance().initialize(this)
        
        setupViews()
        loadLogs()
        
        // 추가 테스트 로그
        LogManager.getInstance().d("LogViewerActivity", "onCreate 완료")
        LogManager.getInstance().i("LogViewerActivity", "데이터베이스 연결 완료")
    }
    
    private fun setupViews() {
        // DrawerLayout과 NavigationView 초기화
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        
        // NavigationManager 설정
        NavigationManager.setupNavigation(this, navigationView, drawerLayout, LogViewerActivity::class.java)
        NavigationManager.setActiveMenuItem(navigationView, LogViewerActivity::class.java)
        
        // AppToolbar 설정
        appToolbar = findViewById(R.id.appToolbar)
        appToolbar.setupWithDrawer(this, drawerLayout)
        appToolbar.setTitle("📝 로그 확인")
        
        // RecyclerView 설정
        recyclerView = findViewById(R.id.recyclerView)
        logAdapter = LogAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = logAdapter
        
        // 테스트 로그 추가
        LogManager.getInstance().d("LogViewerActivity", "LogViewerActivity 시작됨")
        LogManager.getInstance().i("LogViewerActivity", "로그 뷰어 화면 로드 완료")
    }
    
    private fun loadLogs() {
        lifecycleScope.launch {
            try {
                LogManager.getInstance().d("LogViewerActivity", "로그 로드 시작")
                
                // 데이터베이스 연결 확인
                val logDao = database.logDao()
                LogManager.getInstance().d("LogViewerActivity", "LogDao 획득 성공")
                
                // 테스트 로그 추가
                LogManager.getInstance().d("LogViewerActivity", "테스트 로그 추가 중...")
                LogManager.getInstance().i("LogViewerActivity", "로그 뷰어 테스트 메시지")
                LogManager.getInstance().w("LogViewerActivity", "경고 메시지 테스트")
                LogManager.getInstance().e("LogViewerActivity", "에러 메시지 테스트")
                
                // 로그 개수 확인
                val logCount = logDao.getLogCount()
                LogManager.getInstance().d("LogViewerActivity", "현재 로그 개수: $logCount")
                
                // Flow 수집 시작
                LogManager.getInstance().d("LogViewerActivity", "Flow 수집 시작")
                logDao.getAllLogs().collect { logs ->
                    LogManager.getInstance().d("LogViewerActivity", "로그 수신: ${logs.size}개")
                    logAdapter.submitList(logs)
                    LogManager.getInstance().d("LogViewerActivity", "RecyclerView 업데이트 완료")
                }
            } catch (e: Exception) {
                LogManager.getInstance().e("LogViewerActivity", "로그 로드 실패: ${e.message}", e)
                Toast.makeText(this@LogViewerActivity, "로그 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
