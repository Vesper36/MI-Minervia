import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.about' });
  return {
    title: t('title'),
    description: t('description'),
    openGraph: {
      title: t('title'),
      description: t('description'),
    },
  };
}

export default async function AboutPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.about' });

  const historyEvents = [
    { year: '1998', title: t('history.1998.title'), description: t('history.1998.description') },
    { year: '2003', title: t('history.2003.title'), description: t('history.2003.description') },
    { year: '2008', title: t('history.2008.title'), description: t('history.2008.description') },
    { year: '2012', title: t('history.2012.title'), description: t('history.2012.description') },
    { year: '2018', title: t('history.2018.title'), description: t('history.2018.description') },
    { year: '2024', title: t('history.2024.title'), description: t('history.2024.description') },
  ];

  const leaders = [
    {
      name: t('leadership.rector.name'),
      title: t('leadership.rector.title'),
      bio: t('leadership.rector.bio')
    },
    {
      name: t('leadership.viceRector.name'),
      title: t('leadership.viceRector.title'),
      bio: t('leadership.viceRector.bio')
    },
    {
      name: t('leadership.dean.name'),
      title: t('leadership.dean.title'),
      bio: t('leadership.dean.bio')
    },
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

        {/* Mission & Vision */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto mb-20">
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle className="text-lg">{t('mission')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground text-sm">{t('missionText')}</p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle className="text-lg">{t('vision')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground text-sm">{t('visionText')}</p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle className="text-lg">{t('values')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-muted-foreground text-sm">{t('valuesText')}</p>
            </CardContent>
          </Card>
        </div>

        {/* History Timeline */}
        <div className="mb-20">
          <h2 className="text-2xl font-bold text-center mb-4">{t('history.title')}</h2>
          <p className="text-muted-foreground text-center mb-12">{t('history.description')}</p>

          <div className="relative max-w-4xl mx-auto">
            {/* Timeline line */}
            <div className="absolute left-1/2 transform -translate-x-1/2 w-0.5 h-full bg-slate-200 hidden md:block" />

            <div className="space-y-8">
              {historyEvents.map((event, index) => (
                <div
                  key={event.year}
                  className={`flex flex-col md:flex-row items-center gap-4 ${
                    index % 2 === 0 ? 'md:flex-row' : 'md:flex-row-reverse'
                  }`}
                >
                  <div className={`flex-1 ${index % 2 === 0 ? 'md:text-right' : 'md:text-left'}`}>
                    <Card className="border-0 shadow-sm inline-block">
                      <CardContent className="p-4">
                        <div className="font-bold text-slate-900 mb-1">{event.title}</div>
                        <p className="text-sm text-muted-foreground">{event.description}</p>
                      </CardContent>
                    </Card>
                  </div>

                  <div className="relative z-10 w-16 h-16 rounded-full bg-slate-900 text-white flex items-center justify-center font-bold text-sm">
                    {event.year}
                  </div>

                  <div className="flex-1 hidden md:block" />
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Leadership */}
        <div>
          <h2 className="text-2xl font-bold text-center mb-12">{t('leadership.title')}</h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
            {leaders.map((leader) => (
              <Card key={leader.name} className="border-0 shadow-md text-center">
                <CardContent className="pt-6">
                  <div className="w-24 h-24 rounded-full bg-slate-200 mx-auto mb-4" />
                  <h3 className="font-bold text-lg">{leader.name}</h3>
                  <p className="text-sm text-muted-foreground mb-4">{leader.title}</p>
                  <p className="text-sm text-muted-foreground">{leader.bio}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
