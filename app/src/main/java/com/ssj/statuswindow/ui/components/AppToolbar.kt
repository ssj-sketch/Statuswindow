package com.ssj.statuswindow.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.ssj.statuswindow.R

/**
 * 재사용 가능한 AppToolbar 컴포넌트
 * 햄버거 버튼, 타이틀, 액션 버튼을 포함
 */
class AppToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var toolbar: Toolbar
    private var drawerToggle: ActionBarDrawerToggle? = null

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_app_toolbar, this, true)
        toolbar = findViewById(R.id.toolbar)
    }

    /**
     * 햄버거 버튼과 드로어 레이아웃 연결
     */
    fun setupWithDrawer(
        activity: androidx.appcompat.app.AppCompatActivity,
        drawerLayout: DrawerLayout
    ) {
        activity.setSupportActionBar(toolbar)
        
        drawerToggle = ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ).apply {
            isDrawerIndicatorEnabled = true
            drawerLayout.addDrawerListener(this)
            syncState()
        }
    }

    /**
     * 타이틀 설정
     */
    fun setTitle(title: String) {
        toolbar.title = title
    }

    /**
     * 타이틀 설정 (리소스 ID)
     */
    fun setTitle(titleRes: Int) {
        toolbar.setTitle(titleRes)
    }

    /**
     * 메뉴 설정
     */
    fun setMenu(menuRes: Int, onMenuItemClick: (MenuItem) -> Boolean) {
        toolbar.inflateMenu(menuRes)
        toolbar.setOnMenuItemClickListener(onMenuItemClick)
    }

    /**
     * 액션 버튼 추가
     */
    fun addActionButton(
        iconRes: Int,
        onClick: () -> Unit
    ) {
        toolbar.menu.add(0, iconRes, 0, "")
            .setIcon(iconRes)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == iconRes) {
                onClick()
                true
            } else {
                false
            }
        }
    }

    /**
     * 뒤로가기 버튼 활성화
     */
    fun enableBackButton(onBackClick: () -> Unit) {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { onBackClick() }
    }

    /**
     * 컴포넌트 정리
     */
    fun cleanup() {
        drawerToggle?.let { toggle ->
            // DrawerLayout에서 리스너 제거는 호출하는 쪽에서 처리
        }
    }
}

