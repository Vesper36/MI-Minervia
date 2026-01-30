-- Minervia Platform Initial Schema
-- V1__init.sql

-- Admin table
CREATE TABLE admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('SUPER_ADMIN', 'ADMIN') NOT NULL DEFAULT 'ADMIN',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    totp_secret VARCHAR(255) NULL,
    totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_admins_username (username),
    INDEX idx_admins_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Registration codes table
CREATE TABLE registration_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    status ENUM('UNUSED', 'USED', 'EXPIRED', 'REVOKED') NOT NULL DEFAULT 'UNUSED',
    created_by BIGINT NOT NULL,
    used_by BIGINT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reg_codes_code (code),
    INDEX idx_reg_codes_status (status),
    FOREIGN KEY (created_by) REFERENCES admins(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Registration applications table
CREATE TABLE registration_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_code_id BIGINT NOT NULL,
    external_email VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    identity_type ENUM('LOCAL', 'INTERNATIONAL') NOT NULL,
    country_code VARCHAR(3) NULL,
    major_id BIGINT NULL,
    class_id BIGINT NULL,
    status ENUM('CODE_VERIFIED', 'EMAIL_VERIFIED', 'INFO_SELECTED', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'GENERATING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'CODE_VERIFIED',
    rejection_reason TEXT NULL,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    oauth_provider VARCHAR(50) NULL,
    oauth_user_id VARCHAR(255) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_reg_apps_status (status),
    INDEX idx_reg_apps_email (external_email),
    INDEX idx_reg_apps_created (created_at),
    FOREIGN KEY (registration_code_id) REFERENCES registration_codes(id),
    FOREIGN KEY (approved_by) REFERENCES admins(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Email verification codes table
CREATE TABLE email_verification_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_codes_app (application_id),
    FOREIGN KEY (application_id) REFERENCES registration_applications(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Students table (partitioned by enrollment year)
CREATE TABLE students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_number VARCHAR(20) NOT NULL UNIQUE,
    edu_email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    application_id BIGINT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    identity_type ENUM('LOCAL', 'INTERNATIONAL') NOT NULL,
    country_code VARCHAR(3) NOT NULL,
    major_id BIGINT NULL,
    class_id BIGINT NULL,
    enrollment_year INT NOT NULL,
    enrollment_date DATE NOT NULL,
    admission_date DATE NOT NULL,
    gpa DECIMAL(3, 2) NULL,
    status ENUM('ACTIVE', 'SUSPENDED', 'GRADUATED', 'EXPELLED') NOT NULL DEFAULT 'ACTIVE',
    suspension_reason TEXT NULL,
    daily_email_limit INT NOT NULL DEFAULT 1,
    is_simulated BOOLEAN NOT NULL DEFAULT TRUE,
    generation_seed VARCHAR(255) NULL,
    generation_version VARCHAR(20) NULL,
    photo_url VARCHAR(500) NULL,
    family_background TEXT NULL,
    interests TEXT NULL,
    academic_goals TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_students_number (student_number),
    INDEX idx_students_email (edu_email),
    INDEX idx_students_status (status),
    INDEX idx_students_enrollment (enrollment_year),
    FOREIGN KEY (application_id) REFERENCES registration_applications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Student family info table
CREATE TABLE student_family_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL UNIQUE,
    father_name VARCHAR(200) NULL,
    father_occupation VARCHAR(100) NULL,
    mother_name VARCHAR(200) NULL,
    mother_occupation VARCHAR(100) NULL,
    home_address TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit logs table (partitioned by month)
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL,
    actor_type ENUM('ADMIN', 'STUDENT', 'SYSTEM') NOT NULL,
    actor_id BIGINT NULL,
    actor_username VARCHAR(100) NULL,
    target_type VARCHAR(50) NULL,
    target_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    result ENUM('SUCCESS', 'FAILURE') NOT NULL,
    error_message TEXT NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    session_id VARCHAR(100) NULL,
    hash_value VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_event (event_type),
    INDEX idx_audit_actor (actor_type, actor_id),
    INDEX idx_audit_target (target_type, target_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- System configuration table
CREATE TABLE system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description VARCHAR(500) NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key),
    FOREIGN KEY (updated_by) REFERENCES admins(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default system configs
INSERT INTO system_configs (config_key, config_value, description) VALUES
('default_email_limit', '1', 'Default daily email send limit for new students'),
('registration_code_expiry_days', '30', 'Default expiration days for registration codes'),
('jwt_expiration_hours', '1', 'JWT token expiration in hours'),
('password_min_length', '8', 'Minimum password length'),
('login_max_attempts', '5', 'Maximum login attempts before lockout'),
('login_lockout_minutes', '30', 'Account lockout duration in minutes');

-- NOTE: Default admin should be created via deployment script or environment variables
-- DO NOT insert default credentials in migration scripts for security reasons
