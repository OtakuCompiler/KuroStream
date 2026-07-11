package com.kurostream.legacyui.anistream.ui.search

import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kurostream.legacyui.anistream.util.DpadNavigationHelper

/**
 * Enhanced D-pad navigation for search results grid with focus wrapping
 * and directional focus memory.
 */
class SearchDpadNavigator(
    private val recyclerView: RecyclerView,
    private val layoutManager: GridLayoutManager
) {

    private var lastFocusedPosition = RecyclerView.NO_POSITION
    private val focusMemory = mutableMapOf<Int, Int>() // row -> column position

    fun attach() {
        recyclerView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                handleKeyPress(keyCode)
            } else {
                false
            }
        }
    }

    private fun handleKeyPress(keyCode: Int): Boolean {
        val focusedView = recyclerView.focusedChild ?: return false
        val currentPosition = recyclerView.getChildAdapterPosition(focusedView)
        if (currentPosition == RecyclerView.NO_POSITION) return false

        val spanCount = layoutManager.spanCount
        val totalItems = recyclerView.adapter?.itemCount ?: 0
        val currentRow = currentPosition / spanCount
        val currentCol = currentPosition % spanCount

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (currentRow == 0) {
                    // At top row - move focus to search input or filters
                    false // Let parent handle
                } else {
                    val targetRow = currentRow - 1
                    val targetCol = focusMemory[targetRow] ?: currentCol
                    val targetPos = targetRow * spanCount + targetCol.coerceIn(0, spanCount - 1)
                    if (targetPos >= 0) {
                        focusPosition(targetPos)
                        true
                    } else false
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                val targetRow = currentRow + 1
                val targetCol = focusMemory[targetRow] ?: currentCol
                val targetPos = targetRow * spanCount + targetCol.coerceIn(0, spanCount - 1)
                if (targetPos < totalItems) {
                    focusPosition(targetPos)
                    true
                } else false
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (currentCol == 0) {
                    // Wrap to previous row end
                    val targetPos = (currentRow * spanCount) - 1
                    if (targetPos >= 0) {
                        focusPosition(targetPos)
                        true
                    } else false
                } else {
                    focusPosition(currentPosition - 1)
                    true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val isLastInRow = currentCol == spanCount - 1 || currentPosition == totalItems - 1
                if (isLastInRow) {
                    // Wrap to next row start
                    val targetPos = (currentRow + 1) * spanCount
                    if (targetPos < totalItems) {
                        focusPosition(targetPos)
                        true
                    } else false
                } else {
                    focusPosition(currentPosition + 1)
                    true
                }
            }
            else -> false
        }
    }

    private fun focusPosition(position: Int) {
        val layoutManager = recyclerView.layoutManager as? GridLayoutManager ?: return
        val view = layoutManager.findViewByPosition(position)
        if (view != null) {
            view.requestFocus()
        } else {
            recyclerView.smoothScrollToPosition(position)
            recyclerView.post {
                layoutManager.findViewByPosition(position)?.requestFocus()
            }
        }
        lastFocusedPosition = position
        val spanCount = layoutManager.spanCount
        focusMemory[position / spanCount] = position % spanCount
    }

    fun saveFocusState(): Int = lastFocusedPosition

    fun restoreFocusState(position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            recyclerView.post {
                focusPosition(position)
            }
        }
    }
}
