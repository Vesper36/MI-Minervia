package edu.minervia.platform.web.dto

import java.time.LocalDateTime

data class InitUploadRequest(
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long
)

data class InitUploadResponse(
    val documentId: Long,
    val uploadUrl: String,
    val expiresAt: LocalDateTime
)

data class CompleteUploadRequest(
    val etag: String?,
    val sha256: String?
)

data class DocumentDto(
    val id: Long,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val status: String,
    val createdAt: LocalDateTime
)

data class DownloadUrlResponse(
    val downloadUrl: String,
    val expiresAt: LocalDateTime
)
