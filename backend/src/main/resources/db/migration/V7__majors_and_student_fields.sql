-- Minervia Platform - Majors Table and Related Fields
-- V7__majors_and_student_fields.sql

-- Majors table for dynamic major management
CREATE TABLE majors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(10) NOT NULL UNIQUE,
    name_en VARCHAR(100) NOT NULL,
    name_pl VARCHAR(100) NOT NULL,
    name_zh VARCHAR(100) NULL,
    faculty_id BIGINT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_majors_code (code),
    INDEX idx_majors_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed data for majors
INSERT INTO majors (code, name_en, name_pl, name_zh) VALUES
('CS', 'Computer Science', 'Informatyka', '计算机科学'),
('BA', 'Business Administration', 'Administracja Biznesu', '工商管理'),
('ENG', 'Engineering', 'Inżynieria', '工程学'),
('MED', 'Medicine', 'Medycyna', '医学');

-- Add welcome_email_sent_at to students table
ALTER TABLE students
ADD COLUMN welcome_email_sent_at TIMESTAMP NULL AFTER generation_version;

-- Add rejection_email_sent_at to registration_applications table
ALTER TABLE registration_applications
ADD COLUMN rejection_email_sent_at TIMESTAMP NULL AFTER rejection_reason;

-- Audit notifications table for tracking alert notifications
CREATE TABLE audit_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    recipients JSON NOT NULL,
    send_status ENUM('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_notif_type (alert_type),
    INDEX idx_audit_notif_status (send_status),
    INDEX idx_audit_notif_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
