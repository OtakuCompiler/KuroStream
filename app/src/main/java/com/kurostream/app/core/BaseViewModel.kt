package com.kurostream.app.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseViewModel : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Unhandled coroutine exception in ${this::class.simpleName}")
    }

    private val supervisorJob = SupervisorJob()
    protected val scope: CoroutineScope = CoroutineScope(
        supervisorJob + exceptionHandler
    )

    protected fun launchMain(block: suspend CoroutineScope.() -> Unit) =
        scope.launch(kotlinx.coroutines.Dispatchers.Main) { block() }

    protected fun launchIO(block: suspend CoroutineScope.() -> Unit) =
        scope.launch(kotlinx.coroutines.Dispatchers.IO) { block() }

    override fun onCleared() {
        supervisorJob.cancel()
        scope.cancel()
        super.onCleared()
    }
}
