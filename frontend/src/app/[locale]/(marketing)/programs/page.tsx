import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.programs' });
  return {
    title: t('title'),
    description: t('description'),
    openGraph: {
      title: t('title'),
      description: t('description'),
    },
  };
}

export default async function ProgramsPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.programs' });

  const programs = [
    { key: 'cs', name: t('cs'), abbrev: t('csAbbrev') },
    { key: 'business', name: t('business'), abbrev: t('businessAbbrev') },
    { key: 'engineering', name: t('engineering'), abbrev: t('engineeringAbbrev') },
    { key: 'medicine', name: t('medicine'), abbrev: t('medicineAbbrev') },
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

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 max-w-6xl mx-auto">
          {programs.map((program) => (
            <Card key={program.key} className="text-center">
              <CardHeader>
                <CardTitle className="text-lg">{program.name}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="h-24 flex items-center justify-center bg-muted rounded-md">
                  <span className="text-4xl text-muted-foreground">
                    {program.abbrev}
                  </span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
