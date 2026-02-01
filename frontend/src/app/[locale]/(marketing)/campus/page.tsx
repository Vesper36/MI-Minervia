import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.campus' });
  return {
    title: t('title'),
    description: t('description'),
  };
}

export default async function CampusPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.campus' });

  const facilities = [
    { key: 'library', title: t('facilities.library.title'), description: t('facilities.library.description') },
    { key: 'labs', title: t('facilities.labs.title'), description: t('facilities.labs.description') },
    { key: 'sports', title: t('facilities.sports.title'), description: t('facilities.sports.description') },
    { key: 'dining', title: t('facilities.dining.title'), description: t('facilities.dining.description') },
  ];

  const organizations = [
    { key: 'academic', label: t('organizations.academic') },
    { key: 'cultural', label: t('organizations.cultural') },
    { key: 'sports', label: t('organizations.sports') },
    { key: 'volunteer', label: t('organizations.volunteer') },
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

        {/* Location */}
        <div className="mb-16">
          <Card className="border-0 shadow-md overflow-hidden">
            <div className="grid md:grid-cols-2">
              <div className="bg-slate-200 min-h-[300px]" />
              <CardContent className="p-8 flex flex-col justify-center">
                <h2 className="text-2xl font-bold mb-4">{t('location.title')}</h2>
                <p className="text-muted-foreground">{t('location.description')}</p>
              </CardContent>
            </div>
          </Card>
        </div>

        {/* Facilities */}
        <div className="mb-16">
          <h2 className="text-2xl font-bold text-center mb-8">{t('facilities.title')}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {facilities.map((facility) => (
              <Card key={facility.key} className="border-0 shadow-md">
                <CardHeader>
                  <div className="w-12 h-12 rounded-lg bg-slate-100 mb-4" />
                  <CardTitle className="text-lg">{facility.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-sm">{facility.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        {/* Housing */}
        <div className="mb-16">
          <Card className="border-0 shadow-md bg-slate-50">
            <CardContent className="p-8">
              <div className="max-w-3xl mx-auto text-center">
                <h2 className="text-2xl font-bold mb-4">{t('housing.title')}</h2>
                <p className="text-muted-foreground mb-6">{t('housing.description')}</p>
                <div className="flex flex-wrap justify-center gap-4">
                  {t('housing.features').split(', ').map((feature, index) => (
                    <span
                      key={index}
                      className="px-4 py-2 bg-white rounded-full text-sm text-muted-foreground shadow-sm"
                    >
                      {feature}
                    </span>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Student Organizations */}
        <div>
          <h2 className="text-2xl font-bold text-center mb-4">{t('organizations.title')}</h2>
          <p className="text-muted-foreground text-center max-w-2xl mx-auto mb-8">
            {t('organizations.description')}
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 max-w-3xl mx-auto">
            {organizations.map((org) => (
              <Card key={org.key} className="border-0 shadow-sm text-center">
                <CardContent className="p-6">
                  <div className="w-12 h-12 rounded-full bg-slate-100 mx-auto mb-3" />
                  <p className="font-medium text-sm">{org.label}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
