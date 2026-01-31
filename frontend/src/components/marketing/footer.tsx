'use client';

import { useTranslations } from 'next-intl';

export function MarketingFooter() {
  const t = useTranslations('Marketing.footer');
  const tCommon = useTranslations('Common');
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t bg-background">
      <div className="container py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <h3 className="text-lg font-semibold mb-4">{tCommon('siteName')}</h3>
            <p className="text-sm text-muted-foreground">
              {t('address')}
            </p>
          </div>
          <div>
            <h3 className="text-lg font-semibold mb-4">{t('contact')}</h3>
            <div className="space-y-2 text-sm text-muted-foreground">
              <p>{t('email')}</p>
              <p>{t('phone')}</p>
            </div>
          </div>
          <div className="md:text-right">
            <p className="text-sm text-muted-foreground">
              {t('copyright', { year: currentYear })}
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}
