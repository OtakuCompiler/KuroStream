package com.kurostream.legacyui.anistream.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kurostream.legacyui.anistream.R
import com.kurostream.legacyui.anistream.util.TvUiUtils

class SettingsAdapter(
    private val onSettingFocused: (SettingItem, View) -> Unit,
    private val onSettingClicked: (SettingItem) -> Unit,
    private val onToggleChanged: (SettingItem.Toggle, Boolean) -> Unit,
    private val onValueSelected: (SettingItem, String) -> Unit
) : ListAdapter<SettingItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TOGGLE = 1
        private const val TYPE_NAVIGATE = 2
        private const val TYPE_ACTION = 3
        private const val TYPE_THEME = 4
        private const val TYPE_DROPDOWN = 5
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SettingItem.Header -> TYPE_HEADER
            is SettingItem.Toggle -> TYPE_TOGGLE
            is SettingItem.Navigate -> TYPE_NAVIGATE
            is SettingItem.Action -> TYPE_ACTION
            is SettingItem.ThemeSelector -> TYPE_THEME
            is SettingItem.Dropdown -> TYPE_DROPDOWN
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_setting_header, parent, false)
            )
            TYPE_TOGGLE -> ToggleViewHolder(
                inflater.inflate(R.layout.item_setting_toggle, parent, false)
            )
            TYPE_NAVIGATE -> NavigateViewHolder(
                inflater.inflate(R.layout.item_setting_navigate, parent, false)
            )
            TYPE_ACTION -> ActionViewHolder(
                inflater.inflate(R.layout.item_setting_action, parent, false)
            )
            TYPE_THEME -> ThemeViewHolder(
                inflater.inflate(R.layout.item_setting_theme, parent, false)
            )
            TYPE_DROPDOWN -> DropdownViewHolder(
                inflater.inflate(R.layout.item_setting_dropdown, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SettingItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SettingItem.Toggle -> (holder as ToggleViewHolder).bind(item)
            is SettingItem.Navigate -> (holder as NavigateViewHolder).bind(item)
            is SettingItem.Action -> (holder as ActionViewHolder).bind(item)
            is SettingItem.ThemeSelector -> (holder as ThemeViewHolder).bind(item)
            is SettingItem.Dropdown -> (holder as DropdownViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(item: SettingItem.Header) {
            titleText.text = item.title
        }
    }

    inner class ToggleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitleText)
        private val toggleSwitch: Switch = itemView.findViewById(R.id.toggleSwitch)

        init {
            itemView.setOnClickListener {
                toggleSwitch.isChecked = !toggleSwitch.isChecked
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                val item = getItem(bindingAdapterPosition) as SettingItem.Toggle
                onSettingFocused(item, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }

            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                val item = getItem(bindingAdapterPosition) as SettingItem.Toggle
                onToggleChanged(item, isChecked)
            }
        }

        fun bind(item: SettingItem.Toggle) {
            titleText.text = item.title
            subtitleText.text = item.subtitle
            toggleSwitch.isChecked = item.isChecked
        }
    }

    inner class NavigateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitleText)
        private val arrowIcon: ImageView = itemView.findViewById(R.id.arrowIcon)

        init {
            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                onSettingClicked(item)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                val item = getItem(bindingAdapterPosition)
                onSettingFocused(item, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }
        }

        fun bind(item: SettingItem.Navigate) {
            titleText.text = item.title
            subtitleText.text = item.subtitle
        }
    }

    inner class ActionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitleText)

        init {
            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                onSettingClicked(item)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                val item = getItem(bindingAdapterPosition)
                onSettingFocused(item, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }
        }

        fun bind(item: SettingItem.Action) {
            titleText.text = item.title
            subtitleText.text = item.subtitle
        }
    }

    inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitleText)
        private val themePreview: View = itemView.findViewById(R.id.themePreview)

        init {
            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition)
                onSettingClicked(item)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                val item = getItem(bindingAdapterPosition)
                onSettingFocused(item, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }
        }

        fun bind(item: SettingItem.ThemeSelector) {
            titleText.text = item.title
            subtitleText.text = item.subtitle
            // Set theme preview color based on current theme
        }
    }

    inner class DropdownViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val valueText: TextView = itemView.findViewById(R.id.valueText)

        init {
            itemView.setOnClickListener {
                val item = getItem(bindingAdapterPosition) as SettingItem.Dropdown
                // Show dropdown dialog
            }
        }

        fun bind(item: SettingItem.Dropdown) {
            titleText.text = item.title
            valueText.text = item.options.getOrNull(item.selectedIndex) ?: ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SettingItem>() {
        override fun areItemsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: SettingItem, newItem: SettingItem): Boolean {
            return oldItem == newItem
        }
    }
}
