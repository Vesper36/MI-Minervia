'use client';

import { useTranslations } from 'next-intl';
import { StudentShell } from '@/components/portal';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export default function StudentCoursesPage() {
  const t = useTranslations('Portal.courses');

  return (
    <StudentShell>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('title')}</h1>
          <p className="text-muted-foreground">{t('subtitle')}</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>{t('enrolled')}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">{t('noCourses')}</p>
          </CardContent>
        </Card>
      </div>
    </StudentShell>
  );
}
