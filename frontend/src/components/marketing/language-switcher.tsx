'use client';

import { useParams, usePathname, useRouter } from 'next/navigation';
import { locales, type Locale } from '@/i18n';

const localeLabels: Record<Locale, string> = {
  en: 'EN',
  pl: 'PL',
  'zh-CN': '中文',
};

export function LanguageSwitcher() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useParams();
  const currentLocale = params.locale as string;

  const switchLocale = (newLocale: string) => {
    if (newLocale === currentLocale) return;
    const segments = pathname.split('/');
    segments[1] = newLocale;
    router.push(segments.join('/'));
  };

  return (
    <div className="flex items-center gap-1">
      {locales.map((locale) => (
        <button
          key={locale}
          onClick={() => switchLocale(locale)}
          className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
            currentLocale === locale
              ? 'bg-slate-900 text-white'
              : 'text-muted-foreground hover:text-primary hover:bg-slate-100'
          }`}
        >
          {localeLabels[locale]}
        </button>
      ))}
    </div>
  );
}
