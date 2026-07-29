package com.kurostream.app.benchmark

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BenchmarkScreen()
        }
    }
}

@Composable
fun BenchmarkScreen() {
    val isRunning by remember { mutableStateOf(false) }
    val results by remember { mutableStateOf<String>("") }
    val currentTest by remember { mutableStateOf<String>("") }
    val progress by remember { mutableStateOf<Float>(0f) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = remember { lifecycleOwner.lifecycleScope }
    
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text(
            text = "KuroStream Benchmark Mode",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.Cyan,
        )
        
        androidx.compose.material3.Text(
            text = "Device: ${Build.MODEL} (${Build.MANUFACTURER})",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
        )
        
        androidx.compose.material3.Text(
            text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            fontSize = 14.sp,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
        )
        
        Spacer(modifier = Modifier.size(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Benchmark Tests",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Cyan,
                )
                
                BenchmarkButton(
                    text = "Video Decode Benchmark",
                    description = "Tests H264, HEVC, VP9 decode performance at 1080p/4K",
                    isRunning = isRunning && currentTest == "decode",
                    onClick = { scope.launch { runDecodeBenchmark() } }
                )
                
                BenchmarkButton(
                    text = "Memory Benchmark",
                    description = "Measures allocation rate, GC overhead, peak memory",
                    isRunning = isRunning && currentTest == "memory",
                    onClick = { scope.launch { runMemoryBenchmark() } }
                )
                
                BenchmarkButton(
                    text = "Thermal Benchmark",
                    description = "Stress tests CPU to measure thermal throttling",
                    isRunning = isRunning && currentTest == "thermal",
                    onClick = { scope.launch { runThermalBenchmark() } }
                )
                
                BenchmarkButton(
                    text = "Run All Benchmarks",
                    description = "Executes all benchmarks sequentially (~2 min)",
                    isRunning = isRunning && currentTest == "all",
                    onClick = { scope.launch { runAllBenchmarks() } }
                )
            }
        }
        
        if (isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Running: $currentTest",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.Yellow,
                    )
                    
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { progress },
                        color = androidx.compose.ui.graphics.Color.Cyan,
                        trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                    )
                }
            }
        }
        
        if (results.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Results",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.Cyan,
                    )
                    
                    Text(
                        text = results,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun BenchmarkButton(
    text: String,
    description: String,
    isRunning: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isRunning) 
                androidx.compose.ui.graphics.Color.Yellow.copy(alpha = 0.2f)
            else 
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)
        )
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRunning) androidx.compose.ui.graphics.Color.Yellow else androidx.compose.ui.graphics.Color.White,
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                )
                if (isRunning) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = androidx.compose.ui.graphics.Color.Cyan,
                    )
                }
            }
        }
    }
}

private suspend fun runDecodeBenchmark() {
    withContext(Dispatchers.IO) {
        // Run decode benchmarks
    }
}

private suspend fun runMemoryBenchmark() {
    withContext(Dispatchers.IO) {
        // Run memory benchmark
    }
}

private suspend fun runThermalBenchmark() {
    withContext(Dispatchers.IO) {
        // Run thermal benchmark
    }
}

private suspend fun runAllBenchmarks() {
    withContext(Dispatchers.IO) {
        // Run all benchmarks
    }
}