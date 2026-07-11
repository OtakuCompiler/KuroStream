package com.kurostream.legacyui.anistream.ui.addons

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
import androidx.recyclerview.widget.GridLayoutManager
import com.kurostream.data.anistreambinding.FragmentAddonMarketplaceBinding
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddonMarketplaceFragment : Fragment() {

    private var _binding: FragmentAddonMarketplaceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MarketplaceViewModel by viewModels()

    private lateinit var marketplaceAdapter: MarketplaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddonMarketplaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.loadMarketplace()
    }

    private fun setupRecyclerView() {
        marketplaceAdapter = MarketplaceAdapter(
            onAddonFocused = { addon, view ->
                TvUiUtils.animateFocus(view, true)
                viewModel.onAddonFocused(addon)
            },
            onInstallClicked = { addon ->
                viewModel.installAddon(addon)
            },
            onPreviewClicked = { addon ->
                showAddonPreview(addon)
            }
        )

        binding.marketplaceRecycler.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = marketplaceAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.marketplaceItems.collectLatest { items ->
                    marketplaceAdapter.submitList(items)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is MarketplaceEvent.InstallSuccess -> {
                            Toast.makeText(context, "${event.addonName} installed", Toast.LENGTH_SHORT).show()
                        }
                        is MarketplaceEvent.InstallError -> {
                            Toast.makeText(context, "Failed to install: ${event.message}", Toast.LENGTH_LONG).show()
                        }
                        is MarketplaceEvent.ShowDetails -> {
                            showAddonDetails(event.addon)
                        }
                    }
                }
            }
        }
    }

    private fun showAddonPreview(addon: MarketplaceAddon) {
        // Show preview dialog/screenshots
    }

    private fun showAddonDetails(addon: MarketplaceAddon) {
        // Navigate to details screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
