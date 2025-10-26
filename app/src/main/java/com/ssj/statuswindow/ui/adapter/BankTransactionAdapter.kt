package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.entity.BankTransactionEntity
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 입출금내역 RecyclerView 어댑터
 */
class BankTransactionAdapter(
    private var transactions: List<BankTransactionEntity> = emptyList()
) : RecyclerView.Adapter<BankTransactionAdapter.BankTransactionViewHolder>() {

    private val formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankTransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bank_transaction, parent, false)
        return BankTransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: BankTransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<BankTransactionEntity>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    inner class BankTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvBalance: TextView = itemView.findViewById(R.id.tvBalance)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvMemo: TextView = itemView.findViewById(R.id.tvMemo)

        fun bind(transaction: BankTransactionEntity) {
            tvDate.text = transaction.transactionDate.format(formatter)
            tvType.text = transaction.transactionType ?: ""
            tvAmount.text = "${numberFormat.format(transaction.amount)}원"
            tvBalance.text = "${numberFormat.format(transaction.balance)}원"
            tvDescription.text = transaction.description
            tvMemo.text = transaction.memo ?: ""

            // 거래 유형에 따른 색상 설정
            when (transaction.transactionType) {
                "입금" -> {
                    tvAmount.setTextColor(itemView.context.getColor(R.color.deposit_color))
                    tvType.setTextColor(itemView.context.getColor(R.color.deposit_color))
                }
                "출금" -> {
                    tvAmount.setTextColor(itemView.context.getColor(R.color.withdrawal_color))
                    tvType.setTextColor(itemView.context.getColor(R.color.withdrawal_color))
                }
                else -> {
                    tvAmount.setTextColor(itemView.context.getColor(android.R.color.black))
                    tvType.setTextColor(itemView.context.getColor(android.R.color.black))
                }
            }
        }
    }
}