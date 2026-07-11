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
import androidx.recyclerview.widget.LinearLayoutManager
import com.kurostream.data.anistreambinding.FragmentAddonManagerBinding
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddonManagerFragment : Fragment() {

    private var _binding: FragmentAddonManagerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddonManagerViewModel by viewModels()

    private lateinit var addonAdapter: AddonListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddonManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        addonAdapter = AddonListAdapter(
            onAddonFocused = { addon, view ->
                TvUiUtils.animateFocus(view, true)
            },
            onToggleEnabled = { addon ->
                viewModel.toggleAddon(addon.id)
            },
            onUninstallClicked = { addon ->
                showUninstallConfirmation(addon)
            },
            onConfigureClicked = { addon ->
                navigateToAddonConfig(addon)
            }
        )

        binding.addonsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = addonAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupButtons() {
        binding.browseMarketplaceButton.apply {
            setOnClickListener { navigateToMarketplace() }
            setOnFocusChangeListener { _, hasFocus ->
                TvUiUtils.animateFocus(this, hasFocus)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addons.collectLatest { addons ->
                    addonAdapter.submitList(addons)
                    updateEmptyState(addons.isEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is AddonEvent.ShowError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                        is AddonEvent.ShowMessage -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.addonsRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showUninstallConfirmation(addon: Addon) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Uninstall ${addon.name}?")
            .setMessage("This will remove all data associated with this extension.")
            .setPositiveButton("Uninstall") { _, _ -> viewModel.uninstallAddon(addon.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToMarketplace() {
        // Navigate to marketplace
    }

    private fun navigateToAddonConfig(addon: Addon) {
        // Navigate to addon configuration
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
