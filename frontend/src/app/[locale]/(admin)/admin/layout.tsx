import { AdminShell } from '@/components/admin/shell';

export default function AdminPagesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <AdminShell>{children}</AdminShell>;
}
