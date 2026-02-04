'use client';

import Link from 'next/link';
import { useTranslations } from 'next-intl';
import { useParams } from 'next/navigation';

export function MarketingFooter() {
  const t = useTranslations('Marketing.footer');
  const tCommon = useTranslations('Common');
  const params = useParams();
  const locale = params.locale as string;
  const currentYear = new Date().getFullYear();

  const quickLinks = [
    { href: `/${locale}/about`, label: 'About' },
    { href: `/${locale}/programs`, label: 'Programs' },
    { href: `/${locale}/admissions`, label: 'Admissions' },
    { href: `/${locale}/campus`, label: 'Campus' },
  ];

  const resourceLinks = [
    { href: `/${locale}/news`, label: 'News' },
    { href: `/${locale}/calendar`, label: 'Calendar' },
    { href: `/${locale}/faq`, label: 'FAQ' },
    { href: `/${locale}/student-portal`, label: 'Student Portal' },
  ];

  return (
    <footer className="border-t bg-slate-900 text-white">
      <div className="container py-12">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {/* Brand */}
          <div>
            <h3 className="text-lg font-semibold mb-4">{tCommon('siteName')}</h3>
            <p className="text-sm text-slate-400 mb-4">
              {t('address')}
            </p>
            <div className="space-y-1 text-sm text-slate-400">
              <p>
                <a href={`mailto:${t('email')}`} className="hover:text-white">
                  {t('email')}
                </a>
              </p>
              <p>
                <a href={`tel:${t('phone')}`} className="hover:text-white">
                  {t('phone')}
                </a>
              </p>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="text-sm font-semibold mb-4 uppercase tracking-wider">
              {t('quickLinks')}
            </h3>
            <ul className="space-y-2">
              {quickLinks.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-slate-400 hover:text-white transition-colors"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Resources */}
          <div>
            <h3 className="text-sm font-semibold mb-4 uppercase tracking-wider">
              {t('resources')}
            </h3>
            <ul className="space-y-2">
              {resourceLinks.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-slate-400 hover:text-white transition-colors"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Legal */}
          <div>
            <h3 className="text-sm font-semibold mb-4 uppercase tracking-wider">
              {t('legal')}
            </h3>
            <ul className="space-y-2">
              <li>
                <Link href={`/${locale}/privacy`} className="text-sm text-slate-400 hover:text-white transition-colors">
                  {t('privacy')}
                </Link>
              </li>
              <li>
                <Link href={`/${locale}/terms`} className="text-sm text-slate-400 hover:text-white transition-colors">
                  {t('terms')}
                </Link>
              </li>
              <li>
                <Link href="#" className="text-sm text-slate-400 hover:text-white transition-colors">
                  {t('accessibility')}
                </Link>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="mt-12 pt-8 border-t border-slate-800">
          <div className="flex flex-col md:flex-row justify-between items-center gap-4">
            <p className="text-sm text-slate-400">
              {t('copyright', { year: currentYear })}
            </p>
            <div className="flex items-center gap-4">
              <span className="text-xs text-slate-500">
                Accredited by PKA | Erasmus+ Partner
              </span>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
