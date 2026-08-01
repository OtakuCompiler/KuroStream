package com.kurostream.domain.usecase

import com.kurostream.domain.model.PlaybackUrl
import com.kurostream.domain.repository.MediaRepository
import com.kurostream.domain.result.Result
import javax.inject.Inject

class GetPlaybackUrlUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaId: String, episodeId: String?): Result<PlaybackUrl> {
        return repository.getPlaybackUrl(mediaId, episodeId)
    }
}