package edu.minervia.platform.service

import edu.minervia.platform.config.R2Properties
import edu.minervia.platform.domain.entity.DocumentStatus
import edu.minervia.platform.domain.entity.StudentDocument
import edu.minervia.platform.domain.repository.StudentDocumentRepository
import edu.minervia.platform.service.storage.R2StorageService
import edu.minervia.platform.service.storage.ObjectMetadata
import edu.minervia.platform.web.dto.CompleteUploadRequest
import edu.minervia.platform.web.dto.InitUploadRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DocumentServiceTest {

    @Mock
    private lateinit var documentRepository: StudentDocumentRepository

    @Mock
    private lateinit var r2StorageService: R2StorageService

    @Mock
    private lateinit var r2Properties: R2Properties

    private lateinit var documentService: DocumentService

    private val studentId = 1L
    private val documentId = 100L
    private val fileName = "test-document.pdf"
    private val contentType = "application/pdf"
    private val sizeBytes = 1024L
    private val objectKey = "students/1/documents/uuid_test-document.pdf"
    private val bucket = "test-bucket"

    @BeforeEach
    fun setUp() {
        documentService = DocumentService(documentRepository, r2StorageService, r2Properties)
    }

    @Test
    fun `initializeUpload should create document and return presigned URL`() {
        // Given
        val request = InitUploadRequest(
            fileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes
        )

        val uploadUrl = "https://r2.example.com/presigned-upload-url"
        val uploadUrlTtl = 3600

        whenever(r2Properties.maxFileSize).thenReturn(10 * 1024 * 1024L)
        whenever(r2Properties.allowedContentTypes).thenReturn(listOf("application/pdf", "image/jpeg"))
        whenever(r2Properties.bucket).thenReturn(bucket)
        whenever(r2Properties.uploadUrlTtl).thenReturn(uploadUrlTtl)

        val savedDocument = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.PENDING_UPLOAD,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(documentRepository.save(any())).thenReturn(savedDocument)
        whenever(r2StorageService.generatePresignedUploadUrl(any(), any(), any())).thenReturn(uploadUrl)

        // When
        val response = documentService.initializeUpload(studentId, request)

        // Then
        assertNotNull(response)
        assertEquals(documentId, response.documentId)
        assertEquals(uploadUrl, response.uploadUrl)
        assertNotNull(response.expiresAt)

        verify(documentRepository).save(argThat {
            this.studentId == studentId &&
            this.originalFileName == fileName &&
            this.contentType == contentType &&
            this.sizeBytes == sizeBytes &&
            this.status == DocumentStatus.PENDING_UPLOAD
        })
        verify(r2StorageService).generatePresignedUploadUrl(any(), eq(contentType), eq(uploadUrlTtl))
    }

    @Test
    fun `initializeUpload should throw exception when file size exceeds limit`() {
        // Given
        val request = InitUploadRequest(
            fileName = fileName,
            contentType = contentType,
            sizeBytes = 20 * 1024 * 1024L // 20MB
        )

        whenever(r2Properties.maxFileSize).thenReturn(10 * 1024 * 1024L) // 10MB limit

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            documentService.initializeUpload(studentId, request)
        }

        assertTrue(exception.message!!.contains("File size exceeds maximum"))
        verify(documentRepository, never()).save(any())
        verify(r2StorageService, never()).generatePresignedUploadUrl(any(), any(), any())
    }

    @Test
    fun `initializeUpload should throw exception when content type is not allowed`() {
        // Given
        val request = InitUploadRequest(
            fileName = "test.exe",
            contentType = "application/x-msdownload",
            sizeBytes = sizeBytes
        )

        whenever(r2Properties.maxFileSize).thenReturn(10 * 1024 * 1024L)
        whenever(r2Properties.allowedContentTypes).thenReturn(listOf("application/pdf", "image/jpeg"))

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            documentService.initializeUpload(studentId, request)
        }

        assertTrue(exception.message!!.contains("Content type"))
        assertTrue(exception.message!!.contains("not allowed"))
        verify(documentRepository, never()).save(any())
        verify(r2StorageService, never()).generatePresignedUploadUrl(any(), any(), any())
    }

    @Test
    fun `completeUpload should update document status to ACTIVE`() {
        // Given
        val request = CompleteUploadRequest(etag = "test-etag", sha256 = "test-sha256")

        val document = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.PENDING_UPLOAD,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val metadata = ObjectMetadata(
            contentType = contentType,
            contentLength = sizeBytes,
            etag = "test-etag"
        )

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(document)
        whenever(r2StorageService.headObject(objectKey)).thenReturn(metadata)
        whenever(documentRepository.save(any())).thenAnswer { it.arguments[0] as StudentDocument }

        // When
        val result = documentService.completeUpload(studentId, documentId, request)

        // Then
        assertNotNull(result)
        assertEquals(documentId, result.id)
        assertEquals(fileName, result.fileName)
        assertEquals("ACTIVE", result.status)

        verify(documentRepository).findByIdAndStudentId(documentId, studentId)
        verify(r2StorageService).headObject(objectKey)
        verify(documentRepository).save(argThat {
            this.status == DocumentStatus.ACTIVE
        })
    }

    @Test
    fun `completeUpload should throw exception when document not found`() {
        // Given
        val request = CompleteUploadRequest(etag = "test-etag", sha256 = "test-sha256")

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(null)

        // When & Then
        assertThrows(NoSuchElementException::class.java) {
            documentService.completeUpload(studentId, documentId, request)
        }

        verify(documentRepository).findByIdAndStudentId(documentId, studentId)
        verify(r2StorageService, never()).headObject(any())
        verify(documentRepository, never()).save(any())
    }

    @Test
    fun `completeUpload should throw exception when document status is not PENDING_UPLOAD`() {
        // Given
        val request = CompleteUploadRequest(etag = "test-etag", sha256 = "test-sha256")

        val document = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.ACTIVE, // Already active
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(document)

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            documentService.completeUpload(studentId, documentId, request)
        }

        assertTrue(exception.message!!.contains("not in PENDING_UPLOAD status"))
        verify(r2StorageService, never()).headObject(any())
        verify(documentRepository, never()).save(any())
    }

    @Test
    fun `completeUpload should throw exception when content type mismatch`() {
        // Given
        val request = CompleteUploadRequest(etag = "test-etag", sha256 = "test-sha256")

        val document = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.PENDING_UPLOAD,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val metadata = ObjectMetadata(
            contentType = "image/jpeg", // Different content type
            contentLength = sizeBytes,
            etag = "test-etag"
        )

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(document)
        whenever(r2StorageService.headObject(objectKey)).thenReturn(metadata)

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            documentService.completeUpload(studentId, documentId, request)
        }

        assertTrue(exception.message!!.contains("Content type mismatch"))
        verify(documentRepository, never()).save(any())
    }

    @Test
    fun `listDocuments should return paginated documents`() {
        // Given
        val pageable = PageRequest.of(0, 20)

        val document1 = StudentDocument(
            id = 1L,
            studentId = studentId,
            objectKey = "key1",
            bucket = bucket,
            originalFileName = "file1.pdf",
            contentType = contentType,
            sizeBytes = 1024L,
            status = DocumentStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val document2 = StudentDocument(
            id = 2L,
            studentId = studentId,
            objectKey = "key2",
            bucket = bucket,
            originalFileName = "file2.pdf",
            contentType = contentType,
            sizeBytes = 2048L,
            status = DocumentStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val page = PageImpl(listOf(document1, document2), pageable, 2)

        whenever(documentRepository.findByStudentId(studentId, pageable)).thenReturn(page)

        // When
        val result = documentService.listDocuments(studentId, pageable)

        // Then
        assertNotNull(result)
        assertEquals(2, result.content.size)
        assertEquals("file1.pdf", result.content[0].fileName)
        assertEquals("file2.pdf", result.content[1].fileName)

        verify(documentRepository).findByStudentId(studentId, pageable)
    }

    @Test
    fun `getDownloadUrl should return presigned download URL`() {
        // Given
        val document = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val downloadUrl = "https://r2.example.com/presigned-download-url"
        val downloadUrlTtl = 3600

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(document)
        whenever(r2Properties.downloadUrlTtl).thenReturn(downloadUrlTtl)
        whenever(r2StorageService.generatePresignedDownloadUrl(objectKey, downloadUrlTtl)).thenReturn(downloadUrl)

        // When
        val result = documentService.getDownloadUrl(studentId, documentId)

        // Then
        assertNotNull(result)
        assertEquals(downloadUrl, result.downloadUrl)
        assertNotNull(result.expiresAt)

        verify(documentRepository).findByIdAndStudentId(documentId, studentId)
        verify(r2StorageService).generatePresignedDownloadUrl(objectKey, downloadUrlTtl)
    }

    @Test
    fun `getDownloadUrl should throw exception when document not found`() {
        // Given
        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(null)

        // When & Then
        assertThrows(NoSuchElementException::class.java) {
            documentService.getDownloadUrl(studentId, documentId)
        }

        verify(documentRepository).findByIdAndStudentId(documentId, studentId)
        verify(r2StorageService, never()).generatePresignedDownloadUrl(any(), any())
    }

    @Test
    fun `getDownloadUrl should throw exception when document is not active`() {
        // Given
        val document = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.DELETED, // Not active
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(document)

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            documentService.getDownloadUrl(studentId, documentId)
        }

        assertTrue(exception.message!!.contains("not active"))
        verify(r2StorageService, never()).generatePresignedDownloadUrl(any(), any())
    }

    @Test
    fun `deleteDocument should soft delete document and delete from R2`() {
        // Given
        val document = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(document)
        whenever(documentRepository.save(any())).thenAnswer { it.arguments[0] as StudentDocument }
        doNothing().whenever(r2StorageService).deleteObject(objectKey)

        // When
        documentService.deleteDocument(studentId, documentId)

        // Then
        verify(documentRepository).findByIdAndStudentId(documentId, studentId)
        verify(documentRepository).save(argThat {
            this.status == DocumentStatus.DELETED &&
            this.deletedAt != null
        })
        verify(r2StorageService).deleteObject(objectKey)
    }

    @Test
    fun `deleteDocument should throw exception when document not found`() {
        // Given
        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(null)

        // When & Then
        assertThrows(NoSuchElementException::class.java) {
            documentService.deleteDocument(studentId, documentId)
        }

        verify(documentRepository).findByIdAndStudentId(documentId, studentId)
        verify(documentRepository, never()).save(any())
        verify(r2StorageService, never()).deleteObject(any())
    }

    @Test
    fun `deleteDocument should continue even if R2 deletion fails`() {
        // Given
        val document = StudentDocument(
            id = documentId,
            studentId = studentId,
            objectKey = objectKey,
            bucket = bucket,
            originalFileName = fileName,
            contentType = contentType,
            sizeBytes = sizeBytes,
            status = DocumentStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(documentRepository.findByIdAndStudentId(documentId, studentId)).thenReturn(document)
        whenever(documentRepository.save(any())).thenAnswer { it.arguments[0] as StudentDocument }
        whenever(r2StorageService.deleteObject(objectKey)).thenThrow(RuntimeException("R2 error"))

        // When - should not throw exception
        documentService.deleteDocument(studentId, documentId)

        // Then
        verify(documentRepository).save(argThat {
            this.status == DocumentStatus.DELETED
        })
        verify(r2StorageService).deleteObject(objectKey)
    }
}
