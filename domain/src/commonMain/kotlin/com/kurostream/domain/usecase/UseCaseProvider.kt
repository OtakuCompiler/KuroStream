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

package com.kurostream.domain.usecase

import com.kurostream.domain.repository.MediaRepository
import com.kurostream.domain.repository.ProfileRepository
import com.kurostream.domain.repository.SettingsRepository
import com.kurostream.domain.usecase.download.*
import com.kurostream.domain.usecase.favorite.*
import com.kurostream.domain.usecase.media.*
import com.kurostream.domain.usecase.profile.*
import com.kurostream.domain.usecase.settings.*
import com.kurostream.domain.usecase.subtitle.*
import com.kurostream.domain.usecase.watchhistory.*

class UseCaseProvider(
    mediaRepository: MediaRepository,
    profileRepository: ProfileRepository,
    settingsRepository: SettingsRepository
) {
    // Media use cases
    val getMediaByCategory = GetMediaByCategoryUseCase(mediaRepository)
    val getMediaById = GetMediaByIdUseCase(mediaRepository)
    val searchLocal = SearchLocalUseCase(mediaRepository)
    val searchRemote = SearchRemoteUseCase(mediaRepository)
    val getTrending = GetTrendingUseCase(mediaRepository)
    val getRemoteDetails = GetRemoteDetailsUseCase(mediaRepository)
    val saveMediaItem = SaveMediaItemUseCase(mediaRepository)
    val deleteMediaItem = DeleteMediaItemUseCase(mediaRepository)

    // Watch history use cases
    val getWatchHistory = GetWatchHistoryUseCase(mediaRepository)
    val getWatchHistoryByMedia = GetWatchHistoryByMediaUseCase(mediaRepository)
    val saveWatchHistory = SaveWatchHistoryUseCase(mediaRepository)
    val deleteWatchHistory = DeleteWatchHistoryUseCase(mediaRepository)
    val clearWatchHistory = ClearWatchHistoryUseCase(mediaRepository)

    // Favorite use cases
    val getFavorites = GetFavoritesUseCase(mediaRepository)
    val isFavorite = IsFavoriteUseCase(mediaRepository)
    val addFavorite = AddFavoriteUseCase(mediaRepository)
    val removeFavorite = RemoveFavoriteUseCase(mediaRepository)
    val toggleFavorite = ToggleFavoriteUseCase(
        isFavoriteUseCase = isFavorite,
        addFavoriteUseCase = addFavorite,
        removeFavoriteUseCase = removeFavorite
    )

    // Download use cases
    val getDownloads = GetDownloadsUseCase(mediaRepository)
    val getDownload = GetDownloadUseCase(mediaRepository)
    val saveDownload = SaveDownloadUseCase(mediaRepository)
    val updateDownloadProgress = UpdateDownloadProgressUseCase(mediaRepository)
    val deleteDownload = DeleteDownloadUseCase(mediaRepository)
    val pauseDownload = PauseDownloadUseCase(updateDownloadProgress)
    val resumeDownload = ResumeDownloadUseCase(updateDownloadProgress)

    // Subtitle use cases
    val searchSubtitles = SearchSubtitlesUseCase(mediaRepository)
    val getBestSubtitle = GetBestSubtitleUseCase()

    // Profile use cases
    val getProfiles = GetProfilesUseCase(profileRepository)
    val getProfile = GetProfileUseCase(profileRepository)
    val createProfile = CreateProfileUseCase(profileRepository)
    val updateProfile = UpdateProfileUseCase(profileRepository)
    val deleteProfile = DeleteProfileUseCase(profileRepository)
    val getActiveProfile = GetActiveProfileUseCase(profileRepository)
    val setActiveProfile = SetActiveProfileUseCase(profileRepository)

    // Settings use cases
    val getSetting = { <T>(key: String) -> GetSettingUseCase<T>(settingsRepository, key) }
    val setSetting = { <T>(key: String) -> SetSettingUseCase<T>(settingsRepository, key) }
    val boolSetting = { key: String, default: Boolean -> BoolSettingUseCase(settingsRepository, key, default) }
    val intSetting = { key: String, default: Int -> IntSettingUseCase(settingsRepository, key, default) }
    val stringSetting = { key: String, default: String -> StringSettingUseCase(settingsRepository, key, default) }
}