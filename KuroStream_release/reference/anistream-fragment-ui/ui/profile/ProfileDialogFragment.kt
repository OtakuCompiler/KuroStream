package com.kurostream.legacyui.anistream.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistreambinding.DialogProfileEditBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog for creating or editing a user profile.
 * Includes avatar selection, name input, and kids mode toggle.
 */
class ProfileDialogFragment : DialogFragment() {

    private var _binding: DialogProfileEditBinding? = null
    private val binding get() = _binding!!

    private var existingProfile: UserProfile? = null
    private var onProfileSaved: ((UserProfile) -> Unit)? = null
    private var selectedAvatarIndex = 0

    private val avatarResources = listOf(
        R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3,
        R.drawable.avatar_4, R.drawable.avatar_5, R.drawable.avatar_6,
        R.drawable.avatar_7, R.drawable.avatar_8
    )

    companion object {
        private const val ARG_PROFILE = "profile"

        fun newInstance(profile: UserProfile?): ProfileDialogFragment {
            return ProfileDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PROFILE, profile)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        existingProfile = arguments?.getParcelable(ARG_PROFILE)
        selectedAvatarIndex = existingProfile?.avatarIndex ?: 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogProfileEditBinding.inflate(LayoutInflater.from(context))
        setupAvatarSelection()
        setupForm()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(if (existingProfile == null) "Create Profile" else "Edit Profile")
            .setPositiveButton("Save") { _, _ ->
                saveProfile()
            }
            .setNegativeButton("Cancel", null)
            .apply {
                if (existingProfile != null) {
                    setNeutralButton("Delete") { _, _ ->
                        existingProfile?.let { (parentFragment as? ProfileSelectionFragment)?.viewModel?.deleteProfile(it.id) }
                    }
                }
            }
            .create()
    }

    private fun setupAvatarSelection() {
        binding.avatarGrid.removeAllViews()
        avatarResources.forEachIndexed { index, avatarRes ->
            val avatarView = ImageView(requireContext()).apply {
                setImageResource(avatarRes)
                layoutParams = ViewGroup.MarginLayoutParams(120, 120).apply {
                    setMargins(8, 8, 8, 8)
                }
                background = ContextCompat.getDrawable(
                    context,
                    if (index == selectedAvatarIndex) R.drawable.avatar_selected_bg else R.drawable.avatar_unselected_bg
                )
                isFocusable = true
                isFocusableInTouchMode = true
                scaleType = ImageView.ScaleType.CENTER_CROP

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        scaleX = 1.1f
                        scaleY = 1.1f
                    } else {
                        scaleX = 1.0f
                        scaleY = 1.0f
                    }
                }

                setOnClickListener {
                    selectedAvatarIndex = index
                    updateAvatarSelection()
                }
            }
            binding.avatarGrid.addView(avatarView)
        }
    }

    private fun updateAvatarSelection() {
        for (i in 0 until binding.avatarGrid.childCount) {
            val view = binding.avatarGrid.getChildAt(i)
            view.background = ContextCompat.getDrawable(
                requireContext(),
                if (i == selectedAvatarIndex) R.drawable.avatar_selected_bg else R.drawable.avatar_unselected_bg
            )
        }
    }

    private fun setupForm() {
        existingProfile?.let { profile ->
            binding.nameEditText.setText(profile.name)
            binding.kidsModeSwitch.isChecked = profile.isKidsMode
            binding.pinEditText.setText(profile.pin)
        }
    }

    private fun saveProfile() {
        val name = binding.nameEditText.text.toString().trim()
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            return
        }

        val profile = UserProfile(
            id = existingProfile?.id ?: java.util.UUID.randomUUID().toString(),
            name = name,
            avatarIndex = selectedAvatarIndex,
            avatarRes = avatarResources[selectedAvatarIndex],
            isKidsMode = binding.kidsModeSwitch.isChecked,
            pin = binding.pinEditText.text?.toString(),
            createdAt = existingProfile?.createdAt ?: System.currentTimeMillis()
        )

        onProfileSaved?.invoke(profile)
    }

    fun setOnProfileSaved(listener: (UserProfile) -> Unit) {
        onProfileSaved = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
