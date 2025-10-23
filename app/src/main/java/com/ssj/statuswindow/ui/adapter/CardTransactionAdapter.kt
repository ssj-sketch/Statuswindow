package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
    private var onDeleteClickListener: ((CardTransaction) -> Unit)? = null
    private var onItemClickListener: ((CardTransaction) -> Unit)? = null
    
    fun submitList(newTransactions: List<CardTransaction>) {
        transactions = newTransactions.sortedByDescending { it.transactionDate }
        notifyDataSetChanged()
    }
    
    fun setOnDeleteClickListener(listener: (CardTransaction) -> Unit) {
        onDeleteClickListener = listener
    }
    
    fun setOnItemClickListener(listener: (CardTransaction) -> Unit) {
        onItemClickListener = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
        holder.setOnDeleteClickListener { transaction ->
            onDeleteClickListener?.invoke(transaction)
        }
        holder.setOnItemClickListener { transaction ->
            onItemClickListener?.invoke(transaction)
        }
    }
    
    override fun getItemCount(): Int = transactions.size
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvMerchant: TextView = itemView.findViewById(R.id.tvMerchant)
        private val tvTransactionInfo: TextView = itemView.findViewById(R.id.tvTransactionInfo)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        private val numberFormat = NumberFormat.getNumberInstance()
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        
        private lateinit var transaction: CardTransaction
        
        fun bind(transaction: CardTransaction) {
            this.transaction = transaction
            
            // 가맹점명
            tvMerchant.text = transaction.merchant
            
            // 거래일자 및 상세 정보 (날짜 | 사용자 | 카드번호)
            val transactionInfo = "${transaction.transactionDate.format(dateFormatter)} | ${transaction.user}${transaction.cardNumber}"
            tvTransactionInfo.text = transactionInfo
            
            // 금액 (천 단위 구분자 적용)
            tvAmount.text = "${numberFormat.format(transaction.amount)}원"
        }
        
        fun setOnDeleteClickListener(listener: (CardTransaction) -> Unit) {
            btnDelete.setOnClickListener {
                listener(transaction)
            }
        }
        
        fun setOnItemClickListener(listener: (CardTransaction) -> Unit) {
            itemView.setOnClickListener {
                listener(transaction)
            }
        }
    }
}