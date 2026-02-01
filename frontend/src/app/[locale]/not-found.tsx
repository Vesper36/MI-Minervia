import Link from 'next/link';
import { getTranslations } from 'next-intl/server';
import { Button } from '@/components/ui/button';

interface Props {
  params: { locale: string };
}

export default async function NotFound({ params }: Props) {
  const locale = params?.locale || 'en';
  const t = await getTranslations({ locale, namespace: 'Common.notFound' });

  return (
    <div className="min-h-[60vh] flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-9xl font-bold text-slate-200">404</h1>
        <h2 className="text-2xl font-bold mt-4 mb-2">{t('title')}</h2>
        <p className="text-muted-foreground mb-8 max-w-md">
          {t('description')}
        </p>
        <Button asChild>
          <Link href={`/${locale}`}>{t('backHome')}</Link>
        </Button>
      </div>
    </div>
  );
}
