package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.model.CardTransaction
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

/**
 * 카드 거래 내역을 표 형태로 표시하는 어댑터
 */
class CardTransactionAdapter : RecyclerView.Adapter<CardTransactionAdapter.TransactionViewHolder>() {
    
    private var transactions: List<CardTransaction> = emptyList()
    
    fun submitList(newTransactions: List<CardTransaction>) {
        transactions = newTransactions.sortedByDescending { it.transactionDate }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }
    
    override fun getItemCount(): Int = transactions.size
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCardInfo: TextView = itemView.findViewById(R.id.tvCardInfo)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvMerchant: TextView = itemView.findViewById(R.id.tvMerchant)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvUser: TextView = itemView.findViewById(R.id.tvUser)
        
        private val numberFormat = NumberFormat.getNumberInstance()
        private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        
        fun bind(transaction: CardTransaction) {
            // 카드 정보 (카드종류 + 번호)
            tvCardInfo.text = "${transaction.cardType}(${transaction.cardNumber})"
            
            // 금액 (천 단위 구분자 적용)
            tvAmount.text = "${numberFormat.format(transaction.amount)}원"
            
            // 가맹점
            tvMerchant.text = transaction.merchant
            
            // 날짜
            tvDate.text = transaction.transactionDate.format(dateFormatter)
            
            // 사용자
            tvUser.text = transaction.user
        }
    }
}
