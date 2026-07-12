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

package com.kurostream.data.datastore

import androidx.datastore.core.Serializer
import com.kurostream.datastore.ProfileProto
import java.io.InputStream
import java.io.OutputStream

object ProfileSerializer : Serializer<ProfileProto.ProfilePreferences> {
    override val defaultValue: ProfileProto.ProfilePreferences = ProfileProto.ProfilePreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): ProfileProto.ProfilePreferences {
        return ProfileProto.ProfilePreferences.parseFrom(input)
    }

    override suspend fun writeTo(t: ProfileProto.ProfilePreferences, output: OutputStream) {
        t.writeTo(output)
    }
}
