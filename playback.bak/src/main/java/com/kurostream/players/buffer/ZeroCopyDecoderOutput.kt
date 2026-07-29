package com.kurostream.players.buffer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZeroCopyDecoderOutput @Inject constructor() {
    fun sendFrame(frame: Any) { }
}
