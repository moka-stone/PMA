package com.spetsian.pmacalculator.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spetsian.pmacalculator.R
import com.spetsian.pmacalculator.data.HistoryItem

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<HistoryItem>()

    fun submitList(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        holder.expression.text = item.expression
        holder.result.text = "= ${item.result}"
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val expression: TextView = itemView.findViewById(R.id.tvExpression)
        val result: TextView = itemView.findViewById(R.id.tvResult)
    }
}