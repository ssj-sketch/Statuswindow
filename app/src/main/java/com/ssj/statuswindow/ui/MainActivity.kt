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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ActivityMainBinding
import com.ssj.statuswindow.repo.CardEventRepository
import com.ssj.statuswindow.repo.NotificationLogRepository
import com.ssj.statuswindow.ui.adapter.CardEventAdapter
import com.ssj.statuswindow.util.NotificationExportPreferences
import com.ssj.statuswindow.util.NotificationSheetsExporter
import com.ssj.statuswindow.util.SheetsShareConfig
import com.ssj.statuswindow.util.SmsParser
import com.ssj.statuswindow.viewmodel.MainViewModel
import com.ssj.statuswindow.viewmodel.MainViewModelFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
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
    private val adapter = CardEventAdapter()
    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar + drawer
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.title_card_events)

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
                    Snackbar.make(
                        binding.root,
                        getString(R.string.msg_export_success, count),
                        Snackbar.LENGTH_LONG
                    ).show()
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
