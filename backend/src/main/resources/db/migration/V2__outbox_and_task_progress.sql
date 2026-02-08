-- V2__outbox_and_task_progress.sql
-- Outbox pattern tables per CONSTRAINT [ASYNC-OUTBOX]
-- Task progress table per CONSTRAINT [PROGRESS-PERSISTENCE]

-- Outbox table for transactional message publishing
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    INDEX idx_outbox_unprocessed (processed_at, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dead letter table for failed outbox entries
-- per CONSTRAINT [OUTBOX-POLLER-CONFIG]: >10 retries moves to dead letter
CREATE TABLE outbox_dead_letter (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_id BIGINT NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    retry_count INT NOT NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    moved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dead_letter_aggregate (aggregate_type, aggregate_id),
    INDEX idx_dead_letter_moved (moved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Task progress table per CONSTRAINT [PROGRESS-PERSISTENCE]
CREATE TABLE task_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    step VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    message VARCHAR(500) NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_progress_app (application_id),
    INDEX idx_progress_status (status),
    FOREIGN KEY (application_id) REFERENCES registration_applications(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

