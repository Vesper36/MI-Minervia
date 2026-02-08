-- Rate limit fallback table
CREATE TABLE rate_limits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    limit_key VARCHAR(255) NOT NULL,
    count INT NOT NULL DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    window_seconds INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rate_limit_key (limit_key, window_start),
    INDEX idx_rate_limits_window (window_start),
    INDEX idx_rate_limits_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
