package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.databinding.ItemBankBalanceBinding
import com.ssj.statuswindow.model.BankBalance
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class BankBalanceAdapter(
    private val onItemClick: (BankBalance) -> Unit
) : ListAdapter<BankBalance, BankBalanceAdapter.BankBalanceViewHolder>(BankBalanceDiffCallback()) {

    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankBalanceViewHolder {
        val binding = ItemBankBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BankBalanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BankBalanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BankBalanceViewHolder(private val binding: ItemBankBalanceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bankBalance: BankBalance) {
            binding.tvBankName.text = bankBalance.bankName
            binding.tvBalance.text = "${nf.format(bankBalance.balance)}원"
            binding.tvAccountInfo.text = "${bankBalance.accountType} ${bankBalance.accountNumber}"
            binding.tvLastUpdated.text = bankBalance.lastUpdated.format(dateFormatter)
            
            if (bankBalance.memo.isNotBlank()) {
                binding.tvMemo.text = bankBalance.memo
                binding.tvMemo.visibility = android.view.View.VISIBLE
            } else {
                binding.tvMemo.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(bankBalance)
            }
        }
    }

    class BankBalanceDiffCallback : DiffUtil.ItemCallback<BankBalance>() {
        override fun areItemsTheSame(oldItem: BankBalance, newItem: BankBalance): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BankBalance, newItem: BankBalance): Boolean {
            return oldItem == newItem
        }
    }
}
