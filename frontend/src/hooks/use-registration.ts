"use client"

import { useState, useEffect, useCallback } from 'react';
import {
  RegistrationState,
  RegistrationData,
  RegistrationStep,
  INITIAL_DATA,
  STORAGE_KEY,
  RegistrationApplicationDto,
  VerifyCodeResponse,
  VerifyEmailResponse,
} from '@/lib/registration-types';
import { apiClient } from '@/lib/api-client';

export function useRegistration() {
  const [state, setState] = useState<RegistrationState>({
    step: 'verify-code',
    data: INITIAL_DATA,
    isLoading: false,
    error: null,
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        setState(prev => ({
          ...prev,
          data: { ...prev.data, ...parsed.data },
          step: parsed.step || 'verify-code',
        }));
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    if (state.step !== 'progress') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        data: state.data,
        step: state.step,
      }));
    }
  }, [state.data, state.step]);

  const updateData = useCallback((updates: Partial<RegistrationData>) => {
    setState(prev => ({ ...prev, data: { ...prev.data, ...updates }, error: null }));
  }, []);

  const setStep = useCallback((step: RegistrationStep) => {
    setState(prev => ({ ...prev, step, error: null }));
  }, []);

  const setError = useCallback((error: string | null) => {
    setState(prev => ({ ...prev, error, isLoading: false }));
  }, []);

  const setLoading = useCallback((isLoading: boolean) => {
    setState(prev => ({ ...prev, isLoading }));
  }, []);

  const verifyCode = useCallback(async (code: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.post<VerifyCodeResponse>('/api/public/registration-codes/verify', { code });
      if (res.success && res.data?.valid) {
        updateData({ registrationCode: code });
        setStep('basic-info');
      } else {
        setError(res.data?.message || res.message || 'Invalid registration code');
      }
    } catch {
      setError('Failed to verify code. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [updateData, setStep, setError, setLoading]);

  const startRegistration = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.post<RegistrationApplicationDto>('/api/public/registration/start', {
        code: state.data.registrationCode,
        email: state.data.email,
        identityType: state.data.identityType,
        countryCode: state.data.identityType === 'INTERNATIONAL' ? state.data.countryCode : null,
      });

      if (res.success && res.data) {
        updateData({ applicationId: res.data.id });
        setStep('verify-email');
      } else {
        setError(res.message || 'Failed to start registration');
      }
    } catch {
      setError('An error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [state.data, updateData, setStep, setError, setLoading]);

  const sendVerificationCode = useCallback(async () => {
    if (!state.data.applicationId) return;
    setLoading(true);
    try {
      await apiClient.post(`/api/public/registration/${state.data.applicationId}/send-verification`);
    } catch {
      setError('Failed to send verification code');
    } finally {
      setLoading(false);
    }
  }, [state.data.applicationId, setError, setLoading]);

  const verifyEmail = useCallback(async (code: string) => {
    if (!state.data.applicationId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.post<VerifyEmailResponse>(
        `/api/public/registration/${state.data.applicationId}/verify-email`,
        { code }
      );
      if (res.success && res.data?.verified) {
        setStep('select-info');
      } else {
        const remaining = res.data?.attemptsRemaining;
        const msg = remaining !== undefined
          ? `Invalid code. ${remaining} attempts remaining.`
          : res.data?.message || 'Invalid verification code';
        setError(msg);
      }
    } catch {
      setError('Verification failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [state.data.applicationId, setStep, setError, setLoading]);

  const submitInfo = useCallback(async () => {
    if (!state.data.applicationId || !state.data.majorId || !state.data.classId) return;
    setLoading(true);
    setError(null);
    try {
      const infoRes = await apiClient.put<RegistrationApplicationDto>(
        `/api/public/registration/${state.data.applicationId}/info`,
        {
          majorId: state.data.majorId,
          classId: state.data.classId,
          countryCode: state.data.identityType === 'INTERNATIONAL' ? state.data.countryCode : null,
        }
      );

      if (!infoRes.success) {
        setError(infoRes.message || 'Failed to update information');
        return;
      }

      const submitRes = await apiClient.post<RegistrationApplicationDto>(
        `/api/public/registration/${state.data.applicationId}/submit`
      );

      if (submitRes.success) {
        localStorage.removeItem(STORAGE_KEY);
        setStep('progress');
      } else {
        setError(submitRes.message || 'Failed to submit application');
      }
    } catch {
      setError('Submission failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [state.data, setStep, setError, setLoading]);

  const initiateOAuth = useCallback(async () => {
    if (!state.data.applicationId) return;
    try {
      const res = await apiClient.get<{ authorizationUrl: string }>(
        `/api/public/oauth/linuxdo/authorize?applicationId=${state.data.applicationId}`
      );
      if (res.success && res.data?.authorizationUrl) {
        window.location.href = res.data.authorizationUrl;
      }
    } catch {
      setError('Failed to initiate OAuth');
    }
  }, [state.data.applicationId, setError]);

  const reset = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setState({
      step: 'verify-code',
      data: INITIAL_DATA,
      isLoading: false,
      error: null,
    });
  }, []);

  return {
    ...state,
    updateData,
    setStep,
    verifyCode,
    startRegistration,
    sendVerificationCode,
    verifyEmail,
    submitInfo,
    initiateOAuth,
    reset,
  };
}
