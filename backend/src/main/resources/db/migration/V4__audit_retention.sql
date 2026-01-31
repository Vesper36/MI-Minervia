-- V4__audit_retention.sql
-- Audit log retention and partition management
-- Per CONSTRAINT [AUDIT-PARTITION-AUTOMATION]

-- Table to store partition digests for integrity verification
-- When partitions are archived, their digests are preserved
CREATE TABLE audit_partition_digests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    partition_name VARCHAR(20) NOT NULL UNIQUE,
    partition_month VARCHAR(7) NOT NULL,
    record_count BIGINT NOT NULL DEFAULT 0,
    min_id BIGINT NULL,
    max_id BIGINT NULL,
    event_types TEXT NULL,
    hash_digest VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_partition_month (partition_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add configuration for audit retention
INSERT INTO system_configs (config_key, config_value, description) VALUES
('audit_retention_years', '5', 'Audit log retention period in years'),
('audit_archive_enabled', 'false', 'Enable automatic audit log archiving');
