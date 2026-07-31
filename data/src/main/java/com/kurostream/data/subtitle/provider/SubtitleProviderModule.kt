// This file is part of KuroStream.
//
// SubtitleProviderModule — Hilt multibinding for SubtitleProvider set.
// Each provider is bound into a Set<SubtitleProvider> so KuroSubtitleEngine
// can receive all providers via List<SubtitleProvider>.
//
// SPDX-License-Identifier: GPL-3.0-only
package com.kurostream.data.subtitle.provider

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SubtitleProviderModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideOpenSubtitles(provider: OpenSubtitlesProvider): SubtitleProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideSubDL(provider: SubDLProvider): SubtitleProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideTorrentEmbedded(provider: TorrentEmbeddedProvider): SubtitleProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideExtension(provider: ExtensionSubtitleProvider): SubtitleProvider = provider
}
