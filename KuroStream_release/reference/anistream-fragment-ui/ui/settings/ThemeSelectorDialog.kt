package com.kurostream.legacyui.anistream.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistreambinding.DialogThemeSelectorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ThemeSelectorDialog : DialogFragment() {

    private var _binding: DialogThemeSelectorBinding? = null
    private val binding get() = _binding!!

    private var onThemeSelected: ((String) -> Unit)? = null
    private var selectedTheme = "dark"

    private val themes = listOf(
        ThemeOption("dark", "Dark", R.color.theme_dark_primary, R.color.theme_dark_surface),
        ThemeOption("light", "Light", R.color.theme_light_primary, R.color.theme_light_surface),
        ThemeOption("amoled", "AMOLED", R.color.theme_amoled_primary, R.color.theme_amoled_surface),
        ThemeOption("blue", "Ocean Blue", R.color.theme_blue_primary, R.color.theme_blue_surface),
        ThemeOption("purple", "Royal Purple", R.color.theme_purple_primary, R.color.theme_purple_surface),
        ThemeOption("green", "Forest Green", R.color.theme_green_primary, R.color.theme_green_surface),
        ThemeOption("red", "Crimson Red", R.color.theme_red_primary, R.color.theme_red_surface),
        ThemeOption("orange", "Sunset Orange", R.color.theme_orange_primary, R.color.theme_orange_surface),
    )

    companion object {
        fun newInstance(): ThemeSelectorDialog = ThemeSelectorDialog()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogThemeSelectorBinding.inflate(LayoutInflater.from(context))
        setupThemeGrid()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Select Theme")
            .setPositiveButton("Apply") { _, _ ->
                onThemeSelected?.invoke(selectedTheme)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun setupThemeGrid() {
        binding.themeGrid.removeAllViews()
        themes.forEach { theme ->
            val themeView = createThemePreview(theme)
            binding.themeGrid.addView(themeView)
        }
    }

    private fun createThemePreview(theme: ThemeOption): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_theme_preview, binding.themeGrid, false)

        val previewBg = view.findViewById<View>(R.id.themePreviewBg)
        val previewAccent = view.findViewById<View>(R.id.themePreviewAccent)
        val themeName = view.findViewById<android.widget.TextView>(R.id.themeName)
        val checkMark = view.findViewById<ImageView>(R.id.checkMark)

        previewBg.setBackgroundColor(ContextCompat.getColor(requireContext(), theme.surfaceColor))
        previewAccent.setBackgroundColor(ContextCompat.getColor(requireContext(), theme.primaryColor))
        themeName.text = theme.name
        checkMark.visibility = if (theme.id == selectedTheme) View.VISIBLE else View.GONE

        view.isFocusable = true
        view.isFocusableInTouchMode = true

        view.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                view.scaleX = 1.05f
                view.scaleY = 1.05f
                view.elevation = 8f
            } else {
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                view.elevation = 0f
            }
        }

        view.setOnClickListener {
            selectedTheme = theme.id
            updateSelection()
        }

        return view
    }

    private fun updateSelection() {
        for (i in 0 until binding.themeGrid.childCount) {
            val view = binding.themeGrid.getChildAt(i)
            val checkMark = view.findViewById<ImageView>(R.id.checkMark)
            checkMark.visibility = if (themes.getOrNull(i)?.id == selectedTheme) View.VISIBLE else View.GONE
        }
    }

    fun setOnThemeSelected(listener: (String) -> Unit) {
        onThemeSelected = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class ThemeOption(
    val id: String,
    val name: String,
    val primaryColor: Int,
    val surfaceColor: Int
)
