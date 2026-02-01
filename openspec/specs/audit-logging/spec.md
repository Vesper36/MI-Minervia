## MODIFIED Requirements

### Requirement: Audit alert notification mechanism
The system SHALL send actual notifications when audit alerts are triggered.

**CONSTRAINT [ALERT-NOTIFICATION-EMAIL]**: Alert notifications MUST be sent via EmailService to super admin email addresses.
**CONSTRAINT [ALERT-DEDUPLICATION]**: Alert notifications MUST be deduplicated using Redis key per alert type + actor + 1-hour window.

#### Scenario: Bulk ban alert notification
- **WHEN** bulk ban alert is triggered (>10 bans by same actor in 1 hour)
- **THEN** system sends email notification to all SUPER_ADMIN users
- **AND** email contains alert type, actor, count, and timestamp
- **AND** notification is recorded in audit_notifications table

#### Scenario: Anomalous login alert notification
- **WHEN** anomalous login alert is triggered
- **THEN** system sends email notification to super admins
- **AND** email contains login details and risk indicators

#### Scenario: Config modification alert notification
- **WHEN** system configuration is modified
- **THEN** system sends email notification to super admins
- **AND** email contains changed config keys and values

#### Scenario: Integrity failure alert notification
- **WHEN** audit log integrity check fails
- **THEN** system sends CRITICAL alert email to super admins
- **AND** alert is escalated if email delivery fails

#### Scenario: Alert deduplication
- **WHEN** same alert type + actor triggers within 1 hour
- **AND** notification was already sent
- **THEN** system skips duplicate notification
- **AND** logs deduplication skip

#### Scenario: Notification failure handling
- **WHEN** email notification fails to send
- **THEN** system records failure status
- **AND** retries with exponential backoff
- **AND** does NOT crash scheduler

### Requirement: Alert notification audit trail
The system SHALL record all alert notifications for auditability.

#### Scenario: Notification recording
- **WHEN** alert notification is sent
- **THEN** system records in audit_notifications table:
  - alert_type
  - recipients
  - send_status
  - sent_at
  - retry_count

## PBT Properties

### PBT-ALERT-01: Notification deduplication
**INVARIANT**: Same alert type + actor within deduplication window produces at most one notification.
**FALSIFICATION**: Trigger same alert multiple times within window and verify single notification sent.

### PBT-ALERT-02: Notification recording completeness
**INVARIANT**: Every notification attempt (success or failure) is recorded in audit_notifications table.
**FALSIFICATION**: Trigger alerts and verify corresponding records exist regardless of send outcome.

### PBT-ALERT-03: Scheduler resilience
**INVARIANT**: Notification failures do not crash or block the scheduled integrity check.
**FALSIFICATION**: Inject email service failures and verify scheduler continues running.
