"use client"

import { useEffect, useState, useRef } from 'react'
import { useTranslations } from 'next-intl'
import { useRegistration } from '@/hooks/use-registration'
import { useProgressTracking } from '@/hooks/use-progress-tracking'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Progress } from '@/components/ui/progress'
import { Stepper } from '@/components/ui/stepper'
import { Loader2, CheckCircle, XCircle, AlertCircle, ExternalLink } from 'lucide-react'
import { cn } from '@/lib/utils'
import { COUNTRIES, MAJORS, CLASSES, TaskStatus } from '@/lib/registration-types'

const STEP_IDS = ['verify-code', 'basic-info', 'verify-email', 'select-info', 'progress'] as const;

function getStatusKey(status: TaskStatus): string {
  const statusMap: Record<TaskStatus, string> = {
    'PENDING': 'pending',
    'GENERATING_IDENTITY': 'generatingIdentity',
    'GENERATING_PHOTOS': 'generatingPhotos',
    'COMPLETED': 'completed',
    'FAILED': 'failed',
  };
  return statusMap[status] || 'processing';
}

export function RegistrationWizard() {
  const t = useTranslations('Register');
  const tData = useTranslations('Data');

  const {
    step,
    data,
    isLoading,
    error,
    updateData,
    verifyCode,
    startRegistration,
    sendVerificationCode,
    verifyEmail,
    submitInfo,
    initiateOAuth,
    reset,
  } = useRegistration()

  const { progress, isConnected } = useProgressTracking({
    applicationId: data.applicationId,
    enabled: step === 'progress',
  })

  const [otpValue, setOtpValue] = useState('')
  const verificationSentRef = useRef<number | null>(null)

  const steps = STEP_IDS.map(id => ({
    id,
    label: t(`steps.${id === 'verify-code' ? 'code' : id === 'basic-info' ? 'info' : id === 'verify-email' ? 'email' : id === 'select-info' ? 'details' : 'status'}`),
  }));

  useEffect(() => {
    if (step === 'verify-email' && data.applicationId && verificationSentRef.current !== data.applicationId) {
      verificationSentRef.current = data.applicationId
      sendVerificationCode()
    }
  }, [step, data.applicationId, sendVerificationCode])

  const handleOtpChange = (value: string) => {
    const cleaned = value.replace(/\D/g, '').slice(0, 6)
    setOtpValue(cleaned)
    if (cleaned.length === 6) {
      verifyEmail(cleaned)
    }
  }

  const getTitle = () => {
    const titleMap: Record<string, string> = {
      'verify-code': 'verifyCode',
      'basic-info': 'basicInfo',
      'verify-email': 'verifyEmail',
      'select-info': 'selectInfo',
      'progress': 'progress',
    };
    return t(`titles.${titleMap[step]}`);
  };

  const getDescription = () => {
    if (step === 'verify-email') {
      return t('descriptions.verifyEmail', { email: data.email });
    }
    const descMap: Record<string, string> = {
      'verify-code': 'verifyCode',
      'basic-info': 'basicInfo',
      'select-info': 'selectInfo',
      'progress': 'progress',
    };
    return t(`descriptions.${descMap[step]}`);
  };

  return (
    <div className="w-full max-w-lg mx-auto space-y-6">
      <Stepper steps={steps} currentStep={step} className="mb-8" />

      <Card>
        <CardHeader>
          <CardTitle>{getTitle()}</CardTitle>
          <CardDescription>{getDescription()}</CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          {error && (
            <div className="p-3 rounded-md bg-destructive/10 text-destructive text-sm flex items-center gap-2">
              <AlertCircle className="h-4 w-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {step === 'verify-code' && (
            <>
              <div className="space-y-2">
                <Label htmlFor="code">{t('labels.code')}</Label>
                <Input
                  id="code"
                  placeholder={t('placeholders.code')}
                  value={data.registrationCode}
                  onChange={(e) => updateData({ registrationCode: e.target.value.toUpperCase() })}
                  disabled={isLoading}
                />
              </div>
              <Button
                className="w-full"
                onClick={() => verifyCode(data.registrationCode)}
                disabled={isLoading || !data.registrationCode.trim()}
              >
                {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {t('buttons.verifyCode')}
              </Button>
            </>
          )}

          {step === 'basic-info' && (
            <>
              <div className="space-y-2">
                <Label htmlFor="email">{t('labels.email')}</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder={t('placeholders.email')}
                  value={data.email}
                  onChange={(e) => updateData({ email: e.target.value })}
                  disabled={isLoading}
                />
              </div>
              <div className="space-y-2">
                <Label>{t('labels.identityType')}</Label>
                <Select
                  value={data.identityType}
                  onValueChange={(val) => updateData({ identityType: val as 'LOCAL' | 'INTERNATIONAL' })}
                  disabled={isLoading}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('placeholders.identityType')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOCAL">{t('options.local')}</SelectItem>
                    <SelectItem value="INTERNATIONAL">{t('options.international')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              {data.identityType === 'INTERNATIONAL' && (
                <div className="space-y-2">
                  <Label>{t('labels.country')}</Label>
                  <Select
                    value={data.countryCode}
                    onValueChange={(val) => updateData({ countryCode: val })}
                    disabled={isLoading}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={t('placeholders.country')} />
                    </SelectTrigger>
                    <SelectContent>
                      {COUNTRIES.map((country) => (
                        <SelectItem key={country.code} value={country.code}>
                          {tData(`countries.${country.code}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
              <Button
                className="w-full"
                onClick={startRegistration}
                disabled={isLoading || !data.email.trim()}
              >
                {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {t('buttons.continue')}
              </Button>
            </>
          )}

          {step === 'verify-email' && (
            <>
              <div className="space-y-2">
                <Label htmlFor="otp">{t('labels.verificationCode')}</Label>
                <Input
                  id="otp"
                  placeholder={t('placeholders.verificationCode')}
                  value={otpValue}
                  onChange={(e) => handleOtpChange(e.target.value)}
                  className="text-center text-2xl tracking-[0.5em] font-mono"
                  maxLength={6}
                  disabled={isLoading}
                />
              </div>
              <p className="text-sm text-muted-foreground text-center">
                {t('text.didntReceiveCode')}{' '}
                <button
                  type="button"
                  className="text-primary underline-offset-4 hover:underline"
                  onClick={sendVerificationCode}
                  disabled={isLoading}
                >
                  {t('buttons.resend')}
                </button>
              </p>
            </>
          )}

          {step === 'select-info' && (
            <>
              <div className="space-y-2">
                <Label>{t('labels.major')}</Label>
                <Select
                  value={data.majorId?.toString() || ''}
                  onValueChange={(val) => updateData({ majorId: parseInt(val, 10) })}
                  disabled={isLoading}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('placeholders.major')} />
                  </SelectTrigger>
                  <SelectContent>
                    {MAJORS.map((major) => (
                      <SelectItem key={major.id} value={major.id.toString()}>
                        {tData(`majors.${major.id}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>{t('labels.classPreference')}</Label>
                <Select
                  value={data.classId?.toString() || ''}
                  onValueChange={(val) => updateData({ classId: parseInt(val, 10) })}
                  disabled={isLoading}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('placeholders.classPreference')} />
                  </SelectTrigger>
                  <SelectContent>
                    {CLASSES.map((cls) => (
                      <SelectItem key={cls.id} value={cls.id.toString()}>
                        {tData(`classes.${cls.id}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="pt-2 border-t">
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={initiateOAuth}
                  disabled={isLoading}
                >
                  <ExternalLink className="mr-2 h-4 w-4" />
                  {t('buttons.linkAccount')}
                </Button>
              </div>
              <Button
                className="w-full"
                onClick={submitInfo}
                disabled={isLoading || !data.majorId || !data.classId}
              >
                {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {t('buttons.submit')}
              </Button>
            </>
          )}

          {step === 'progress' && (
            <div className="space-y-6">
              <div className="flex flex-col items-center justify-center py-4">
                <div
                  className={cn(
                    "h-16 w-16 rounded-full flex items-center justify-center mb-4",
                    progress?.status === 'COMPLETED' && "bg-green-100 text-green-600",
                    progress?.status === 'FAILED' && "bg-red-100 text-red-600",
                    (!progress || (progress.status !== 'COMPLETED' && progress.status !== 'FAILED')) && "bg-blue-100 text-blue-600"
                  )}
                >
                  {progress?.status === 'COMPLETED' ? (
                    <CheckCircle className="h-8 w-8" />
                  ) : progress?.status === 'FAILED' ? (
                    <XCircle className="h-8 w-8" />
                  ) : (
                    <Loader2 className="h-8 w-8 animate-spin" />
                  )}
                </div>
                <h3 className="text-lg font-semibold">
                  {progress ? t(`status.${getStatusKey(progress.status)}`) : t('status.initializing')}
                </h3>
                {progress?.message && (
                  <p className="text-sm text-muted-foreground mt-1">{progress.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">{t('labels.progress')}</span>
                  <span className="font-medium">{progress?.progressPercent || 0}%</span>
                </div>
                <Progress value={progress?.progressPercent || 0} />
              </div>

              <div className="text-center text-sm text-muted-foreground">
                <p>{t('text.applicationId', { id: data.applicationId })}</p>
                {!isConnected && <p className="text-yellow-600">{t('text.pollingMode')}</p>}
              </div>

              {progress?.status === 'COMPLETED' && (
                <Button className="w-full" onClick={reset}>
                  {t('buttons.startNew')}
                </Button>
              )}

              {progress?.status === 'FAILED' && (
                <Button variant="outline" className="w-full" onClick={reset}>
                  {t('buttons.tryAgain')}
                </Button>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
