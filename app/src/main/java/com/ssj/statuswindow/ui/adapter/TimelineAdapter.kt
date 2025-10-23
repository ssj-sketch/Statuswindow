package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.hud.HudTimelineEvent
import java.time.Duration
import java.time.Instant

class TimelineAdapter : RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder>() {
    
    private var events: List<HudTimelineEvent> = emptyList()
    
    fun submitList(newEvents: List<HudTimelineEvent>) {
        events = newEvents.takeLast(10) // 최근 10개만 표시
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline_event, parent, false)
        return TimelineViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(events[position])
    }
    
    override fun getItemCount(): Int = events.size
    
    class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val message: TextView = itemView.findViewById(R.id.tvTimelineMessage)
        private val time: TextView = itemView.findViewById(R.id.tvTimelineTime)
        
        fun bind(event: HudTimelineEvent) {
            message.text = event.message
            
            // 시간 표시
            val now = Instant.now()
            val duration = Duration.between(event.timestamp, now)
            
            time.text = when {
                duration.toMinutes() < 1 -> "방금 전"
                duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
                duration.toDays() < 1 -> "${duration.toHours()}시간 전"
                else -> "${duration.toDays()}일 전"
            }
        }
    }
}
