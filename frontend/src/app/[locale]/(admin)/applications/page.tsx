'use client';

import { useEffect, useState } from 'react';
import { AdminShell } from '@/components/admin/shell';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { apiClient } from '@/lib/api-client';
import { Check, X, Eye } from 'lucide-react';

interface Application {
  id: number;
  externalEmail: string;
  identityType: 'LOCAL' | 'INTERNATIONAL';
  countryCode: string | null;
  status: string;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export default function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  const fetchApplications = async () => {
    setIsLoading(true);
    const response = await apiClient.get<PageResponse<Application>>(
      `/api/admin/registration-applications?status=PENDING_APPROVAL&page=${page}&size=20`
    );

    if (response.success && response.data) {
      setApplications(response.data.content);
      setTotalPages(response.data.totalPages);
    }
    setIsLoading(false);
  };

  useEffect(() => {
    fetchApplications();
  }, [page]);

  const handleApprove = async (id: number) => {
    await apiClient.post(`/api/admin/registration-applications/${id}/approve`);
    fetchApplications();
  };

  const handleReject = async (id: number) => {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;

    await apiClient.post(`/api/admin/registration-applications/${id}/reject`, {
      reason,
    });
    fetchApplications();
  };

  const handleBatchApprove = async () => {
    if (selectedIds.length === 0) return;

    await apiClient.post('/api/admin/registration-applications/batch-approve', {
      applicationIds: selectedIds,
    });
    setSelectedIds([]);
    fetchApplications();
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
    );
  };

  const toggleSelectAll = () => {
    if (selectedIds.length === applications.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(applications.map((a) => a.id));
    }
  };

  return (
    <AdminShell>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold">Registration Applications</h1>
            <p className="text-muted-foreground">Review and approve applications</p>
          </div>
          {selectedIds.length > 0 && (
            <Button onClick={handleBatchApprove}>
              Approve Selected ({selectedIds.length})
            </Button>
          )}
        </div>

        <Card>
          <CardContent className="pt-6">
            {isLoading ? (
              <div className="text-center py-8">Loading...</div>
            ) : applications.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                No pending applications
              </div>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-12">
                        <input
                          type="checkbox"
                          checked={selectedIds.length === applications.length}
                          onChange={toggleSelectAll}
                          className="rounded"
                        />
                      </TableHead>
                      <TableHead>Email</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Country</TableHead>
                      <TableHead>Submitted</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {applications.map((app) => (
                      <TableRow key={app.id}>
                        <TableCell>
                          <input
                            type="checkbox"
                            checked={selectedIds.includes(app.id)}
                            onChange={() => toggleSelect(app.id)}
                            className="rounded"
                          />
                        </TableCell>
                        <TableCell>{app.externalEmail}</TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              app.identityType === 'INTERNATIONAL'
                                ? 'default'
                                : 'secondary'
                            }
                          >
                            {app.identityType}
                          </Badge>
                        </TableCell>
                        <TableCell>{app.countryCode || '-'}</TableCell>
                        <TableCell>
                          {new Date(app.createdAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleApprove(app.id)}
                            >
                              <Check className="h-4 w-4 mr-1" />
                              Approve
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleReject(app.id)}
                            >
                              <X className="h-4 w-4 mr-1" />
                              Reject
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>

                <div className="flex justify-between items-center mt-4">
                  <div className="text-sm text-muted-foreground">
                    Page {page + 1} of {totalPages || 1}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={page === 0}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={page >= totalPages - 1}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </AdminShell>
  );
}
