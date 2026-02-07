import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.about.history' });
  return {
    title: t('title'),
    description: t('description'),
  };
}

export default async function HistoryPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.about.history' });

  const historyEvents = [
    { year: '1998', title: t('1998.title'), description: t('1998.description') },
    { year: '2003', title: t('2003.title'), description: t('2003.description') },
    { year: '2008', title: t('2008.title'), description: t('2008.description') },
    { year: '2012', title: t('2012.title'), description: t('2012.description') },
    { year: '2018', title: t('2018.title'), description: t('2018.description') },
    { year: '2024', title: t('2024.title'), description: t('2024.description') },
  ];

  return (
    <div className="container py-16 md:py-24">
      <div className="text-center mb-16">
        <h1 className="text-4xl font-bold tracking-tight mb-4">{t('title')}</h1>
        <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
          {t('description')}
        </p>
      </div>

      <div className="relative max-w-4xl mx-auto">
        <div className="absolute left-4 md:left-1/2 top-0 h-full w-0.5 bg-border -translate-x-1/2 md:translate-x-0" />

        <div className="space-y-12">
          {historyEvents.map((event, index) => (
            <div
              key={event.year}
              className={`relative flex flex-col md:flex-row gap-8 ${
                index % 2 === 1 ? 'md:flex-row-reverse' : ''
              }`}
            >
              <div className="flex-1 ml-12 md:ml-0">
                <Card className="h-full">
                  <CardHeader>
                    <div className="flex justify-between items-baseline">
                      <CardTitle>{event.title}</CardTitle>
                      <span className="text-lg font-bold text-primary">{event.year}</span>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground">{event.description}</p>
                  </CardContent>
                </Card>
              </div>

              <div className="absolute left-4 md:left-1/2 w-4 h-4 rounded-full bg-primary ring-4 ring-background -translate-x-1/2 z-10 mt-6" />

              <div className="flex-1 hidden md:block" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
