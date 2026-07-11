package com.kurostream.legacyui.anistream.ui.introskip

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.kurostream.data.anistream.introskip.IntroSkipEntity
import com.kurostream.data.anistream.introskip.SkipSource
import com.kurostream.data.anistreambinding.DialogManualSkipMarkerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ManualSkipMarkerDialog : DialogFragment() {

    private var _binding: DialogManualSkipMarkerBinding? = null
    private val binding get() = _binding!!

    private var currentPositionSec: Double = 0.0
    private var introStart: Double? = null
    private var introEnd: Double? = null
    private var outroStart: Double? = null
    private var outroEnd: Double? = null
    private var animeId: String = ""
    private var episodeNumber: Int = 1

    private var onSave: ((IntroSkipEntity) -> Unit)? = null

    companion object {
        fun newInstance(animeId: String, episodeNumber: Int, currentPositionSec: Double): ManualSkipMarkerDialog {
            return ManualSkipMarkerDialog().apply {
                arguments = Bundle().apply {
                    putString("anime_id", animeId)
                    putInt("episode_number", episodeNumber)
                    putDouble("position", currentPositionSec)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            animeId = it.getString("anime_id", "")
            episodeNumber = it.getInt("episode_number", 1)
            currentPositionSec = it.getDouble("position", 0.0)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogManualSkipMarkerBinding.inflate(layoutInflater)

        binding.currentTimeText.text = formatTime(currentPositionSec)

        binding.markIntroStartButton.setOnClickListener {
            introStart = currentPositionSec
            updateDisplay()
        }

        binding.markIntroEndButton.setOnClickListener {
            introEnd = currentPositionSec
            updateDisplay()
        }

        binding.markOutroStartButton.setOnClickListener {
            outroStart = currentPositionSec
            updateDisplay()
        }

        binding.markOutroEndButton.setOnClickListener {
            outroEnd = currentPositionSec
            updateDisplay()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Mark Skip Segments")
            .setPositiveButton("Save") { _, _ -> saveSegments() }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun updateDisplay() {
        binding.introRangeText.text = "Intro: ${introStart?.let { formatTime(it) } ?: "--:--"} - ${introEnd?.let { formatTime(it) } ?: "--:--"}"
        binding.outroRangeText.text = "Outro: ${outroStart?.let { formatTime(it) } ?: "--:--"} - ${outroEnd?.let { formatTime(it) } ?: "--:--"}"
    }

    private fun saveSegments() {
        val entity = IntroSkipEntity(
            animeId = animeId,
            episodeNumber = episodeNumber,
            introStartSec = introStart,
            introEndSec = introEnd,
            outroStartSec = outroStart,
            outroEndSec = outroEnd,
            source = SkipSource.USER_MARKED
        )
        onSave?.invoke(entity)
    }

    private fun formatTime(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return "%02d:%02d".format(mins, secs)
    }

    fun setOnSaveListener(listener: (IntroSkipEntity) -> Unit) {
        onSave = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
