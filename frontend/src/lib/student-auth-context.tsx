'use client';

import { createContext, useContext, useEffect, useState, ReactNode, useCallback } from 'react';
import { studentApiClient } from '@/lib/student-api-client';

interface Student {
  id: string;
  studentId: string;
  firstName: string;
  lastName: string;
  email: string;
  majorId: number;
}

interface StudentAuthContextType {
  student: Student | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<LoginResult>;
  logout: () => Promise<void>;
}

interface LoginResult {
  success: boolean;
  message?: string;
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  student: Student;
}

const StudentAuthContext = createContext<StudentAuthContextType | undefined>(undefined);

export function StudentAuthProvider({ children }: { children: ReactNode }) {
  const [student, setStudent] = useState<Student | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const handleAuthChange = useCallback(() => {
    setStudent(null);
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
    const response = await studentApiClient.post<LoginResponse>('/api/student/auth/login', {
      email,
      password,
    });

    if (!response.success) {
      return { success: false, message: response.message };
    }

    if (response.data) {
      studentApiClient.setTokens(response.data.accessToken, response.data.refreshToken);
      setStudent(response.data.student);
      localStorage.setItem('student', JSON.stringify(response.data.student));
      return { success: true };
    }

    return { success: false, message: 'Login failed' };
  };

  const logout = async () => {
    await studentApiClient.post('/api/student/auth/logout');
    studentApiClient.clearTokens();
    setStudent(null);
    localStorage.removeItem('student');
  };

  return (
    <StudentAuthContext.Provider
      value={{
        student,
        isLoading,
        isAuthenticated: !!student,
        login,
        logout,
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
