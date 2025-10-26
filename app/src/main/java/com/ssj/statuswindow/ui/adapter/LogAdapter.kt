package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.database.entity.LogEntity
import java.time.format.DateTimeFormatter

/**
 * 로그 어댑터
 */
class LogAdapter : ListAdapter<LogEntity, LogAdapter.LogViewHolder>(LogDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLevel: TextView = itemView.findViewById(R.id.tvLevel)
        private val tvTag: TextView = itemView.findViewById(R.id.tvTag)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvStackTrace: TextView = itemView.findViewById(R.id.tvStackTrace)
        
        fun bind(log: LogEntity) {
            tvLevel.text = log.level
            tvTag.text = log.tag
            tvMessage.text = log.message
            tvTimestamp.text = log.timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
            
            // 스택 트레이스가 있으면 표시
            if (!log.stackTrace.isNullOrEmpty()) {
                tvStackTrace.visibility = View.VISIBLE
                tvStackTrace.text = log.stackTrace
            } else {
                tvStackTrace.visibility = View.GONE
            }
            
            // 로그 레벨에 따른 색상 설정
            when (log.level) {
                "ERROR" -> tvLevel.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                "WARN" -> tvLevel.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                "INFO" -> tvLevel.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                "DEBUG" -> tvLevel.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                else -> tvLevel.setTextColor(itemView.context.getColor(android.R.color.black))
            }
        }
    }
    
    class LogDiffCallback : DiffUtil.ItemCallback<LogEntity>() {
        override fun areItemsTheSame(oldItem: LogEntity, newItem: LogEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LogEntity, newItem: LogEntity): Boolean {
            return oldItem == newItem
        }
    }
}

