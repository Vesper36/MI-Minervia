'use client';

import { useTranslations } from 'next-intl';
import { useStudentAuth } from '@/lib/student-auth-context';
import { StudentShell } from '@/components/portal';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useEffect } from 'react';

export default function StudentProfilePage() {
  const t = useTranslations('Portal.profile');
  const { student, profile, fetchProfile } = useStudentAuth();

  useEffect(() => {
    if (student && !profile) {
      fetchProfile();
    }
  }, [student, profile, fetchProfile]);

  const personalFields = [
    { label: t('fields.studentNumber'), value: profile?.studentNumber || student?.studentNumber },
    { label: t('fields.eduEmail'), value: profile?.eduEmail || student?.eduEmail },
    { label: t('fields.firstName'), value: profile?.firstName },
    { label: t('fields.lastName'), value: profile?.lastName },
    { label: t('fields.birthDate'), value: profile?.birthDate },
    { label: t('fields.countryCode'), value: profile?.countryCode },
  ];

  const academicFields = [
    { label: t('fields.enrollmentYear'), value: profile?.enrollmentYear },
    { label: t('fields.enrollmentDate'), value: profile?.enrollmentDate },
    { label: t('fields.admissionDate'), value: profile?.admissionDate },
    { label: t('fields.gpa'), value: profile?.gpa },
    { label: t('fields.status'), value: profile?.status },
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
              {personalFields.map((field) => (
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

        {profile && (
          <Card>
            <CardHeader>
              <CardTitle>{t('academicInfo')}</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {academicFields.map((field) => (
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
        )}
      </div>
    </StudentShell>
  );
}