package com.ssj.statuswindow.util

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.ui.*

/**
 * 공통 네비게이션 매니저
 * 모든 액티비티에서 동일한 네비게이션 로직을 사용할 수 있도록 모듈화
 */
object NavigationManager {
    
    private const val TAG = "NavigationManager"
    
    /**
     * 네비게이션 메뉴 설정
     * @param activity 현재 액티비티
     * @param navigationView 네비게이션 뷰
     * @param drawerLayout 드로어 레이아웃
     * @param currentActivityClass 현재 액티비티 클래스 (현재 화면에서는 아무것도 하지 않음)
     */
    fun setupNavigation(
        activity: AppCompatActivity,
        navigationView: NavigationView,
        drawerLayout: DrawerLayout,
        currentActivityClass: Class<*>
    ) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            Log.d(TAG, "메뉴 클릭 감지: ${menuItem.title} (ID: ${menuItem.itemId})")
            
            val handled = when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    navigateToActivity(activity, MainActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_card_details -> {
                    navigateToActivity(activity, CardDetailsActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_card_usage_table -> {
                    navigateToActivity(activity, CardTableActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_bank_transaction -> {
                    navigateToActivity(activity, BankTransactionActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_bank_transaction_table -> {
                    navigateToActivity(activity, BankTransactionTableActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_loan_table -> {
                    navigateToActivity(activity, LoanTableActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_sms_test -> {
                    navigateToActivity(activity, SmsDataTestActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_button_test -> {
                    navigateToActivity(activity, ButtonTestActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_realtime_log -> {
                    navigateToActivity(activity, RealTimeLogActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_log_viewer -> {
                    navigateToActivity(activity, LogViewerActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_settings -> {
                    navigateToActivity(activity, SettingsActivity::class.java, currentActivityClass)
                    true
                }
                R.id.nav_about -> {
                    handleAboutMenu(activity)
                    true
                }
                else -> {
                    Log.w(TAG, "알 수 없는 메뉴 클릭: ${menuItem.title} (ID: ${menuItem.itemId})")
                    false
                }
            }
            
            if (handled) {
                drawerLayout.closeDrawers()
            }
            
            handled
        }
    }
    
    /**
     * 액티비티로 네비게이션
     * @param activity 현재 액티비티
     * @param targetActivityClass 이동할 액티비티 클래스
     * @param currentActivityClass 현재 액티비티 클래스
     */
    private fun navigateToActivity(
        activity: AppCompatActivity,
        targetActivityClass: Class<*>,
        currentActivityClass: Class<*>
    ) {
        // 현재 화면이면 아무것도 하지 않음
        if (targetActivityClass == currentActivityClass) {
            Log.d(TAG, "현재 화면이므로 네비게이션하지 않음: ${targetActivityClass.simpleName}")
            return
        }
        
        try {
            Log.d(TAG, "액티비티 이동: ${currentActivityClass.simpleName} → ${targetActivityClass.simpleName}")
            val intent = Intent(activity, targetActivityClass)
            activity.startActivity(intent)
            Log.d(TAG, "${targetActivityClass.simpleName} 시작 성공")
        } catch (e: Exception) {
            Log.e(TAG, "${targetActivityClass.simpleName} 시작 실패: ${e.message}", e)
            Toast.makeText(activity, "${targetActivityClass.simpleName}을(를) 열 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 설정 메뉴 처리
     */
    private fun handleSettingsMenu(activity: AppCompatActivity) {
        Log.d(TAG, "설정 메뉴 클릭")
        Toast.makeText(activity, "설정 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 앱 정보 메뉴 처리
     */
    private fun handleAboutMenu(activity: AppCompatActivity) {
        Log.d(TAG, "앱 정보 메뉴 클릭")
        Toast.makeText(activity, "StatusWindow v1.0\n개발자: SSJ", Toast.LENGTH_LONG).show()
    }
    
    /**
     * 네비게이션 메뉴 아이템 활성화 상태 설정
     * @param navigationView 네비게이션 뷰
     * @param currentActivityClass 현재 액티비티 클래스
     */
    fun setActiveMenuItem(navigationView: NavigationView, currentActivityClass: Class<*>) {
        val menuItemId = when (currentActivityClass) {
            MainActivity::class.java -> R.id.nav_dashboard
            CardDetailsActivity::class.java -> R.id.nav_card_details
            CardTableActivity::class.java -> R.id.nav_card_usage_table
            BankTransactionActivity::class.java -> R.id.nav_bank_transaction
            BankTransactionTableActivity::class.java -> R.id.nav_bank_transaction_table
            LoanTableActivity::class.java -> R.id.nav_loan_table
            SmsDataTestActivity::class.java -> R.id.nav_sms_test
            ButtonTestActivity::class.java -> R.id.nav_button_test
            RealTimeLogActivity::class.java -> R.id.nav_realtime_log
            LogViewerActivity::class.java -> R.id.nav_log_viewer
            SettingsActivity::class.java -> R.id.nav_settings
            else -> null
        }
        
        menuItemId?.let { id ->
            navigationView.menu.findItem(id)?.isChecked = true
            Log.d(TAG, "활성 메뉴 아이템 설정: ${currentActivityClass.simpleName} → $id")
        }
    }
    
    /**
     * 네비게이션 메뉴 아이템 활성화 상태 초기화
     * @param navigationView 네비게이션 뷰
     */
    fun clearActiveMenuItem(navigationView: NavigationView) {
        navigationView.menu.findItem(R.id.nav_dashboard)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_card_details)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_card_usage_table)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_bank_transaction)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_bank_transaction_table)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_loan_table)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_sms_test)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_button_test)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_realtime_log)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_log_viewer)?.isChecked = false
        navigationView.menu.findItem(R.id.nav_settings)?.isChecked = false
        Log.d(TAG, "모든 메뉴 아이템 활성화 상태 초기화")
    }
}
