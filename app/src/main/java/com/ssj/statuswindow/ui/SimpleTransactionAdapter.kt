package com.ssj.statuswindow.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.model.CardTransaction
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 간단한 거래 내역 어댑터
 */
class SimpleTransactionAdapter(
    private val transactions: List<CardTransaction>
) : RecyclerView.Adapter<SimpleTransactionAdapter.TransactionViewHolder>() {
    
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvMerchant: TextView = itemView.findViewById(R.id.tvMerchant)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCardNumber: TextView = itemView.findViewById(R.id.tvCardNumber)
        val tvInstallment: TextView = itemView.findViewById(R.id.tvInstallment)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        // 날짜 포맷팅
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        holder.tvDate.text = transaction.transactionDate.format(dateFormatter)
        
        // 가맹점명
        holder.tvMerchant.text = transaction.merchant ?: "알 수 없음"
        
        // 카드명 표시 변경 (신한카드1054 형식)
        val cardName = "${transaction.cardType}${transaction.cardNumber}"
        holder.tvCardNumber.text = cardName
        
        // 금액 및 승인/취소 구분
        val amountText = when (transaction.transactionType) {
            "취소" -> "-${numberFormat.format(transaction.amount)}원"
            else -> "${numberFormat.format(transaction.amount)}원"
        }
        holder.tvAmount.text = amountText
        
        // 할부 정보 표시
        holder.tvInstallment.text = transaction.installment ?: "일시불"
        
        // 거래 타입에 따른 색상 설정
        when (transaction.transactionType) {
            "승인" -> {
                holder.tvAmount.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_red_dark, null))
            }
            "취소" -> {
                holder.tvAmount.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_green_dark, null))
            }
            else -> {
                holder.tvAmount.setTextColor(holder.itemView.context.resources.getColor(android.R.color.black, null))
            }
        }
    }
    
    override fun getItemCount(): Int = transactions.size
}
