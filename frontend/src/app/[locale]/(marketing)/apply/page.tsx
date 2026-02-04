'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import { faculties } from '@/lib/academic-data';
import { AlertCircle, CheckCircle2, Clock, FileText, Video, Mail, Building2 } from 'lucide-react';

type Step = 'account' | 'personal' | 'education' | 'language' | 'program' | 'documents' | 'review' | 'submitted';

interface FormData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  nationality: string;
  hasFinancialAid: boolean;
  aidType: string;
  acceptedTerms: boolean;
  acceptedPrivacy: boolean;
}

const steps: { key: Step; label: string }[] = [
  { key: 'account', label: 'Account' },
  { key: 'personal', label: 'Personal' },
  { key: 'education', label: 'Education' },
  { key: 'language', label: 'Language' },
  { key: 'program', label: 'Program' },
  { key: 'documents', label: 'Documents' },
  { key: 'review', label: 'Review' },
];

export default function ApplyPage() {
  const params = useParams();
  const locale = params.locale as string;
  const [currentStep, setCurrentStep] = useState<Step>('account');
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [formData, setFormData] = useState<FormData>({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    nationality: '',
    hasFinancialAid: false,
    aidType: '',
    acceptedTerms: false,
    acceptedPrivacy: false,
  });

  const currentStepIndex = steps.findIndex(s => s.key === currentStep);
  const progress = currentStep === 'submitted' ? 100 : ((currentStepIndex + 1) / steps.length) * 100;

  const validateStep = (step: Step): boolean => {
    const newErrors: string[] = [];

    if (step === 'account') {
      if (!formData.email || !formData.email.includes('@')) {
        newErrors.push('Valid email address is required');
      }
      if (!formData.password || formData.password.length < 8) {
        newErrors.push('Password must be at least 8 characters');
      }
    }

    if (step === 'personal') {
      if (!formData.firstName || formData.firstName.trim().length < 2) {
        newErrors.push('First name is required (minimum 2 characters)');
      }
      if (!formData.lastName || formData.lastName.trim().length < 2) {
        newErrors.push('Last name is required (minimum 2 characters)');
      }
      if (!formData.dateOfBirth) {
        newErrors.push('Date of birth is required');
      }
      if (!formData.nationality) {
        newErrors.push('Nationality is required');
      }
    }

    if (step === 'review') {
      if (!formData.acceptedTerms) {
        newErrors.push('You must confirm that all information is accurate');
      }
      if (!formData.acceptedPrivacy) {
        newErrors.push('You must accept the privacy policy');
      }
    }

    setErrors(newErrors);
    return newErrors.length === 0;
  };

  const handleNext = () => {
    if (!validateStep(currentStep)) {
      return;
    }
    const nextIndex = currentStepIndex + 1;
    if (nextIndex < steps.length) {
      setCurrentStep(steps[nextIndex].key);
      setErrors([]);
    }
  };

  const handleBack = () => {
    const prevIndex = currentStepIndex - 1;
    if (prevIndex >= 0) {
      setCurrentStep(steps[prevIndex].key);
      setErrors([]);
    }
  };

  const handleSubmit = () => {
    if (!validateStep('review')) {
      return;
    }
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setCurrentStep('submitted');
    }, 2000);
  };

  if (currentStep === 'submitted') {
    return <SubmittedStep locale={locale} formData={formData} />;
  }

  const stepComponents: Record<Step, React.ReactNode> = {
    account: <AccountStep formData={formData} setFormData={setFormData} />,
    personal: <PersonalStep formData={formData} setFormData={setFormData} />,
    education: <EducationStep />,
    language: <LanguageStep />,
    program: <ProgramStep />,
    documents: <DocumentsStep />,
    review: <ReviewStep locale={locale} formData={formData} setFormData={setFormData} />,
    submitted: null,
  };

  return (
    <div className="py-12 md:py-16">
      <div className="container max-w-4xl">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-2">Online Application</h1>
          <p className="text-muted-foreground">
            Complete all steps to submit your application to Minervia Institute
          </p>
        </div>

        <div className="mb-8">
          <div className="flex justify-between text-sm mb-2">
            <span>Step {currentStepIndex + 1} of {steps.length}</span>
            <span>{Math.round(progress)}% Complete</span>
          </div>
          <Progress value={progress} className="h-2" />
          <div className="flex justify-between mt-2">
            {steps.map((step, idx) => (
              <div
                key={step.key}
                className={`text-xs ${idx <= currentStepIndex ? 'text-slate-900' : 'text-muted-foreground'}`}
              >
                {step.label}
              </div>
            ))}
          </div>
        </div>

        {errors.length > 0 && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-start gap-2">
              <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
              <ul className="list-disc pl-4 space-y-1 text-sm text-red-700">
                {errors.map((error, idx) => (
                  <li key={idx}>{error}</li>
                ))}
              </ul>
            </div>
          </div>
        )}

        <Card className="border-0 shadow-lg">
          {stepComponents[currentStep]}

          <CardContent className="border-t pt-6">
            <div className="flex justify-between">
              <Button
                variant="outline"
                onClick={handleBack}
                disabled={currentStepIndex === 0}
              >
                Back
              </Button>
              {currentStep === 'review' ? (
                <Button onClick={handleSubmit} disabled={isLoading}>
                  {isLoading ? 'Submitting...' : 'Submit Application'}
                </Button>
              ) : (
                <Button onClick={handleNext}>
                  Continue
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

interface StepProps {
  formData: FormData;
  setFormData: React.Dispatch<React.SetStateAction<FormData>>;
}

function AccountStep({ formData, setFormData }: StepProps) {
  return (
    <>
      <CardHeader>
        <CardTitle>Create Your Account</CardTitle>
        <CardDescription>
          Enter your email address to create an application account.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="email">Email Address *</Label>
          <Input
            id="email"
            type="email"
            placeholder="your.email@example.com"
            value={formData.email}
            onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
          />
          <p className="text-xs text-muted-foreground">
            We will send application updates to this email address.
          </p>
        </div>
        <div className="space-y-2">
          <Label htmlFor="password">Create Password *</Label>
          <Input
            id="password"
            type="password"
            placeholder="Minimum 8 characters"
            value={formData.password}
            onChange={(e) => setFormData(prev => ({ ...prev, password: e.target.value }))}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Confirm Password *</Label>
          <Input id="confirmPassword" type="password" placeholder="Re-enter your password" />
        </div>
      </CardContent>
    </>
  );
}

function PersonalStep({ formData, setFormData }: StepProps) {
  return (
    <>
      <CardHeader>
        <CardTitle>Personal Information</CardTitle>
        <CardDescription>
          Enter your personal details exactly as they appear on your passport.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="firstName">First Name *</Label>
            <Input
              id="firstName"
              placeholder="As in passport"
              value={formData.firstName}
              onChange={(e) => setFormData(prev => ({ ...prev, firstName: e.target.value }))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="lastName">Last Name *</Label>
            <Input
              id="lastName"
              placeholder="As in passport"
              value={formData.lastName}
              onChange={(e) => setFormData(prev => ({ ...prev, lastName: e.target.value }))}
            />
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="dob">Date of Birth *</Label>
            <Input
              id="dob"
              type="date"
              value={formData.dateOfBirth}
              onChange={(e) => setFormData(prev => ({ ...prev, dateOfBirth: e.target.value }))}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="gender">Gender *</Label>
            <select id="gender" className="w-full h-10 px-3 rounded-md border">
              <option value="">Select gender</option>
              <option value="male">Male</option>
              <option value="female">Female</option>
              <option value="other">Other</option>
            </select>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="nationality">Nationality *</Label>
            <select
              id="nationality"
              className="w-full h-10 px-3 rounded-md border"
              value={formData.nationality}
              onChange={(e) => setFormData(prev => ({ ...prev, nationality: e.target.value }))}
            >
              <option value="">Select country</option>
              <option value="PL">Poland</option>
              <option value="DE">Germany</option>
              <option value="FR">France</option>
              <option value="GB">United Kingdom</option>
              <option value="US">United States</option>
              <option value="CN">China</option>
              <option value="IN">India</option>
              <option value="UA">Ukraine</option>
              <option value="other">Other</option>
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="passportNo">Passport/ID Number *</Label>
            <Input id="passportNo" placeholder="Document number" />
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="address">Permanent Address *</Label>
          <Input id="address" placeholder="Street address" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="space-y-2">
            <Label htmlFor="city">City *</Label>
            <Input id="city" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="postalCode">Postal Code *</Label>
            <Input id="postalCode" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="country">Country *</Label>
            <Input id="country" />
          </div>
        </div>
        <div className="space-y-2">
          <Label htmlFor="phone">Phone Number *</Label>
          <Input id="phone" type="tel" placeholder="+48 123 456 789" />
        </div>
      </CardContent>
    </>
  );
}

function EducationStep() {
  return (
    <>
      <CardHeader>
        <CardTitle>Educational Background</CardTitle>
        <CardDescription>
          Provide information about your previous education.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="eduLevel">Highest Education Level *</Label>
          <select id="eduLevel" className="w-full h-10 px-3 rounded-md border">
            <option value="">Select level</option>
            <option value="secondary">Secondary School (High School)</option>
            <option value="bachelor">Bachelor Degree</option>
            <option value="master">Master Degree</option>
            <option value="doctoral">Doctoral Degree</option>
          </select>
        </div>
        <div className="space-y-2">
          <Label htmlFor="schoolName">School/University Name *</Label>
          <Input id="schoolName" placeholder="Full name of institution" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="eduCountry">Country of Education *</Label>
            <select id="eduCountry" className="w-full h-10 px-3 rounded-md border">
              <option value="">Select country</option>
              <option value="PL">Poland</option>
              <option value="DE">Germany</option>
              <option value="FR">France</option>
              <option value="GB">United Kingdom</option>
              <option value="US">United States</option>
              <option value="CN">China</option>
              <option value="IN">India</option>
              <option value="UA">Ukraine</option>
              <option value="other">Other</option>
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="gradDate">Graduation Date *</Label>
            <Input id="gradDate" type="month" />
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="gpa">GPA / Final Grade *</Label>
            <Input id="gpa" placeholder="e.g., 3.5/4.0 or 85%" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="fieldOfStudy">Field of Study *</Label>
            <Input id="fieldOfStudy" placeholder="e.g., Science, Humanities" />
          </div>
        </div>
        <div className="space-y-2">
          <Label>Upload Transcripts *</Label>
          <div className="border-2 border-dashed rounded-lg p-6 text-center">
            <input type="file" className="hidden" id="transcripts" accept=".pdf,.jpg,.png" />
            <label htmlFor="transcripts" className="cursor-pointer">
              <div className="text-muted-foreground">
                <p>Click to upload or drag and drop</p>
                <p className="text-xs">PDF, JPG, or PNG (max 5MB)</p>
              </div>
            </label>
          </div>
        </div>
      </CardContent>
    </>
  );
}

function LanguageStep() {
  return (
    <>
      <CardHeader>
        <CardTitle>Language Proficiency</CardTitle>
        <CardDescription>
          Provide information about your language skills and certificates.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="nativeLang">Native Language *</Label>
          <Input id="nativeLang" placeholder="e.g., Polish, English, Chinese" />
        </div>

        <div className="border rounded-lg p-4 space-y-4">
          <h4 className="font-medium">English Proficiency</h4>
          <div className="space-y-2">
            <Label htmlFor="engTest">English Test Type *</Label>
            <select id="engTest" className="w-full h-10 px-3 rounded-md border">
              <option value="">Select test</option>
              <option value="ielts">IELTS Academic</option>
              <option value="toefl">TOEFL iBT</option>
              <option value="cambridge">Cambridge (FCE/CAE/CPE)</option>
              <option value="native">Native Speaker</option>
              <option value="other">Other</option>
            </select>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="engScore">Score/Level *</Label>
              <Input id="engScore" placeholder="e.g., 7.0, 100, C1" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="engDate">Test Date *</Label>
              <Input id="engDate" type="date" />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Upload Certificate</Label>
            <div className="border-2 border-dashed rounded-lg p-4 text-center">
              <input type="file" className="hidden" id="engCert" accept=".pdf,.jpg,.png" />
              <label htmlFor="engCert" className="cursor-pointer text-sm text-muted-foreground">
                Click to upload certificate
              </label>
            </div>
          </div>
        </div>

        <div className="border rounded-lg p-4 space-y-4">
          <h4 className="font-medium">Polish Proficiency (if applicable)</h4>
          <div className="space-y-2">
            <Label htmlFor="polLevel">Polish Level</Label>
            <select id="polLevel" className="w-full h-10 px-3 rounded-md border">
              <option value="">Not applicable</option>
              <option value="a1">A1 - Beginner</option>
              <option value="a2">A2 - Elementary</option>
              <option value="b1">B1 - Intermediate</option>
              <option value="b2">B2 - Upper Intermediate</option>
              <option value="c1">C1 - Advanced</option>
              <option value="c2">C2 - Proficient</option>
              <option value="native">Native Speaker</option>
            </select>
          </div>
        </div>
      </CardContent>
    </>
  );
}

function ProgramStep() {
  const [selectedFaculty, setSelectedFaculty] = useState('');
  const faculty = faculties.find(f => f.id === selectedFaculty);

  return (
    <>
      <CardHeader>
        <CardTitle>Program Selection</CardTitle>
        <CardDescription>
          Choose your preferred program and study options.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="degreeLevel">Degree Level *</Label>
          <select id="degreeLevel" className="w-full h-10 px-3 rounded-md border">
            <option value="">Select level</option>
            <option value="bachelor">Bachelor Degree</option>
            <option value="master">Master Degree</option>
            <option value="doctoral">Doctoral Degree</option>
          </select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="faculty">Faculty *</Label>
          <select
            id="faculty"
            className="w-full h-10 px-3 rounded-md border"
            value={selectedFaculty}
            onChange={(e) => setSelectedFaculty(e.target.value)}
          >
            <option value="">Select faculty</option>
            {faculties.map((f) => (
              <option key={f.id} value={f.id}>{f.name}</option>
            ))}
          </select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="program1">First Choice Program *</Label>
          <select id="program1" className="w-full h-10 px-3 rounded-md border">
            <option value="">Select program</option>
            {faculty?.programs.map((p) => (
              <option key={p.id} value={p.id}>{p.name} ({p.degree})</option>
            ))}
          </select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="program2">Second Choice Program (Optional)</Label>
          <select id="program2" className="w-full h-10 px-3 rounded-md border">
            <option value="">Select program</option>
            {faculty?.programs.map((p) => (
              <option key={p.id} value={p.id}>{p.name} ({p.degree})</option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="studyMode">Study Mode *</Label>
            <select id="studyMode" className="w-full h-10 px-3 rounded-md border">
              <option value="">Select mode</option>
              <option value="fulltime">Full-time</option>
              <option value="parttime">Part-time</option>
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="startSemester">Start Semester *</Label>
            <select id="startSemester" className="w-full h-10 px-3 rounded-md border">
              <option value="">Select semester</option>
              <option value="fall2026">Fall 2026 (October)</option>
              <option value="spring2027">Spring 2027 (February)</option>
            </select>
          </div>
        </div>
      </CardContent>
    </>
  );
}

function DocumentsStep() {
  return (
    <>
      <CardHeader>
        <CardTitle>Supporting Documents</CardTitle>
        <CardDescription>
          Upload required documents to complete your application.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label>Motivation Letter *</Label>
          <p className="text-xs text-muted-foreground mb-2">
            Explain why you want to study at Minervia Institute and your career goals (500-1000 words).
          </p>
          <div className="border-2 border-dashed rounded-lg p-6 text-center">
            <input type="file" className="hidden" id="motivation" accept=".pdf,.doc,.docx" />
            <label htmlFor="motivation" className="cursor-pointer">
              <div className="text-muted-foreground">
                <p>Click to upload or drag and drop</p>
                <p className="text-xs">PDF or Word document (max 2MB)</p>
              </div>
            </label>
          </div>
        </div>

        <div className="space-y-2">
          <Label>CV / Resume *</Label>
          <div className="border-2 border-dashed rounded-lg p-6 text-center">
            <input type="file" className="hidden" id="cv" accept=".pdf,.doc,.docx" />
            <label htmlFor="cv" className="cursor-pointer">
              <div className="text-muted-foreground">
                <p>Click to upload or drag and drop</p>
                <p className="text-xs">PDF or Word document (max 2MB)</p>
              </div>
            </label>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Passport Photo *</Label>
          <p className="text-xs text-muted-foreground mb-2">
            Recent passport-style photo with white background.
          </p>
          <div className="border-2 border-dashed rounded-lg p-6 text-center">
            <input type="file" className="hidden" id="photo" accept=".jpg,.png" />
            <label htmlFor="photo" className="cursor-pointer">
              <div className="text-muted-foreground">
                <p>Click to upload or drag and drop</p>
                <p className="text-xs">JPG or PNG (max 1MB)</p>
              </div>
            </label>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Passport Copy *</Label>
          <div className="border-2 border-dashed rounded-lg p-6 text-center">
            <input type="file" className="hidden" id="passport" accept=".pdf,.jpg,.png" />
            <label htmlFor="passport" className="cursor-pointer">
              <div className="text-muted-foreground">
                <p>Click to upload or drag and drop</p>
                <p className="text-xs">PDF, JPG, or PNG (max 5MB)</p>
              </div>
            </label>
          </div>
        </div>

        <div className="space-y-2">
          <Label>Additional Documents (Optional)</Label>
          <p className="text-xs text-muted-foreground mb-2">
            Letters of recommendation, portfolio, or other supporting documents.
          </p>
          <div className="border-2 border-dashed rounded-lg p-6 text-center">
            <input type="file" className="hidden" id="additional" accept=".pdf,.jpg,.png" multiple />
            <label htmlFor="additional" className="cursor-pointer">
              <div className="text-muted-foreground">
                <p>Click to upload or drag and drop</p>
                <p className="text-xs">Multiple files allowed (max 10MB total)</p>
              </div>
            </label>
          </div>
        </div>
      </CardContent>
    </>
  );
}

interface ReviewStepProps extends StepProps {
  locale: string;
}

function ReviewStep({ locale, formData, setFormData }: ReviewStepProps) {
  return (
    <>
      <CardHeader>
        <CardTitle>Review & Submit</CardTitle>
        <CardDescription>
          Please review your application before submitting.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="bg-slate-50 rounded-lg p-4">
          <h4 className="font-medium mb-2">Application Summary</h4>
          <p className="text-sm text-muted-foreground">
            Please ensure all information is accurate. You will not be able to modify your application after submission.
          </p>
        </div>

        <div className="border rounded-lg p-4 space-y-4">
          <h4 className="font-medium">Application Fee</h4>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm">Standard Application Fee</span>
              <span className="font-medium">EUR 50</span>
            </div>
            <div className="border-t pt-3">
              <div className="flex items-start gap-2">
                <input
                  type="checkbox"
                  id="financialAid"
                  className="mt-1"
                  checked={formData.hasFinancialAid}
                  onChange={(e) => setFormData(prev => ({ ...prev, hasFinancialAid: e.target.checked }))}
                />
                <label htmlFor="financialAid" className="text-sm">
                  I would like to apply for fee reduction/waiver
                </label>
              </div>
              {formData.hasFinancialAid && (
                <div className="mt-3 ml-6 space-y-2">
                  <Label htmlFor="aidType" className="text-sm">Select category:</Label>
                  <select
                    id="aidType"
                    className="w-full h-10 px-3 rounded-md border text-sm"
                    value={formData.aidType}
                    onChange={(e) => setFormData(prev => ({ ...prev, aidType: e.target.value }))}
                  >
                    <option value="">Select category</option>
                    <option value="financial">Financial Hardship (50% reduction)</option>
                    <option value="refugee">Refugee/Asylum Seeker (Full waiver)</option>
                    <option value="scholarship">Scholarship Recipient (Full waiver)</option>
                    <option value="partner">Partner Institution Student (25% reduction)</option>
                  </select>
                  <p className="text-xs text-muted-foreground">
                    Supporting documentation will be required. Details will be sent via email.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex items-start gap-2">
            <input
              type="checkbox"
              id="terms"
              className="mt-1"
              checked={formData.acceptedTerms}
              onChange={(e) => setFormData(prev => ({ ...prev, acceptedTerms: e.target.checked }))}
            />
            <label htmlFor="terms" className="text-sm">
              I confirm that all information provided is accurate and complete. I understand that providing false information may result in rejection of my application. *
            </label>
          </div>

          <div className="flex items-start gap-2">
            <input
              type="checkbox"
              id="privacy"
              className="mt-1"
              checked={formData.acceptedPrivacy}
              onChange={(e) => setFormData(prev => ({ ...prev, acceptedPrivacy: e.target.checked }))}
            />
            <label htmlFor="privacy" className="text-sm">
              I consent to the processing of my personal data in accordance with the{' '}
              <Link href={`/${locale}/privacy`} className="text-slate-900 underline" target="_blank">
                Privacy Policy
              </Link>{' '}
              and GDPR regulations. *
            </label>
          </div>

          <div className="flex items-start gap-2">
            <input type="checkbox" id="communication" className="mt-1" />
            <label htmlFor="communication" className="text-sm">
              I agree to receive communications about my application status and other relevant information from Minervia Institute.
            </label>
          </div>
        </div>

        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <h4 className="font-medium text-blue-800 mb-2">Important Information</h4>
          <ul className="text-sm text-blue-700 space-y-1 list-disc pl-4">
            <li>Payment instructions will be sent to your email after submission</li>
            <li>A video interview will be scheduled as part of the admission process</li>
            <li>EDU email will be issued only after in-person enrollment</li>
          </ul>
        </div>
      </CardContent>
    </>
  );
}

function SubmittedStep({ locale, formData }: { locale: string; formData: FormData }) {
  const applicationId = `MI-2026-${Math.random().toString(36).substring(2, 8).toUpperCase()}`;

  return (
    <div className="py-16 md:py-24">
      <div className="container max-w-2xl">
        <Card className="border-0 shadow-lg">
          <CardContent className="pt-12 pb-8">
            <div className="text-center mb-8">
              <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="w-10 h-10 text-green-600" />
              </div>
              <h1 className="text-2xl font-bold mb-2">Application Submitted</h1>
              <p className="text-muted-foreground">
                Thank you for applying to Minervia Institute.
              </p>
            </div>

            <div className="bg-slate-50 rounded-lg p-4 mb-6 text-center">
              <div className="text-sm text-muted-foreground mb-1">Application Reference Number</div>
              <div className="text-xl font-mono font-bold">{applicationId}</div>
            </div>

            <div className="space-y-6 mb-8">
              <div>
                <h3 className="font-semibold mb-3 flex items-center gap-2">
                  <Clock className="w-5 h-5" />
                  What Happens Next
                </h3>
                <div className="space-y-4">
                  <div className="flex gap-3">
                    <div className="w-6 h-6 rounded-full bg-slate-200 flex items-center justify-center flex-shrink-0 text-xs font-medium">1</div>
                    <div>
                      <div className="font-medium text-sm">Confirmation Email</div>
                      <p className="text-sm text-muted-foreground">
                        You will receive a confirmation email at <span className="font-medium">{formData.email}</span> with your application details and payment instructions.
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-3">
                    <div className="w-6 h-6 rounded-full bg-slate-200 flex items-center justify-center flex-shrink-0 text-xs font-medium">2</div>
                    <div>
                      <div className="font-medium text-sm">Application Fee Payment</div>
                      <p className="text-sm text-muted-foreground">
                        {formData.hasFinancialAid
                          ? 'Your fee reduction request will be reviewed. Payment details will be sent after verification.'
                          : 'A payment link (EUR 50) will be included in your confirmation email. Payment must be completed within 7 days.'}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-3">
                    <div className="w-6 h-6 rounded-full bg-slate-200 flex items-center justify-center flex-shrink-0 text-xs font-medium">3</div>
                    <div>
                      <div className="font-medium text-sm">Document Verification</div>
                      <p className="text-sm text-muted-foreground">
                        Our admissions team will verify your submitted documents within 2-3 weeks.
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-3">
                    <div className="w-6 h-6 rounded-full bg-slate-200 flex items-center justify-center flex-shrink-0 text-xs font-medium">4</div>
                    <div>
                      <div className="font-medium text-sm flex items-center gap-2">
                        <Video className="w-4 h-4" />
                        Video Interview
                      </div>
                      <p className="text-sm text-muted-foreground">
                        After document verification, you will be invited to a 15-20 minute online video interview. This is a mandatory step to verify your identity and assess your motivation.
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-3">
                    <div className="w-6 h-6 rounded-full bg-slate-200 flex items-center justify-center flex-shrink-0 text-xs font-medium">5</div>
                    <div>
                      <div className="font-medium text-sm">Final Decision</div>
                      <p className="text-sm text-muted-foreground">
                        The admission decision will be communicated via email within 4-6 weeks of completing all steps.
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="border rounded-lg p-4 space-y-3">
                <h4 className="font-medium flex items-center gap-2">
                  <Mail className="w-5 h-5" />
                  About Your Student Account
                </h4>
                <div className="text-sm text-muted-foreground space-y-2">
                  <p>
                    <strong>Pre-enrollment Access:</strong> After passing the video interview, you can log in to the Student Portal using your application email ({formData.email}) to view course schedules and pre-enrollment information.
                  </p>
                  <p>
                    <strong>EDU Email:</strong> To ensure authenticity and prevent misuse, your institutional email address (@minervia.edu.pl) will be issued only after you complete in-person enrollment at our campus.
                  </p>
                  <p>
                    <strong>Full Access:</strong> Complete student privileges (library access, course registration, grade viewing) are activated after physical enrollment verification.
                  </p>
                </div>
              </div>

              <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
                <h4 className="font-medium text-amber-800 mb-2 flex items-center gap-2">
                  <Building2 className="w-5 h-5" />
                  In-Person Enrollment Required
                </h4>
                <p className="text-sm text-amber-700">
                  Please note that admission is conditional until you complete in-person enrollment at our campus in Krakow. This includes identity verification, document submission, and orientation attendance.
                </p>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Button asChild variant="outline">
                <Link href={`/${locale}`}>Return to Homepage</Link>
              </Button>
              <Button asChild>
                <Link href={`/${locale}/faculties`}>Explore Faculties</Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
