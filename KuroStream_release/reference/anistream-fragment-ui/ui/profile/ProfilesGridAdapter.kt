package com.kurostream.legacyui.anistream.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kurostream.legacyui.anistream.R
import com.kurostream.legacyui.anistream.util.TvUiUtils

class ProfilesGridAdapter(
    private val onProfileSelected: (UserProfile) -> Unit,
    private val onProfileFocused: (UserProfile, View) -> Unit,
    private val onEditClicked: (UserProfile) -> Unit
) : ListAdapter<UserProfile, ProfilesGridAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val kidsBadge: View = itemView.findViewById(R.id.kidsBadge)
        private val editButton: View = itemView.findViewById(R.id.editButton)

        init {
            itemView.setOnClickListener {
                val profile = getItem(bindingAdapterPosition)
                onProfileSelected(profile)
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->
                val profile = getItem(bindingAdapterPosition)
                onProfileFocused(profile, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }

            editButton.setOnClickListener {
                val profile = getItem(bindingAdapterPosition)
                onEditClicked(profile)
            }
        }

        fun bind(profile: UserProfile) {
            nameText.text = profile.name
            avatarImage.setImageResource(profile.avatarRes)
            kidsBadge.visibility = if (profile.isKidsMode) View.VISIBLE else View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile) =
            oldItem == newItem
    }
}
