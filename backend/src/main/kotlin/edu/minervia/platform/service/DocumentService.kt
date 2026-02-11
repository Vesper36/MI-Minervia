package edu.minervia.platform.service

import edu.minervia.platform.config.R2Properties
import edu.minervia.platform.domain.entity.DocumentStatus
import edu.minervia.platform.domain.entity.StudentDocument
import edu.minervia.platform.domain.repository.StudentDocumentRepository
import edu.minervia.platform.service.storage.R2StorageService
import edu.minervia.platform.web.dto.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class DocumentService(
    private val documentRepository: StudentDocumentRepository,
    private val r2StorageService: R2StorageService,
    private val r2Properties: R2Properties
) {

    @Transactional
    fun initializeUpload(studentId: Long, request: InitUploadRequest): InitUploadResponse {
        // Validate file size
        require(request.sizeBytes <= r2Properties.maxFileSize) {
            "File size exceeds maximum allowed size of ${r2Properties.maxFileSize} bytes"
        }

        // Validate content type
        require(request.contentType in r2Properties.allowedContentTypes) {
            "Content type ${request.contentType} is not allowed"
        }

        // Validate file name
        val sanitizedFileName = sanitizeFileName(request.fileName)
        require(sanitizedFileName.isNotEmpty()) {
            "Invalid file name"
        }

        // Generate object key
        val objectKey = generateObjectKey(studentId, sanitizedFileName)

        // Create document record
        val document = StudentDocument(
            studentId = studentId,
            objectKey = objectKey,
            bucket = r2Properties.bucket,
            originalFileName = request.fileName,
            contentType = request.contentType,
            sizeBytes = request.sizeBytes,
            status = DocumentStatus.PENDING_UPLOAD
        )

        val savedDocument = documentRepository.save(document)

        // Generate presigned upload URL
        val uploadUrl = r2StorageService.generatePresignedUploadUrl(
            objectKey = objectKey,
            contentType = request.contentType,
            ttlSeconds = r2Properties.uploadUrlTtl
        )

        val expiresAt = LocalDateTime.now().plusSeconds(r2Properties.uploadUrlTtl.toLong())

        return InitUploadResponse(
            documentId = savedDocument.id!!,
            uploadUrl = uploadUrl,
            expiresAt = expiresAt
        )
    }

    @Transactional
    fun completeUpload(studentId: Long, documentId: Long, request: CompleteUploadRequest): DocumentDto {
        val document = documentRepository.findByIdAndStudentId(documentId, studentId)
            ?: throw NoSuchElementException("Document not found")

        require(document.status == DocumentStatus.PENDING_UPLOAD) {
            "Document is not in PENDING_UPLOAD status"
        }

        // Verify object exists in R2
        val metadata = r2StorageService.headObject(document.objectKey)

        // Verify content type matches
        require(metadata.contentType == document.contentType) {
            "Content type mismatch"
        }

        // Verify size matches
        require(metadata.contentLength == document.sizeBytes) {
            "File size mismatch"
        }

        // Update document status
        val updatedDocument = document.copy(
            status = DocumentStatus.ACTIVE,
            updatedAt = LocalDateTime.now()
        )

        documentRepository.save(updatedDocument)

        return toDto(updatedDocument)
    }

    fun listDocuments(studentId: Long, pageable: Pageable): Page<DocumentDto> {
        return documentRepository.findByStudentId(studentId, pageable)
            .map { toDto(it) }
    }

    fun getDownloadUrl(studentId: Long, documentId: Long): DownloadUrlResponse {
        val document = documentRepository.findByIdAndStudentId(documentId, studentId)
            ?: throw NoSuchElementException("Document not found")

        require(document.status == DocumentStatus.ACTIVE) {
            "Document is not active"
        }

        val downloadUrl = r2StorageService.generatePresignedDownloadUrl(
            objectKey = document.objectKey,
            ttlSeconds = r2Properties.downloadUrlTtl
        )

        val expiresAt = LocalDateTime.now().plusSeconds(r2Properties.downloadUrlTtl.toLong())

        return DownloadUrlResponse(
            downloadUrl = downloadUrl,
            expiresAt = expiresAt
        )
    }

    @Transactional
    fun deleteDocument(studentId: Long, documentId: Long) {
        val document = documentRepository.findByIdAndStudentId(documentId, studentId)
            ?: throw NoSuchElementException("Document not found")

        // Soft delete
        val deletedDocument = document.copy(
            status = DocumentStatus.DELETED,
            deletedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        documentRepository.save(deletedDocument)

        // Async delete from R2 (could be done via scheduled task)
        try {
            r2StorageService.deleteObject(document.objectKey)
        } catch (e: Exception) {
            // Log error but don't fail the operation
            // The object can be cleaned up later by a scheduled task
        }
    }

    private fun generateObjectKey(studentId: Long, fileName: String): String {
        val uuid = UUID.randomUUID().toString()
        return "students/$studentId/documents/${uuid}_$fileName"
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(255)
    }

    private fun toDto(document: StudentDocument): DocumentDto {
        return DocumentDto(
            id = document.id!!,
            fileName = document.originalFileName,
            contentType = document.contentType,
            sizeBytes = document.sizeBytes,
            status = document.status.name,
            createdAt = document.createdAt
        )
    }
}
