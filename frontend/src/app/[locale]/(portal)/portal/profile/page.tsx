'use client';

import { useTranslations } from 'next-intl';
import { useStudentAuth } from '@/lib/student-auth-context';
import { StudentShell } from '@/components/portal';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export default function StudentProfilePage() {
  const t = useTranslations('Portal.profile');
  const { student } = useStudentAuth();

  const profileFields = [
    { label: t('fields.studentId'), value: student?.studentId },
    { label: t('fields.firstName'), value: student?.firstName },
    { label: t('fields.lastName'), value: student?.lastName },
    { label: t('fields.email'), value: student?.email },
  ];

  return (
    <StudentShell>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('title')}</h1>
          <p className="text-muted-foreground">{t('subtitle')}</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>{t('personalInfo')}</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {profileFields.map((field) => (
                <div key={field.label} className="space-y-1">
                  <dt className="text-sm font-medium text-muted-foreground">
                    {field.label}
                  </dt>
                  <dd className="text-lg">{field.value || '-'}</dd>
                </div>
              ))}
            </dl>
          </CardContent>
        </Card>
      </div>
    </StudentShell>
  );
}
