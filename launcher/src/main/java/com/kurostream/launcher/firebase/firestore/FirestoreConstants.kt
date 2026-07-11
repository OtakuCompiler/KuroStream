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

package com.kurostream.launcher.firebase.firestore

object FirestoreConstants {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_PURCHASES = "purchases"
    const val COLLECTION_DEVICES = "devices"
    const val COLLECTION_WATCH_HISTORY = "watch_history"
    const val COLLECTION_FAVORITES = "favorites"

    const val FIELD_PURCHASED_ADDONS = "purchasedAddons"
    const val FIELD_LAST_SYNCED = "lastSynced"
    const val FIELD_USER_ID = "userId"
    const val FIELD_ADDON_ID = "addonId"
    const val FIELD_PURCHASED_AT = "purchasedAt"

    const val DOCUMENT_ADDONS = "addons"
}
