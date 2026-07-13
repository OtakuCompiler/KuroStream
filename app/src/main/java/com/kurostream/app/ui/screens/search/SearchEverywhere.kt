// This file is part of KuroStream.
//
// KuroStream is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// KuroStream is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with KuroStream.  If not, see <https://www.gnu.org/licenses/>.

package com.kurostream.app.ui.screens.search

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.TextField
import androidx.tv.material3.TextFieldDefaults
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kurostream.app.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchEverywhere(
    onBackClick: () -> Unit,
    onResultClick: (String) -> Unit,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var isVoiceListening by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(focusRequester)
    ) {
        SearchHeader(
            query = query,
            onQueryChange = { query = it },
            onSearchClick = {
                isSearching = true
                scope.launch {
                    delay(500)
                    searchResults = viewModel.search(query)
                    isSearching = false
                }
            },
            onBackClick = onBackClick,
            onVoiceClick = {
                isVoiceListening = !isVoiceListening
                if (isVoiceListening) {
                    scope.launch {
                        val recognizedText = withContext(Dispatchers.IO) {
                            performVoiceRecognition(context)
                        }
                        query = recognizedText ?: ""
                        isVoiceListening = false
                    }
                }
            },
            isVoiceListening = isVoiceListening,
            onBarcodeClick = { showBarcodeScanner = true },
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            searchResults.isEmpty() && query.isNotEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            searchResults.isNotEmpty() -> {
                SearchResultList(
                    results = searchResults,
                    onItemClick = onResultClick
                )
            }
            
            else -> {
                SearchSuggestions(
                    onSuggestionClick = { query = it }
                )
            }
        }
    }
    
    if (showBarcodeScanner) {
        BarcodeScannerOverlay(
            onBarcodeScanned = { code ->
                query = code
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onBackClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onBarcodeClick: () -> Unit,
    isVoiceListening: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        TextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search anime, movies, shows...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier.weight(1f).height(56.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        IconButton(
            onClick = onVoiceClick,
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (isVoiceListening) {
                        Modifier.border(
                            2.dp,
                            Color(0xFF00E5FF),
                            RoundedCornerShape(24.dp)
                        )
                    } else Modifier
                )
        ) {
            Icon(
                imageVector = if (isVoiceListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isVoiceListening) "Stop listening" else "Voice search",
                tint = if (isVoiceListening) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface
            )
        }
        
        IconButton(onClick = onBarcodeClick) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan barcode",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Button(
            onClick = onSearchClick,
            colors = ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Search")
        }
    }
}

@Composable
private fun SearchSuggestions(
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp)
    ) {
        Text(
            text = "Popular Searches",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        listOf(
            "Attack on Titan",
            "Demon Slayer",
            "One Piece",
            "Jujutsu Kaisen",
            "My Hero Academia",
            "Chainsaw Man"
        ).forEach { suggestion ->
            SuggestionChip(
                text = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Browse by Genre",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Action", "Romance", "Comedy", "Drama",
                "Fantasy", "Sci-Fi", "Thriller", "Horror"
            ).forEach { genre ->
                SuggestionChip(
                    text = genre,
                    onClick = { onSuggestionClick(genre) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        modifier = Modifier.height(40.dp),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SearchResultList(
    results: List<MediaItem>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        state = rememberLazyListState(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        items(results) { item ->
            SearchResultCard(
                item = item,
                onClick = { onItemClick(item.id) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SearchResultCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(item.posterImage),
                contentDescription = item.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp, 168.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${item.year} • ${item.format} • ${item.episodeCount ?: 0} eps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (item.genres.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.genres.take(3).forEach { genre ->
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeScannerOverlay(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scannedCode by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        CameraPreview(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onBarcodeScanned = { code ->
                scannedCode = code
                onBarcodeScanned(code)
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp)
        ) {
            Text(
                text = "Scan Barcode",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Point camera at a barcode or QR code",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
        
        if (scannedCode != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF00E5FF).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Scanned: $scannedCode",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onBarcodeScanned: (String) -> Unit
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraPermissionGranted by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }
    
    if (cameraPermissionGranted) {
        AndroidView(
            factory = {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        LaunchedEffect(context) {
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(Dispatchers.IO.asExecutor()) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            val scanner = BarcodeScanning.getClient()
                            
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let { code ->
                                        withContext(Dispatchers.Main) {
                                            onBarcodeScanned(code)
                                        }
                                    }
                                }
                                .addOnFailureListener { }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
            }
        }
    }
}

private suspend fun performVoiceRecognition(context: Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            val recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
            val speechIntent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            var result: String? = null
            val listener = object : android.speech.RecognitionListener {
                override fun onResults(results: android.os.Bundle?) {
                    results?.getStringArrayList(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                        result = it
                    }
                }
                override fun onError(error: Int) {}
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                override fun onEndOfSession() {}
            }
            
            recognizer.setRecognitionListener(listener)
            recognizer.startListening(speechIntent)
            
            delay(5000)
            recognizer.destroy()
            
            result
        } catch (e: Exception) {
            null
        }
    }
}