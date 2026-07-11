package com.kurostream.legacyui.anistream.ui.backup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kurostream.data.anistreambinding.FragmentBackupRestoreBinding
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BackupRestoreFragment : Fragment() {

    private var _binding: FragmentBackupRestoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackupRestoreViewModel by viewModels()

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val password = binding.backupPasswordInput.text?.toString() ?: ""
                if (password.length < 6) {
                    binding.backupPasswordLayout.error = "Password must be at least 6 characters"
                    return@let
                }
                viewModel.exportBackup(uri, password)
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val password = binding.restorePasswordInput.text?.toString() ?: ""
                viewModel.importBackup(uri, password)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.exportButton.setOnClickListener {
            openExportPicker()
        }
        binding.exportButton.setOnFocusChangeListener { _, hasFocus ->
            TvUiUtils.animateFocus(binding.exportButton, hasFocus)
        }

        binding.importButton.setOnClickListener {
            openImportPicker()
        }
        binding.importButton.setOnFocusChangeListener { _, hasFocus ->
            TvUiUtils.animateFocus(binding.importButton, hasFocus)
        }
    }

    private fun openExportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        exportLauncher.launch(intent)
    }

    private fun openImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        importLauncher.launch(intent)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is BackupUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                        }
                        is BackupUiState.Exporting -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.statusText.text = "Creating backup..."
                        }
                        is BackupUiState.Importing -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.statusText.text = "Restoring backup..."
                        }
                        is BackupUiState.ExportSuccess -> {
                            binding.progressBar.visibility = View.GONE
                            binding.statusText.text = "Backup saved to: ${state.uri}"
                            Toast.makeText(context, "Backup created successfully", Toast.LENGTH_LONG).show()
                        }
                        is BackupUiState.ImportSuccess -> {
                            binding.progressBar.visibility = View.GONE
                            binding.statusText.text = buildString {
                                appendLine("Restore complete!")
                                appendLine("Profiles: ${state.summary.profilesRestored}")
                                appendLine("Downloads: ${state.summary.downloadsRestored}")
                                appendLine("Searches: ${state.summary.searchesRestored}")
                                appendLine("Settings: ${state.summary.settingsRestored}")
                                appendLine("Sync providers: ${state.summary.syncProvidersRestored}")
                                appendLine("Intro skips: ${state.summary.introSkipsRestored}")
                            }
                            Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_LONG).show()
                        }
                        is BackupUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.statusText.text = "Error: ${state.message}"
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
