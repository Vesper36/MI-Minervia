export type RegistrationStep =
  | 'verify-code'
  | 'basic-info'
  | 'verify-email'
  | 'select-info'
  | 'progress';

export type IdentityType = 'LOCAL' | 'INTERNATIONAL';

export type ApplicationStatus =
  | 'CODE_VERIFIED'
  | 'EMAIL_VERIFIED'
  | 'INFO_SELECTED'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'GENERATING'
  | 'COMPLETED'
  | 'FAILED';

export type TaskStatus =
  | 'PENDING'
  | 'GENERATING_IDENTITY'
  | 'GENERATING_PHOTOS'
  | 'COMPLETED'
  | 'FAILED';

export type TaskStep =
  | 'IDENTITY_RULES'
  | 'IDENTITY_LLM'
  | 'PHOTO_GENERATION';

export interface RegistrationData {
  registrationCode: string;
  email: string;
  identityType: IdentityType;
  countryCode: string;
  applicationId: number | null;
  majorId: number | null;
  classId: number | null;
}

export interface RegistrationState {
  step: RegistrationStep;
  data: RegistrationData;
  isLoading: boolean;
  error: string | null;
}

export interface ProgressStatus {
  applicationId: number;
  step: TaskStep;
  status: TaskStatus;
  progressPercent: number;
  message: string | null;
  version: number;
  timestamp: number;
}

export interface VerifyCodeResponse {
  valid: boolean;
  message?: string;
}

export interface RegistrationApplicationDto {
  id: number;
  registrationCode: string;
  externalEmail: string;
  emailVerified: boolean;
  identityType: IdentityType;
  countryCode: string | null;
  majorId: number | null;
  classId: number | null;
  status: ApplicationStatus;
  rejectionReason: string | null;
  approvedByUsername: string | null;
  approvedAt: string | null;
  oauthProvider: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface VerifyEmailResponse {
  verified: boolean;
  message?: string;
  attemptsRemaining?: number;
}

export const INITIAL_DATA: RegistrationData = {
  registrationCode: '',
  email: '',
  identityType: 'LOCAL',
  countryCode: 'PL',
  applicationId: null,
  majorId: null,
  classId: null,
};

export const STORAGE_KEY = 'minervia_registration_draft';

export const COUNTRIES = [
  { code: 'PL', name: 'Poland' },
  { code: 'CN', name: 'China' },
  { code: 'US', name: 'United States' },
  { code: 'DE', name: 'Germany' },
  { code: 'FR', name: 'France' },
  { code: 'GB', name: 'United Kingdom' },
  { code: 'JP', name: 'Japan' },
  { code: 'KR', name: 'South Korea' },
] as const;

export const MAJORS = [
  { id: 1, code: 'CS', name: 'Computer Science' },
  { id: 2, code: 'BA', name: 'Business Administration' },
  { id: 3, code: 'ENG', name: 'Engineering' },
  { id: 4, code: 'MED', name: 'Medicine' },
] as const;

export const CLASSES = [
  { id: 1, name: 'Class A (Morning)' },
  { id: 2, name: 'Class B (Afternoon)' },
  { id: 3, name: 'Class C (Evening)' },
] as const;
