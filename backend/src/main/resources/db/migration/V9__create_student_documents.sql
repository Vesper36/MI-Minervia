-- Create student_documents table for document metadata
-- Supports Cloudflare R2 object storage integration

CREATE TABLE student_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    bucket VARCHAR(128) NOT NULL DEFAULT 'minervia-documents',
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status ENUM('PENDING_UPLOAD', 'ACTIVE', 'DELETED') NOT NULL DEFAULT 'PENDING_UPLOAD',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,

    CONSTRAINT fk_student_documents_student
        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,

    INDEX idx_student_created (student_id, created_at),
    INDEX idx_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
