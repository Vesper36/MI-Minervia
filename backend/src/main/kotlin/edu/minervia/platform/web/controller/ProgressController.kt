package edu.minervia.platform.web.controller

import edu.minervia.platform.service.async.ProgressEventMessage
import edu.minervia.platform.service.async.TaskProgressService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for progress polling fallback.
 * Per CONSTRAINT [POLLING-FALLBACK]:
 * - Normal polling interval: 5 seconds
 * - After expected completion time: 2 seconds
 * - Version/timestamp check prevents stale updates
 */
@RestController
@RequestMapping("/api/applications")
class ProgressController(
    private val taskProgressService: TaskProgressService
) {

    /**
     * Get current progress status for an application.
     * Used as fallback when WebSocket disconnects.
     */
    @GetMapping("/{id}/status")
    fun getStatus(@PathVariable id: Long): ResponseEntity<ProgressStatusResponse> {
        val progress = taskProgressService.getProgress(id)

        return if (progress != null) {
            ResponseEntity.ok(
                ProgressStatusResponse(
                    applicationId = progress.applicationId,
                    step = progress.step.name,
                    status = progress.status.name,
                    progressPercent = progress.progressPercent,
                    message = progress.message,
                    version = progress.version,
                    timestamp = progress.timestamp.toEpochMilli()
                )
            )
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get progress with version check.
     * Returns 304 Not Modified if client version is current.
     */
    @GetMapping("/{id}/status/poll")
    fun pollStatus(
        @PathVariable id: Long,
        @RequestParam(required = false) lastVersion: Long?
    ): ResponseEntity<ProgressStatusResponse> {
        val progress = taskProgressService.getProgress(id)
            ?: return ResponseEntity.notFound().build()

        // If client has current version, return 304
        if (lastVersion != null && lastVersion >= progress.version) {
            return ResponseEntity.status(304).build()
        }

        return ResponseEntity.ok(
            ProgressStatusResponse(
                applicationId = progress.applicationId,
                step = progress.step.name,
                status = progress.status.name,
                progressPercent = progress.progressPercent,
                message = progress.message,
                version = progress.version,
                timestamp = progress.timestamp.toEpochMilli()
            )
        )
    }
}

data class ProgressStatusResponse(
    val applicationId: Long,
    val step: String,
    val status: String,
    val progressPercent: Int,
    val message: String?,
    val version: Long,
    val timestamp: Long
)
