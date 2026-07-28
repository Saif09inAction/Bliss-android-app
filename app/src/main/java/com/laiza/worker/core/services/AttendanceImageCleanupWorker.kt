package com.laiza.worker.core.services

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.laiza.worker.core.local.dao.AttendanceDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@HiltWorker
class AttendanceImageCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val attendanceDao: AttendanceDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("Starting background cleanup of attendance images older than 10 days...")
        
        return try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -10)
            val cutoffDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

            val recordsToPurge = attendanceDao.getRecordsWithOldImages(cutoffDate)
            var deletedFilesCount = 0

            for (record in recordsToPurge) {
                record.signInImageLocalPath?.let { path ->
                    if (path.startsWith("content://")) {
                        try {
                            val uri = Uri.parse(path)
                            if (applicationContext.contentResolver.delete(uri, null, null) > 0) {
                                deletedFilesCount++
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to delete content URI: $path")
                        }
                    } else {
                        val file = File(path)
                        if (file.exists() && file.delete()) {
                            deletedFilesCount++
                        }
                    }
                }
                record.signOutImageLocalPath?.let { path ->
                    if (path.startsWith("content://")) {
                        try {
                            val uri = Uri.parse(path)
                            if (applicationContext.contentResolver.delete(uri, null, null) > 0) {
                                deletedFilesCount++
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to delete content URI: $path")
                        }
                    } else {
                        val file = File(path)
                        if (file.exists() && file.delete()) {
                            deletedFilesCount++
                        }
                    }
                }
            }

            val updatedRows = attendanceDao.clearOldImagePaths(cutoffDate)
            
            Timber.d("Cleanup completed. Deleted $deletedFilesCount local image files from disk. Cleared DB paths on $updatedRows rows.")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error occurred during daily photo cleanup.")
            Result.failure()
        }
    }
}
