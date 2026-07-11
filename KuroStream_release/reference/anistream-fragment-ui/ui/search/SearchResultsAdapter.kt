package com.kurostream.legacyui.anistream.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistream.model.AnimeItem
import com.kurostream.legacyui.anistream.util.TvUiUtils

class SearchResultsAdapter(
    private val onItemFocused: (AnimeItem, View) -> Unit,
    private val onItemClicked: (AnimeItem) -> Unit
) : ListAdapter<AnimeItem, SearchResultsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.posterImage)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val yearText: TextView = itemView.findViewById(R.id.yearText)

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val item = getItem(bindingAdapterPosition)
                    onItemFocused(item, itemView)
                }
                TvUiUtils.animateFocus(itemView, hasFocus)
            }

            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                onItemClicked(item)
            }
        }

        fun bind(item: AnimeItem) {
            titleText.text = item.title
            yearText.text = item.year?.toString() ?: ""
            posterImage.load(item.posterUrl) {
                crossfade(true)
                placeholder(R.drawable.poster_placeholder)
                error(R.drawable.poster_error)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AnimeItem>() {
        override fun areItemsTheSame(oldItem: AnimeItem, newItem: AnimeItem) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AnimeItem, newItem: AnimeItem) =
            oldItem == newItem
    }
}
