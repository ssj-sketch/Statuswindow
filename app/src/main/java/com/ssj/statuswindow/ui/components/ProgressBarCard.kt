package com.ssj.statuswindow.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ssj.statuswindow.R
import java.text.NumberFormat
import java.util.*

/**
 * 재사용 가능한 ProgressBarCard 컴포넌트
 * 소비 진척도나 목표 달성률을 표시하는 카드
 */
class ProgressBarCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var tvTitle: TextView
    private lateinit var tvPercent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCurrent: TextView
    private lateinit var tvTarget: TextView
    private lateinit var tvSubtitle: TextView

    // 프로그레스 타입 열거형
    enum class ProgressType {
        SPENDING, SAVING, GOAL, CUSTOM
    }

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_progress_bar_card, this, true)
        
        tvTitle = findViewById(R.id.tvTitle)
        tvPercent = findViewById(R.id.tvPercent)
        progressBar = findViewById(R.id.progressBar)
        tvCurrent = findViewById(R.id.tvCurrent)
        tvTarget = findViewById(R.id.tvTarget)
        tvSubtitle = findViewById(R.id.tvSubtitle)
    }

    /**
     * 기본 설정
     */
    fun setup(
        title: String,
        current: Long,
        target: Long,
        progressType: ProgressType = ProgressType.CUSTOM,
        subtitle: String? = null
    ) {
        setTitle(title)
        setCurrentValue(current)
        setTargetValue(target)
        setProgressType(progressType)
        subtitle?.let { setSubtitle(it) }
        updateProgress()
    }

    /**
     * 제목 설정
     */
    fun setTitle(title: String) {
        tvTitle.text = title
    }

    /**
     * 현재 값 설정
     */
    fun setCurrentValue(current: Long) {
        tvCurrent.text = "현재: ${formatCurrency(current)}"
    }

    /**
     * 목표 값 설정
     */
    fun setTargetValue(target: Long) {
        tvTarget.text = "목표: ${formatCurrency(target)}"
    }

    /**
     * 프로그레스 타입 설정 (색상 변경)
     */
    fun setProgressType(progressType: ProgressType) {
        val progressColor = when (progressType) {
            ProgressType.SPENDING -> R.color.negative
            ProgressType.SAVING -> R.color.positive
            ProgressType.GOAL -> R.color.primary
            ProgressType.CUSTOM -> R.color.primary
        }
        
        progressBar.progressTintList = ContextCompat.getColorStateList(context, progressColor)
    }

    /**
     * 부제목 설정
     */
    fun setSubtitle(subtitle: String) {
        tvSubtitle.text = subtitle
        tvSubtitle.visibility = VISIBLE
    }

    /**
     * 부제목 숨기기
     */
    fun hideSubtitle() {
        tvSubtitle.visibility = GONE
    }

    /**
     * 프로그레스 업데이트
     */
    fun updateProgress() {
        val currentText = tvCurrent.text.toString()
        val targetText = tvTarget.text.toString()
        
        val current = extractNumberFromText(currentText)
        val target = extractNumberFromText(targetText)
        
        if (target > 0) {
            val percent = ((current.toFloat() / target.toFloat()) * 100).toInt()
            val clampedPercent = percent.coerceIn(0, 100)
            
            animateProgress(clampedPercent)
            tvPercent.text = "${clampedPercent}%"
        } else {
            progressBar.progress = 0
            tvPercent.text = "0%"
        }
    }

    /**
     * 프로그레스 애니메이션
     */
    private fun animateProgress(targetProgress: Int) {
        val animator = ValueAnimator.ofInt(progressBar.progress, targetProgress)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            progressBar.progress = animatedValue
        }
        animator.start()
    }

    /**
     * 텍스트에서 숫자 추출
     */
    private fun extractNumberFromText(text: String): Long {
        return try {
            val numberText = text.replace(Regex("[^0-9]"), "")
            if (numberText.isNotEmpty()) numberText.toLong() else 0L
        } catch (e: NumberFormatException) {
            0L
        }
    }

    /**
     * 통화 포맷팅
     */
    private fun formatCurrency(value: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        return "${formatter.format(value)}원"
    }

    /**
     * 프로그레스 직접 설정
     */
    fun setProgress(percent: Int, animate: Boolean = true) {
        val clampedPercent = percent.coerceIn(0, 100)
        
        if (animate) {
            animateProgress(clampedPercent)
        } else {
            progressBar.progress = clampedPercent
        }
        
        tvPercent.text = "${clampedPercent}%"
    }

    /**
     * 커스텀 색상 설정
     */
    fun setCustomProgressColor(colorRes: Int) {
        progressBar.progressTintList = ContextCompat.getColorStateList(context, colorRes)
    }

    /**
     * 프로그레스 바 높이 설정
     */
    fun setProgressBarHeight(heightDp: Int) {
        val heightPx = (heightDp * context.resources.displayMetrics.density).toInt()
        progressBar.layoutParams.height = heightPx
        progressBar.requestLayout()
    }

    /**
     * 로딩 상태 설정
     */
    fun setLoadingState() {
        tvTitle.text = "로딩 중..."
        tvCurrent.text = "현재: 계산 중..."
        tvTarget.text = "목표: 계산 중..."
        tvPercent.text = "0%"
        progressBar.progress = 0
        hideSubtitle()
    }

    /**
     * 오류 상태 설정
     */
    fun setErrorState() {
        tvTitle.text = "오류"
        tvCurrent.text = "현재: 오류"
        tvTarget.text = "목표: 오류"
        tvPercent.text = "0%"
        progressBar.progress = 0
        hideSubtitle()
    }
}
