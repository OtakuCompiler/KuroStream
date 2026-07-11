package com.kurostream.legacyui.anistream.ui.addons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kurostream.legacyui.anistream.R
import com.kurostream.legacyui.anistream.util.TvUiUtils

class AddonListAdapter(
    private val onAddonFocused: (Addon, View) -> Unit,
    private val onToggleEnabled: (Addon) -> Unit,
    private val onUninstallClicked: (Addon) -> Unit,
    private val onConfigureClicked: (Addon) -> Unit
) : ListAdapter<Addon, AddonListAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImage: ImageView = itemView.findViewById(R.id.addonIcon)
        private val nameText: TextView = itemView.findViewById(R.id.addonName)
        private val versionText: TextView = itemView.findViewById(R.id.addonVersion)
        private val descText: TextView = itemView.findViewById(R.id.addonDescription)
        private val authorText: TextView = itemView.findViewById(R.id.addonAuthor)
        private val enabledSwitch: Switch = itemView.findViewById(R.id.enabledSwitch)
        private val configureButton: ImageButton = itemView.findViewById(R.id.configureButton)
        private val uninstallButton: ImageButton = itemView.findViewById(R.id.uninstallButton)
        private val officialBadge: View = itemView.findViewById(R.id.officialBadge)

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->
                val addon = getItem(bindingAdapterPosition)
                onAddonFocused(addon, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }

            enabledSwitch.setOnCheckedChangeListener { _, _ ->
                val addon = getItem(bindingAdapterPosition)
                onToggleEnabled(addon)
            }

            configureButton.setOnClickListener {
                onConfigureClicked(getItem(bindingAdapterPosition))
            }

            uninstallButton.setOnClickListener {
                onUninstallClicked(getItem(bindingAdapterPosition))
            }
        }

        fun bind(addon: Addon) {
            nameText.text = addon.name
            versionText.text = "v${addon.version}"
            descText.text = addon.description
            authorText.text = "by ${addon.author}"
            enabledSwitch.isChecked = addon.isEnabled
            officialBadge.visibility = if (addon.isOfficial) View.VISIBLE else View.GONE

            iconImage.load(addon.iconUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_extension)
                error(R.drawable.ic_extension)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Addon>() {
        override fun areItemsTheSame(oldItem: Addon, newItem: Addon) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Addon, newItem: Addon) = oldItem == newItem
    }
}
