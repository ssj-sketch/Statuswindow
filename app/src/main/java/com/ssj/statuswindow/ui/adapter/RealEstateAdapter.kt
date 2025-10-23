package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.databinding.ItemRealEstateBinding
import com.ssj.statuswindow.model.RealEstate
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class RealEstateAdapter(
    private val onItemClick: (RealEstate) -> Unit
) : ListAdapter<RealEstate, RealEstateAdapter.RealEstateViewHolder>(RealEstateDiffCallback()) {

    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RealEstateViewHolder {
        val binding = ItemRealEstateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RealEstateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RealEstateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RealEstateViewHolder(private val binding: ItemRealEstateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(realEstate: RealEstate) {
            binding.tvAddress.text = realEstate.address
            binding.tvEstimatedValue.text = "${nf.format(realEstate.estimatedValue)}원"
            binding.tvPropertyInfo.text = "${realEstate.propertyType} ${realEstate.area}㎡"
            binding.tvLastUpdated.text = realEstate.lastUpdated.format(dateFormatter)
            
            if (realEstate.memo.isNotBlank()) {
                binding.tvMemo.text = realEstate.memo
                binding.tvMemo.visibility = android.view.View.VISIBLE
            } else {
                binding.tvMemo.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(realEstate)
            }
        }
    }

    class RealEstateDiffCallback : DiffUtil.ItemCallback<RealEstate>() {
        override fun areItemsTheSame(oldItem: RealEstate, newItem: RealEstate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RealEstate, newItem: RealEstate): Boolean {
            return oldItem == newItem
        }
    }
}
