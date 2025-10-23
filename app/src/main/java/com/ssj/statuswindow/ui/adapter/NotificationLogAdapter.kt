package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.databinding.ItemNotificationLogBinding
import com.ssj.statuswindow.model.AppNotificationLog

class NotificationLogAdapter :
    ListAdapter<AppNotificationLog, NotificationLogAdapter.NotificationLogViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationLogViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNotificationLogBinding.inflate(inflater, parent, false)
        return NotificationLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationLogViewHolder(
        private val binding: ItemNotificationLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppNotificationLog) {
            binding.tvAppName.text = item.appName
            binding.tvAppCategory.text = item.appCategory
            binding.tvTimestamp.text = item.postedAtIso
            binding.tvContent.text = item.content

            val hasCategory = !item.notificationCategory.isNullOrBlank()
            binding.tvNotificationCategory.apply {
                if (hasCategory) {
                    text = item.notificationCategory
                    visibility = View.VISIBLE
                } else {
                    text = ""
                    visibility = View.GONE
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AppNotificationLog>() {
        override fun areItemsTheSame(oldItem: AppNotificationLog, newItem: AppNotificationLog): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AppNotificationLog, newItem: AppNotificationLog): Boolean =
            oldItem == newItem
    }
}
