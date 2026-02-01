import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.news' });
  return {
    title: t('title'),
    description: t('description'),
  };
}

export default async function NewsPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.news' });

  const newsItems = [
    {
      id: '1',
      title: t('items.1.title'),
      date: t('items.1.date'),
      summary: t('items.1.summary'),
      category: 'academic'
    },
    {
      id: '2',
      title: t('items.2.title'),
      date: t('items.2.date'),
      summary: t('items.2.summary'),
      category: 'research'
    },
    {
      id: '3',
      title: t('items.3.title'),
      date: t('items.3.date'),
      summary: t('items.3.summary'),
      category: 'campus'
    },
    {
      id: '4',
      title: t('items.4.title'),
      date: t('items.4.date'),
      summary: t('items.4.summary'),
      category: 'academic'
    },
  ];

  const getCategoryLabel = (category: string) => {
    const key = `categories.${category}` as const;
    return t(key);
  };

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

        {/* News Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-5xl mx-auto">
          {newsItems.map((item) => (
            <Card key={item.id} className="border-0 shadow-md overflow-hidden">
              <div className="bg-slate-200 h-48" />
              <CardHeader>
                <div className="flex items-center gap-2 mb-2">
                  <Badge variant="secondary">{getCategoryLabel(item.category)}</Badge>
                  <span className="text-sm text-muted-foreground">{item.date}</span>
                </div>
                <CardTitle className="text-lg leading-tight">{item.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-sm">{item.summary}</p>
                <button className="text-sm font-medium text-slate-900 hover:underline mt-4">
                  {t('readMore')} &rarr;
                </button>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
