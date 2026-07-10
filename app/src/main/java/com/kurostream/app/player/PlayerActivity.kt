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

package com.kurostream.app.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import com.kurostream.app.ui.theme.AnimeStreamTVTheme
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    companion object {
        private const val EXTRA_MEDIA_ID = "media_id"
        private const val EXTRA_EPISODE_ID = "episode_id"
        private const val EXTRA_START_POSITION = "start_position_ms"

        fun createIntent(
            context: Context,
            mediaId: String,
            episodeId: String? = null,
            startPositionMs: Long = 0L
        ): Intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(EXTRA_MEDIA_ID, mediaId)
            putExtra(EXTRA_EPISODE_ID, episodeId)
            putExtra(EXTRA_START_POSITION, startPositionMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID) ?: return finish()
        val episodeId = intent.getStringExtra(EXTRA_EPISODE_ID)
        val startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0L)

        viewModel.preparePlayback(mediaId, episodeId, startPosition)

        setContent {
            AnimeStreamTVTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }
}
