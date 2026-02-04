import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

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

  const studentServices = [
    { key: 'international', title: t('services.international.title'), description: t('services.international.description') },
    { key: 'career', title: t('services.career.title'), description: t('services.career.description') },
    { key: 'counseling', title: t('services.counseling.title'), description: t('services.counseling.description') },
    { key: 'health', title: t('services.health.title'), description: t('services.health.description') },
  ];

  const events = [
    { key: 'orientation', title: t('events.orientation.title'), date: t('events.orientation.date'), description: t('events.orientation.description') },
    { key: 'international', title: t('events.international.title'), date: t('events.international.date'), description: t('events.international.description') },
    { key: 'career', title: t('events.career.title'), date: t('events.career.date'), description: t('events.career.description') },
    { key: 'graduation', title: t('events.graduation.title'), date: t('events.graduation.date'), description: t('events.graduation.description') },
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
        <div className="mb-16">
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

        {/* Student Services */}
        <div className="mb-16">
          <h2 className="text-2xl font-bold text-center mb-8">{t('services.title')}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {studentServices.map((service) => (
              <Card key={service.key} className="border-0 shadow-md">
                <CardHeader>
                  <div className="w-12 h-12 rounded-lg bg-primary/10 mb-4" />
                  <CardTitle className="text-lg">{service.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-sm">{service.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        {/* Annual Events */}
        <div className="mb-16">
          <h2 className="text-2xl font-bold text-center mb-8">{t('events.title')}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {events.map((event) => (
              <Card key={event.key} className="border shadow-sm">
                <CardContent className="p-6">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-semibold">{event.title}</h3>
                    <span className="text-sm text-muted-foreground">{event.date}</span>
                  </div>
                  <p className="text-sm text-muted-foreground">{event.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        {/* Safety & Security */}
        <div className="mb-16">
          <Card className="border-0 shadow-md bg-slate-50">
            <CardContent className="p-8">
              <div className="max-w-3xl mx-auto text-center">
                <h2 className="text-2xl font-bold mb-4">{t('safety.title')}</h2>
                <p className="text-muted-foreground mb-6">{t('safety.description')}</p>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="p-4 bg-white rounded-lg shadow-sm">
                    <p className="font-medium text-sm">{t('safety.security')}</p>
                  </div>
                  <div className="p-4 bg-white rounded-lg shadow-sm">
                    <p className="font-medium text-sm">{t('safety.cctv')}</p>
                  </div>
                  <div className="p-4 bg-white rounded-lg shadow-sm">
                    <p className="font-medium text-sm">{t('safety.emergency')}</p>
                  </div>
                  <div className="p-4 bg-white rounded-lg shadow-sm">
                    <p className="font-medium text-sm">{t('safety.access')}</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Virtual Tour CTA */}
        <div>
          <Card className="border-0 shadow-lg bg-primary text-primary-foreground">
            <CardContent className="py-12 text-center">
              <h2 className="text-2xl font-bold mb-4">{t('tour.title')}</h2>
              <p className="mb-6 opacity-90 max-w-2xl mx-auto">{t('tour.description')}</p>
              <Button variant="secondary" size="lg" asChild>
                <Link href={`/${locale}/contact`}>{t('tour.cta')}</Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
