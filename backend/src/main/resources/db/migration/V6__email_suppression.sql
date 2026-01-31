-- Email suppression table for bounces/complaints
CREATE TABLE email_suppression (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    reason ENUM('HARD_BOUNCE', 'SOFT_BOUNCE', 'SPAM_COMPLAINT') NULL,
    bounce_count INT NOT NULL DEFAULT 0,
    first_bounce_at TIMESTAMP NULL,
    last_bounce_at TIMESTAMP NULL,
    suppressed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_email_suppression_email (email),
    INDEX idx_email_suppression_email (email),
    INDEX idx_email_suppression_suppressed (suppressed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
