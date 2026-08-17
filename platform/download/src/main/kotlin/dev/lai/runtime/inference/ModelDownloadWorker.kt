package dev.lai.runtime.inference

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Background model download that survives app exit and process death.
 *
 * Design decisions:
 * - The worker owns its OWN [ModelRepository] instance built from the application context: the
 *   repository is stateless besides files, and the worker must not reach into the app's
 *   composition root (WorkManager may start it in a fresh process with no UI).
 * - Interruptions are cheap by construction: [ModelRepository.download] already resumes `.part`
 *   files with HTTP Range requests, so a killed worker retries from the last flushed byte, not
 *   from zero. This is why no foreground service is used — a stopped worker loses nothing.
 * - Retry policy separates the two failure families: transient transport errors return
 *   `Result.retry()` (exponential backoff); policy/validation/integrity errors are final and
 *   return `Result.failure` with an LAI-authored reason — a corrupt or oversized artifact must
 *   never be silently re-fetched in a loop.
 */
class ModelDownloadWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val spec = specFromData(inputData)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Download request was malformed"))
        val repository = ModelRepository(applicationContext)
        val outcome = repository.download(spec) { progress ->
            setProgressAsync(
                workDataOf(
                    KEY_DOWNLOADED_BYTES to progress.downloadedBytes,
                    KEY_TOTAL_BYTES to (progress.totalBytes ?: -1L),
                ),
            )
        }
        return outcome.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (isTransient(error) && runAttemptCount < MAX_TRANSPORT_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure(workDataOf(KEY_ERROR to (error.message ?: "Download failed")))
                }
            },
        )
    }

    /** Transport-level interruptions are retryable; policy and integrity failures are final. */
    private fun isTransient(error: Throwable): Boolean =
        error is java.io.IOException && error !is StreamLimitExceededException

    companion object {
        internal const val KEY_ID = "id"
        internal const val KEY_DISPLAY_NAME = "displayName"
        internal const val KEY_URL = "url"
        internal const val KEY_SHA256 = "sha256"
        internal const val KEY_EXPECTED_BYTES = "expectedBytes"
        internal const val KEY_DOWNLOADED_BYTES = "downloadedBytes"
        internal const val KEY_TOTAL_BYTES = "totalBytes"
        internal const val KEY_ERROR = "error"
        internal const val TAG = "lai-model-download"
        internal const val ID_TAG_PREFIX = "lai-model-id:"
        private const val MAX_TRANSPORT_RETRIES = 8

        internal fun dataFromSpec(spec: ModelSpec): Data = workDataOf(
            KEY_ID to spec.id,
            KEY_DISPLAY_NAME to spec.displayName,
            KEY_URL to spec.url,
            KEY_SHA256 to spec.sha256,
            KEY_EXPECTED_BYTES to (spec.expectedBytes ?: -1L),
        )

        internal fun specFromData(data: Data): ModelSpec? {
            val id = data.getString(KEY_ID) ?: return null
            val displayName = data.getString(KEY_DISPLAY_NAME) ?: return null
            val url = data.getString(KEY_URL) ?: return null
            return ModelSpec(
                id = id,
                displayName = displayName,
                url = url,
                sha256 = data.getString(KEY_SHA256),
                expectedBytes = data.getLong(KEY_EXPECTED_BYTES, -1L).takeIf { it > 0 },
            )
        }
    }
}

/**
 * The app-facing seam for background downloads. The app module never imports androidx.work:
 * platform:download owns the Android authority, the app observes typed contract states.
 */
class ModelDownloadCoordinator(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** Enqueues (or keeps an already-running) unique download for this model id. */
    fun enqueue(spec: ModelSpec) {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(ModelDownloadWorker.dataFromSpec(spec))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(ModelDownloadWorker.TAG)
            .addTag(ModelDownloadWorker.ID_TAG_PREFIX + spec.id)
            .build()
        workManager.enqueueUniqueWork(workName(spec.id), ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Stops the work. The `.part` staging file is intentionally kept: re-enqueueing the same
     * spec resumes from the last byte, which is what "pause" means here.
     */
    fun stop(modelId: String) {
        workManager.cancelUniqueWork(workName(modelId))
    }

    /** Typed status stream for one model's unique download work. */
    fun observe(modelId: String): Flow<BackgroundDownloadStatus?> =
        workManager.getWorkInfosForUniqueWorkFlow(workName(modelId))
            .map { infos -> infos.firstOrNull()?.toStatus(modelId) }

    /** All known background download work, for reattaching UI after process death. */
    fun observeAll(): Flow<List<BackgroundDownloadStatus>> =
        workManager.getWorkInfosByTagFlow(ModelDownloadWorker.TAG)
            .map { infos ->
                infos.map { info ->
                    // WorkInfo does not expose input data, so the model id travels on a tag.
                    val id = info.tags
                        .firstOrNull { it.startsWith(ModelDownloadWorker.ID_TAG_PREFIX) }
                        ?.removePrefix(ModelDownloadWorker.ID_TAG_PREFIX)
                    info.toStatus(id)
                }
            }

    private fun WorkInfo.toStatus(modelId: String?): BackgroundDownloadStatus {
        val downloaded = progress.getLong(ModelDownloadWorker.KEY_DOWNLOADED_BYTES, -1L)
        val total = progress.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, -1L)
        return BackgroundDownloadStatus(
            modelId = modelId ?: "",
            state = when (state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> BackgroundDownloadState.ENQUEUED
                WorkInfo.State.RUNNING -> BackgroundDownloadState.RUNNING
                WorkInfo.State.SUCCEEDED -> BackgroundDownloadState.SUCCEEDED
                WorkInfo.State.FAILED -> BackgroundDownloadState.FAILED
                WorkInfo.State.CANCELLED -> BackgroundDownloadState.CANCELLED
            },
            progress = if (downloaded >= 0) {
                DownloadProgress(downloaded, total.takeIf { it > 0 })
            } else {
                null
            },
            failureReason = outputData.getString(ModelDownloadWorker.KEY_ERROR),
        )
    }

    private fun workName(modelId: String) = "model-download-$modelId"
}
