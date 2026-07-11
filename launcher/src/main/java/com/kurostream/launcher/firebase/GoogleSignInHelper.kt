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

package com.kurostream.launcher.firebase

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GoogleSignInHelper private constructor() {

    companion object {
        const val RC_SIGN_IN = 9001

        /**
         * Register for Google Sign-In result in an Activity
         */
        fun registerForResult(
            activity: FragmentActivity,
            onResult: (Result<String>) -> Unit
        ): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            onResult(Result.success(idToken))
                        } else {
                            onResult(Result.failure(Exception("ID token is null")))
                        }
                    } catch (e: ApiException) {
                        onResult(Result.failure(e))
                    }
                } else {
                    onResult(Result.failure(Exception("Sign-in cancelled")))
                }
            }
        }

        /**
         * Register for Google Sign-In result in a Fragment
         */
        fun registerForResult(
            fragment: Fragment,
            onResult: (Result<String>) -> Unit
        ): ActivityResultLauncher<Intent> {
            return fragment.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            onResult(Result.success(idToken))
                        } else {
                            onResult(Result.failure(Exception("ID token is null")))
                        }
                    } catch (e: ApiException) {
                        onResult(Result.failure(e))
                    }
                } else {
                    onResult(Result.failure(Exception("Sign-in cancelled")))
                }
            }
        }
    }
}
