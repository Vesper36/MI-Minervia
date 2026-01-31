'use client';

import { useTranslations } from 'next-intl';
import { useStudentAuth } from '@/lib/student-auth-context';
import { StudentShell } from '@/components/portal';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export default function StudentDashboardPage() {
  const t = useTranslations('Portal.dashboard');
  const { student } = useStudentAuth();

  return (
    <StudentShell>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {t('welcome', { name: student?.firstName || '' })}
          </h1>
          <p className="text-muted-foreground">{t('subtitle')}</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{t('cards.profile')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{student?.studentId}</p>
              <p className="text-sm text-muted-foreground">{t('cards.studentId')}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{t('cards.courses')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">0</p>
              <p className="text-sm text-muted-foreground">{t('cards.enrolled')}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">{t('cards.status')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold text-green-600">{t('cards.active')}</p>
              <p className="text-sm text-muted-foreground">{t('cards.enrollment')}</p>
            </CardContent>
          </Card>
        </div>
      </div>
    </StudentShell>
  );
}
