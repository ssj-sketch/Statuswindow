package com.ssj.statuswindow.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.ssj.statuswindow.R

/**
 * 재사용 가능한 버튼 컴포넌트들
 * Primary, Secondary, Ghost 스타일 지원
 */
class Buttons @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    /**
     * Primary Button - 주요 액션용
     */
    class PrimaryButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : android.widget.Button(context, attrs, defStyleAttr) {

        init {
            setupPrimaryStyle()
        }

        private fun setupPrimaryStyle() {
            try {
                background = context.getDrawable(R.drawable.button_primary_selector)
                setTextColor(context.getColorStateList(R.color.button_primary_text_color))
                textSize = context.resources.getDimension(R.dimen.text_md) / context.resources.displayMetrics.scaledDensity
                minHeight = context.resources.getDimensionPixelSize(R.dimen.button_height)
                setPadding(
                    context.resources.getDimensionPixelSize(R.dimen.padding_lg),
                    context.resources.getDimensionPixelSize(R.dimen.padding_sm),
                    context.resources.getDimensionPixelSize(R.dimen.padding_lg),
                    context.resources.getDimensionPixelSize(R.dimen.padding_sm)
                )
            } catch (e: Exception) {
                // 리소스 로딩 실패 시 기본 스타일 적용
                android.util.Log.e("PrimaryButton", "스타일 설정 실패: ${e.message}")
                setBackgroundColor(context.getColor(android.R.color.holo_blue_bright))
                setTextColor(context.getColor(android.R.color.white))
            }
        }

        fun setLoading(loading: Boolean) {
            isEnabled = !loading
            text = if (loading) "로딩 중..." else originalText
        }

        private var originalText: String = ""
        override fun setText(text: CharSequence?, type: BufferType?) {
            originalText = text?.toString() ?: ""
            super.setText(text, type)
        }
    }

    /**
     * Secondary Button - 보조 액션용
     */
    class SecondaryButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : Button(context, attrs, defStyleAttr) {

        init {
            setupSecondaryStyle()
        }

        private fun setupSecondaryStyle() {
            background = context.getDrawable(R.drawable.button_secondary_selector)
            setTextColor(context.getColorStateList(R.color.button_secondary_text_color))
            textSize = context.resources.getDimension(R.dimen.text_md) / context.resources.displayMetrics.scaledDensity
            minHeight = context.resources.getDimensionPixelSize(R.dimen.button_height)
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.padding_lg),
                context.resources.getDimensionPixelSize(R.dimen.padding_sm),
                context.resources.getDimensionPixelSize(R.dimen.padding_lg),
                context.resources.getDimensionPixelSize(R.dimen.padding_sm)
            )
        }

        fun setLoading(loading: Boolean) {
            isEnabled = !loading
            text = if (loading) "로딩 중..." else originalText
        }

        private var originalText: String = ""
        override fun setText(text: CharSequence?, type: BufferType?) {
            originalText = text?.toString() ?: ""
            super.setText(text, type)
        }
    }

    /**
     * Ghost Button - 텍스트만 있는 버튼
     */
    class GhostButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : Button(context, attrs, defStyleAttr) {

        init {
            setupGhostStyle()
        }

        private fun setupGhostStyle() {
            background = context.getDrawable(R.drawable.button_ghost_selector)
            setTextColor(context.getColorStateList(R.color.button_ghost_text_color))
            textSize = context.resources.getDimension(R.dimen.text_md) / context.resources.displayMetrics.scaledDensity
            minHeight = context.resources.getDimensionPixelSize(R.dimen.button_height)
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.padding_lg),
                context.resources.getDimensionPixelSize(R.dimen.padding_sm),
                context.resources.getDimensionPixelSize(R.dimen.padding_lg),
                context.resources.getDimensionPixelSize(R.dimen.padding_sm)
            )
        }

        fun setLoading(loading: Boolean) {
            isEnabled = !loading
            text = if (loading) "로딩 중..." else originalText
        }

        private var originalText: String = ""
        override fun setText(text: CharSequence?, type: BufferType?) {
            originalText = text?.toString() ?: ""
            super.setText(text, type)
        }
    }

    /**
     * Loading Button - 로딩 상태를 표시하는 버튼
     */
    class LoadingButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : LinearLayout(context, attrs, defStyleAttr) {

        private lateinit var button: Button
        private lateinit var progressBar: ProgressBar
        private var isLoading = false

        init {
            setupLoadingButton()
        }

        private fun setupLoadingButton() {
            orientation = HORIZONTAL
            gravity = android.view.Gravity.CENTER
            
            // ProgressBar 추가
            progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
                layoutParams = LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.icon_sm),
                    context.resources.getDimensionPixelSize(R.dimen.icon_sm)
                ).apply {
                    marginEnd = context.resources.getDimensionPixelSize(R.dimen.margin_sm)
                }
                visibility = GONE
            }
            addView(progressBar)
            
            // Button 추가
            button = PrimaryButton(context)
            addView(button)
        }

        fun setText(text: String) {
            button.text = text
        }

        fun setButtonClickListener(listener: OnClickListener) {
            button.setOnClickListener(listener)
        }

        fun setLoading(loading: Boolean) {
            isLoading = loading
            button.isEnabled = !loading
            progressBar.visibility = if (loading) VISIBLE else GONE
        }

        override fun isEnabled(): Boolean = button.isEnabled
        override fun setEnabled(enabled: Boolean) {
            button.isEnabled = enabled
        }
    }
}
