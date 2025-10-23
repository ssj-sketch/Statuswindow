package com.ssj.statuswindow.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivityNotificationLogBinding
import com.ssj.statuswindow.repo.NotificationLogRepository
import com.ssj.statuswindow.ui.adapter.NotificationLogAdapter
import com.ssj.statuswindow.util.NotificationHistoryReader
import com.ssj.statuswindow.viewmodel.NotificationLogViewModel
import com.ssj.statuswindow.viewmodel.NotificationLogViewModelFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationLogBinding
    private val repo by lazy { NotificationLogRepository.instance(this) }
    private val vm: NotificationLogViewModel by viewModels {
        NotificationLogViewModelFactory(repo)
    }
    private val scope = MainScope()
    private val adapter = NotificationLogAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_notification_log)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        scope.launch {
            vm.logs.collectLatest { list ->
                adapter.submitList(list)
                binding.emptyView.isVisible = list.isEmpty()
            }
        }
        
        // 히스토리 가져오기 버튼 이벤트
        binding.btnLoadHistory7Days.setOnClickListener {
            loadNotificationHistory(7)
        }
        
        binding.btnLoadHistory30Days.setOnClickListener {
            loadNotificationHistory(30)
        }
    }

    private fun loadNotificationHistory(days: Int) {
        val historyReader = NotificationHistoryReader(this)
        
        if (!historyReader.hasNotificationHistoryPermission()) {
            Snackbar.make(binding.root, "알림 히스토리 접근 권한이 필요합니다", Snackbar.LENGTH_LONG).show()
            return
        }
        
        scope.launch {
            try {
                val historyNotifications = historyReader.getNotificationHistory(days)
                
                if (historyNotifications.isNotEmpty()) {
                    // Repository에 히스토리 알림 추가
                    historyNotifications.forEach { notification ->
                        repo.add(notification)
                    }
                    
                    Snackbar.make(
                        binding.root, 
                        "${days}일간 ${historyNotifications.size}개의 알림을 가져왔습니다", 
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root, 
                        "${days}일간 가져올 알림이 없습니다", 
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root, 
                    "알림 히스토리 가져오기 실패: ${e.message}", 
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
