import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.calendar' });
  return {
    title: t('title'),
    description: t('description'),
  };
}

export default async function CalendarPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.calendar' });

  const fallEvents = [
    { key: 'fallStart', date: t('events.fallStart.date'), title: t('events.fallStart.title') },
    { key: 'fallBreak', date: t('events.fallBreak.date'), title: t('events.fallBreak.title') },
    { key: 'winterBreak', date: t('events.winterBreak.date'), title: t('events.winterBreak.title') },
    { key: 'fallExams', date: t('events.fallExams.date'), title: t('events.fallExams.title') },
  ];

  const springEvents = [
    { key: 'springStart', date: t('events.springStart.date'), title: t('events.springStart.title') },
    { key: 'easterBreak', date: t('events.easterBreak.date'), title: t('events.easterBreak.title') },
    { key: 'springExams', date: t('events.springExams.date'), title: t('events.springExams.title') },
  ];

  const summerEvents = [
    { key: 'summerStart', date: t('events.summerStart.date'), title: t('events.summerStart.title') },
    { key: 'summerEnd', date: t('events.summerEnd.date'), title: t('events.summerEnd.title') },
  ];

  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Header */}
        <div className="text-center mb-16">
          <h1 className="text-4xl font-bold tracking-tight mb-4">{t('title')}</h1>
          <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
            {t('description')}
          </p>
        </div>

        <div className="max-w-4xl mx-auto space-y-8">
          {/* Fall Semester */}
          <Card className="border-0 shadow-md">
            <CardHeader className="bg-slate-900 text-white rounded-t-lg">
              <CardTitle>{t('fallSemester')}</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="divide-y">
                {fallEvents.map((event) => (
                  <div key={event.key} className="flex items-center justify-between p-4">
                    <span className="font-medium">{event.title}</span>
                    <span className="text-muted-foreground text-sm">{event.date}</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Spring Semester */}
          <Card className="border-0 shadow-md">
            <CardHeader className="bg-slate-800 text-white rounded-t-lg">
              <CardTitle>{t('springSemester')}</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="divide-y">
                {springEvents.map((event) => (
                  <div key={event.key} className="flex items-center justify-between p-4">
                    <span className="font-medium">{event.title}</span>
                    <span className="text-muted-foreground text-sm">{event.date}</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Summer Session */}
          <Card className="border-0 shadow-md">
            <CardHeader className="bg-slate-700 text-white rounded-t-lg">
              <CardTitle>{t('summerSession')}</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="divide-y">
                {summerEvents.map((event) => (
                  <div key={event.key} className="flex items-center justify-between p-4">
                    <span className="font-medium">{event.title}</span>
                    <span className="text-muted-foreground text-sm">{event.date}</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
