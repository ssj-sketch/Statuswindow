package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.entity.BankTransactionEntity
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 소득 상세 어댑터
 */
class IncomeDetailAdapter(
    private val transactions: MutableList<BankTransactionEntity>,
    private val onDeleteClick: (BankTransactionEntity) -> Unit = {}
) : RecyclerView.Adapter<IncomeDetailAdapter.TransactionViewHolder>() {
    
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvBankName: TextView = itemView.findViewById(R.id.tvBankName)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvBalance: TextView = itemView.findViewById(R.id.tvBalance)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_income_detail, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        // 날짜 포맷팅
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        holder.tvDate.text = transaction.transactionDate.format(dateFormatter)
        
        // 은행명
        holder.tvBankName.text = transaction.bankName
        
        // 거래내용
        holder.tvDescription.text = transaction.description
        
        // 금액 (입금이므로 양수로 표시)
        holder.tvAmount.text = "+${numberFormat.format(transaction.amount)}원"
        holder.tvAmount.setTextColor(holder.itemView.context.resources.getColor(android.R.color.holo_green_dark, null))
        
        // 잔액
        holder.tvBalance.text = "잔액: ${numberFormat.format(transaction.balance)}원"
        
        // 삭제 버튼 클릭 이벤트
        holder.btnDelete.setOnClickListener {
            onDeleteClick(transaction)
        }
    }
    
    override fun getItemCount(): Int = transactions.size
}
