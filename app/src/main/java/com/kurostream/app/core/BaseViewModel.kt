package com.kurostream.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kurostream.app.di.DispatcherProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Base ViewModel providing structured concurrency, error handling, and dispatchers.
 *
 * [scope] is a child of [viewModelScope] with [SupervisorJob] so one child failure
 * does not cancel siblings. The entire scope is cancelled when cleart() is called.
 */
abstract class BaseViewModel(
    private val dispatchers: DispatcherProvider? = null,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Unhandled coroutine exception in ${this::class.simpleName}")
    }

    private val supervisorJob = SupervisorJob(viewModelScope.coroutineContext[Job])
    protected val scope: CoroutineScope = CoroutineScope(
        viewModelScope.coroutineContext + supervisorJob + exceptionHandler
    )

    protected val ioDispatcher get() = dispatchers?.io ?: kotlinx.coroutines.Dispatchers.IO
    protected val mainDispatcher get() = dispatchers?.main ?: kotlinx.coroutines.Dispatchers.Main
    protected val defaultDispatcher get() = dispatchers?.default ?: kotlinx.coroutines.Dispatchers.Default

    /**
     * Launch a coroutine on the main dispatcher with automatic error logging.
     */
    protected fun launchMain(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(mainDispatcher) { block() }

    /**
     * Launch a coroutine on the IO dispatcher with automatic error logging.
     */
    protected fun launchIO(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(ioDispatcher) { block() }

    override fun onCleared() {
        supervisorJob.cancel()
        scope.cancel()
        super.onCleared()
    }
}
