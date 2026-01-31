'use client';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { useParams } from 'next/navigation';
import { cn } from '@/lib/utils';

export function MarketingNavbar() {
  const t = useTranslations('Marketing.nav');
  const tCommon = useTranslations('Common');
  const params = useParams();
  const locale = params.locale as string;

  const navItems = [
    { href: `/${locale}`, label: t('home') },
    { href: `/${locale}/about`, label: t('about') },
    { href: `/${locale}/programs`, label: t('programs') },
    { href: `/${locale}/admissions`, label: t('admissions') },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center justify-between">
        <Link href={`/${locale}`} className="flex items-center space-x-2">
          <span className="text-xl font-bold">{tCommon('siteName')}</span>
        </Link>
        <nav className="hidden md:flex items-center space-x-6">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                'text-sm font-medium transition-colors hover:text-primary',
                'text-muted-foreground'
              )}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="flex items-center space-x-4">
          <Link
            href={`/${locale}/portal/login`}
            className="text-sm font-medium text-muted-foreground hover:text-primary"
          >
            {t('login')}
          </Link>
          <Link
            href={`/${locale}/admin/login`}
            className="text-sm font-medium text-muted-foreground hover:text-primary"
          >
            {t('admin')}
          </Link>
        </div>
      </div>
    </header>
  );
}
