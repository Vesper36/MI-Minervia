'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { Button } from '@/components/ui/button';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const t = useTranslations('Common');
  const params = useParams();
  const locale = params.locale as string;

  useEffect(() => {
    if (process.env.NODE_ENV === 'development') {
      console.error(error);
    }
  }, [error]);

  return (
    <div className="flex h-[calc(100vh-4rem)] flex-col items-center justify-center gap-4 px-4 text-center">
      <h2 className="text-2xl font-bold tracking-tight">{t('serverError.title')}</h2>
      <p className="text-muted-foreground">{t('serverError.description')}</p>
      <div className="flex gap-2">
        <Button onClick={() => reset()}>{t('serverError.retry')}</Button>
        <Button variant="outline" asChild>
          <Link href={`/${locale}`}>{t('notFound.backHome')}</Link>
        </Button>
      </div>
    </div>
  );
}
