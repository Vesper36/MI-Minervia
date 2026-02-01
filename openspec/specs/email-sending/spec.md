## ADDED Requirements

### Requirement: Email service abstraction
The system SHALL provide an EmailService interface with SendGrid SMTP implementation for all transactional emails.

**CONSTRAINT [EMAIL-SERVICE-ABSTRACTION]**: All email sending MUST go through EmailService interface, not direct SMTP calls.

#### Scenario: Verification code email
- **WHEN** user requests email verification
- **THEN** system sends verification code email via SendGrid SMTP
- **AND** email contains localized content based on user's country_code
- **AND** verification code is NOT logged in plaintext

#### Scenario: SendGrid unavailable
- **WHEN** SendGrid SMTP connection fails
- **THEN** system records delivery failure in email_delivery table
- **AND** system retries with exponential backoff (1s-8s, max 3 attempts)
- **AND** user receives clear error message

### Requirement: Welcome email with credentials
The system SHALL send welcome email with temporary password after student account creation.

**CONSTRAINT [WELCOME-EMAIL-IDEMPOTENCY]**: Welcome email MUST be sent exactly once per student, tracked via welcome_email_sent_at timestamp.

#### Scenario: Successful welcome email
- **WHEN** student account is created from approved application
- **THEN** system sends welcome email to external email address (not edu email)
- **AND** email contains temporary password
- **AND** welcome_email_sent_at is set on student record

#### Scenario: Duplicate prevention
- **WHEN** async task retries student creation step
- **AND** welcome_email_sent_at is already set
- **THEN** system skips welcome email sending

### Requirement: Rejection notification email
The system SHALL send rejection email when application is rejected.

**CONSTRAINT [REJECTION-EMAIL-AFTER-COMMIT]**: Rejection email MUST be sent after transaction commits to avoid sending on rollback.

#### Scenario: Application rejected
- **WHEN** admin rejects registration application
- **THEN** system sends rejection email to applicant's external email
- **AND** email contains sanitized rejection reason
- **AND** rejection_email_sent_at is recorded

### Requirement: Email suppression check
The system SHALL check email suppression list before sending any email.

#### Scenario: Suppressed email
- **WHEN** system attempts to send email to suppressed address
- **THEN** system skips sending
- **AND** records suppression skip in audit log

### Requirement: Email template localization
The system SHALL support localized email templates for all supported languages.

**CONSTRAINT [EMAIL-TEMPLATE-LOCATION]**: Templates stored in resources/email-templates/{locale}/*.html

#### Scenario: Localized template selection
- **WHEN** sending email to user with country_code='CN'
- **THEN** system uses zh-CN template if available
- **AND** falls back to en template if locale not found

## PBT Properties

### PBT-EMAIL-01: Email sending idempotency
**INVARIANT**: Email sending for a given logical message (same recipient + template + payload + dedupe key) produces at most one delivery record and one external send.
**FALSIFICATION**: Generate random permutations of repeated send requests with identical dedupe keys (including concurrent bursts) and assert no duplicate deliveries.

### PBT-EMAIL-02: Suppression check consistency
**INVARIANT**: No email is sent to an address in the suppression list.
**FALSIFICATION**: Add addresses to suppression list and attempt sends; verify zero external calls for suppressed addresses.
