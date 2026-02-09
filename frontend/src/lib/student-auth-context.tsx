'use client';

import { createContext, useContext, useEffect, useState, ReactNode, useCallback } from 'react';
import { studentApiClient } from '@/lib/student-api-client';

interface Student {
  studentNumber: string;
  fullName: string;
  eduEmail: string;
}

interface StudentProfile {
  id: number;
  studentNumber: string;
  eduEmail: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  identityType: string;
  countryCode: string;
  majorId: number | null;
  classId: number | null;
  enrollmentYear: number;
  enrollmentDate: string;
  admissionDate: string;
  gpa: number | null;
  status: string;
  suspensionReason: string | null;
  dailyEmailLimit: number;
  photoUrl: string | null;
  familyBackground: string | null;
  interests: string | null;
  academicGoals: string | null;
  createdAt: string;
  updatedAt: string;
}

interface StudentAuthContextType {
  student: Student | null;
  profile: StudentProfile | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<LoginResult>;
  logout: () => Promise<void>;
  fetchProfile: () => Promise<void>;
}

interface LoginResult {
  success: boolean;
  message?: string;
}

interface LoginResponseData {
  accessToken: string;
  refreshToken: string;
  studentNumber: string;
  fullName: string;
  eduEmail: string;
}

const StudentAuthContext = createContext<StudentAuthContextType | undefined>(undefined);

export function StudentAuthProvider({ children }: { children: ReactNode }) {
  const [student, setStudent] = useState<Student | null>(null);
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const handleAuthChange = useCallback(() => {
    setStudent(null);
    setProfile(null);
  }, []);

  const fetchProfile = useCallback(async () => {
    const response = await studentApiClient.get<StudentProfile>('/api/student/portal/me');
    if (response.success && response.data) {
      setProfile(response.data);
    }
  }, []);

  useEffect(() => {
    studentApiClient.setAuthChangeCallback(handleAuthChange);

    const token = studentApiClient.getAccessToken();
    if (token) {
      const stored = localStorage.getItem('student');
      if (stored) {
        try {
          setStudent(JSON.parse(stored));
        } catch {
          studentApiClient.clearTokens();
        }
      }
    }
    setIsLoading(false);

    return () => {
      studentApiClient.setAuthChangeCallback(null);
    };
  }, [handleAuthChange]);

  const login = async (email: string, password: string): Promise<LoginResult> => {
    const response = await studentApiClient.post<LoginResponseData>('/api/student/auth/login', {
      email,
      password,
    });

    if (!response.success || !response.data) {
      return { success: false, message: response.message || 'Login failed' };
    }

    const data = response.data;
    studentApiClient.setTokens(data.accessToken, data.refreshToken);

    const studentInfo: Student = {
      studentNumber: data.studentNumber,
      fullName: data.fullName,
      eduEmail: data.eduEmail,
    };
    setStudent(studentInfo);
    localStorage.setItem('student', JSON.stringify(studentInfo));

    return { success: true };
  };

  const logout = async () => {
    await studentApiClient.post('/api/student/auth/logout');
    studentApiClient.clearTokens();
    setStudent(null);
    setProfile(null);
    localStorage.removeItem('student');
  };

  return (
    <StudentAuthContext.Provider
      value={{
        student,
        profile,
        isLoading,
        isAuthenticated: !!student,
        login,
        logout,
        fetchProfile,
      }}
    >
      {children}
    </StudentAuthContext.Provider>
  );
}

export function useStudentAuth() {
  const context = useContext(StudentAuthContext);
  if (context === undefined) {
    throw new Error('useStudentAuth must be used within a StudentAuthProvider');
  }
  return context;
}

export type { Student, StudentProfile };
