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

package com.kurostream.extensions.search

import com.kurostream.extensions.domain.model.CatalogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoSearchSource @Inject constructor() {

    private val demoCatalog = listOf(
        CatalogItem(id = "demo_1", title = "Attack on Titan", posterUrl = "https://example.com/aot.jpg", type = "anime", source = "demo"),
        CatalogItem(id = "demo_2", title = "Demon Slayer", posterUrl = "https://example.com/ds.jpg", type = "anime", source = "demo"),
        CatalogItem(id = "demo_3", title = "Jujutsu Kaisen", posterUrl = "https://example.com/jjk.jpg", type = "anime", source = "demo"),
        CatalogItem(id = "demo_4", title = "One Piece", posterUrl = "https://example.com/op.jpg", type = "anime", source = "demo"),
        CatalogItem(id = "demo_5", title = "My Hero Academia", posterUrl = "https://example.com/mha.jpg", type = "anime", source = "demo")
    )

    fun search(query: String): Flow<Result<List<CatalogItem>>> = flow {
        val filtered = demoCatalog.filter { it.title.contains(query, ignoreCase = true) }
        emit(Result.success(filtered))
    }
}
