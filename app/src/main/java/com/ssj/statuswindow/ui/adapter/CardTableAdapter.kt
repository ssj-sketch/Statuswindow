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
 * 카드테이블 어댑터
 */
class CardTableAdapter(
    private val cardUsages: MutableList<CreditCardUsageEntity>,
    private val onDeleteClick: (CreditCardUsageEntity) -> Unit = {}
) : RecyclerView.Adapter<CardTableAdapter.CardTableViewHolder>() {
    
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    
    class CardTableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvMerchant: TextView = itemView.findViewById(R.id.tvMerchant)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCardName: TextView = itemView.findViewById(R.id.tvCardName)
        val tvInstallment: TextView = itemView.findViewById(R.id.tvInstallment)
        val tvBillingInfo: TextView = itemView.findViewById(R.id.tvBillingInfo)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardTableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_table, parent, false)
        return CardTableViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CardTableViewHolder, position: Int) {
        val cardUsage = cardUsages[position]
        
        // 날짜 포맷팅
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        holder.tvDate.text = cardUsage.transactionDate.format(dateFormatter)
        
        // 가맹점명
        holder.tvMerchant.text = cardUsage.merchant
        
        // 카테고리
        holder.tvCategory.text = cardUsage.merchantCategory ?: "기타"
        
        // 금액 (취소는 음수로 표시)
        val amountText = if (cardUsage.transactionType == "취소") {
            "-${numberFormat.format(cardUsage.amount)}원"
        } else {
            "${numberFormat.format(cardUsage.amount)}원"
        }
        holder.tvAmount.text = amountText
        
        // 색상 설정
        val color = if (cardUsage.transactionType == "취소") {
            android.R.color.holo_green_dark
        } else {
            android.R.color.holo_red_dark
        }
        holder.tvAmount.setTextColor(holder.itemView.context.resources.getColor(color, null))
        
        // 카드명
        holder.tvCardName.text = "${cardUsage.cardType}${cardUsage.cardNumber}"
        
        // 할부 정보
        holder.tvInstallment.text = cardUsage.installment
        
        // 청구 정보
        val billingInfo = "${cardUsage.billingYear}년 ${cardUsage.billingMonth}월 청구: ${numberFormat.format(cardUsage.billingAmount)}원"
        holder.tvBillingInfo.text = billingInfo
        
        // 삭제 버튼 클릭 이벤트
        holder.btnDelete.setOnClickListener {
            onDeleteClick(cardUsage)
        }
    }
    
    override fun getItemCount(): Int = cardUsages.size
}
