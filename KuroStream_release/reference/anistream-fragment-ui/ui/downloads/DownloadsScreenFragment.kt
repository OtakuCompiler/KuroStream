package com.kurostream.legacyui.anistream.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistreambinding.FragmentDownloadsScreenBinding
import com.kurostream.data.anistream.downloads.DownloadItem
import com.kurostream.data.anistream.downloads.DownloadStatus
import com.kurostream.legacyui.anistream.ui.player.PlayerActivity
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsScreenFragment : Fragment() {

    private var _binding: FragmentDownloadsScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadsViewModel by viewModels()

    private lateinit var downloadsAdapter: DownloadsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupActionButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        downloadsAdapter = DownloadsAdapter(
            onItemFocused = { item, view ->
                TvUiUtils.animateFocus(view, true)
                viewModel.onItemFocused(item)
            },
            onPlayClicked = { item ->
                playOffline(item)
            },
            onPauseClicked = { item ->
                viewModel.pauseDownload(item.id)
            },
            onResumeClicked = { item ->
                viewModel.resumeDownload(item.id)
            },
            onCancelClicked = { item ->
                viewModel.cancelDownload(item.id)
            },
            onRetryClicked = { item ->
                viewModel.retryDownload(item.id)
            },
            onDeleteClicked = { item ->
                showDeleteConfirmation(item)
            }
        )

        binding.downloadsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = downloadsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupActionButtons() {
        binding.pauseAllButton.setOnClickListener {
            viewModel.pauseAll()
        }
        binding.pauseAllButton.setOnFocusChangeListener { _, hasFocus ->
            TvUiUtils.animateFocus(binding.pauseAllButton, hasFocus)
        }

        binding.resumeAllButton.setOnClickListener {
            viewModel.resumeAll()
        }
        binding.resumeAllButton.setOnFocusChangeListener { _, hasFocus ->
            TvUiUtils.animateFocus(binding.resumeAllButton, hasFocus)
        }

        binding.clearCompletedButton.setOnClickListener {
            viewModel.clearCompleted()
        }
        binding.clearCompletedButton.setOnFocusChangeListener { _, hasFocus ->
            TvUiUtils.animateFocus(binding.clearCompletedButton, hasFocus)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.downloads.collectLatest { downloads ->
                    downloadsAdapter.submitList(downloads)
                    updateEmptyState(downloads.isEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeProgress.collectLatest { progressMap ->
                    downloadsAdapter.updateProgress(progressMap)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is DownloadsEvent.ShowError -> {
                            Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                        }
                        is DownloadsEvent.ShowMessage -> {
                            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.downloadsRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun playOffline(item: DownloadItem) {
        if (item.status != DownloadStatus.COMPLETED || item.filePath == null) {
            Toast.makeText(context, "Download not complete", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent(context, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_VIDEO_URI, item.filePath)
            putExtra(PlayerActivity.EXTRA_TITLE, item.title)
            putExtra(PlayerActivity.EXTRA_IS_OFFLINE, true)
            putExtra(PlayerActivity.EXTRA_ANIME_ID, item.animeId)
            putExtra(PlayerActivity.EXTRA_EPISODE_ID, item.episodeId)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(item: DownloadItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Download")
            .setMessage("Delete "${item.title}"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDownload(item.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
