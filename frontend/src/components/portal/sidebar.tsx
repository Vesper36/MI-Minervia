'use client';

import Link from 'next/link';
import { usePathname, useParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { cn } from '@/lib/utils';
import { useStudentAuth } from '@/lib/student-auth-context';
import { Button } from '@/components/ui/button';
import {
  LayoutDashboard,
  User,
  BookOpen,
  LogOut,
} from 'lucide-react';

export function StudentSidebar() {
  const pathname = usePathname();
  const params = useParams();
  const locale = params.locale as string;
  const t = useTranslations('Portal.sidebar');
  const { student, logout } = useStudentAuth();

  const navItems = [
    { href: `/${locale}/portal/dashboard`, label: t('dashboard'), icon: LayoutDashboard },
    { href: `/${locale}/portal/profile`, label: t('profile'), icon: User },
    { href: `/${locale}/portal/courses`, label: t('courses'), icon: BookOpen },
  ];

  return (
    <aside className="w-64 bg-slate-900 text-white min-h-screen flex flex-col">
      <div className="p-4 border-b border-slate-800">
        <h1 className="text-xl font-bold">{t('title')}</h1>
        {student && (
          <p className="text-sm text-slate-400 mt-1">
            {student.firstName} {student.lastName}
          </p>
        )}
      </div>
      <nav className="flex-1 p-4">
        <ul className="space-y-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={cn(
                    'flex items-center gap-3 px-3 py-2 rounded-md transition-colors',
                    isActive
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                  )}
                >
                  <Icon className="h-5 w-5" />
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
      <div className="p-4 border-t border-slate-800">
        <Button
          variant="ghost"
          className="w-full justify-start text-slate-400 hover:text-white hover:bg-slate-800"
          onClick={logout}
        >
          <LogOut className="h-5 w-5 mr-3" />
          {t('logout')}
        </Button>
      </div>
    </aside>
  );
}
