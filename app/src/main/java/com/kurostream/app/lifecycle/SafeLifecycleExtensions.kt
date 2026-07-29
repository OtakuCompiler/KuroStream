package com.kurostream.app.lifecycle

import android.os.StrictMode
import timber.log.Timber
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
import androidx.lifecycle.lifecycleScope
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
            Timber.tag("SafeLaunchedEffect").e("Exception in LaunchedEffect: ${e.message}", e)
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
            Timber.tag("SafeLaunchedEffect").e("Exception in LaunchedEffect: ${e.message}", e)
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
            Timber.tag("SafeLaunchedEffect").e("Exception in LaunchedEffect: ${e.message}", e)
        }
    }
}

class LifecycleAwareScope(
    private val lifecycle: Lifecycle,
) : CoroutineScope {
    override val coroutineContext = SupervisorJob() + kotlinx.coroutines.Dispatchers.Main

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
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ViewModel>(viewModelStoreOwner)
    
    LaunchedEffect(viewModel) {
        viewModel.viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Timber.tag("SafeViewModelScope").e("Exception in ViewModelScope: ${e.message}", e)
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
                Timber.tag("LeakSafeViewModel").e("Exception in viewModelScope: ${e.message}", e)
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
            Timber.tag("ViewModelExt").e("Exception in viewModelScope: ${e.message}", e)
        }
    }
}

@Suppress("DEPRECATION")
fun LifecycleOwner.launchWhenStarted(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
        try {
            block()
        } catch (e: Exception) {
            Timber.tag("LifecycleExt").e("Exception in launchWhenStarted: ${e.message}", e)
        }
    }
}

@Suppress("DEPRECATION")
fun LifecycleOwner.launchWhenResumed(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
        try {
            block()
        } catch (e: Exception) {
            Timber.tag("LifecycleExt").e("Exception in launchWhenResumed: ${e.message}", e)
        }
    }
}

@Suppress("DEPRECATION")
fun LifecycleOwner.launchWhenCreated(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launchWhenCreated {
        try {
            block()
        } catch (e: Exception) {
            Timber.tag("LifecycleExt").e("Exception in launchWhenCreated: ${e.message}", e)
        }
    }
}

@Composable
fun StrictModeDebug() {
    DisposableEffect(Unit) {
        if (com.kurostream.app.BuildConfig.DEBUG) {
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
            
            Timber.tag("StrictMode").d("StrictMode enabled for debug build")
        }
        
        onDispose {
        }
    }
}

@Composable
fun StrictModeDebug(onViolation: (String) -> Unit = { Timber.tag("StrictMode").w(it) }) {
    DisposableEffect(Unit) {
        if (com.kurostream.app.BuildConfig.DEBUG) {
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

@Composable
fun rememberLeakSafeScope(): CoroutineScope {
    return remember { kotlinx.coroutines.CoroutineScope(SupervisorJob()) }
}

@Composable
fun LaunchedEffectLeakSafe(
    key: Any?,
    onLeakDetected: (String) -> Unit = { Timber.tag("LeakSafe").w(it) },
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key) {
        try {
            block()
        } catch (e: Exception) {
            Timber.tag("LaunchedEffectLeakSafe").e("Exception: ${e.message}", e)
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
                Timber.tag("ViewModelScopeTracker").e("Exception: ${e.message}", e)
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