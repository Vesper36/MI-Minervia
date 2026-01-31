"use client"

import { useState, useEffect, useCallback, useRef } from 'react';
import { ProgressStatus, TaskStatus } from '@/lib/registration-types';
import { apiClient } from '@/lib/api-client';

interface UseProgressTrackingOptions {
  applicationId: number | null;
  enabled?: boolean;
  pollingInterval?: number;
  fastPollingInterval?: number;
}

interface UseProgressTrackingResult {
  progress: ProgressStatus | null;
  isConnected: boolean;
  error: string | null;
}

export function useProgressTracking({
  applicationId,
  enabled = true,
  pollingInterval = 5000,
  fastPollingInterval = 2000,
}: UseProgressTrackingOptions): UseProgressTrackingResult {
  const [progress, setProgress] = useState<ProgressStatus | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const lastVersionRef = useRef<number>(0);
  const wsRef = useRef<WebSocket | null>(null);

  const fetchStatus = useCallback(async () => {
    if (!applicationId) return;

    try {
      const res = await apiClient.get<ProgressStatus>(
        `/api/applications/${applicationId}/status/poll?lastVersion=${lastVersionRef.current}`
      );

      if (res.success && res.data) {
        if (res.data.version > lastVersionRef.current) {
          lastVersionRef.current = res.data.version;
          setProgress(res.data);
        }
      }
    } catch {
      setError('Failed to fetch status');
    }
  }, [applicationId]);

  useEffect(() => {
    if (!enabled || !applicationId) return;

    const wsUrl = `${process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080'}/ws`;

    try {
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        setIsConnected(true);
        setError(null);
        ws.send(JSON.stringify({
          type: 'SUBSCRIBE',
          destination: `/topic/applications/${applicationId}/progress`,
        }));
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data) as ProgressStatus;
          if (data.version > lastVersionRef.current) {
            lastVersionRef.current = data.version;
            setProgress(data);
          }
        } catch {
          // Ignore parse errors
        }
      };

      ws.onerror = () => {
        setIsConnected(false);
        setError('WebSocket connection failed, falling back to polling');
      };

      ws.onclose = () => {
        setIsConnected(false);
      };

      return () => {
        ws.close();
        wsRef.current = null;
      };
    } catch {
      setIsConnected(false);
    }
  }, [applicationId, enabled]);

  useEffect(() => {
    if (!enabled || !applicationId) return;

    fetchStatus();

    const isTerminal = progress?.status === 'COMPLETED' || progress?.status === 'FAILED';
    if (isTerminal) return;

    // Skip polling when WebSocket is connected
    if (isConnected) return;

    const interval = progress && progress.progressPercent > 80
      ? fastPollingInterval
      : pollingInterval;

    const timer = setInterval(fetchStatus, interval);
    return () => clearInterval(timer);
  }, [enabled, applicationId, isConnected, progress?.status, progress?.progressPercent, pollingInterval, fastPollingInterval, fetchStatus]);

  return { progress, isConnected, error };
}

export function getProgressLabel(status: TaskStatus): string {
  switch (status) {
    case 'PENDING':
      return 'Waiting to start...';
    case 'GENERATING_IDENTITY':
      return 'Generating identity information...';
    case 'GENERATING_PHOTOS':
      return 'Generating photos...';
    case 'COMPLETED':
      return 'Registration complete!';
    case 'FAILED':
      return 'Registration failed';
    default:
      return 'Processing...';
  }
}
