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

package com.kurostream.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kurostream.data.worker.DownloadWorker
import timber.log.Timber

/**
 * Resumes pending downloads after device boot or app package replacement.
 *
 * Enqueues a unique tagged work request ([DownloadWorker.UNIQUE_WORK_NAME]) that the
 * download pipeline can pick up. The actual enumeration of pending downloads is
 * delegated to the worker layer; this receiver only kicks off the resume flow.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.i("Boot/replaced received — resuming downloads")
                enqueueResumeDownloads(context)
            }
        }
    }

    private fun enqueueResumeDownloads(context: Context) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag(DownloadWorker.UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DownloadWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
