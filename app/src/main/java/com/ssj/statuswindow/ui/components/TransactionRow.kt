package com.ssj.statuswindow.ui.components

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
 * 재사용 가능한 TransactionRow 컴포넌트
 * 거래 내역을 표시하는 단일 행
 */
class TransactionRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var ivIcon: ImageView
    private lateinit var tvMerchant: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvMemo: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvInstallment: TextView

    // 거래 타입 열거형
    enum class TransactionType {
        CARD_PURCHASE, CARD_CANCEL, BANK_DEPOSIT, BANK_WITHDRAWAL
    }

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_transaction_row, this, true)
        
        ivIcon = findViewById(R.id.ivIcon)
        tvMerchant = findViewById(R.id.tvMerchant)
        tvCategory = findViewById(R.id.tvCategory)
        tvDate = findViewById(R.id.tvDate)
        tvMemo = findViewById(R.id.tvMemo)
        tvAmount = findViewById(R.id.tvAmount)
        tvInstallment = findViewById(R.id.tvInstallment)
    }

    /**
     * 거래 정보 설정
     */
    fun setTransaction(
        merchant: String,
        category: String? = null,
        date: String,
        amount: Long,
        transactionType: TransactionType,
        installmentPeriod: String? = null,
        memo: String? = null
    ) {
        setMerchant(merchant)
        setCategory(category)
        setDate(date)
        setAmount(amount, transactionType)
        setInstallmentPeriod(installmentPeriod)
        setMemo(memo)
        setTransactionIcon(transactionType)
    }

    /**
     * 상호명 설정
     */
    fun setMerchant(merchant: String) {
        tvMerchant.text = merchant
    }

    /**
     * 카테고리 설정
     */
    fun setCategory(category: String?) {
        if (category != null && category.isNotEmpty()) {
            tvCategory.text = category
            tvCategory.visibility = VISIBLE
        } else {
            tvCategory.visibility = GONE
        }
    }

    /**
     * 날짜 설정
     */
    fun setDate(date: String) {
        tvDate.text = date
    }

    /**
     * 금액 설정
     */
    fun setAmount(amount: Long, transactionType: TransactionType) {
        val formattedAmount = formatCurrency(amount)
        tvAmount.text = formattedAmount
        
        // 거래 타입에 따른 색상 설정
        val amountColor = when (transactionType) {
            TransactionType.CARD_PURCHASE, TransactionType.BANK_WITHDRAWAL -> 
                ContextCompat.getColor(context, R.color.negative)
            TransactionType.CARD_CANCEL, TransactionType.BANK_DEPOSIT -> 
                ContextCompat.getColor(context, R.color.positive)
        }
        
        tvAmount.setTextColor(amountColor)
    }

    /**
     * 할부 기간 설정
     */
    fun setInstallmentPeriod(installmentPeriod: String?) {
        if (installmentPeriod != null && installmentPeriod.isNotEmpty()) {
            tvInstallment.text = installmentPeriod
            tvInstallment.visibility = VISIBLE
        } else {
            tvInstallment.visibility = GONE
        }
    }

    /**
     * 메모 설정
     */
    fun setMemo(memo: String?) {
        if (memo != null && memo.isNotEmpty()) {
            tvMemo.text = memo
            tvMemo.visibility = VISIBLE
        } else {
            tvMemo.visibility = GONE
        }
    }

    /**
     * 거래 타입에 따른 아이콘 설정
     */
    fun setTransactionIcon(transactionType: TransactionType) {
        val iconRes = when (transactionType) {
            TransactionType.CARD_PURCHASE -> R.drawable.ic_launcher_foreground // 카드 아이콘
            TransactionType.CARD_CANCEL -> R.drawable.ic_launcher_foreground // 취소 아이콘
            TransactionType.BANK_DEPOSIT -> R.drawable.ic_launcher_foreground // 입금 아이콘
            TransactionType.BANK_WITHDRAWAL -> R.drawable.ic_launcher_foreground // 출금 아이콘
        }
        
        ivIcon.setImageResource(iconRes)
    }

    /**
     * 커스텀 아이콘 설정
     */
    fun setCustomIcon(iconRes: Int) {
        ivIcon.setImageResource(iconRes)
    }

    /**
     * 통화 포맷팅
     */
    private fun formatCurrency(value: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        return "${formatter.format(value)}원"
    }

    /**
     * 카테고리 표시/숨기기
     */
    fun showCategory(show: Boolean) {
        tvCategory.visibility = if (show) VISIBLE else GONE
    }

    /**
     * 메모 표시/숨기기
     */
    fun showMemo(show: Boolean) {
        tvMemo.visibility = if (show) VISIBLE else GONE
    }

    /**
     * 할부 정보 표시/숨기기
     */
    fun showInstallment(show: Boolean) {
        tvInstallment.visibility = if (show) VISIBLE else GONE
    }

    /**
     * 전체 정보 숨기기 (로딩 상태용)
     */
    fun setLoadingState() {
        tvMerchant.text = "로딩 중..."
        tvDate.text = ""
        tvAmount.text = ""
        tvCategory.visibility = GONE
        tvMemo.visibility = GONE
        tvInstallment.visibility = GONE
    }

    /**
     * 빈 상태 표시
     */
    fun setEmptyState() {
        tvMerchant.text = "거래 내역이 없습니다"
        tvDate.text = ""
        tvAmount.text = ""
        tvCategory.visibility = GONE
        tvMemo.visibility = GONE
        tvInstallment.visibility = GONE
    }
}

