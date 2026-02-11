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
import { apiClient } from '@/lib/api-client';
import { Plus, Copy, XCircle } from 'lucide-react';

interface RegistrationCode {
  id: number;
  code: string;
  status: 'UNUSED' | 'USED' | 'EXPIRED' | 'REVOKED';
  expiresAt: string;
  createdAt: string;
  usedAt: string | null;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export default function CodesPage() {
  const [codes, setCodes] = useState<RegistrationCode[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [showGenerate, setShowGenerate] = useState(false);
  const [generateCount, setGenerateCount] = useState(1);
  const [expiryDays, setExpiryDays] = useState(30);

  const fetchCodes = async () => {
    setIsLoading(true);
    const response = await apiClient.get<PageResponse<RegistrationCode>>(
      `/api/admin/registration-codes?page=${page}&size=20`
    );

    if (response.success && response.data) {
      setCodes(response.data.content);
      setTotalPages(response.data.totalPages);
    }
    setIsLoading(false);
  };

  useEffect(() => {
    fetchCodes();
  }, [page]);

  const handleGenerate = async () => {
    if (generateCount === 1) {
      await apiClient.post('/api/admin/registration-codes', { expiryDays });
    } else {
      await apiClient.post('/api/admin/registration-codes/batch', {
        count: generateCount,
        expiryDays,
      });
    }
    setShowGenerate(false);
    fetchCodes();
  };

  const handleRevoke = async (id: number) => {
    if (!confirm('Are you sure you want to revoke this code?')) return;
    await apiClient.post(`/api/admin/registration-codes/${id}/revoke`);
    fetchCodes();
  };

  const copyToClipboard = (code: string) => {
    navigator.clipboard.writeText(code);
  };

  const getStatusBadge = (status: RegistrationCode['status']) => {
    switch (status) {
      case 'UNUSED':
        return <Badge variant="success">Unused</Badge>;
      case 'USED':
        return <Badge variant="secondary">Used</Badge>;
      case 'EXPIRED':
        return <Badge variant="warning">Expired</Badge>;
      case 'REVOKED':
        return <Badge variant="destructive">Revoked</Badge>;
      default:
        return <Badge>{status}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold">Registration Codes</h1>
            <p className="text-muted-foreground">Generate and manage registration codes</p>
          </div>
          <Button onClick={() => setShowGenerate(true)}>
            <Plus className="h-4 w-4 mr-2" />
            Generate Codes
          </Button>
        </div>

        {showGenerate && (
          <Card>
            <CardHeader>
              <CardTitle>Generate Registration Codes</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 md:grid-cols-3">
                <div className="space-y-2">
                  <Label htmlFor="count">Number of Codes</Label>
                  <Input
                    id="count"
                    type="number"
                    min={1}
                    max={100}
                    value={generateCount}
                    onChange={(e) => setGenerateCount(parseInt(e.target.value) || 1)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="expiry">Expiry (days)</Label>
                  <Input
                    id="expiry"
                    type="number"
                    min={1}
                    max={365}
                    value={expiryDays}
                    onChange={(e) => setExpiryDays(parseInt(e.target.value) || 30)}
                  />
                </div>
                <div className="flex items-end gap-2">
                  <Button onClick={handleGenerate}>Generate</Button>
                  <Button variant="outline" onClick={() => setShowGenerate(false)}>
                    Cancel
                  </Button>
                </div>
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
                      <TableHead>Code</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Expires</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead>Used</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {codes.map((code) => (
                      <TableRow key={code.id}>
                        <TableCell className="font-mono">{code.code}</TableCell>
                        <TableCell>{getStatusBadge(code.status)}</TableCell>
                        <TableCell>
                          {new Date(code.expiresAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          {new Date(code.createdAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          {code.usedAt
                            ? new Date(code.usedAt).toLocaleDateString()
                            : '-'}
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => copyToClipboard(code.code)}
                            >
                              <Copy className="h-4 w-4" />
                            </Button>
                            {code.status === 'UNUSED' && (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleRevoke(code.id)}
                              >
                                <XCircle className="h-4 w-4" />
                              </Button>
                            )}
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
    </div>
  );
}
