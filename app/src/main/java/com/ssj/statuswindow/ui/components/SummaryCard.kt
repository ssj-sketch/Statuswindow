package com.ssj.statuswindow.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ssj.statuswindow.R
import java.text.NumberFormat
import java.util.*

/**
 * 재사용 가능한 SummaryCard 컴포넌트
 * KPI 요약 정보를 표시하는 카드
 */
class SummaryCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var ivIcon: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDelta: TextView
    private lateinit var tvPrimaryValue: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvTrend: TextView

    // 상태 열거형
    enum class State {
        LOADING, NORMAL, ERROR
    }

    // 트렌드 열거형
    enum class Trend {
        UP, DOWN, NEUTRAL
    }

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_summary_card, this, true)
        
        ivIcon = findViewById(R.id.ivIcon)
        tvTitle = findViewById(R.id.tvTitle)
        tvDelta = findViewById(R.id.tvDelta)
        tvPrimaryValue = findViewById(R.id.tvPrimaryValue)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvTrend = findViewById(R.id.tvTrend)
    }

    /**
     * 제목 설정
     */
    fun setTitle(title: String) {
        tvTitle.text = title
    }

    /**
     * 아이콘 설정
     */
    fun setIcon(iconRes: Int) {
        ivIcon.setImageResource(iconRes)
    }

    /**
     * 메인 값 설정 (숫자 애니메이션 포함)
     */
    fun setPrimaryValue(value: Long, animate: Boolean = true) {
        if (animate) {
            animateValue(tvPrimaryValue, value)
        } else {
            tvPrimaryValue.text = formatCurrency(value)
        }
    }

    /**
     * 메인 값 설정 (문자열)
     */
    fun setPrimaryValue(value: String) {
        tvPrimaryValue.text = value
    }

    /**
     * 부제목 설정
     */
    fun setSubtitle(subtitle: String) {
        tvSubtitle.text = subtitle
    }

    /**
     * 변화율 설정
     */
    fun setDelta(deltaPercent: Float, trend: Trend) {
        val deltaText = when {
            deltaPercent > 0 -> "+${String.format("%.1f", deltaPercent)}%"
            deltaPercent < 0 -> "${String.format("%.1f", deltaPercent)}%"
            else -> "0%"
        }
        
        tvDelta.text = deltaText
        tvDelta.visibility = VISIBLE
        
        // 배경색과 텍스트 색상 설정
        when (trend) {
            Trend.UP -> {
                tvDelta.setBackgroundResource(R.drawable.chip_background_positive)
                tvDelta.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
            }
            Trend.DOWN -> {
                tvDelta.setBackgroundResource(R.drawable.chip_background_negative)
                tvDelta.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
            }
            Trend.NEUTRAL -> {
                tvDelta.setBackgroundResource(R.drawable.chip_background_neutral)
                tvDelta.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }
    }

    /**
     * 트렌드 아이콘 설정
     */
    fun setTrendIcon(trend: Trend) {
        val trendIcon = when (trend) {
            Trend.UP -> "↗"
            Trend.DOWN -> "↘"
            Trend.NEUTRAL -> "→"
        }
        
        tvTrend.text = trendIcon
        tvTrend.visibility = VISIBLE
        
        // 트렌드 색상 설정
        val trendColor = when (trend) {
            Trend.UP -> ContextCompat.getColor(context, R.color.positive)
            Trend.DOWN -> ContextCompat.getColor(context, R.color.negative)
            Trend.NEUTRAL -> ContextCompat.getColor(context, R.color.text_secondary)
        }
        
        tvTrend.setTextColor(trendColor)
    }

    /**
     * 상태 설정
     */
    fun setState(state: State) {
        when (state) {
            State.LOADING -> {
                tvPrimaryValue.text = "로딩 중..."
                tvDelta.visibility = GONE
                tvTrend.visibility = GONE
            }
            State.ERROR -> {
                tvPrimaryValue.text = "오류"
                tvDelta.visibility = GONE
                tvTrend.visibility = GONE
            }
            State.NORMAL -> {
                // 정상 상태에서는 별도 처리 없음
            }
        }
    }

    /**
     * 숫자 애니메이션
     */
    private fun animateValue(textView: TextView, targetValue: Long) {
        val animator = ValueAnimator.ofInt(0, targetValue.toInt())
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            textView.text = formatCurrency(animatedValue.toLong())
        }
        animator.start()
    }

    /**
     * 통화 포맷팅
     */
    private fun formatCurrency(value: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        return "${formatter.format(value)}원"
    }

    /**
     * 모든 델타 정보 숨기기
     */
    fun hideDelta() {
        tvDelta.visibility = GONE
        tvTrend.visibility = GONE
    }

    /**
     * 모든 델타 정보 보이기
     */
    fun showDelta() {
        tvDelta.visibility = VISIBLE
        tvTrend.visibility = VISIBLE
    }
}

