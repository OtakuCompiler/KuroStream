package com.kurostream.legacyui.anistream.ui.addons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kurostream.legacyui.anistream.R
import com.kurostream.legacyui.anistream.util.TvUiUtils

class MarketplaceAdapter(
    private val onAddonFocused: (MarketplaceAddon, View) -> Unit,
    private val onInstallClicked: (MarketplaceAddon) -> Unit,
    private val onPreviewClicked: (MarketplaceAddon) -> Unit
) : ListAdapter<MarketplaceAddon, MarketplaceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marketplace_addon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImage: ImageView = itemView.findViewById(R.id.addonIcon)
        private val nameText: TextView = itemView.findViewById(R.id.addonName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val downloadCountText: TextView = itemView.findViewById(R.id.downloadCount)
        private val descText: TextView = itemView.findViewById(R.id.addonDescription)
        private val authorText: TextView = itemView.findViewById(R.id.addonAuthor)
        private val installButton: Button = itemView.findViewById(R.id.installButton)
        private val previewButton: Button = itemView.findViewById(R.id.previewButton)
        private val officialBadge: View = itemView.findViewById(R.id.officialBadge)

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->
                val addon = getItem(bindingAdapterPosition)
                onAddonFocused(addon, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }

            installButton.setOnClickListener {
                onInstallClicked(getItem(bindingAdapterPosition))
            }

            previewButton.setOnClickListener {
                onPreviewClicked(getItem(bindingAdapterPosition))
            }
        }

        fun bind(addon: MarketplaceAddon) {
            nameText.text = addon.name
            ratingBar.rating = addon.rating
            downloadCountText.text = "${addon.downloadCount} downloads"
            descText.text = addon.description
            authorText.text = "by ${addon.author}"
            officialBadge.visibility = if (addon.isOfficial) View.VISIBLE else View.GONE

            iconImage.load(addon.iconUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_extension)
                error(R.drawable.ic_extension)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MarketplaceAddon>() {
        override fun areItemsTheSame(oldItem: MarketplaceAddon, newItem: MarketplaceAddon) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MarketplaceAddon, newItem: MarketplaceAddon) = oldItem == newItem
    }
}
