package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.entity.CreditCardUsageEntity
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 카드 사용내역 상세 어댑터
 */
class CreditCardDetailAdapter(
    private val transactions: MutableList<CreditCardUsageEntity>,
    private val onDeleteClick: (CreditCardUsageEntity) -> Unit = {},
    private val onEditClick: (CreditCardUsageEntity) -> Unit = {}
) : RecyclerView.Adapter<CreditCardDetailAdapter.TransactionViewHolder>() {
    
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvMerchant: TextView = itemView.findViewById(R.id.tvMerchant)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCardName: TextView = itemView.findViewById(R.id.tvCardName)
        val tvInstallment: TextView = itemView.findViewById(R.id.tvInstallment)
        val tvBillingInfo: TextView = itemView.findViewById(R.id.tvBillingInfo)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_credit_card_detail, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        
        // 날짜 포맷팅
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        holder.tvDate.text = transaction.transactionDate.format(dateFormatter)
        
        // 가맹점명
        holder.tvMerchant.text = transaction.merchant
        
        // 카테고리
        holder.tvCategory.text = transaction.merchantCategory ?: "기타"
        
        // 카드명
        holder.tvCardName.text = transaction.cardName
        
        // 금액 및 승인/취소 구분
        val amountText = when (transaction.transactionType) {
            "취소" -> "-${numberFormat.format(transaction.amount)}원"
            else -> "${numberFormat.format(transaction.amount)}원"
        }
        holder.tvAmount.text = amountText
        
        // 할부 정보
        holder.tvInstallment.text = transaction.installment
        
        // 청구 정보
        val billingText = "${transaction.billingYear}년 ${transaction.billingMonth}월 청구: ${numberFormat.format(transaction.billingAmount)}원"
        holder.tvBillingInfo.text = billingText
        
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
        
        // 삭제 버튼 클릭 이벤트
        holder.btnDelete.setOnClickListener {
            onDeleteClick(transaction)
        }
        
        // 수정 버튼 클릭 이벤트
        holder.btnEdit.setOnClickListener {
            onEditClick(transaction)
        }
    }
    
    override fun getItemCount(): Int = transactions.size
}
