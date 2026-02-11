package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "student_documents")
data class StudentDocument(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "student_id", nullable = false)
    val studentId: Long,

    @Column(name = "object_key", nullable = false, unique = true, length = 512)
    val objectKey: String,

    @Column(name = "bucket", nullable = false, length = 128)
    val bucket: String = "minervia-documents",

    @Column(name = "original_file_name", nullable = false)
    val originalFileName: String,

    @Column(name = "content_type", nullable = false, length = 128)
    val contentType: String,

    @Column(name = "size_bytes", nullable = false)
    val sizeBytes: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: DocumentStatus = DocumentStatus.PENDING_UPLOAD,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)

enum class DocumentStatus {
    PENDING_UPLOAD,
    ACTIVE,
    DELETED
}
