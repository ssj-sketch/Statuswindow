package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.hud.StatModifier
import com.ssj.statuswindow.hud.ModifierKind
import java.time.Duration
import java.time.Instant

class ModifierAdapter : RecyclerView.Adapter<ModifierAdapter.ModifierViewHolder>() {
    
    private var modifiers: List<StatModifier> = emptyList()
    
    fun submitList(newModifiers: List<StatModifier>) {
        modifiers = newModifiers.filter { it.isActive(Instant.now()) }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModifierViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_modifier, parent, false)
        return ModifierViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ModifierViewHolder, position: Int) {
        holder.bind(modifiers[position])
    }
    
    override fun getItemCount(): Int = modifiers.size
    
    class ModifierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.ivModifierIcon)
        private val description: TextView = itemView.findViewById(R.id.tvModifierDescription)
        private val expires: TextView = itemView.findViewById(R.id.tvModifierExpires)
        private val type: TextView = itemView.findViewById(R.id.tvModifierType)
        
        fun bind(modifier: StatModifier) {
            val sign = if (modifier.magnitude >= 0) "+" else ""
            val magnitudeText = "${sign}${modifier.magnitude.toInt()}"
            val statName = modifier.stat.displayName
            
            description.text = "$statName $magnitudeText (${modifier.description})"
            
            // 만료 시간 표시
            modifier.expiresAt?.let { expiresAt ->
                val now = Instant.now()
                val duration = Duration.between(now, expiresAt)
                val hours = duration.toHours()
                val minutes = duration.toMinutesPart()
                
                expires.text = when {
                    hours > 0 -> "${hours}시간 ${minutes}분 남음"
                    minutes > 0 -> "${minutes}분 남음"
                    else -> "곧 만료"
                }
            } ?: run {
                expires.text = "영구 지속"
            }
            
            // 버프/디버프 타입에 따른 색상 설정
            when (modifier.kind) {
                ModifierKind.BUFF -> {
                    type.text = "버프"
                    type.setBackgroundResource(R.drawable.modifier_type_background)
                }
                ModifierKind.DEBUFF -> {
                    type.text = "디버프"
                    type.setBackgroundColor(itemView.context.getColor(R.color.debuff_color))
                }
            }
            
            // 아이콘 설정 (간단한 예시)
            icon.setImageResource(
                when (modifier.kind) {
                    ModifierKind.BUFF -> android.R.drawable.ic_input_add
                    ModifierKind.DEBUFF -> android.R.drawable.ic_delete
                }
            )
        }
    }
}
