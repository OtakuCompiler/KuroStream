package com.kurostream.legacyui.anistream.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistreambinding.FragmentSettingsScreenBinding
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsScreenFragment : Fragment() {

    private var _binding: FragmentSettingsScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var settingsAdapter: SettingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        settingsAdapter = SettingsAdapter(
            onSettingFocused = { setting, view ->
                TvUiUtils.animateFocus(view, true)
            },
            onSettingClicked = { setting ->
                handleSettingClick(setting)
            },
            onToggleChanged = { setting, isChecked ->
                viewModel.updateToggle(setting.key, isChecked)
            },
            onValueSelected = { setting, value ->
                viewModel.updateValue(setting.key, value)
            }
        )

        binding.settingsRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = settingsAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collectLatest { settings ->
                    settingsAdapter.submitList(settings)
                }
            }
        }
    }

    private fun handleSettingClick(setting: SettingItem) {
        when (setting.key) {
            "theme" -> showThemeSelector()
            "subtitle_settings" -> navigateToSubtitleSettings()
            "playback_defaults" -> navigateToPlaybackDefaults()
            "sync_settings" -> navigateToSyncSettings()
            "backup_restore" -> navigateToBackupRestore()
            "performance" -> navigateToPerformanceSettings()
            "about" -> showAboutDialog()
            "clear_cache" -> viewModel.clearCache()
            "check_updates" -> viewModel.checkForUpdates()
        }
    }

    private fun showThemeSelector() {
        val dialog = ThemeSelectorDialog.newInstance()
        dialog.setOnThemeSelected { theme ->
            viewModel.updateTheme(theme)
        }
        dialog.show(childFragmentManager, "theme_selector")
    }

    private fun navigateToSubtitleSettings() {
        findNavController().navigate(R.id.action_settings_to_subtitle)
    }

    private fun navigateToPlaybackDefaults() {
        findNavController().navigate(R.id.action_settings_to_playback)
    }

    private fun navigateToSyncSettings() {
        findNavController().navigate(R.id.action_settings_to_sync)
    }

    private fun navigateToBackupRestore() {
        findNavController().navigate(R.id.action_settings_to_backup)
    }

    private fun navigateToPerformanceSettings() {
        findNavController().navigate(R.id.action_settings_to_performance)
    }

    private fun showAboutDialog() {
        // Show app version, credits, licenses
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
