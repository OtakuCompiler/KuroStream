package com.kurostream.legacyui.anistream.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kurostream.legacyui.anistream.R
import com.kurostream.data.anistreambinding.FragmentSearchScreenBinding
import com.kurostream.data.anistream.model.AnimeItem
import com.kurostream.legacyui.anistream.ui.common.FocusAwareEditText
import com.kurostream.legacyui.anistream.util.DpadNavigationHelper
import com.kurostream.legacyui.anistream.util.TvUiUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SearchScreenFragment : Fragment() {

    private var _binding: FragmentSearchScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()

    @Inject
    lateinit var dpadHelper: DpadNavigationHelper

    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var suggestionsAdapter: SearchSuggestionsAdapter
    private lateinit var recentSearchesAdapter: RecentSearchesAdapter

    private var speechRecognizer: SpeechRecognizer? = null
    private val searchQueryFlow = MutableStateFlow("")

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1001
        private const val SEARCH_DEBOUNCE_MS = 300L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchInput()
        setupRecyclerViews()
        setupVoiceInput()
        setupFilterChips()
        observeViewModel()
        setupSearchDebounce()
    }

    private fun setupSearchInput() {
        binding.searchEditText.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(text.toString())
                    true
                } else {
                    false
                }
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    TvUiUtils.animateFocus(this, true)
                    showKeyboard()
                } else {
                    TvUiUtils.animateFocus(this, false)
                }
            }

            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val query = s?.toString()?.trim() ?: ""
                    searchQueryFlow.value = query
                    viewModel.updateQuery(query)
                }
            })
        }

        binding.searchEditText.requestFocus()
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                searchQueryFlow
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .distinctUntilChanged()
                    .collectLatest { query ->
                        if (query.length >= 2) {
                            viewModel.performSearch(query)
                        } else if (query.isEmpty()) {
                            viewModel.showRecentSearches()
                        }
                    }
            }
        }
    }

    private fun setupRecyclerViews() {
        searchResultsAdapter = SearchResultsAdapter(
            onItemFocused = { item, view ->
                TvUiUtils.animateFocus(view, true)
                viewModel.onItemFocused(item)
            },
            onItemClicked = { item ->
                navigateToDetail(item)
            }
        )

        binding.searchResultsRecycler.apply {
            layoutManager = GridLayoutManager(context, 5).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }
            adapter = searchResultsAdapter
            setHasFixedSize(true)
            dpadHelper.attachToRecyclerView(this)
        }

        suggestionsAdapter = SearchSuggestionsAdapter(
            onSuggestionClicked = { suggestion ->
                binding.searchEditText.setText(suggestion)
                binding.searchEditText.setSelection(suggestion.length)
                performSearch(suggestion)
            }
        )

        binding.suggestionsRecycler.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = suggestionsAdapter
        }

        recentSearchesAdapter = RecentSearchesAdapter(
            onSearchClicked = { query ->
                binding.searchEditText.setText(query)
                binding.searchEditText.setSelection(query.length)
                performSearch(query)
            },
            onClearClicked = { query ->
                viewModel.removeRecentSearch(query)
            }
        )

        binding.recentSearchesRecycler.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = recentSearchesAdapter
        }
    }

    private fun setupVoiceInput() {
        binding.voiceSearchButton.apply {
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startVoiceRecognition()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_RECORD_AUDIO
                    )
                }
            }

            setOnFocusChangeListener { _, hasFocus ->
                TvUiUtils.animateFocus(this, hasFocus)
            }
        }
    }

    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(context, "Voice recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext()).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding.voiceSearchButton.isActivated = true
                    binding.searchEditText.hint = "Listening..."
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    binding.voiceSearchButton.isActivated = false
                }

                override fun onError(error: Int) {
                    binding.voiceSearchButton.isActivated = false
                    binding.searchEditText.hint = getString(R.string.search_hint)
                    Toast.makeText(context, "Voice recognition error", Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val query = matches[0]
                        binding.searchEditText.setText(query)
                        binding.searchEditText.setSelection(query.length)
                        performSearch(query)
                    }
                    binding.searchEditText.hint = getString(R.string.search_hint)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            startListening(intent)
        }
    }

    private fun setupFilterChips() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filters = checkedIds.map { chipId ->
                when (chipId) {
                    R.id.chip_movies -> SearchFilter.MOVIES
                    R.id.chip_series -> SearchFilter.SERIES
                    R.id.chip_ova -> SearchFilter.OVA
                    R.id.chip_special -> SearchFilter.SPECIAL
                    else -> SearchFilter.ALL
                }
            }
            viewModel.updateFilters(filters)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is SearchUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.emptyStateView.visibility = View.GONE
                            binding.searchResultsRecycler.visibility = View.GONE
                            binding.suggestionsRecycler.visibility = View.GONE
                            binding.recentSearchesRecycler.visibility = View.VISIBLE
                            binding.recentSearchesLabel.visibility = View.VISIBLE
                            recentSearchesAdapter.submitList(state.recentSearches)
                        }
                        is SearchUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.emptyStateView.visibility = View.GONE
                        }
                        is SearchUiState.Suggestions -> {
                            binding.progressBar.visibility = View.GONE
                            binding.searchResultsRecycler.visibility = View.GONE
                            binding.suggestionsRecycler.visibility = View.VISIBLE
                            suggestionsAdapter.submitList(state.suggestions)
                        }
                        is SearchUiState.Results -> {
                            binding.progressBar.visibility = View.GONE
                            binding.emptyStateView.visibility = View.GONE
                            binding.searchResultsRecycler.visibility = View.VISIBLE
                            binding.suggestionsRecycler.visibility = View.GONE
                            binding.recentSearchesRecycler.visibility = View.GONE
                            binding.recentSearchesLabel.visibility = View.GONE
                            searchResultsAdapter.submitList(state.results)
                        }
                        is SearchUiState.Empty -> {
                            binding.progressBar.visibility = View.GONE
                            binding.emptyStateView.visibility = View.VISIBLE
                            binding.searchResultsRecycler.visibility = View.GONE
                            binding.emptyStateText.text = state.message
                        }
                        is SearchUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun performSearch(query: String) {
        viewModel.performSearch(query)
        viewModel.saveRecentSearch(query)
    }

    private fun navigateToDetail(item: AnimeItem) {
        val action = SearchScreenFragmentDirections
            .actionSearchToDetail(item.id, item.title)
        findNavController().navigate(action)
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceRecognition()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _binding = null
    }
}
