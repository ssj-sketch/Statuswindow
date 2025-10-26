package com.ssj.statuswindow.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R

/**
 * 재사용 가능한 TransactionList 컴포넌트
 * RecyclerView 기반 거래 목록
 */
class TransactionList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var errorView: TextView
    private lateinit var loadingView: TextView
    
    private lateinit var adapter: TransactionListAdapter

    // 상태 열거형
    enum class State {
        LOADING, EMPTY, ERROR, NORMAL
    }

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.component_transaction_list, this, true)
        
        recyclerView = findViewById(R.id.recyclerView)
        
        // 상태 뷰들 생성
        createStateViews()
        
        // RecyclerView 설정
        setupRecyclerView()
    }

    private fun createStateViews() {
        // Empty View
        emptyView = TextView(context).apply {
            text = "거래 내역이 없습니다"
            textSize = context.resources.getDimension(R.dimen.text_lg) / context.resources.displayMetrics.scaledDensity
            setTextColor(context.getColor(R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
            visibility = GONE
        }
        addView(emptyView)

        // Error View
        errorView = TextView(context).apply {
            text = "데이터를 불러올 수 없습니다"
            textSize = context.resources.getDimension(R.dimen.text_lg) / context.resources.displayMetrics.scaledDensity
            setTextColor(context.getColor(R.color.negative))
            gravity = android.view.Gravity.CENTER
            visibility = GONE
        }
        addView(errorView)

        // Loading View
        loadingView = TextView(context).apply {
            text = "로딩 중..."
            textSize = context.resources.getDimension(R.dimen.text_lg) / context.resources.displayMetrics.scaledDensity
            setTextColor(context.getColor(R.color.text_secondary))
            gravity = android.view.Gravity.CENTER
            visibility = GONE
        }
        addView(loadingView)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TransactionListAdapter()
        recyclerView.adapter = adapter
    }

    /**
     * 거래 목록 설정
     */
    fun setTransactions(transactions: List<TransactionItem>) {
        adapter.updateTransactions(transactions)
        setState(if (transactions.isEmpty()) State.EMPTY else State.NORMAL)
    }

    /**
     * 상태 설정
     */
    fun setState(state: State) {
        when (state) {
            State.LOADING -> {
                recyclerView.visibility = GONE
                emptyView.visibility = GONE
                errorView.visibility = GONE
                loadingView.visibility = VISIBLE
            }
            State.EMPTY -> {
                recyclerView.visibility = GONE
                emptyView.visibility = VISIBLE
                errorView.visibility = GONE
                loadingView.visibility = GONE
            }
            State.ERROR -> {
                recyclerView.visibility = GONE
                emptyView.visibility = GONE
                errorView.visibility = VISIBLE
                loadingView.visibility = GONE
            }
            State.NORMAL -> {
                recyclerView.visibility = VISIBLE
                emptyView.visibility = GONE
                errorView.visibility = GONE
                loadingView.visibility = GONE
            }
        }
    }

    /**
     * 클릭 리스너 설정
     */
    fun setOnTransactionClickListener(listener: (TransactionItem) -> Unit) {
        adapter.setOnTransactionClickListener(listener)
    }

    /**
     * 삭제 리스너 설정
     */
    fun setOnTransactionDeleteListener(listener: (TransactionItem) -> Unit) {
        adapter.setOnTransactionDeleteListener(listener)
    }

    /**
     * 커스텀 어댑터 설정
     */
    fun setCustomAdapter(customAdapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = customAdapter
    }

    /**
     * 레이아웃 매니저 설정
     */
    fun setLayoutManager(layoutManager: RecyclerView.LayoutManager) {
        recyclerView.layoutManager = layoutManager
    }

    /**
     * 아이템 애니메이션 설정
     */
    fun setItemAnimator(itemAnimator: RecyclerView.ItemAnimator?) {
        recyclerView.itemAnimator = itemAnimator
    }

    /**
     * 스크롤 리스너 설정
     */
    fun addOnScrollListener(listener: RecyclerView.OnScrollListener) {
        recyclerView.addOnScrollListener(listener)
    }

    /**
     * 거래 아이템 데이터 클래스
     */
    data class TransactionItem(
        val id: String,
        val merchant: String,
        val category: String? = null,
        val date: String,
        val amount: Long,
        val transactionType: TransactionRow.TransactionType,
        val installmentPeriod: String? = null,
        val memo: String? = null,
        val iconRes: Int? = null
    )
}

