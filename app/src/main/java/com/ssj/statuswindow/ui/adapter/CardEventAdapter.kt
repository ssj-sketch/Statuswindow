package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.databinding.ItemCardEventBinding
import com.ssj.statuswindow.model.CardEvent
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

class CardEventAdapter :
    ListAdapter<CardEvent, CardEventAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CardEvent>() {
            override fun areItemsTheSame(oldItem: CardEvent, newItem: CardEvent): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CardEvent, newItem: CardEvent): Boolean =
                oldItem == newItem
        }

        private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)

        private fun formatAmount(amount: Long): String {
            val body = nf.format(amount.absoluteValue)
            return if (amount < 0) "-$body" else body
        }

        private fun formatInstallment(months: Int?): String =
            when {
                months == null -> "-"                   // 미제공
                months == 0 -> "일시불"
                months > 0 -> "${months}개월"
                else -> "-"                             // 방어
            }

        private fun formatCard(brand: String?, last4: String?): String =
            when {
                !brand.isNullOrBlank() && !last4.isNullOrBlank() -> "$brand($last4)"
                !brand.isNullOrBlank() -> brand
                !last4.isNullOrBlank() -> "****($last4)"
                else -> "-"
            }

        private fun safe(text: String?): String = if (text.isNullOrBlank()) "-" else text
    }

    inner class VH(val b: ItemCardEventBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCardEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        // 1행
        holder.b.tvTime.text = item.time
        holder.b.tvMerchant.text = item.merchant
        holder.b.tvAmount.text = formatAmount(item.amount)

        // 2행
        holder.b.tvCard.text = formatCard(item.cardBrand, item.cardLast4)
        holder.b.tvInstallment.text = formatInstallment(item.installmentMonths)
        holder.b.tvCategory.text = safe(item.category)
        holder.b.tvSource.text = item.sourceApp
    }
}
