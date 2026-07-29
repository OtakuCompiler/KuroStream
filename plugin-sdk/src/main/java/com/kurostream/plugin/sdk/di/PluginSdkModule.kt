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

package com.kurostream.plugin.sdk.di

import android.content.Context
import com.kurostream.plugin.sdk.manifest.ExtensionManifestValidator
import com.kurostream.plugin.sdk.security.PermissiveSignatureVerifier
import com.kurostream.plugin.sdk.security.RealSignatureVerifier
import com.kurostream.plugin.sdk.security.SignatureVerifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PluginSdkModule {
    @Provides
    @Singleton
    fun provideSignatureVerifier(
        context: Context
    ): SignatureVerifier {
        // In production, always use RealSignatureVerifier
        // For development builds, you can switch to PermissiveSignatureVerifier
        // by changing this to: return PermissiveSignatureVerifier()
        return RealSignatureVerifier(context)
    }
    
    @Provides
    @Singleton
    fun provideExtensionManifestValidator(): ExtensionManifestValidator {
        return ExtensionManifestValidator()
    }
}