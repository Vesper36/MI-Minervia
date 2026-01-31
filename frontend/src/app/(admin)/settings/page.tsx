'use client';

import { useEffect, useState } from 'react';
import { AdminShell } from '@/components/admin/shell';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { apiClient } from '@/lib/api-client';
import { Save, Shield, ShieldCheck } from 'lucide-react';

interface SystemConfig {
  id: number;
  configKey: string;
  configValue: string;
  description: string | null;
}

interface TotpStatus {
  enabled: boolean;
}

interface TotpSetup {
  secret: string;
  qrCodeUri: string;
}

export default function SettingsPage() {
  const [configs, setConfigs] = useState<SystemConfig[]>([]);
  const [editedValues, setEditedValues] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [totpStatus, setTotpStatus] = useState<TotpStatus | null>(null);
  const [totpSetup, setTotpSetup] = useState<TotpSetup | null>(null);
  const [totpCode, setTotpCode] = useState('');

  const fetchConfigs = async () => {
    setIsLoading(true);
    const response = await apiClient.get<SystemConfig[]>('/api/admin/system-configs');
    if (response.success && response.data) {
      setConfigs(response.data);
      const values: Record<string, string> = {};
      response.data.forEach((c) => {
        values[c.configKey] = c.configValue;
      });
      setEditedValues(values);
    }
    setIsLoading(false);
  };

  const fetchTotpStatus = async () => {
    const response = await apiClient.get<TotpStatus>('/api/admin/totp/status');
    if (response.success && response.data) {
      setTotpStatus(response.data);
    }
  };

  useEffect(() => {
    fetchConfigs();
    fetchTotpStatus();
  }, []);

  const handleSave = async (key: string) => {
    setIsSaving(true);
    await apiClient.put(`/api/admin/system-configs/${key}`, {
      value: editedValues[key],
    });
    setIsSaving(false);
    fetchConfigs();
  };

  const handleSetupTotp = async () => {
    const response = await apiClient.post<TotpSetup>('/api/admin/totp/setup');
    if (response.success && response.data) {
      setTotpSetup(response.data);
    }
  };

  const handleEnableTotp = async () => {
    const response = await apiClient.post('/api/admin/totp/enable', {
      code: totpCode,
    });
    if (response.success) {
      setTotpSetup(null);
      setTotpCode('');
      fetchTotpStatus();
    }
  };

  const handleDisableTotp = async () => {
    const code = prompt('Enter your TOTP code to disable 2FA:');
    if (!code) return;

    const response = await apiClient.post('/api/admin/totp/disable', { code });
    if (response.success) {
      fetchTotpStatus();
    }
  };

  const configGroups = [
    {
      title: 'Authentication',
      keys: ['jwt_expiration_hours', 'login_max_attempts', 'login_lockout_minutes'],
    },
    {
      title: 'Registration',
      keys: ['registration_code_expiry_days', 'default_email_limit'],
    },
    {
      title: 'Security',
      keys: ['password_min_length'],
    },
  ];

  return (
    <AdminShell>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Settings</h1>
          <p className="text-muted-foreground">Configure system settings</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Two-Factor Authentication
            </CardTitle>
            <CardDescription>
              Secure your account with TOTP-based two-factor authentication
            </CardDescription>
          </CardHeader>
          <CardContent>
            {totpStatus?.enabled ? (
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-5 w-5 text-green-500" />
                  <span>Two-factor authentication is enabled</span>
                </div>
                <Button variant="destructive" onClick={handleDisableTotp}>
                  Disable 2FA
                </Button>
              </div>
            ) : totpSetup ? (
              <div className="space-y-4">
                <div>
                  <p className="text-sm text-muted-foreground mb-2">
                    Scan this QR code with your authenticator app:
                  </p>
                  <div className="bg-white p-4 inline-block rounded">
                    <img
                      src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(totpSetup.qrCodeUri)}`}
                      alt="TOTP QR Code"
                      width={200}
                      height={200}
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>Manual entry key</Label>
                  <code className="block p-2 bg-muted rounded text-sm">
                    {totpSetup.secret}
                  </code>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="totp-verify">Enter code to verify</Label>
                  <div className="flex gap-2">
                    <Input
                      id="totp-verify"
                      value={totpCode}
                      onChange={(e) => setTotpCode(e.target.value.replace(/\D/g, ''))}
                      maxLength={6}
                      placeholder="000000"
                      className="w-32"
                    />
                    <Button onClick={handleEnableTotp}>Verify & Enable</Button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">
                  Two-factor authentication is not enabled
                </span>
                <Button onClick={handleSetupTotp}>Setup 2FA</Button>
              </div>
            )}
          </CardContent>
        </Card>

        {isLoading ? (
          <div className="text-center py-8">Loading...</div>
        ) : (
          configGroups.map((group) => (
            <Card key={group.title}>
              <CardHeader>
                <CardTitle>{group.title}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {group.keys.map((key) => {
                  const config = configs.find((c) => c.configKey === key);
                  if (!config) return null;

                  return (
                    <div key={key} className="flex items-end gap-4">
                      <div className="flex-1 space-y-2">
                        <Label htmlFor={key}>
                          {key.replace(/_/g, ' ').replace(/\b\w/g, (l) => l.toUpperCase())}
                        </Label>
                        {config.description && (
                          <p className="text-xs text-muted-foreground">
                            {config.description}
                          </p>
                        )}
                        <Input
                          id={key}
                          value={editedValues[key] || ''}
                          onChange={(e) =>
                            setEditedValues((prev) => ({
                              ...prev,
                              [key]: e.target.value,
                            }))
                          }
                        />
                      </div>
                      <Button
                        onClick={() => handleSave(key)}
                        disabled={isSaving || editedValues[key] === config.configValue}
                      >
                        <Save className="h-4 w-4 mr-2" />
                        Save
                      </Button>
                    </div>
                  );
                })}
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </AdminShell>
  );
}
