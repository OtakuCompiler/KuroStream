package com.kurostream.app.lifecycle

import android.os.StrictMode
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Composable
fun SafeLaunchedEffect(
    key1: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key1) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("SafeLaunchedEffect", "Exception in LaunchedEffect: ${e.message}", e)
        }
    }
}

@Composable
fun SafeLaunchedEffect(
    key1: Any?,
    key2: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key1, key2) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("SafeLaunchedEffect", "Exception in LaunchedEffect: ${e.message}", e)
        }
    }
}

@Composable
fun SafeLaunchedEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key1, key2, key3) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("SafeLaunchedEffect", "Exception in LaunchedEffect: ${e.message}", e)
        }
    }
}

class LifecycleAwareScope(
    private val lifecycle: Lifecycle,
) : CoroutineScope by SupervisorJob() {
    
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            coroutineContext.cancel()
        }
    }
    
    init {
        lifecycle.addObserver(lifecycleObserver)
    }
    
    fun cancel() {
        coroutineContext.cancel()
        lifecycle.removeObserver(lifecycleObserver)
    }
}

@Composable
fun rememberLifecycleAwareScope(lifecycle: Lifecycle): LifecycleAwareScope {
    return remember(lifecycle) { LifecycleAwareScope(lifecycle) }
}

@Composable
fun SafeViewModelScope(
    viewModelStoreOwner: ViewModelStoreOwner,
    block: suspend CoroutineScope.() -> Unit
) {
    val scope = remember { SupervisorJob() }
    
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ViewModel>(viewModelStoreOwner)
    
    LaunchedEffect(viewModel) {
        val job = scope.launch(viewModel.viewModelScope.coroutineContext) {
            try {
                block()
            } catch (e: Exception) {
                Log.e("SafeViewModelScope", "Exception in ViewModelScope: ${e.message}", e)
            }
        }
        
        DisposableEffect(viewModel) {
            onDispose {
                job.cancel()
            }
        }
    }
}

open class LeakSafeViewModel : ViewModel() {
    private val _cleanupJobs = mutableListOf<Job>()
    private val _disposables = mutableListOf<() -> Unit>()
    
    override fun onCleared() {
        super.onCleared()
        
        _cleanupJobs.forEach { it.cancel() }
        _cleanupJobs.clear()
        
        _disposables.forEach { it() }
        _disposables.clear()
    }
    
    fun addCleanupJob(job: Job) {
        _cleanupJobs.add(job)
    }
    
    fun addDisposable(disposable: () -> Unit) {
        _disposables.add(disposable)
    }
    
    fun launchInViewModelScope(block: suspend CoroutineScope.() -> Unit): Job {
        val job = viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e("LeakSafeViewModel", "Exception in viewModelScope: ${e.message}", e)
            }
        }
        _cleanupJobs.add(job)
        return job
    }
}

fun ViewModel.launchInViewModelScope(block: suspend CoroutineScope.() -> Unit): Job {
    return viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            Log.e("ViewModelExt", "Exception in viewModelScope: ${e.message}", e)
        }
    }
}

fun LifecycleOwner.launchWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launchWhenStarted {
        try {
            block()
        } catch (e: Exception) {
            Log.e("LifecycleExt", "Exception in launchWhenStarted: ${e.message}", e)
        }
    }
}

fun LifecycleOwner.launchWhenResumed(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launchWhenResumed {
        try {
            block()
        } catch (e: Exception) {
            Log.e("LifecycleExt", "Exception in launchWhenResumed: ${e.message}", e)
        }
    }
}

fun LifecycleOwner.launchWhenCreated(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launchWhenCreated {
        try {
            block()
        } catch (e: Exception) {
            Log.e("LifecycleExt", "Exception in launchWhenCreated: ${e.message}", e)
        }
    }
}

@Composable
fun StrictModeDebug() {
    DisposableEffect(Unit) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )
            
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            
            Log.d("StrictMode", "StrictMode enabled for debug build")
        }
        
        onDispose {
        }
    }
}

@Composable
fun StrictModeDebug(onViolation: (String) -> Unit = { Log.w("StrictMode", it) }) {
    DisposableEffect(Unit) {
        if (BuildConfig.DEBUG) {
            val originalThreadPolicy = StrictMode.getThreadPolicy()
            val originalVmPolicy = StrictMode.getVmPolicy()
            
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder(originalThreadPolicy)
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )
            
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder(originalVmPolicy)
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        
        onDispose {
        }
    }
}

object LeakDetector {
    private var isEnabled = false
    
    fun enable() {
        if (BuildConfig.DEBUG && !isEnabled) {
            isEnabled = true
            
            System.setProperty("kotlinx.coroutines.debug", "on")
            
            val leakCanary = try {
                Class.forName("leakcanary.LeakCanary")
                leakCanary.getMethod("install", android.app.Application::class.java)
                true
            } catch (e: Exception) {
                false
            }
            
            if (leakCanary) {
                Log.d("LeakDetector", "LeakCanary integration enabled")
            }
            
            Log.d("LeakDetector", "Leak detection enabled")
        }
    }
    
    fun disable() {
        isEnabled = false
    }
}

@Composable
fun rememberLeakSafeScope(): CoroutineScope {
    return remember { SupervisorJob() }
}

@Composable
fun LaunchedEffectLeakSafe(
    key: Any?,
    onLeakDetected: (String) -> Unit = { Log.w("LeakSafe", it) },
    block: suspend CoroutineScope.() -> Unit
) {
    val scope = rememberLeakSafeScope()
    val job = remember { Job() }
    
    LaunchedEffect(key) {
        val childJob = scope.launch(job) {
            try {
                block()
            } catch (e: Exception) {
                Log.e("LaunchedEffectLeakSafe", "Exception: ${e.message}", e)
            }
        }
        
        DisposableEffect(key) {
            onDispose {
                childJob.cancel()
                job.cancel()
            }
        }
    }
}

class ViewModelScopeTracker(
    private val viewModel: ViewModel,
) {
    private val _activeScopes = mutableListOf<Job>()
    
    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        val job = viewModel.viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e("ViewModelScopeTracker", "Exception: ${e.message}", e)
            }
        }
        _activeScopes.add(job)
        job.invokeOnCompletion { _activeScopes.remove(job) }
        return job
    }
    
    fun cancelAll() {
        _activeScopes.forEach { it.cancel() }
        _activeScopes.clear()
    }
    
    val activeScopeCount: Int get() = _activeScopes.count { it.isActive }
}

@Composable
fun rememberViewModelScopeTracker(viewModel: ViewModel): ViewModelScopeTracker {
    return remember(viewModel) { ViewModelScopeTracker(viewModel) }
}