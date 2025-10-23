package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.databinding.ItemStockPortfolioBinding
import com.ssj.statuswindow.model.StockPortfolio
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class StockPortfolioAdapter(
    private val onItemClick: (StockPortfolio) -> Unit
) : ListAdapter<StockPortfolio, StockPortfolioAdapter.StockPortfolioViewHolder>(StockPortfolioDiffCallback()) {

    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockPortfolioViewHolder {
        val binding = ItemStockPortfolioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockPortfolioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockPortfolioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StockPortfolioViewHolder(private val binding: ItemStockPortfolioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stockPortfolio: StockPortfolio) {
            binding.tvStockName.text = stockPortfolio.stockName
            binding.tvTotalValue.text = "${nf.format(stockPortfolio.totalValue)}원"
            binding.tvStockInfo.text = "${stockPortfolio.quantity}주 (${nf.format(stockPortfolio.currentPrice)}원)"
            
            // 손익 표시
            val profitLossText = if (stockPortfolio.profitLoss >= 0) {
                "+${nf.format(stockPortfolio.profitLoss)}원 (+${String.format("%.1f", stockPortfolio.profitLossRate)}%)"
            } else {
                "${nf.format(stockPortfolio.profitLoss)}원 (${String.format("%.1f", stockPortfolio.profitLossRate)}%)"
            }
            
            binding.tvProfitLoss.text = profitLossText
            
            // 손익에 따른 색상 변경
            val color = if (stockPortfolio.profitLoss >= 0) {
                ContextCompat.getColor(binding.root.context, R.color.profit_color)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.loss_color)
            }
            binding.tvProfitLoss.setTextColor(color)
            
            binding.tvLastUpdated.text = stockPortfolio.lastUpdated.format(dateFormatter)
            
            if (stockPortfolio.memo.isNotBlank()) {
                binding.tvMemo.text = stockPortfolio.memo
                binding.tvMemo.visibility = android.view.View.VISIBLE
            } else {
                binding.tvMemo.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(stockPortfolio)
            }
        }
    }

    class StockPortfolioDiffCallback : DiffUtil.ItemCallback<StockPortfolio>() {
        override fun areItemsTheSame(oldItem: StockPortfolio, newItem: StockPortfolio): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StockPortfolio, newItem: StockPortfolio): Boolean {
            return oldItem == newItem
        }
    }
}
