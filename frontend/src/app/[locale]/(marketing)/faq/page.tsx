import { Metadata } from 'next';
import { getTranslations } from 'next-intl/server';
import { Card, CardContent } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  const t = await getTranslations({ locale, namespace: 'Marketing.faq' });
  return {
    title: t('title'),
    description: t('description'),
  };
}

export default async function FAQPage({ params: { locale } }: Props) {
  const t = await getTranslations({ locale, namespace: 'Marketing.faq' });

  const faqs = [
    { id: '1', question: t('questions.1.question'), answer: t('questions.1.answer') },
    { id: '2', question: t('questions.2.question'), answer: t('questions.2.answer') },
    { id: '3', question: t('questions.3.question'), answer: t('questions.3.answer') },
    { id: '4', question: t('questions.4.question'), answer: t('questions.4.answer') },
    { id: '5', question: t('questions.5.question'), answer: t('questions.5.answer') },
    { id: '6', question: t('questions.6.question'), answer: t('questions.6.answer') },
    { id: '7', question: t('questions.7.question'), answer: t('questions.7.answer') },
    { id: '8', question: t('questions.8.question'), answer: t('questions.8.answer') },
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

        {/* FAQ List */}
        <div className="max-w-3xl mx-auto space-y-4">
          {faqs.map((faq) => (
            <Card key={faq.id} className="border-0 shadow-sm">
              <CardContent className="p-6">
                <h3 className="font-semibold text-lg mb-2">{faq.question}</h3>
                <p className="text-muted-foreground">{faq.answer}</p>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Contact Section */}
        <div className="mt-16">
          <Card className="border-0 shadow-md bg-slate-50 max-w-2xl mx-auto">
            <CardContent className="p-8 text-center">
              <h2 className="text-xl font-bold mb-2">{t('contact.title')}</h2>
              <p className="text-muted-foreground mb-4">{t('contact.description')}</p>
              <div className="space-y-2">
                <p className="text-sm">
                  <span className="font-medium">Email:</span>{' '}
                  <a href={`mailto:${t('contact.email')}`} className="text-slate-900 hover:underline">
                    {t('contact.email')}
                  </a>
                </p>
                <p className="text-sm">
                  <span className="font-medium">Phone:</span>{' '}
                  <a href={`tel:${t('contact.phone')}`} className="text-slate-900 hover:underline">
                    {t('contact.phone')}
                  </a>
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
