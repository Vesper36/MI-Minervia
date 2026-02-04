import { getRequestConfig } from 'next-intl/server';

export const locales = ['en', 'pl', 'zh-CN'] as const;
export const defaultLocale = 'en' as const;

export type Locale = (typeof locales)[number];

export default getRequestConfig(async ({ requestLocale }) => {
  const locale = await requestLocale;

  return {
    locale,
    messages: (await import(`../messages/${locale}.json`)).default,
  };
});
