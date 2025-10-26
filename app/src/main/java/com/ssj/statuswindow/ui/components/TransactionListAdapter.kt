package com.ssj.statuswindow.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R

/**
 * TransactionList를 위한 RecyclerView Adapter
 */
class TransactionListAdapter : ListAdapter<TransactionList.TransactionItem, TransactionListAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    private var onTransactionClickListener: ((TransactionList.TransactionItem) -> Unit)? = null
    private var onTransactionDeleteListener: ((TransactionList.TransactionItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val transactionRow = TransactionRow(parent.context)
        return TransactionViewHolder(transactionRow)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction, onTransactionClickListener, onTransactionDeleteListener)
    }

    fun setOnTransactionClickListener(listener: (TransactionList.TransactionItem) -> Unit) {
        onTransactionClickListener = listener
    }

    fun setOnTransactionDeleteListener(listener: (TransactionList.TransactionItem) -> Unit) {
        onTransactionDeleteListener = listener
    }

    fun updateTransactions(transactions: List<TransactionList.TransactionItem>) {
        submitList(transactions)
    }

    class TransactionViewHolder(private val transactionRow: TransactionRow) : RecyclerView.ViewHolder(transactionRow) {
        
        fun bind(
            transaction: TransactionList.TransactionItem,
            onTransactionClickListener: ((TransactionList.TransactionItem) -> Unit)?,
            onTransactionDeleteListener: ((TransactionList.TransactionItem) -> Unit)?
        ) {
            // 거래 정보 설정
            transactionRow.setTransaction(
                merchant = transaction.merchant,
                category = transaction.category,
                date = transaction.date,
                amount = transaction.amount,
                transactionType = transaction.transactionType,
                installmentPeriod = transaction.installmentPeriod,
                memo = transaction.memo
            )

            // 커스텀 아이콘 설정
            transaction.iconRes?.let { iconRes ->
                transactionRow.setCustomIcon(iconRes)
            }

            // 클릭 리스너 설정
            transactionRow.setOnClickListener {
                onTransactionClickListener?.invoke(transaction)
            }

            // 롱 클릭 리스너 설정 (삭제용)
            transactionRow.setOnLongClickListener {
                onTransactionDeleteListener?.invoke(transaction)
                true
            }
        }
    }

    /**
     * DiffUtil Callback for efficient list updates
     */
    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionList.TransactionItem>() {
        override fun areItemsTheSame(
            oldItem: TransactionList.TransactionItem,
            newItem: TransactionList.TransactionItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TransactionList.TransactionItem,
            newItem: TransactionList.TransactionItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}

