package com.kurostream.app.lifecycle

import android.app.Application
import android.os.Build
import android.os.Debug
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.LinkedHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object LeakDetector {
    private var isEnabled = false
    /** Bounded tracked-object registry with simple size cap. */
    private val trackedObjects = object : LinkedHashMap<String, WeakReference<Any>>(0, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WeakReference<Any>>): Boolean {
            val shouldRemove = size > MAX_TRACKED_OBJECTS
            if (shouldRemove) {
                Timber.tag("LeakDetector").d("Evicting tracked object: %s", eldest.key)
            }
            return shouldRemove
        }
    }
    private const val MAX_TRACKED_OBJECTS = 1000
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "LeakDetector-Cleanup").apply { isDaemon = true }
    }
    
    fun enable() {
        if (isEnabled) return
        isEnabled = true
        
        if (com.kurostream.app.BuildConfig.DEBUG) {
            schedulePeriodicCheck()
            Timber.tag("LeakDetector").d( "Leak detection enabled")
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
                }
            } catch (e: Exception) {
                Timber.tag("LeakDetector").e( "Error during leak check", e)
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
    /** Bounded activity reference registry. */
    private val activityRefs = object : LinkedHashMap<String, WeakReference<android.app.Activity>>(0, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WeakReference<android.app.Activity>>): Boolean {
            return size > MAX_REF_TRACKING
        }
    }
    /** Bounded fragment reference registry. */
    private val fragmentRefs = object : LinkedHashMap<String, WeakReference<androidx.fragment.app.Fragment>>(0, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WeakReference<androidx.fragment.app.Fragment>>): Boolean {
            return size > MAX_REF_TRACKING
        }
    }
    /** Bounded ViewModel reference registry. */
    private val viewModelRefs = object : LinkedHashMap<String, WeakReference<androidx.lifecycle.ViewModel>>(0, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WeakReference<androidx.lifecycle.ViewModel>>): Boolean {
            return size > MAX_REF_TRACKING
        }
    }
    private companion object {
        const val MAX_REF_TRACKING = 200
    }
    
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
        val leakedActivities = activityRefs.entries.filter { it.value.get() == null }.map { it.key }
        val leakedFragments = fragmentRefs.entries.filter { it.value.get() == null }.map { it.key }
        val leakedViewModels = viewModelRefs.entries.filter { it.value.get() == null }.map { it.key }
        
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