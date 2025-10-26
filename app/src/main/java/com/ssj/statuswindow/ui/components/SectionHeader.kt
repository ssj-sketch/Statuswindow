package com.ssj.statuswindow.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ssj.statuswindow.R

/**
 * 재사용 가능한 SectionHeader 컴포넌트
 * 제목과 액션 버튼을 포함하는 섹션 헤더
 */
class SectionHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var ivIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var btnAction: Button

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_section_header, this, true)
        
        ivIcon = findViewById(R.id.ivIcon)
        tvTitle = findViewById(R.id.tvTitle)
        btnAction = findViewById(R.id.btnAction)
    }

    /**
     * 제목 설정
     */
    fun setTitle(title: String) {
        tvTitle.text = title
    }

    /**
     * 제목 설정 (리소스 ID)
     */
    fun setTitle(titleRes: Int) {
        tvTitle.setText(titleRes)
    }

    /**
     * 아이콘 설정
     */
    fun setIcon(iconRes: Int) {
        ivIcon.setImageResource(iconRes)
        ivIcon.visibility = VISIBLE
    }

    /**
     * 아이콘 숨기기
     */
    fun hideIcon() {
        ivIcon.visibility = GONE
    }

    /**
     * 액션 버튼 설정
     */
    fun setActionButton(
        actionText: String,
        onClick: () -> Unit
    ) {
        btnAction.text = actionText
        btnAction.setOnClickListener { onClick() }
        btnAction.visibility = VISIBLE
    }

    /**
     * 액션 버튼 설정 (리소스 ID)
     */
    fun setActionButton(
        actionTextRes: Int,
        onClick: () -> Unit
    ) {
        btnAction.setText(actionTextRes)
        btnAction.setOnClickListener { onClick() }
        btnAction.visibility = VISIBLE
    }

    /**
     * 액션 버튼 숨기기
     */
    fun hideActionButton() {
        btnAction.visibility = GONE
    }

    /**
     * 액션 버튼 활성화/비활성화
     */
    fun setActionButtonEnabled(enabled: Boolean) {
        btnAction.isEnabled = enabled
    }

    /**
     * 액션 버튼 텍스트 업데이트
     */
    fun updateActionButtonText(text: String) {
        btnAction.text = text
    }

    /**
     * 액션 버튼 텍스트 업데이트 (리소스 ID)
     */
    fun updateActionButtonText(textRes: Int) {
        btnAction.setText(textRes)
    }

    /**
     * 전체 헤더 설정 (편의 메서드)
     */
    fun setup(
        title: String,
        iconRes: Int? = null,
        actionText: String? = null,
        onActionClick: (() -> Unit)? = null
    ) {
        setTitle(title)
        
        iconRes?.let { 
            setIcon(it) 
        } ?: hideIcon()
        
        if (actionText != null && onActionClick != null) {
            setActionButton(actionText, onActionClick)
        } else {
            hideActionButton()
        }
    }

    /**
     * 전체 헤더 설정 (리소스 ID 버전)
     */
    fun setup(
        titleRes: Int,
        iconRes: Int? = null,
        actionTextRes: Int? = null,
        onActionClick: (() -> Unit)? = null
    ) {
        setTitle(titleRes)
        
        iconRes?.let { 
            setIcon(it) 
        } ?: hideIcon()
        
        if (actionTextRes != null && onActionClick != null) {
            setActionButton(actionTextRes, onActionClick)
        } else {
            hideActionButton()
        }
    }
}
