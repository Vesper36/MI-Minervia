-- Add AUDITOR role to admins table
-- This migration extends the role enum to support read-only audit access

ALTER TABLE admins
MODIFY COLUMN role ENUM('SUPER_ADMIN', 'ADMIN', 'AUDITOR')
NOT NULL DEFAULT 'ADMIN';

-- Note: This is a non-reversible change in MySQL
-- Removing an enum value requires full table rewrite
