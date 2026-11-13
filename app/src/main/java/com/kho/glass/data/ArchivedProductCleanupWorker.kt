package com.kho.glass.data

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.IOException
import java.util.concurrent.TimeUnit

class ArchivedProductCleanupWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val warehouseId = inputData.getString(KEY_WAREHOUSE_ID) ?: return Result.failure()
        return try {
            val deleted = LocalStore(applicationContext, userId, warehouseId)
                .deleteExpiredArchivedProducts()
            val accountUid = inputData.getString(KEY_ACCOUNT_UID).orEmpty()
            if (deleted > 0 && accountUid.isNotBlank()) {
                FirestoreWarehouseSync.schedule(
                    applicationContext,
                    accountUid,
                    userId,
                    warehouseId,
                    inputData.getString(KEY_REMOTE_WAREHOUSE_ID).orEmpty()
                        .ifBlank { warehouseId }
                )
            }
            Result.success()
        } catch (failure: IOException) {
            Result.retry()
        } catch (failure: SQLiteException) {
            Result.retry()
        }
    }

    companion object {
        private const val KEY_USER_ID = "archive_cleanup_user_id"
        private const val KEY_WAREHOUSE_ID = "archive_cleanup_warehouse_id"
        private const val KEY_ACCOUNT_UID = "archive_cleanup_account_uid"
        private const val KEY_REMOTE_WAREHOUSE_ID = "archive_cleanup_remote_warehouse_id"

        fun schedule(
            context: Context,
            userId: String,
            warehouseId: String,
            accountUid: String?,
            remoteWarehouseId: String
        ) {
            val request = PeriodicWorkRequestBuilder<ArchivedProductCleanupWorker>(
                1,
                TimeUnit.DAYS
            ).setInputData(
                workDataOf(
                    KEY_USER_ID to userId,
                    KEY_WAREHOUSE_ID to warehouseId,
                    KEY_ACCOUNT_UID to accountUid.orEmpty(),
                    KEY_REMOTE_WAREHOUSE_ID to remoteWarehouseId
                )
            ).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                "archive-cleanup-$userId-$warehouseId",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
