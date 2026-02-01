import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.home' });
  return {
    title: t('title'),
    description: t('description'),
    openGraph: {
      title: t('title'),
      description: t('description'),
    },
  };
}

export default async function HomePage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.home' });

  const features = [
    { key: '1', title: t('features.1.title'), description: t('features.1.description') },
    { key: '2', title: t('features.2.title'), description: t('features.2.description') },
    { key: '3', title: t('features.3.title'), description: t('features.3.description') },
    { key: '4', title: t('features.4.title'), description: t('features.4.description') },
  ];

  const stats = [
    { label: t('stats.students'), value: t('stats.studentsCount') },
    { label: t('stats.faculty'), value: t('stats.facultyCount') },
    { label: t('stats.programs'), value: t('stats.programsCount') },
    { label: t('stats.countries'), value: t('stats.countriesCount') },
  ];

  return (
    <div className="flex flex-col">
      {/* Hero Section */}
      <section className="relative py-20 md:py-32 bg-gradient-to-b from-slate-900 to-slate-800 text-white">
        <div className="absolute inset-0 bg-[url('/grid.svg')] bg-center [mask-image:linear-gradient(180deg,white,rgba(255,255,255,0))]" />
        <div className="container relative text-center">
          <h1 className="text-4xl md:text-6xl font-bold tracking-tight mb-6">
            {t('heroTitle')}
          </h1>
          <p className="text-xl text-slate-300 max-w-3xl mx-auto mb-8">
            {t('heroDescription')}
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Button asChild size="lg" className="bg-white text-slate-900 hover:bg-slate-100">
              <Link href={`/${locale}/apply`}>{t('cta')}</Link>
            </Button>
            <Button asChild size="lg" variant="outline" className="border-white text-white hover:bg-white/10">
              <Link href={`/${locale}/about`}>{t('learnMore')}</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-12 bg-slate-50 border-y">
        <div className="container">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            {stats.map((stat, index) => (
              <div key={index} className="text-center">
                <div className="text-3xl md:text-4xl font-bold text-slate-900">{stat.value}</div>
                <div className="text-sm text-muted-foreground mt-1">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 md:py-24">
        <div className="container">
          <h2 className="text-3xl font-bold text-center mb-4">
            {t('features.title')}
          </h2>
          <p className="text-muted-foreground text-center max-w-2xl mx-auto mb-12">
            {t('description')}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature) => (
              <Card key={feature.key} className="border-0 shadow-md">
                <CardHeader>
                  <div className="w-12 h-12 rounded-lg bg-slate-100 flex items-center justify-center mb-4">
                    <span className="text-2xl font-bold text-slate-600">{feature.key}</span>
                  </div>
                  <CardTitle className="text-lg">{feature.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground text-sm">{feature.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Accreditation Section */}
      <section className="py-16 bg-slate-50">
        <div className="container">
          <div className="max-w-3xl mx-auto text-center">
            <h2 className="text-2xl font-bold mb-4">{t('accreditation.title')}</h2>
            <p className="text-muted-foreground mb-8">{t('accreditation.description')}</p>
            <div className="flex flex-wrap justify-center gap-8">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <div className="w-8 h-8 rounded bg-slate-200" />
                <span>{t('accreditation.pka')}</span>
              </div>
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <div className="w-8 h-8 rounded bg-slate-200" />
                <span>{t('accreditation.ministry')}</span>
              </div>
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <div className="w-8 h-8 rounded bg-slate-200" />
                <span>{t('accreditation.erasmus')}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 md:py-24 bg-slate-900 text-white">
        <div className="container text-center">
          <h2 className="text-3xl font-bold mb-4">Ready to Begin Your Journey?</h2>
          <p className="text-slate-300 max-w-2xl mx-auto mb-8">
            Join thousands of students from around the world who have chosen Minervia Institute for their academic journey.
          </p>
          <Button asChild size="lg" className="bg-white text-slate-900 hover:bg-slate-100">
            <Link href={`/${locale}/apply`}>{t('cta')}</Link>
          </Button>
        </div>
      </section>
    </div>
  );
}
