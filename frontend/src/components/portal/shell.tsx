'use client';

import { useEffect } from 'react';
import { useRouter, useParams, usePathname } from 'next/navigation';
import { useStudentAuth } from '@/lib/student-auth-context';
import { StudentSidebar } from '@/components/portal/sidebar';

export function StudentShell({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useStudentAuth();
  const router = useRouter();
  const params = useParams();
  const pathname = usePathname();
  const locale = params.locale as string;

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      const returnUrl = encodeURIComponent(pathname);
      router.push(`/${locale}/portal/login?returnUrl=${returnUrl}`);
    }
  }, [isAuthenticated, isLoading, router, locale, pathname]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="flex min-h-screen">
      <StudentSidebar />
      <main className="flex-1 bg-slate-50">
        <div className="p-8">{children}</div>
      </main>
    </div>
  );
}
