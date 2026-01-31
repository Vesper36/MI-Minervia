-- V3__task_progress_retry_count.sql
-- Add retry_count column to task_progress table
-- Per CONSTRAINT [AI-TIMEOUT-CLEANUP]: Track retry attempts for timeout cleanup

ALTER TABLE task_progress
ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER message;

-- Add index for timeout cleanup queries
CREATE INDEX idx_progress_status_updated ON task_progress (status, updated_at);
