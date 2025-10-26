package com.ssj.statuswindow.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ssj.statuswindow.R

/**
 * 재사용 가능한 MetricChip 컴포넌트
 * 변화율이나 지표를 표시하는 작은 칩
 */
class MetricChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    // 톤 열거형
    enum class Tone {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    init {
        initView()
    }

    private fun initView() {
        // 기본 스타일 설정
        textSize = context.resources.getDimension(R.dimen.text_sm) / context.resources.displayMetrics.scaledDensity
        setPadding(
            context.resources.getDimensionPixelSize(R.dimen.padding_xs),
            context.resources.getDimensionPixelSize(R.dimen.padding_xs),
            context.resources.getDimensionPixelSize(R.dimen.padding_xs),
            context.resources.getDimensionPixelSize(R.dimen.padding_xs)
        )
        gravity = android.view.Gravity.CENTER
        minWidth = context.resources.getDimensionPixelSize(R.dimen.touch_target_min)
        minHeight = context.resources.getDimensionPixelSize(R.dimen.touch_target_min)
    }

    /**
     * 값과 톤 설정
     */
    fun setValue(valueText: String, tone: Tone) {
        text = valueText
        setTone(tone)
    }

    /**
     * 톤 설정 (색상 변경)
     */
    fun setTone(tone: Tone) {
        when (tone) {
            Tone.POSITIVE -> {
                background = ContextCompat.getDrawable(context, R.drawable.chip_background_positive)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
            }
            Tone.NEGATIVE -> {
                background = ContextCompat.getDrawable(context, R.drawable.chip_background_negative)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
            }
            Tone.NEUTRAL -> {
                background = ContextCompat.getDrawable(context, R.drawable.chip_background_neutral)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }
    }

    /**
     * 변화율 설정 (자동 톤 결정)
     */
    fun setDeltaPercent(deltaPercent: Float) {
        val deltaText = when {
            deltaPercent > 0 -> "+${String.format("%.1f", deltaPercent)}%"
            deltaPercent < 0 -> "${String.format("%.1f", deltaPercent)}%"
            else -> "0%"
        }
        
        val tone = when {
            deltaPercent > 0 -> Tone.POSITIVE
            deltaPercent < 0 -> Tone.NEGATIVE
            else -> Tone.NEUTRAL
        }
        
        setValue(deltaText, tone)
    }

    /**
     * 커스텀 배경 설정
     */
    fun setCustomBackground(backgroundRes: Int, textColorRes: Int) {
        background = ContextCompat.getDrawable(context, backgroundRes)
        setTextColor(ContextCompat.getColor(context, textColorRes))
    }

    /**
     * 칩 크기 설정
     */
    fun setSize(size: ChipSize) {
        when (size) {
            ChipSize.SMALL -> {
                textSize = context.resources.getDimension(R.dimen.text_xs) / context.resources.displayMetrics.scaledDensity
                setPadding(
                    context.resources.getDimensionPixelSize(R.dimen.padding_xs),
                    context.resources.getDimensionPixelSize(R.dimen.padding_xs),
                    context.resources.getDimensionPixelSize(R.dimen.padding_xs),
                    context.resources.getDimensionPixelSize(R.dimen.padding_xs)
                )
            }
            ChipSize.MEDIUM -> {
                textSize = context.resources.getDimension(R.dimen.text_sm) / context.resources.displayMetrics.scaledDensity
                setPadding(
                    context.resources.getDimensionPixelSize(R.dimen.padding_sm),
                    context.resources.getDimensionPixelSize(R.dimen.padding_sm),
                    context.resources.getDimensionPixelSize(R.dimen.padding_sm),
                    context.resources.getDimensionPixelSize(R.dimen.padding_sm)
                )
            }
            ChipSize.LARGE -> {
                textSize = context.resources.getDimension(R.dimen.text_md) / context.resources.displayMetrics.scaledDensity
                setPadding(
                    context.resources.getDimensionPixelSize(R.dimen.padding_md),
                    context.resources.getDimensionPixelSize(R.dimen.padding_md),
                    context.resources.getDimensionPixelSize(R.dimen.padding_md),
                    context.resources.getDimensionPixelSize(R.dimen.padding_md)
                )
            }
        }
    }

    /**
     * 칩 크기 열거형
     */
    enum class ChipSize {
        SMALL, MEDIUM, LARGE
    }
}
