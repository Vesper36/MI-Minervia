'use client';

import { useTranslations } from 'next-intl';
import { useStudentAuth } from '@/lib/student-auth-context';
import { StudentShell } from '@/components/portal';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useEffect } from 'react';

export default function StudentDashboardPage() {
  const t = useTranslations('Portal.dashboard');
  const { student, profile, fetchProfile } = useStudentAuth();
  const params = useParams();
  const locale = params.locale as string;

  useEffect(() => {
    if (student && !profile) {
      fetchProfile();
    }
  }, [student, profile, fetchProfile]);

  const displayName = profile
    ? `${profile.firstName} ${profile.lastName}`
    : student?.fullName?.split(' ')[0] || '';

  return (
    <StudentShell>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {t('welcome', { name: displayName })}
          </h1>
          <p className="text-muted-foreground">{t('subtitle')}</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Link href={`/${locale}/portal/profile`}>
            <Card className="hover:shadow-md transition-shadow cursor-pointer">
              <CardHeader>
                <CardTitle className="text-lg">{t('cards.profile')}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-2xl font-bold">{student?.studentNumber || '-'}</p>
                <p className="text-sm text-muted-foreground">{t('cards.studentId')}</p>
              </CardContent>
            </Card>
          </Link>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{t('cards.email')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-lg font-medium truncate">{student?.eduEmail || '-'}</p>
              <p className="text-sm text-muted-foreground">{t('cards.eduEmail')}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{t('cards.status')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold text-green-600">
                {profile?.status === 'ACTIVE' ? t('cards.active') : (profile?.status || t('cards.active'))}
              </p>
              <p className="text-sm text-muted-foreground">{t('cards.enrollment')}</p>
            </CardContent>
          </Card>
        </div>

        {profile && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">{t('cards.academic')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">{t('cards.enrollmentYear')}</span>
                  <span className="font-medium">{profile.enrollmentYear}</span>
                </div>
                {profile.gpa && (
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">{t('cards.gpa')}</span>
                    <span className="font-medium">{profile.gpa}</span>
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-lg">{t('cards.quickLinks')}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <Link
                  href={`/${locale}/portal/profile`}
                  className="block text-sm text-primary hover:underline"
                >
                  {t('cards.viewProfile')}
                </Link>
                <Link
                  href={`/${locale}/portal/documents`}
                  className="block text-sm text-primary hover:underline"
                >
                  {t('cards.viewDocuments')}
                </Link>
                <Link
                  href={`/${locale}/portal/settings`}
                  className="block text-sm text-primary hover:underline"
                >
                  {t('cards.viewSettings')}
                </Link>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </StudentShell>
  );
}
