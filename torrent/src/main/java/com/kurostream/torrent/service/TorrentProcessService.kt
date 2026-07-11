package com.kurostream.torrent.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Messenger
import android.util.Log
import com.kurostream.torrent.engine.TorrentEngine
import com.kurostream.torrent.engine.WriteCoalescer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TorrentProcessService : Service() {

    companion object {
        const val TAG = "TorrentProcessService"
        const val ACTION_START = "com.kurostream.torrent.action.START_ENGINE"
        const val ACTION_STOP = "com.kurostream.torrent.action.STOP_ENGINE"
        const val EXTRA_TORRENT_URI = "extra_torrent_uri"
        const val EXTRA_SAVE_PATH = "extra_save_path"
        const val EXTRA_SEQUENTIAL = "extra_sequential"
    }

    @Inject
    lateinit var engine: TorrentEngine

    @Inject
    lateinit var writeCoalescer: WriteCoalescer

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messenger: Messenger? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Torrent process service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val uri = intent.getStringExtra(EXTRA_TORRENT_URI)
                val savePath = intent.getStringExtra(EXTRA_SAVE_PATH)
                val sequential = intent.getBooleanExtra(EXTRA_SEQUENTIAL, true)

                if (uri != null) {
                    scope.launch {
                        try {
                            if (uri.startsWith("magnet:")) {
                                engine.addMagnet(uri, savePath ?: engine.toString(), sequential)
                            } else {
                                engine.addTorrentFile(uri, savePath ?: engine.toString(), sequential)
                            }
                            Log.i(TAG, "Torrent added in process: $uri")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to add torrent in process", e)
                        }
                    }
                }
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stopping torrent engine in process")
                engine.stop()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        messenger = Messenger(engine)
        return messenger?.binder
    }

    override fun onDestroy() {
        engine.stop()
        super.onDestroy()
        Log.i(TAG, "Torrent process service destroyed")
    }
}
