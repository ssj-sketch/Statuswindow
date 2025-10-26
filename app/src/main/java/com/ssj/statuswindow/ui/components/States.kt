package com.ssj.statuswindow.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ssj.statuswindow.R

/**
 * 재사용 가능한 States 컴포넌트
 * Empty, Error, Loading 상태를 표시
 */
class States @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var ivIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnRetry: Buttons.PrimaryButton

    // 상태 열거형
    enum class State {
        LOADING, EMPTY, ERROR, SUCCESS
    }

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_states, this, true)
        
        ivIcon = findViewById(R.id.ivIcon)
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        btnRetry = findViewById(R.id.btnRetry)
    }

    /**
     * 로딩 상태 설정
     */
    fun setLoadingState(
        title: String = "로딩 중...",
        description: String = "잠시만 기다려주세요"
    ) {
        setState(State.LOADING, title, description)
    }

    /**
     * 빈 상태 설정
     */
    fun setEmptyState(
        title: String = "데이터가 없습니다",
        description: String = "표시할 내용이 없습니다",
        iconRes: Int = R.drawable.ic_launcher_foreground
    ) {
        setState(State.EMPTY, title, description, iconRes)
    }

    /**
     * 오류 상태 설정
     */
    fun setErrorState(
        title: String = "오류가 발생했습니다",
        description: String = "다시 시도해주세요",
        onRetryClick: (() -> Unit)? = null
    ) {
        setState(State.ERROR, title, description, R.drawable.ic_launcher_foreground, onRetryClick)
    }

    /**
     * 성공 상태 설정
     */
    fun setSuccessState(
        title: String = "완료되었습니다",
        description: String = "작업이 성공적으로 완료되었습니다",
        iconRes: Int = R.drawable.ic_launcher_foreground
    ) {
        setState(State.SUCCESS, title, description, iconRes)
    }

    /**
     * 상태 설정 (일반)
     */
    private fun setState(
        state: State,
        title: String,
        description: String,
        iconRes: Int = R.drawable.ic_launcher_foreground,
        onRetryClick: (() -> Unit)? = null
    ) {
        tvTitle.text = title
        tvDescription.text = description
        ivIcon.setImageResource(iconRes)

        when (state) {
            State.LOADING -> {
                ivIcon.visibility = VISIBLE
                btnRetry.visibility = GONE
                // 로딩 애니메이션 추가 가능
            }
            State.EMPTY -> {
                ivIcon.visibility = VISIBLE
                btnRetry.visibility = GONE
            }
            State.ERROR -> {
                ivIcon.visibility = VISIBLE
                btnRetry.visibility = VISIBLE
                btnRetry.text = "다시 시도"
                btnRetry.setOnClickListener { onRetryClick?.invoke() }
            }
            State.SUCCESS -> {
                ivIcon.visibility = VISIBLE
                btnRetry.visibility = GONE
            }
        }
    }

    /**
     * 커스텀 상태 설정
     */
    fun setCustomState(
        title: String,
        description: String,
        iconRes: Int,
        showRetryButton: Boolean = false,
        retryButtonText: String = "다시 시도",
        onRetryClick: (() -> Unit)? = null
    ) {
        tvTitle.text = title
        tvDescription.text = description
        ivIcon.setImageResource(iconRes)
        
        if (showRetryButton) {
            btnRetry.visibility = VISIBLE
            btnRetry.text = retryButtonText
            btnRetry.setOnClickListener { onRetryClick?.invoke() }
        } else {
            btnRetry.visibility = GONE
        }
    }

    /**
     * 아이콘 숨기기
     */
    fun hideIcon() {
        ivIcon.visibility = GONE
    }

    /**
     * 아이콘 보이기
     */
    fun showIcon() {
        ivIcon.visibility = VISIBLE
    }

    /**
     * 재시도 버튼 숨기기
     */
    fun hideRetryButton() {
        btnRetry.visibility = GONE
    }

    /**
     * 재시도 버튼 보이기
     */
    fun showRetryButton() {
        btnRetry.visibility = VISIBLE
    }

    /**
     * 재시도 버튼 텍스트 설정
     */
    fun setRetryButtonText(text: String) {
        btnRetry.text = text
    }

    /**
     * 재시도 버튼 클릭 리스너 설정
     */
    fun setOnRetryClickListener(listener: () -> Unit) {
        btnRetry.setOnClickListener { listener() }
    }
}

