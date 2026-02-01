## ADDED Requirements

### Requirement: Marketing route group structure
The system SHALL provide marketing pages under app/[locale]/(marketing)/ route group.

**CONSTRAINT [MARKETING-ROUTE-STRUCTURE]**: All marketing pages MUST be under [locale]/(marketing)/ path.

#### Scenario: Homepage route
- **WHEN** user visits /{locale}/
- **THEN** system renders marketing homepage
- **AND** page is publicly accessible without authentication

#### Scenario: About page route
- **WHEN** user visits /{locale}/about
- **THEN** system renders about page with institution information

#### Scenario: Programs page route
- **WHEN** user visits /{locale}/programs
- **THEN** system renders programs/majors listing page

#### Scenario: Admissions page route
- **WHEN** user visits /{locale}/admissions
- **THEN** system renders admissions information page

### Requirement: Marketing page SEO metadata
The system SHALL provide localized SEO metadata for all marketing pages.

**CONSTRAINT [MARKETING-SEO]**: All marketing pages MUST export generateMetadata function with localized title and description.

#### Scenario: Localized metadata
- **WHEN** user visits /pl/ (Polish locale)
- **THEN** page title and description are in Polish
- **AND** Open Graph tags use Polish content

#### Scenario: Default locale metadata
- **WHEN** user visits /en/ (English locale)
- **THEN** page uses English metadata as default

### Requirement: Marketing content from static files
The system SHALL load marketing content from static JSON/MDX files.

**CONSTRAINT [MARKETING-CONTENT-SOURCE]**: Marketing content MUST be stored in messages/{locale}.json under Marketing namespace.

#### Scenario: Content loading
- **WHEN** marketing page renders
- **THEN** system loads content from messages/{locale}.json
- **AND** uses Marketing.* namespace keys

#### Scenario: Missing locale fallback
- **WHEN** requested locale content is missing
- **THEN** system falls back to en locale content

### Requirement: Marketing layout separation
The system SHALL use independent layout for marketing pages.

#### Scenario: Marketing layout
- **WHEN** user visits marketing page
- **THEN** page uses MarketingLayout with hero header and public navigation
- **AND** layout does NOT include admin sidebar or portal navigation

### Requirement: Marketing i18n namespace completeness
The system SHALL ensure all marketing content keys exist in all supported locales.

**CONSTRAINT [MARKETING-I18N-COMPLETE]**: Build MUST fail if marketing keys are missing in any supported locale (en, pl, zh-CN).

#### Scenario: Missing key detection
- **WHEN** Marketing.home.title key exists in en.json
- **AND** Marketing.home.title key is missing in pl.json
- **THEN** build process fails with clear error message

## PBT Properties

### PBT-MARKETING-01: Route structure compliance
**INVARIANT**: All marketing page paths match pattern app/[locale]/(marketing)/**/*.
**FALSIFICATION**: Scan filesystem for page.tsx files and verify all marketing pages are under correct route group.

### PBT-MARKETING-02: i18n namespace completeness
**INVARIANT**: Every Marketing.* key in en.json exists in all other locale files.
**FALSIFICATION**: Randomly delete/rename keys in locale files and verify build validation fails.

### PBT-MARKETING-03: SEO metadata presence
**INVARIANT**: All marketing pages export generateMetadata function.
**FALSIFICATION**: Scan marketing page files and verify metadata export exists.
