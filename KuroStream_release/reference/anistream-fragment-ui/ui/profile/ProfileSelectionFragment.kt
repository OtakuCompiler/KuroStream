package com.kurostream.legacyui.anistream.ui.profile

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
import androidx.recyclerview.widget.GridLayoutManager
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistreambinding.FragmentProfileSelectionBinding
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile selection screen shown at app start.
 * Displays existing profiles in a grid with add/edit options.
 */
@AndroidEntryPoint
class ProfileSelectionFragment : Fragment() {

    private var _binding: FragmentProfileSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileSelectionViewModel by viewModels()

    private lateinit var profilesAdapter: ProfilesGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupAddProfileButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        profilesAdapter = ProfilesGridAdapter(
            onProfileSelected = { profile ->
                viewModel.selectProfile(profile)
            },
            onProfileFocused = { profile, view ->
                TvUiUtils.animateFocus(view, true)
                viewModel.onProfileFocused(profile)
            },
            onEditClicked = { profile ->
                showProfileDialog(profile)
            }
        )

        binding.profilesRecycler.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = profilesAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupAddProfileButton() {
        binding.addProfileButton.apply {
            setOnClickListener {
                showProfileDialog(null)
            }
            setOnFocusChangeListener { _, hasFocus ->
                TvUiUtils.animateFocus(this, hasFocus)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profiles.collectLatest { profiles ->
                    profilesAdapter.submitList(profiles)
                    updateEmptyState(profiles.isEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collectLatest { event ->
                    when (event) {
                        is ProfileNavEvent.NavigateToHome -> {
                            findNavController().navigate(
                                ProfileSelectionFragmentDirections.actionProfilesToHome()
                            )
                        }
                        is ProfileNavEvent.ShowError -> {
                            Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showProfileDialog(profile: UserProfile?) {
        val dialog = ProfileDialogFragment.newInstance(profile)
        dialog.setOnProfileSaved { savedProfile ->
            if (profile == null) {
                viewModel.createProfile(savedProfile)
            } else {
                viewModel.updateProfile(savedProfile)
            }
        }
        dialog.show(childFragmentManager, "profile_dialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
