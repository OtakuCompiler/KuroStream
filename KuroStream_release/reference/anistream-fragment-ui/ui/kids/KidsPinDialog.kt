package com.kurostream.legacyui.anistream.ui.kids

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.kurostream.data.anistreambinding.DialogKidsPinBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * PIN entry dialog for parental controls in kids mode.
 */
class KidsPinDialog : DialogFragment() {

    private var _binding: DialogKidsPinBinding? = null
    private val binding get() = _binding!!

    private var correctPin: String? = null
    private var onPinCorrect: (() -> Unit)? = null
    private var onPinIncorrect: (() -> Unit)? = null

    companion object {
        fun newInstance(pin: String?): KidsPinDialog {
            return KidsPinDialog().apply {
                arguments = Bundle().apply {
                    putString("pin", pin)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        correctPin = arguments?.getString("pin")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogKidsPinBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle("Enter PIN")
            .setMessage("Parental control - Enter your PIN to continue")
            .setPositiveButton("Verify") { _, _ ->
                verifyPin()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .create()
    }

    private fun verifyPin() {
        val enteredPin = binding.pinEditText.text?.toString()
        if (enteredPin == correctPin) {
            onPinCorrect?.invoke()
            dismiss()
        } else {
            binding.pinInputLayout.error = "Incorrect PIN"
            onPinIncorrect?.invoke()
        }
    }

    fun setOnPinCorrectListener(listener: () -> Unit) {
        onPinCorrect = listener
    }

    fun setOnPinIncorrectListener(listener: () -> Unit) {
        onPinIncorrect = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
