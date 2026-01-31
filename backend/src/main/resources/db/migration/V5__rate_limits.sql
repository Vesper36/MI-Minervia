-- Rate limit fallback table
CREATE TABLE rate_limits (
    limit_key VARCHAR(255) PRIMARY KEY,
    count INT NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_seconds INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rate_limits_window (window_start),
    INDEX idx_rate_limits_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
