import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.admissions' });
  return {
    title: t('title'),
    description: t('description'),
    openGraph: {
      title: t('title'),
      description: t('description'),
    },
  };
}

export default async function AdmissionsPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.admissions' });

  const steps = [
    { number: 1, text: t('step1') },
    { number: 2, text: t('step2') },
    { number: 3, text: t('step3') },
  ];

  return (
    <div className="py-16 md:py-24">
      <div className="container">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold tracking-tight mb-4">{t('title')}</h1>
          <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
            {t('description')}
          </p>
        </div>

        <Card className="max-w-2xl mx-auto">
          <CardHeader>
            <CardTitle>{t('apply')}</CardTitle>
          </CardHeader>
          <CardContent>
            <ol className="space-y-4 mb-8">
              {steps.map((step) => (
                <li key={step.number} className="flex items-start gap-4">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-medium">
                    {step.number}
                  </span>
                  <span className="text-muted-foreground pt-1">{step.text}</span>
                </li>
              ))}
            </ol>
            <Button asChild className="w-full">
              <Link href={`/${locale}/register`}>{t('cta')}</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
