package com.kurostream.legacyui.anistream.ui.search

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistreambinding.DialogAdvancedFiltersBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Advanced filter dialog for search refinement.
 * Includes genre multi-select, year range, rating filter, and sort options.
 */
class AdvancedFilterDialog : DialogFragment() {

    private var _binding: DialogAdvancedFiltersBinding? = null
    private val binding get() = _binding!!

    private var onApplyFilters: ((AdvancedFilterState) -> Unit)? = null
    private var currentState = AdvancedFilterState()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(createView())
            .setPositiveButton("Apply") { _, _ ->
                onApplyFilters?.invoke(collectFilterState())
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset") { _, _ ->
                onApplyFilters?.invoke(AdvancedFilterState())
            }
            .create()
    }

    private fun createView(): View {
        _binding = DialogAdvancedFiltersBinding.inflate(LayoutInflater.from(context))
        setupGenreChips()
        setupYearRange()
        setupRatingFilter()
        setupSortOptions()
        return binding.root
    }

    private fun setupGenreChips() {
        val genres = listOf(
            "Action", "Adventure", "Comedy", "Drama", "Fantasy",
            "Horror", "Mecha", "Mystery", "Romance", "Sci-Fi",
            "Slice of Life", "Sports", "Supernatural", "Thriller"
        )

        genres.forEach { genre ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = genre
                isCheckable = true
                isChecked = currentState.selectedGenres.contains(genre)
            }
            binding.genreChipGroup.addView(chip)
        }
    }

    private fun setupYearRange() {
        binding.yearFromSeekBar.max = 2026 - 1980
        binding.yearToSeekBar.max = 2026 - 1980
        binding.yearFromSeekBar.progress = (currentState.yearFrom ?: 1980) - 1980
        binding.yearToSeekBar.progress = (currentState.yearTo ?: 2026) - 1980

        val yearListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fromYear = binding.yearFromSeekBar.progress + 1980
                val toYear = binding.yearToSeekBar.progress + 1980
                binding.yearRangeText.text = "$fromYear - $toYear"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        binding.yearFromSeekBar.setOnSeekBarChangeListener(yearListener)
        binding.yearToSeekBar.setOnSeekBarChangeListener(yearListener)
    }

    private fun setupRatingFilter() {
        binding.ratingSeekBar.max = 100
        binding.ratingSeekBar.progress = ((currentState.minRating ?: 0f) * 10).toInt()
        binding.ratingSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.ratingValueText.text = "${progress / 10.0f}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSortOptions() {
        val sortOptions = listOf("Relevance", "Rating", "Year", "Title", "Popularity")
        binding.sortRadioGroup.removeAllViews()
        sortOptions.forEachIndexed { index, option ->
            val radioButton = android.widget.RadioButton(requireContext()).apply {
                text = option
                id = index
                isChecked = currentState.sortBy == option
            }
            binding.sortRadioGroup.addView(radioButton)
        }
    }

    private fun collectFilterState(): AdvancedFilterState {
        val selectedGenres = mutableListOf<String>()
        for (i in 0 until binding.genreChipGroup.childCount) {
            val chip = binding.genreChipGroup.getChildAt(i) as? com.google.android.material.chip.Chip
            if (chip?.isChecked == true) {
                selectedGenres.add(chip.text.toString())
            }
        }

        val checkedSortId = binding.sortRadioGroup.checkedRadioButtonId
        val sortBy = when (checkedSortId) {
            0 -> "Relevance"
            1 -> "Rating"
            2 -> "Year"
            3 -> "Title"
            4 -> "Popularity"
            else -> "Relevance"
        }

        return AdvancedFilterState(
            selectedGenres = selectedGenres,
            yearFrom = binding.yearFromSeekBar.progress + 1980,
            yearTo = binding.yearToSeekBar.progress + 1980,
            minRating = binding.ratingSeekBar.progress / 10.0f,
            sortBy = sortBy
        )
    }

    fun setOnApplyFiltersListener(listener: (AdvancedFilterState) -> Unit) {
        onApplyFilters = listener
    }

    fun setInitialState(state: AdvancedFilterState) {
        currentState = state
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class AdvancedFilterState(
    val selectedGenres: List<String> = emptyList(),
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val minRating: Float? = null,
    val sortBy: String = "Relevance"
)
