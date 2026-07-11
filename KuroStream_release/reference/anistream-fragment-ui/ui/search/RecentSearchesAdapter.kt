package com.kurostream.legacyui.anistream.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kurostream.legacyui.anistream.R

class RecentSearchesAdapter(
    private val onSearchClicked: (String) -> Unit,
    private val onClearClicked: (String) -> Unit
) : ListAdapter<String, RecentSearchesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val queryText: TextView = itemView.findViewById(R.id.queryText)
        private val clearButton: ImageButton = itemView.findViewById(R.id.clearButton)

        init {
            itemView.setOnClickListener {
                onSearchClicked(getItem(bindingAdapterPosition))
            }
            clearButton.setOnClickListener {
                onClearClicked(getItem(bindingAdapterPosition))
            }
        }

        fun bind(query: String) {
            queryText.text = query
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
