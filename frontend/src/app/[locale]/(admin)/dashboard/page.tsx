'use client';

import { useEffect, useState } from 'react';
import { AdminShell } from '@/components/admin/shell';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { apiClient } from '@/lib/api-client';
import { Users, FileCheck, Mail, KeyRound } from 'lucide-react';

interface Statistics {
  students: {
    total: number;
    active: number;
    suspended: number;
  };
  registrations: {
    pending: number;
    approved: number;
    rejected: number;
  };
  emails: {
    sentToday: number;
    sentThisMonth: number;
  };
  codes: {
    unused: number;
    used: number;
    expired: number;
  };
}

export default function DashboardPage() {
  const [stats, setStats] = useState<Statistics | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function fetchStats() {
      const [studentRes, registrationRes, emailRes] = await Promise.all([
        apiClient.get<Statistics['students']>('/api/admin/statistics/students'),
        apiClient.get<Statistics['registrations']>('/api/admin/statistics/registrations'),
        apiClient.get<Statistics['emails']>('/api/admin/statistics/emails'),
      ]);

      setStats({
        students: studentRes.data || { total: 0, active: 0, suspended: 0 },
        registrations: registrationRes.data || { pending: 0, approved: 0, rejected: 0 },
        emails: emailRes.data || { sentToday: 0, sentThisMonth: 0 },
        codes: { unused: 0, used: 0, expired: 0 },
      });
      setIsLoading(false);
    }

    fetchStats();
  }, []);

  return (
    <AdminShell>
      <div className="space-y-8">
        <div>
          <h1 className="text-3xl font-bold">Dashboard</h1>
          <p className="text-muted-foreground">Overview of your platform</p>
        </div>

        {isLoading ? (
          <div>Loading statistics...</div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Students</CardTitle>
                <Users className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats?.students.total || 0}</div>
                <p className="text-xs text-muted-foreground">
                  {stats?.students.active || 0} active, {stats?.students.suspended || 0} suspended
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Pending Applications</CardTitle>
                <FileCheck className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats?.registrations.pending || 0}</div>
                <p className="text-xs text-muted-foreground">
                  {stats?.registrations.approved || 0} approved this month
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Emails Sent Today</CardTitle>
                <Mail className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats?.emails.sentToday || 0}</div>
                <p className="text-xs text-muted-foreground">
                  {stats?.emails.sentThisMonth || 0} this month
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Unused Codes</CardTitle>
                <KeyRound className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats?.codes.unused || 0}</div>
                <p className="text-xs text-muted-foreground">
                  {stats?.codes.used || 0} used, {stats?.codes.expired || 0} expired
                </p>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </AdminShell>
  );
}
