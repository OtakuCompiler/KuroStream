package com.kurostream.app.voice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.tint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VoiceSearchScreen(
    viewModel: VoiceSearchViewModel = hiltViewModel(),
    onSearchResult: (String) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val results by viewModel.recognitionResults.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val error by viewModel.error.collectAsState()
    var pulse by remember { mutableStateOf(false) }

    val voiceActivity = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (matches != null && matches.isNotEmpty()) {
                    onSearchResult(matches.first())
                }
            }
        }
    )

    androidx.compose.runtime.LaunchedEffect(isListening) {
        if (isListening) {
            pulse = true
            while (pulse) {
                delay(500)
                pulse = !pulse
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Voice visualizer
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        color = if (isListening) MaterialTheme.colorScheme.primary else Color.Gray,
                        colorFilter = if (isListening) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                    )
                    .graphicsLayer {
                        scaleX = if (pulse) 1.1f else 1f
                        scaleY = if (pulse) 1.1f else 1f
                    }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isListening) "Listening..." else "Tap to speak",
                    tint = Color.White,
                    modifier = Modifier.size(80.dp).padding(60.dp),
                )
            }

            Text(
                text = if (isListening) "Listening..." else "Tap and hold to speak",
                color = Color.White,
                fontSize = 24.sp,
            )

            if (results.isNotEmpty()) {
                Text(
                    text = "Heard: ${results.first()}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                )
            }

            error?.let { err ->
                Text(
                    text = err,
                    color = Color.Red,
                    fontSize = 16.sp,
                )
            }

            if (!isListening && results.isEmpty()) {
                Button(
                    onClick = { viewModel.startListening() },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(24.dp))
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(8.dp))
                    Text("Start Voice Search")
                }
            }

            if (isListening) {
                Button(
                    onClick = { viewModel.stopListening() },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Stop Listening", color = Color.Red)
                }
            }

            if (results.isNotEmpty()) {
                Button(
                    onClick = { onSearchResult(results.first()) },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Search for \"${results.first()}\"")
                }
            }

            Button(
                onClick = onClose,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
class VoiceSearchActivity : ComponentActivity() {

    private val viewModel: VoiceSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initializeSpeechRecognizer(this)
        setContent {
            VoiceSearchScreen(
                viewModel = viewModel,
                onSearchResult = { query ->
                    val intent = Intent().putExtra("search_query", query)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                },
                onClose = { finish() },
            )
        }
    }

    override fun onDestroy() {
        viewModel.destroy()
        super.onDestroy()
    }
}