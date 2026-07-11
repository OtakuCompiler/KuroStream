package com.kurostream.legacyui.anistream.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kurostream.data.anistreambinding.FragmentPerformanceSettingsBinding
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PerformanceSettingsFragment : Fragment() {

    private var _binding: FragmentPerformanceSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerformanceSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerformanceSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBufferSizeControl()
        setupDecoderPreference()
        setupCoilCacheSize()
        setupAnimationSpeed()
        observeViewModel()
    }

    private fun setupBufferSizeControl() {
        binding.bufferSizeSeekBar.apply {
            max = 4
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val labels = listOf("Default", "Low (16MB)", "Medium (32MB)", "High (64MB)", "Very High (128MB)")
                    binding.bufferSizeLabel.text = "Buffer Size: ${labels[progress]}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.setBufferSize(seekBar?.progress ?: 0)
                }
            })
        }
    }

    private fun setupDecoderPreference() {
        binding.decoderRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.decoder_auto -> viewModel.setDecoderPreference(DecoderPreference.AUTO)
                R.id.decoder_hardware -> viewModel.setDecoderPreference(DecoderPreference.HARDWARE)
                R.id.decoder_software -> viewModel.setDecoderPreference(DecoderPreference.SOFTWARE)
            }
        }
    }

    private fun setupCoilCacheSize() {
        binding.coilCacheSeekBar.apply {
            max = 4
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val labels = listOf("50MB", "100MB", "250MB", "500MB", "1GB")
                    binding.coilCacheLabel.text = "Image Cache: ${labels[progress]}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.setCoilCacheSize(seekBar?.progress ?: 1)
                }
            })
        }
    }

    private fun setupAnimationSpeed() {
        binding.animationSpeedSeekBar.apply {
            max = 3
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val labels = listOf("Off", "Fast (0.5x)", "Normal (1x)", "Slow (1.5x)")
                    binding.animationSpeedLabel.text = "Animation Speed: ${labels[progress]}"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.setAnimationSpeed(seekBar?.progress ?: 2)
                }
            })
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collectLatest { perf ->
                    binding.bufferSizeSeekBar.progress = perf.bufferSizeIndex
                    binding.coilCacheSeekBar.progress = perf.coilCacheIndex
                    binding.animationSpeedSeekBar.progress = perf.animationSpeedIndex

                    when (perf.decoderPreference) {
                        DecoderPreference.AUTO -> binding.decoderAuto.isChecked = true
                        DecoderPreference.HARDWARE -> binding.decoderHardware.isChecked = true
                        DecoderPreference.SOFTWARE -> binding.decoderSoftware.isChecked = true
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
