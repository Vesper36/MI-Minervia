'use client';

import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { apiClient } from '@/lib/api-client';

interface Admin {
  username: string;
  role: string;
}

interface AuthContextType {
  admin: Admin | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (username: string, password: string, totpCode?: string) => Promise<LoginResult>;
  logout: () => Promise<void>;
}

interface LoginResult {
  success: boolean;
  requiresTotp?: boolean;
  message?: string;
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  accessExpiresIn: number;
  refreshExpiresIn: number;
  username: string;
  role: string;
  requiresTotp: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [admin, setAdmin] = useState<Admin | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = apiClient.getAccessToken();
    if (token) {
      const stored = localStorage.getItem('admin');
      if (stored) {
        try {
          setAdmin(JSON.parse(stored));
        } catch {
          apiClient.clearTokens();
        }
      }
    }
    setIsLoading(false);
  }, []);

  const login = async (username: string, password: string, totpCode?: string): Promise<LoginResult> => {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', {
      username,
      password,
      totpCode,
    });

    if (!response.success) {
      return { success: false, message: response.message };
    }

    if (response.data?.requiresTotp && !response.data.accessToken) {
      return { success: false, requiresTotp: true };
    }

    if (response.data) {
      apiClient.setTokens(response.data.accessToken, response.data.refreshToken);
      const adminData = { username: response.data.username, role: response.data.role };
      setAdmin(adminData);
      localStorage.setItem('admin', JSON.stringify(adminData));
      return { success: true };
    }

    return { success: false, message: 'Login failed' };
  };

  const logout = async () => {
    await apiClient.post('/api/auth/logout');
    apiClient.clearTokens();
    setAdmin(null);
    localStorage.removeItem('admin');
  };

  return (
    <AuthContext.Provider
      value={{
        admin,
        isLoading,
        isAuthenticated: !!admin,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
