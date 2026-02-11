-- Create email_deliveries table for email retry mechanism
-- Supports idempotency, retry with exponential backoff, and observability

CREATE TABLE email_deliveries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dedupe_key VARCHAR(128) NOT NULL UNIQUE,
    recipient_email VARCHAR(255) NOT NULL,
    template VARCHAR(50) NOT NULL,
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    params_json TEXT NOT NULL,
    status ENUM('PENDING', 'SENT', 'FAILED', 'SUPPRESSED') NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(3) NULL,
    last_error TEXT NULL,
    provider_message_id VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    INDEX idx_status_next_attempt (status, next_attempt_at),
    INDEX idx_recipient_created (recipient_email, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
