## ADDED Requirements

### Requirement: Student authentication system
The system SHALL provide independent authentication for students, separate from admin authentication.

**CONSTRAINT [STUDENT-AUTH-ISOLATION]**: Student authentication MUST use separate students table and issue JWTs with actor_type=STUDENT claim.

#### Scenario: Student login
- **WHEN** student submits credentials to /api/student/auth/login
- **THEN** system validates against students table
- **AND** issues JWT with actor_type=STUDENT claim
- **AND** JWT is NOT valid for admin endpoints

#### Scenario: Admin token rejected
- **WHEN** request to student endpoint includes admin JWT
- **THEN** system rejects request with 403 Forbidden
- **AND** logs cross-domain access attempt

#### Scenario: Student token rejected by admin
- **WHEN** request to admin endpoint includes student JWT
- **THEN** system rejects request with 403 Forbidden

### Requirement: Student login page
The system SHALL provide login page for students at app/[locale]/(portal)/login.

#### Scenario: Login page access
- **WHEN** unauthenticated user visits /{locale}/portal/dashboard
- **THEN** system redirects to /{locale}/portal/login

#### Scenario: Successful login redirect
- **WHEN** student successfully logs in
- **THEN** system redirects to /{locale}/portal/dashboard

### Requirement: Student dashboard page
The system SHALL provide dashboard page for authenticated students.

#### Scenario: Dashboard access
- **WHEN** authenticated student visits /{locale}/portal/dashboard
- **THEN** system renders student dashboard with profile summary
- **AND** shows recent activity and quick links

### Requirement: Student profile page
The system SHALL provide profile page for students to view their information.

#### Scenario: Profile view
- **WHEN** authenticated student visits /{locale}/portal/profile
- **THEN** system displays student's generated identity information
- **AND** shows edu email address
- **AND** shows enrollment status

### Requirement: Student courses page
The system SHALL provide courses page showing student's enrolled courses.

#### Scenario: Courses list
- **WHEN** authenticated student visits /{locale}/portal/courses
- **THEN** system displays list of enrolled courses
- **AND** shows grades for completed courses

### Requirement: Portal route protection
The system SHALL protect portal routes (except login and register) with authentication middleware.

**CONSTRAINT [PORTAL-AUTH-MIDDLEWARE]**: Portal routes MUST require valid student JWT, except /login and /register.

#### Scenario: Unauthenticated access
- **WHEN** unauthenticated user visits /{locale}/portal/dashboard
- **THEN** system redirects to /{locale}/portal/login
- **AND** preserves original URL for post-login redirect

#### Scenario: Register page public access
- **WHEN** unauthenticated user visits /{locale}/portal/register
- **THEN** system allows access without authentication

### Requirement: Portal layout with student navigation
The system SHALL use StudentLayout with sidebar navigation for portal pages.

#### Scenario: Portal layout
- **WHEN** authenticated student visits portal page
- **THEN** page uses StudentLayout with sidebar
- **AND** sidebar shows Dashboard, Profile, Courses links
- **AND** header shows student name and logout button

### Requirement: Portal i18n namespace
The system SHALL use Portal.* namespace for student portal translations.

**CONSTRAINT [PORTAL-I18N-NAMESPACE]**: Portal content MUST use Portal.* keys in messages/{locale}.json.

#### Scenario: Portal translations
- **WHEN** portal page renders
- **THEN** system uses Portal.* namespace keys
- **AND** all keys exist in supported locales

## PBT Properties

### PBT-PORTAL-01: Student session isolation
**INVARIANT**: Student credentials/sessions cannot access admin endpoints; admin credentials cannot authenticate against student auth.
**FALSIFICATION**: Fuzz tokens across auth domains (swap student token into admin requests and vice-versa), mutate claims, assert all cross-domain access rejected.

### PBT-PORTAL-02: Route protection completeness
**INVARIANT**: All portal routes except /login and /register require valid student JWT.
**FALSIFICATION**: Attempt unauthenticated access to all portal routes and verify redirect to login (except whitelist).

### PBT-PORTAL-03: Portal i18n completeness
**INVARIANT**: Every Portal.* key in en.json exists in all other locale files.
**FALSIFICATION**: Randomly delete Portal keys and verify build/runtime validation fails.
