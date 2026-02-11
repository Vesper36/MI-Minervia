'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import {
  LayoutDashboard,
  Users,
  FileCheck,
  KeyRound,
  ScrollText,
  Settings,
  LogOut,
  BarChart3,
  UserCog,
} from 'lucide-react';

type NavItem = {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  roles: string[];
};

const navItems: NavItem[] = [
  { href: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, roles: ['AUDITOR', 'ADMIN', 'SUPER_ADMIN'] },
  { href: '/students', label: 'Students', icon: Users, roles: ['AUDITOR', 'ADMIN', 'SUPER_ADMIN'] },
  { href: '/applications', label: 'Applications', icon: FileCheck, roles: ['AUDITOR', 'ADMIN', 'SUPER_ADMIN'] },
  { href: '/codes', label: 'Registration Codes', icon: KeyRound, roles: ['ADMIN', 'SUPER_ADMIN'] },
  { href: '/audit', label: 'Audit Logs', icon: ScrollText, roles: ['AUDITOR', 'ADMIN', 'SUPER_ADMIN'] },
  { href: '/statistics', label: 'Statistics', icon: BarChart3, roles: ['AUDITOR', 'ADMIN', 'SUPER_ADMIN'] },
  { href: '/admins', label: 'Admin Management', icon: UserCog, roles: ['SUPER_ADMIN'] },
  { href: '/settings', label: 'Settings', icon: Settings, roles: ['SUPER_ADMIN'] },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const { admin, logout } = useAuth();

  const visibleItems = navItems.filter((item) =>
    admin?.role && item.roles.includes(admin.role)
  );

  return (
    <aside className="w-64 bg-gray-900 text-white min-h-screen flex flex-col">
      <div className="p-4 border-b border-gray-800">
        <h1 className="text-xl font-bold">Minervia Admin</h1>
        {admin && (
          <p className="text-sm text-gray-400 mt-1">
            {admin.username} ({admin.role})
          </p>
        )}
      </div>
      <nav className="flex-1 p-4">
        <ul className="space-y-2">
          {visibleItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={cn(
                    'flex items-center gap-3 px-3 py-2 rounded-md transition-colors',
                    isActive
                      ? 'bg-gray-800 text-white'
                      : 'text-gray-400 hover:bg-gray-800 hover:text-white'
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
      <div className="p-4 border-t border-gray-800">
        <Button
          variant="ghost"
          className="w-full justify-start text-gray-400 hover:text-white hover:bg-gray-800"
          onClick={logout}
        >
          <LogOut className="h-5 w-5 mr-3" />
          Sign Out
        </Button>
      </div>
    </aside>
  );
}
