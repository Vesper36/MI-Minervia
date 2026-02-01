## ADDED Requirements

### Requirement: Identity generation step integration
The system SHALL execute identity generation as the first async task step.

**CONSTRAINT [IDENTITY-STEP-TRANSACTION]**: Identity generation step MUST commit its own transaction before proceeding to next step.

#### Scenario: Identity generation success
- **WHEN** async task executes identity generation step
- **THEN** system calls IdentityGenerationService to create student identity
- **AND** student record is created with generated identity data
- **AND** step status is updated to COMPLETED
- **AND** transaction is committed

#### Scenario: Identity generation failure
- **WHEN** identity generation fails
- **THEN** system marks step as FAILED
- **AND** task enters retry queue with exponential backoff
- **AND** no partial student record exists

### Requirement: LLM polish step integration
The system SHALL execute LLM polish as the second async task step.

**CONSTRAINT [LLM-STEP-IDEMPOTENCY]**: LLM polish step MUST check if fields are already populated before calling LLM.

#### Scenario: LLM polish success
- **WHEN** async task executes LLM polish step
- **AND** student.familyBackground is null
- **THEN** system calls LlmPolishService.generateProfile()
- **AND** student record is updated with familyBackground, interests, academicGoals
- **AND** step status is updated to COMPLETED

#### Scenario: LLM polish already done
- **WHEN** async task executes LLM polish step
- **AND** student.familyBackground is already populated
- **THEN** system skips LLM call
- **AND** step status is updated to COMPLETED

#### Scenario: LLM service failure with fallback
- **WHEN** LLM service call fails or times out (60s)
- **THEN** system uses fallback template from config/llm-templates/{nationality}/{major}/{identity_type}.yaml
- **AND** step completes successfully with fallback data

### Requirement: Photo generation step integration
The system SHALL execute photo generation as the third async task step using placeholder images.

**CONSTRAINT [PHOTO-PLACEHOLDER]**: Photo generation MUST use UI Avatars or DiceBear for placeholder images until FLUX.1 integration.

#### Scenario: Placeholder photo generation
- **WHEN** async task executes photo generation step
- **THEN** system generates placeholder avatar URL based on student name
- **AND** URL is properly URL-encoded for special characters
- **AND** student.photoUrl is updated with placeholder URL
- **AND** step status is updated to COMPLETED

#### Scenario: Photo URL encoding
- **WHEN** student name contains special characters (e.g., "Renee O'Connor")
- **THEN** system URL-encodes the name parameter
- **AND** generated URL is valid and accessible

### Requirement: Async task trigger on approval
The system SHALL trigger async identity generation task when application is approved.

**CONSTRAINT [APPROVAL-OUTBOX]**: Task trigger MUST use outbox pattern within approval transaction.

#### Scenario: Application approval triggers task
- **WHEN** admin approves registration application
- **THEN** system inserts outbox entry in same transaction
- **AND** application status changes to GENERATING
- **AND** outbox poller publishes Kafka message

#### Scenario: Outbox insert failure
- **WHEN** outbox insert fails during approval
- **THEN** entire transaction rolls back
- **AND** application remains in APPROVED status
- **AND** admin can retry approval

### Requirement: Step execution order enforcement
The system SHALL enforce step execution order: IDENTITY_RULES -> IDENTITY_LLM -> PHOTO_GENERATION.

#### Scenario: Out-of-order step rejection
- **WHEN** system attempts to execute PHOTO_GENERATION step
- **AND** IDENTITY_RULES step is not COMPLETED
- **THEN** system rejects execution
- **AND** logs error with step dependency violation

## PBT Properties

### PBT-ASYNC-01: Step completion exactly-once
**INVARIANT**: Each step transitions from pending to complete exactly once without skipping predecessors or duplicating completion.
**FALSIFICATION**: Generate random sequences of step completion events (including duplicates, out-of-order, retries) and assert no missing steps, no duplicate markers, no illegal orderings.

### PBT-ASYNC-02: Task idempotency
**INVARIANT**: Repeated task execution with same applicationId produces identical final state.
**FALSIFICATION**: Execute same task multiple times concurrently and verify single student record with consistent data.

### PBT-ASYNC-03: Placeholder URL validity
**INVARIANT**: All generated placeholder URLs are valid HTTP URLs with properly encoded parameters.
**FALSIFICATION**: Generate names with unicode, special characters, and edge cases; verify all URLs parse correctly and return 200.
