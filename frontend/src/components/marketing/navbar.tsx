'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { useParams } from 'next/navigation';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Menu, X } from 'lucide-react';

export function MarketingNavbar() {
  const [isOpen, setIsOpen] = useState(false);
  const t = useTranslations('Marketing.nav');
  const tCommon = useTranslations('Common');
  const params = useParams();
  const locale = params.locale as string;

  const navItems = [
    { href: `/${locale}`, label: t('home') },
    { href: `/${locale}/about`, label: t('about') },
    { href: `/${locale}/faculties`, label: t('faculties') },
    { href: `/${locale}/admissions`, label: t('admissions') },
    { href: `/${locale}/schedule`, label: t('schedule') },
    { href: `/${locale}/campus`, label: t('campus') },
    { href: `/${locale}/news`, label: t('news') },
  ];

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center justify-between">
        <Link href={`/${locale}`} className="flex items-center space-x-2">
          <span className="text-xl font-bold">{tCommon('siteName')}</span>
        </Link>

        {/* Desktop Navigation */}
        <nav className="hidden lg:flex items-center space-x-6">
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

        {/* Desktop Auth Links */}
        <div className="hidden lg:flex items-center space-x-4">
          <Link
            href={`/${locale}/student-portal`}
            className="text-sm font-medium text-muted-foreground hover:text-primary"
          >
            {t('login')}
          </Link>
          <Button asChild size="sm">
            <Link href={`/${locale}/apply`}>Apply</Link>
          </Button>
        </div>

        {/* Mobile Menu Button */}
        <button
          className="lg:hidden p-2"
          onClick={() => setIsOpen(!isOpen)}
          aria-label="Toggle menu"
        >
          {isOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </div>

      {/* Mobile Navigation */}
      {isOpen && (
        <div className="lg:hidden border-t bg-background">
          <nav className="container py-4 space-y-2">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="block py-2 text-sm font-medium text-muted-foreground hover:text-primary"
                onClick={() => setIsOpen(false)}
              >
                {item.label}
              </Link>
            ))}
            <div className="pt-4 border-t space-y-2">
              <Link
                href={`/${locale}/student-portal`}
                className="block py-2 text-sm font-medium text-muted-foreground hover:text-primary"
                onClick={() => setIsOpen(false)}
              >
                {t('login')}
              </Link>
              <Link
                href={`/${locale}/staff-portal`}
                className="block py-2 text-sm font-medium text-muted-foreground hover:text-primary"
                onClick={() => setIsOpen(false)}
              >
                Staff Portal
              </Link>
            </div>
          </nav>
        </div>
      )}
    </header>
  );
}
