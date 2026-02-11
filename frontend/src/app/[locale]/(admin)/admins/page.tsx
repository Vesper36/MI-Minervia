'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { apiClient } from '@/lib/api-client';
import { Plus, UserCog, Ban, CheckCircle } from 'lucide-react';

interface Admin {
  id: number;
  username: string;
  email: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'AUDITOR';
  isActive: boolean;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export default function AdminsPage() {
  const [admins, setAdmins] = useState<Admin[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({
    username: '',
    email: '',
    password: '',
    role: 'ADMIN' as Admin['role'],
  });

  const fetchAdmins = async () => {
    setIsLoading(true);
    const response = await apiClient.get<PageResponse<Admin>>(
      `/api/super-admin/admins?page=${page}&size=20`
    );

    if (response.success && response.data) {
      setAdmins(response.data.content);
      setTotalPages(response.data.totalPages);
    }
    setIsLoading(false);
  };

  useEffect(() => {
    fetchAdmins();
  }, [page]);

  const handleCreate = async () => {
    const response = await apiClient.post('/api/super-admin/admins', createForm);
    if (response.success) {
      setShowCreate(false);
      setCreateForm({ username: '', email: '', password: '', role: 'ADMIN' });
      fetchAdmins();
    }
  };

  const handleUpdateRole = async (id: number, role: Admin['role']) => {
    await apiClient.put(`/api/super-admin/admins/${id}/role`, { role });
    fetchAdmins();
  };

  const handleDeactivate = async (id: number) => {
    if (!confirm('Are you sure you want to deactivate this admin?')) return;
    await apiClient.post(`/api/super-admin/admins/${id}/deactivate`);
    fetchAdmins();
  };

  const handleActivate = async (id: number) => {
    await apiClient.post(`/api/super-admin/admins/${id}/activate`);
    fetchAdmins();
  };

  const getRoleBadge = (role: Admin['role']) => {
    switch (role) {
      case 'SUPER_ADMIN':
        return <Badge variant="destructive">Super Admin</Badge>;
      case 'ADMIN':
        return <Badge variant="default">Admin</Badge>;
      case 'AUDITOR':
        return <Badge variant="secondary">Auditor</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold">Admin Management</h1>
          <p className="text-muted-foreground">Manage administrator accounts</p>
        </div>
        <Button onClick={() => setShowCreate(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Create Admin
        </Button>
      </div>

      {showCreate && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Admin</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  value={createForm.username}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, username: e.target.value }))
                  }
                  placeholder="admin123"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={createForm.email}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, email: e.target.value }))
                  }
                  placeholder="admin@example.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input
                  id="password"
                  type="password"
                  value={createForm.password}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, password: e.target.value }))
                  }
                  placeholder="••••••••"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="role">Role</Label>
                <Select
                  value={createForm.role}
                  onValueChange={(value) =>
                    setCreateForm((prev) => ({ ...prev, role: value as Admin['role'] }))
                  }
                >
                  <SelectTrigger id="role">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ADMIN">Admin</SelectItem>
                    <SelectItem value="AUDITOR">Auditor</SelectItem>
                    <SelectItem value="SUPER_ADMIN">Super Admin</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="flex gap-2 mt-4">
              <Button onClick={handleCreate}>Create</Button>
              <Button variant="outline" onClick={() => setShowCreate(false)}>
                Cancel
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardContent className="pt-6">
          {isLoading ? (
            <div className="text-center py-8">Loading...</div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Username</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {admins.map((admin) => (
                    <TableRow key={admin.id}>
                      <TableCell className="font-medium">{admin.username}</TableCell>
                      <TableCell>{admin.email}</TableCell>
                      <TableCell>
                        <Select
                          value={admin.role}
                          onValueChange={(value) =>
                            handleUpdateRole(admin.id, value as Admin['role'])
                          }
                        >
                          <SelectTrigger className="w-[140px]">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="ADMIN">Admin</SelectItem>
                            <SelectItem value="AUDITOR">Auditor</SelectItem>
                            <SelectItem value="SUPER_ADMIN">Super Admin</SelectItem>
                          </SelectContent>
                        </Select>
                      </TableCell>
                      <TableCell>
                        <Badge variant={admin.isActive ? 'success' : 'secondary'}>
                          {admin.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {admin.isActive ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeactivate(admin.id)}
                          >
                            <Ban className="h-4 w-4 mr-1" />
                            Deactivate
                          </Button>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleActivate(admin.id)}
                          >
                            <CheckCircle className="h-4 w-4 mr-1" />
                            Activate
                          </Button>
                        )}
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
  );
}
