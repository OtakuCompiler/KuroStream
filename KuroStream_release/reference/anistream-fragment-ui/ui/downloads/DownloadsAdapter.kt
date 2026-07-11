package com.kurostream.legacyui.anistream.ui.downloads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistream.downloads.DownloadItem
import com.kurostream.data.anistream.downloads.DownloadProgress
import com.kurostream.data.anistream.downloads.DownloadStatus
import com.kurostream.legacyui.anistream.util.TvUiUtils

class DownloadsAdapter(
    private val onItemFocused: (DownloadItem, View) -> Unit,
    private val onPlayClicked: (DownloadItem) -> Unit,
    private val onPauseClicked: (DownloadItem) -> Unit,
    private val onResumeClicked: (DownloadItem) -> Unit,
    private val onCancelClicked: (DownloadItem) -> Unit,
    private val onRetryClicked: (DownloadItem) -> Unit,
    private val onDeleteClicked: (DownloadItem) -> Unit
) : ListAdapter<DownloadItem, DownloadsAdapter.ViewHolder>(DiffCallback()) {

    private val progressMap = mutableMapOf<String, DownloadProgress>()

    fun updateProgress(progress: Map<String, DownloadProgress>) {
        progressMap.clear()
        progressMap.putAll(progress)
        currentList.forEachIndexed { index, item ->
            if (progress.containsKey(item.id)) {
                notifyItemChanged(index, PAYLOAD_PROGRESS)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), progressMap[getItem(position).id])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_PROGRESS)) {
            holder.updateProgress(getItem(position), progressMap[getItem(position).id])
        } else {
            onBindViewHolder(holder, position)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.posterImage)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val progressText: TextView = itemView.findViewById(R.id.progressText)
        private val playButton: ImageButton = itemView.findViewById(R.id.playButton)
        private val pauseButton: ImageButton = itemView.findViewById(R.id.pauseButton)
        private val resumeButton: ImageButton = itemView.findViewById(R.id.resumeButton)
        private val cancelButton: ImageButton = itemView.findViewById(R.id.cancelButton)
        private val retryButton: ImageButton = itemView.findViewById(R.id.retryButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->
                val item = getItem(bindingAdapterPosition)
                onItemFocused(item, itemView)
                TvUiUtils.animateFocus(itemView, hasFocus)
            }
        }

        fun bind(item: DownloadItem, progress: DownloadProgress?) {
            titleText.text = item.title
            posterImage.load(item.posterUrl) {
                crossfade(true)
                placeholder(R.drawable.poster_placeholder)
                error(R.drawable.poster_error)
            }

            updateStatus(item, progress)
            setupActionButtons(item)
        }

        fun updateProgress(item: DownloadItem, progress: DownloadProgress?) {
            updateStatus(item, progress)
        }

        private fun updateStatus(item: DownloadItem, progress: DownloadProgress?) {
            when (item.status) {
                DownloadStatus.QUEUED -> {
                    statusText.text = "Queued"
                    progressBar.isIndeterminate = true
                    progressBar.visibility = View.VISIBLE
                    progressText.visibility = View.GONE
                }
                DownloadStatus.DOWNLOADING -> {
                    val percent = progress?.percent ?: item.downloadedBytes * 100 / maxOf(item.totalBytes, 1)
                    statusText.text = "Downloading"
                    progressBar.isIndeterminate = false
                    progressBar.progress = percent
                    progressBar.visibility = View.VISIBLE
                    progressText.text = "$percent%"
                    progressText.visibility = View.VISIBLE
                }
                DownloadStatus.PAUSED -> {
                    val percent = item.downloadedBytes * 100 / maxOf(item.totalBytes, 1)
                    statusText.text = "Paused"
                    progressBar.isIndeterminate = false
                    progressBar.progress = percent
                    progressBar.visibility = View.VISIBLE
                    progressText.text = "$percent%"
                    progressText.visibility = View.VISIBLE
                }
                DownloadStatus.COMPLETED -> {
                    statusText.text = "Completed"
                    progressBar.visibility = View.GONE
                    progressText.visibility = View.GONE
                }
                DownloadStatus.FAILED -> {
                    statusText.text = "Failed"
                    progressBar.visibility = View.GONE
                    progressText.visibility = View.GONE
                }
                DownloadStatus.CANCELLED -> {
                    statusText.text = "Cancelled"
                    progressBar.visibility = View.GONE
                    progressText.visibility = View.GONE
                }
            }
        }

        private fun setupActionButtons(item: DownloadItem) {
            // Hide all first
            playButton.visibility = View.GONE
            pauseButton.visibility = View.GONE
            resumeButton.visibility = View.GONE
            cancelButton.visibility = View.GONE
            retryButton.visibility = View.GONE
            deleteButton.visibility = View.GONE

            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    pauseButton.visibility = View.VISIBLE
                    cancelButton.visibility = View.VISIBLE
                    pauseButton.setOnClickListener { onPauseClicked(item) }
                    cancelButton.setOnClickListener { onCancelClicked(item) }
                }
                DownloadStatus.PAUSED -> {
                    resumeButton.visibility = View.VISIBLE
                    cancelButton.visibility = View.VISIBLE
                    resumeButton.setOnClickListener { onResumeClicked(item) }
                    cancelButton.setOnClickListener { onCancelClicked(item) }
                }
                DownloadStatus.QUEUED -> {
                    cancelButton.visibility = View.VISIBLE
                    cancelButton.setOnClickListener { onCancelClicked(item) }
                }
                DownloadStatus.FAILED -> {
                    retryButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.VISIBLE
                    retryButton.setOnClickListener { onRetryClicked(item) }
                    deleteButton.setOnClickListener { onDeleteClicked(item) }
                }
                DownloadStatus.COMPLETED -> {
                    playButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.VISIBLE
                    playButton.setOnClickListener { onPlayClicked(item) }
                    deleteButton.setOnClickListener { onDeleteClicked(item) }
                }
                DownloadStatus.CANCELLED -> {
                    retryButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.VISIBLE
                    retryButton.setOnClickListener { onRetryClicked(item) }
                    deleteButton.setOnClickListener { onDeleteClicked(item) }
                }
            }

            // Setup focus for visible buttons
            listOf(playButton, pauseButton, resumeButton, cancelButton, retryButton, deleteButton)
                .filter { it.visibility == View.VISIBLE }
                .forEach { button ->
                    button.setOnFocusChangeListener { _, hasFocus ->
                        TvUiUtils.animateFocus(button, hasFocus)
                    }
                }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem) =
            oldItem == newItem
    }

    companion object {
        private const val PAYLOAD_PROGRESS = "progress"
    }
}
