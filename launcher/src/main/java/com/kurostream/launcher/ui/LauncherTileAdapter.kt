// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kurostream.launcher.R

class LauncherTileAdapter(
    private val onTileClicked: (LauncherTile) -> Unit,
    private val onTileFocused: (LauncherTile, View) -> Unit
) : ListAdapter<LauncherTile, LauncherTileAdapter.TileViewHolder>(TileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_launcher_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivTileIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTileTitle)

        fun bind(tile: LauncherTile) {
            tvTitle.text = tile.title

            when {
                tile.iconDrawable != null -> ivIcon.setImageDrawable(tile.iconDrawable)
                tile.iconRes != 0 -> ivIcon.setImageResource(tile.iconRes)
                else -> ivIcon.setImageResource(R.drawable.ic_app_placeholder)
            }

            itemView.setOnClickListener { onTileClicked(tile) }
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    onTileFocused(tile, itemView)
                    itemView.animate().scaleX(1.08f).scaleY(1.08f).setDuration(200).start()
                    itemView.elevation = 16f
                } else {
                    itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                    itemView.elevation = 0f
                }
            }

            // TV focus handling
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true
        }
    }

    class TileDiffCallback : DiffUtil.ItemCallback<LauncherTile>() {
        override fun areItemsTheSame(oldItem: LauncherTile, newItem: LauncherTile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LauncherTile, newItem: LauncherTile): Boolean {
            return oldItem == newItem
        }
    }
}
