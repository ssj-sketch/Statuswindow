package com.ssj.statuswindow.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssj.statuswindow.R
import com.ssj.statuswindow.hud.CoachingSuggestion

class CoachingSuggestionAdapter : RecyclerView.Adapter<CoachingSuggestionAdapter.SuggestionViewHolder>() {
    
    private var suggestions: List<CoachingSuggestion> = emptyList()
    
    fun submitList(newSuggestions: List<CoachingSuggestion>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coaching_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }
    
    override fun getItemCount(): Int = suggestions.size
    
    class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvSuggestionTitle)
        private val option1: TextView = itemView.findViewById(R.id.tvSuggestionOption1)
        private val option2: TextView = itemView.findViewById(R.id.tvSuggestionOption2)
        
        fun bind(suggestion: CoachingSuggestion) {
            title.text = suggestion.title
            
            // 옵션들을 설정
            suggestion.options.forEachIndexed { index, option ->
                when (index) {
                    0 -> {
                        option1.text = "• $option"
                        option1.visibility = View.VISIBLE
                    }
                    1 -> {
                        option2.text = "• $option"
                        option2.visibility = View.VISIBLE
                    }
                }
            }
            
            // 사용되지 않는 옵션 숨기기
            if (suggestion.options.size < 2) {
                option2.visibility = View.GONE
            }
            if (suggestion.options.isEmpty()) {
                option1.visibility = View.GONE
            }
        }
    }
}
