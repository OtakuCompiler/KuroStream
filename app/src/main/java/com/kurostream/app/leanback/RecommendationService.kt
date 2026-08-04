package com.kurostream.app.leanback

import android.app.IntentService
import android.content.Intent
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.PreviewProgram
import com.kurostream.app.model.MediaItem
import com.kurostream.domain.entity.MediaItem as DomainMediaItem
import com.kurostream.domain.repository.MediaRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class RecommendationService : IntentService("RecommendationService") {

    @Inject lateinit var mediaRepository: MediaRepository

    override fun onHandleIntent(intent: Intent?) {
        runBlocking {
            val domainItems = mediaRepository.getTrending()
            val items = domainItems.map { d ->
                MediaItem(
                    id = d.id,
                    title = d.title,
                    description = d.description,
                    posterUrl = d.posterUrl,
                    backdropUrl = d.backdropUrl,
                    genre = d.genre,
                    rating = d.rating,
                    year = d.year,
                    duration = d.duration,
                )
            }
            items.take(5).forEachIndexed { index, item ->
                addRecommendation(item, index)
            }
        }
    }

    private fun addRecommendation(item: MediaItem, index: Int) {
        val builder = PreviewProgram.Builder()
            .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
            .setTitle(item.title)
            .setDescription(item.description)
            .setPosterArtUri(android.net.Uri.parse(item.posterUrl))
            .setIntentUri(android.net.Uri.parse("kurostream://details/${item.id}"))
            .setWeight(index)
            .setInternalProviderId(item.id)

        contentResolver.insert(
            TvContractCompat.PreviewPrograms.CONTENT_URI,
            builder.build().toContentValues()
        )
    }
}
