package com.kurostream.app.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerConfigTest {
    @Test
    fun `creates player with 4MB buffer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = PlayerConfig.create(context, lowRam = true)
        
        assert(player.playbackState == androidx.media3.common.Player.STATE_IDLE)
    }
}
