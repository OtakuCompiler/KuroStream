package com.kurostream.app.lifecycle

import android.app.Application
import android.os.Build
import android.os.Debug
import android.util.Log
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object LeakDetector {
    private var isEnabled = false
    private val trackedObjects = ConcurrentHashMap<String, WeakReference<Any>>()
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "LeakDetector-Cleanup").apply { isDaemon = true }
    }
    
    fun enable() {
        if (isEnabled) return
        isEnabled = true
        
        if (BuildConfig.DEBUG) {
            schedulePeriodicCheck()
            Log.d("LeakDetector", "Leak detection enabled")
        }
    }
    
    fun trackObject(name: String, obj: Any) {
        if (!isEnabled) return
        trackedObjects[name] = WeakReference(obj)
    }
    
    fun untrackObject(name: String) {
        trackedObjects.remove(name)
    }
    
    fun checkForLeaks(): Int {
        var leakedCount = 0
        
        trackedObjects.forEach { (name, ref) ->
            if (ref.get() != null) {
                // Object still reachable
            } else {
                // Object has been GC'd, remove from tracking
                trackedObjects.remove(name)
            }
        }
        
        return leakedCount
    }
    
    private fun schedulePeriodicCheck() {
        cleanupExecutor.scheduleAtFixedRate({
            try {
                if (isEnabled) {
                    val count = checkForLeaks()
                    if (count > 0) {
                        Timber.w("LeakDetector: Found $count potentially leaked objects")
                    }
                    
                    // Force GC for more accurate detection
                    System.gc()
                    System.runFinalization()
                }
            } catch (e: Exception) {
                Log.e("LeakDetector", "Error during leak check", e)
            }
        }, 30, 30, TimeUnit.SECONDS)
    }
    
    fun disable() {
        isEnabled = false
        trackedObjects.clear()
        cleanupExecutor.shutdown()
    }
    
    fun dumpTrackedObjects(): String {
        val builder = StringBuilder()
        builder.append("Tracked objects:\n")
        trackedObjects.forEach { (name, ref) ->
            val obj = ref.get()
            builder.append("  $name: ${if (obj != null) "ALIVE" else "GC'd"}\n")
        }
        return builder.toString()
    }
}

class LeakWatcher(
    private val application: Application,
) {
    private val activityRefs = ConcurrentHashMap<String, WeakReference<android.app.Activity>>()
    private val fragmentRefs = ConcurrentHashMap<String, WeakReference<androidx.fragment.app.Fragment>>()
    private val viewModelRefs = ConcurrentHashMap<String, WeakReference<androidx.lifecycle.ViewModel>>()
    
    fun watchActivity(activity: android.app.Activity) {
        val name = "${activity.javaClass.simpleName}@${System.identityHashCode(activity)}"
        activityRefs[name] = WeakReference(activity)
        LeakDetector.trackObject("Activity:$name", activity)
    }
    
    fun watchFragment(fragment: androidx.fragment.app.Fragment) {
        val name = "${fragment.javaClass.simpleName}@${System.identityHashCode(fragment)}"
        fragmentRefs[name] = WeakReference(fragment)
        LeakDetector.trackObject("Fragment:$name", fragment)
    }
    
    fun watchViewModel(viewModel: androidx.lifecycle.ViewModel) {
        val name = "${viewModel.javaClass.simpleName}@${System.identityHashCode(viewModel)}"
        viewModelRefs[name] = WeakReference(viewModel)
        LeakDetector.trackObject("ViewModel:$name", viewModel)
    }
    
    fun checkLeaks(): LeakReport {
        val leakedActivities = activityRefs.entries.filter { it.value.get() == null }.keys.toList()
        val leakedFragments = fragmentRefs.entries.filter { it.value.get() == null }.keys.toList()
        val leakedViewModels = viewModelRefs.entries.filter { it.value.get() == null }.keys.toList()
        
        return LeakReport(
            leakedActivities = leakedActivities,
            leakedFragments = leakedFragments,
            leakedViewModels = leakedViewModels,
        )
    }
    
    data class LeakReport(
        val leakedActivities: List<String>,
        val leakedFragments: List<String>,
        val leakedViewModels: List<String>,
    ) {
        val hasLeaks: Boolean get() = leakedActivities.isNotEmpty() || leakedFragments.isNotEmpty() || leakedViewModels.isNotEmpty()
        val totalLeaks: Int get() = leakedActivities.size + leakedFragments.size + leakedViewModels.size
    }
}