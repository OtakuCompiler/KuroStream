package com.kurostream.legacyui.anistream.ui.kids

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog shown when kids mode time limit is reached or about to be reached.
 */
class KidsTimeLimitDialog : DialogFragment() {

    private var remainingSeconds: Int = 0
    private var onTimeUp: (() -> Unit)? = null
    private var onExtendTime: (() -> Unit)? = null
    private var countdownTimer: CountDownTimer? = null

    companion object {
        private const val ARG_REMAINING_SECONDS = "remaining_seconds"
        private const val ARG_IS_WARNING = "is_warning"

        fun newInstance(remainingSeconds: Int, isWarning: Boolean = false): KidsTimeLimitDialog {
            return KidsTimeLimitDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_REMAINING_SECONDS, remainingSeconds)
                    putBoolean(ARG_IS_WARNING, isWarning)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remainingSeconds = arguments?.getInt(ARG_REMAINING_SECONDS, 0) ?: 0
        val isWarning = arguments?.getBoolean(ARG_IS_WARNING, false) ?: false
        isCancelable = !isWarning
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Time Limit")

        if (remainingSeconds <= 0) {
            builder.setMessage("Your daily watch time limit has been reached. Please take a break!")
                .setPositiveButton("OK") { _, _ ->
                    onTimeUp?.invoke()
                }
                .setCancelable(false)
        } else {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            builder.setMessage("You have ${minutes}m ${seconds}s of watch time remaining.")
                .setPositiveButton("Continue Watching") { _, _ -> }
                .setNegativeButton("Stop") { _, _ ->
                    onTimeUp?.invoke()
                }

            // PIN required to extend time
            if (remainingSeconds <= 300) { // 5 minutes warning
                builder.setNeutralButton("Extend Time (PIN)") { _, _ ->
                    onExtendTime?.invoke()
                }
            }
        }

        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        if (remainingSeconds > 0) {
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                (dialog as? androidx.appcompat.app.AlertDialog)?.setMessage(
                    "You have ${minutes}m ${seconds}s of watch time remaining."
                )
            }

            override fun onFinish() {
                onTimeUp?.invoke()
                dismiss()
            }
        }.start()
    }

    fun setOnTimeUpListener(listener: () -> Unit) {
        onTimeUp = listener
    }

    fun setOnExtendTimeListener(listener: () -> Unit) {
        onExtendTime = listener
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
}
