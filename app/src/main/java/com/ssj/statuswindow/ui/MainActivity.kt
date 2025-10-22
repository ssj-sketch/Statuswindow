package com.ssj.statuswindow.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivityMainBinding
import com.ssj.statuswindow.repo.CardEventRepository
import com.ssj.statuswindow.ui.adapter.CardEventAdapter
import com.ssj.statuswindow.util.SmsParser
import com.ssj.statuswindow.viewmodel.MainViewModel
import com.ssj.statuswindow.viewmodel.MainViewModelFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels {
        MainViewModelFactory(CardEventRepository.instance(this))
    }
    private val scope = MainScope()
    private val adapter = CardEventAdapter()
    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar 타이틀
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_card_events)

        // 리사이클러뷰 (표처럼 구분선 포함)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        // FAB: 붙여넣기 입력 → 파싱 → 저장
        binding.fabAdd.setOnClickListener { showPasteDialog() }

        // 알림 접근 상태 표시 & 설정 화면 이동
        binding.btnOpenNotificationAccess.setOnClickListener { openNotificationAccessSettings() }
        updateNotificationAccessIndicator()

        // 수집 목록 구독
        scope.launch {
            vm.events.collectLatest { list ->
                adapter.submitList(list)
                val total = list.sumOf { it.amount } // ✅ 취소는 음수로 반영
                binding.tvTotal.text = getString(R.string.total_amount_fmt, total)
                binding.emptyView.isVisible = list.isEmpty()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationAccessIndicator()
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
                val parsed = SmsParser.parse(text)
                if (parsed.isEmpty()) {
                    Snackbar.make(binding.root, R.string.msg_no_parsable, Snackbar.LENGTH_SHORT).show()
                } else {
                    vm.addEvents(parsed)
                    Snackbar.make(binding.root, getString(R.string.msg_imported_n, parsed.size), Snackbar.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
            .show()
    }
}
