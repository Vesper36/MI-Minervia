'use client';

import { useEffect, useState } from 'react';
import { AdminShell } from '@/components/admin/shell';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
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
import { Search, Ban, CheckCircle } from 'lucide-react';

interface Student {
  id: number;
  studentNumber: string;
  eduEmail: string;
  firstName: string;
  lastName: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'GRADUATED' | 'EXPELLED';
  enrollmentYear: number;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export default function StudentsPage() {
  const [students, setStudents] = useState<Student[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  const fetchStudents = async () => {
    setIsLoading(true);
    const params = new URLSearchParams({
      page: page.toString(),
      size: '20',
    });
    if (search) {
      params.append('search', search);
    }

    const response = await apiClient.get<PageResponse<Student>>(
      `/api/admin/students?${params}`
    );

    if (response.success && response.data) {
      setStudents(response.data.content);
      setTotalPages(response.data.totalPages);
    }
    setIsLoading(false);
  };

  useEffect(() => {
    fetchStudents();
  }, [page]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchStudents();
  };

  const handleBan = async (studentId: number) => {
    const reason = prompt('Enter ban reason:');
    if (!reason) return;

    await apiClient.post(`/api/admin/students/${studentId}/ban`, { reason });
    fetchStudents();
  };

  const handleUnban = async (studentId: number) => {
    await apiClient.post(`/api/admin/students/${studentId}/unban`);
    fetchStudents();
  };

  const getStatusBadge = (status: Student['status']) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge variant="success">Active</Badge>;
      case 'SUSPENDED':
        return <Badge variant="destructive">Suspended</Badge>;
      case 'GRADUATED':
        return <Badge variant="secondary">Graduated</Badge>;
      case 'EXPELLED':
        return <Badge variant="destructive">Expelled</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  return (
    <AdminShell>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold">Students</h1>
            <p className="text-muted-foreground">Manage student accounts</p>
          </div>
        </div>

        <Card>
          <CardHeader>
            <form onSubmit={handleSearch} className="flex gap-4">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by name, email, or student number..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-10"
                />
              </div>
              <Button type="submit">Search</Button>
            </form>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="text-center py-8">Loading...</div>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Student Number</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead>Email</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Enrollment</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {students.map((student) => (
                      <TableRow key={student.id}>
                        <TableCell className="font-mono">
                          {student.studentNumber}
                        </TableCell>
                        <TableCell>
                          {student.firstName} {student.lastName}
                        </TableCell>
                        <TableCell>{student.eduEmail}</TableCell>
                        <TableCell>{getStatusBadge(student.status)}</TableCell>
                        <TableCell>{student.enrollmentYear}</TableCell>
                        <TableCell>
                          {student.status === 'ACTIVE' ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleBan(student.id)}
                            >
                              <Ban className="h-4 w-4 mr-1" />
                              Ban
                            </Button>
                          ) : student.status === 'SUSPENDED' ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleUnban(student.id)}
                            >
                              <CheckCircle className="h-4 w-4 mr-1" />
                              Unban
                            </Button>
                          ) : null}
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
