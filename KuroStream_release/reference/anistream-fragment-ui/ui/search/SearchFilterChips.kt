package com.kurostream.legacyui.anistream.ui.search

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.kurostream.legacyui.anistream.R
import com.google.android.material.chip.Chip
import com.kurostream.legacyui.anistream.util.TvUiUtils

/**
 * Custom horizontal chip group optimized for TV D-pad navigation.
 * Handles focus wrapping, scroll tracking, and advanced filter states.
 */
class SearchFilterChips @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val chipContainer: LinearLayout
    private val chips = mutableListOf<FilterChipData>()
    private var onFilterChanged: ((List<SearchFilter>) -> Unit)? = null

    init {
        isHorizontalScrollBarEnabled = false
        chipContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }
        addView(chipContainer)
    }

    fun setFilters(filters: List<FilterChipData>) {
        chips.clear()
        chips.addAll(filters)
        chipContainer.removeAllViews()

        filters.forEach { filterData ->
            val chip = createChip(filterData)
            chipContainer.addView(chip)
        }
    }

    private fun createChip(filterData: FilterChipData): Chip {
        return Chip(context).apply {
            text = filterData.label
            isCheckable = true
            isChecked = filterData.isSelected
            chipBackgroundColor = context.getColorStateList(R.color.chip_background_selector)
            setTextColor(context.getColorStateList(R.color.chip_text_selector))
            chipStrokeWidth = if (filterData.isSelected) 2f else 0f
            chipStrokeColor = context.getColorStateList(R.color.accent)
            chipIcon = filterData.iconRes?.let { context.getDrawable(it) }
            chipIconTint = context.getColorStateList(R.color.chip_icon_selector)
            isFocusable = true
            isFocusableInTouchMode = true

            setOnFocusChangeListener { _, hasFocus ->
                TvUiUtils.animateFocus(this, hasFocus)
                if (hasFocus) {
                    smoothScrollTo(this)
                }
            }

            setOnCheckedChangeListener { _, isChecked ->
                filterData.isSelected = isChecked
                chipStrokeWidth = if (isChecked) 2f else 0f
                notifyFilterChanged()
            }

            setOnKeyListener { _, keyCode, event ->
                handleDpadNavigation(keyCode, event, this)
            }
        }
    }

    private fun handleDpadNavigation(keyCode: Int, event: KeyEvent, currentChip: Chip): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val children = chipContainer.children().toList()
        val currentIndex = children.indexOf(currentChip)

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (currentIndex > 0) {
                    children[currentIndex - 1].requestFocus()
                    true
                } else {
                    // Wrap to end
                    children.lastOrNull()?.requestFocus()
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (currentIndex < children.size - 1) {
                    children[currentIndex + 1].requestFocus()
                    true
                } else {
                    // Wrap to start
                    children.firstOrNull()?.requestFocus()
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Allow focus to leave chips downward to results
                false
            }
            else -> false
        }
    }

    private fun smoothScrollTo(view: View) {
        post {
            smoothScrollTo(view.left - 100, 0)
        }
    }

    private fun notifyFilterChanged() {
        val selected = chips.filter { it.isSelected }.map { it.filter }
        onFilterChanged?.invoke(selected)
    }

    fun setOnFilterChangedListener(listener: (List<SearchFilter>) -> Unit) {
        onFilterChanged = listener
    }

    fun clearFilters() {
        chips.forEach { it.isSelected = false }
        chipContainer.children().forEach { (it as? Chip)?.isChecked = false }
        notifyFilterChanged()
    }

    private fun LinearLayout.children() = (0 until childCount).map { getChildAt(it) }
}

data class FilterChipData(
    val filter: SearchFilter,
    val label: String,
    val iconRes: Int? = null,
    var isSelected: Boolean = false
)
