'use client';

import { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import { StudentShell } from '@/components/portal';
import { DocumentUpload } from '@/components/portal/document-upload';
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
import { Download, Trash2 } from 'lucide-react';

interface Document {
  id: number;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  status: 'PENDING_UPLOAD' | 'ACTIVE' | 'DELETED';
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export default function StudentDocumentsPage() {
  const t = useTranslations('Portal.documents');
  const [documents, setDocuments] = useState<Document[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  const fetchDocuments = async () => {
    setIsLoading(true);
    const response = await apiClient.get<PageResponse<Document>>(
      `/api/student/documents?page=${page}&size=20`
    );

    if (response.success && response.data) {
      setDocuments(response.data.content);
      setTotalPages(response.data.totalPages);
    }
    setIsLoading(false);
  };

  useEffect(() => {
    fetchDocuments();
  }, [page]);

  const handleDownload = async (id: number) => {
    const response = await apiClient.get<{ downloadUrl: string }>(
      `/api/student/documents/${id}/download-url`
    );
    if (response.success && response.data) {
      window.open(response.data.downloadUrl, '_blank');
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this document?')) return;
    await apiClient.delete(`/api/student/documents/${id}`);
    fetchDocuments();
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <StudentShell>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('title')}</h1>
          <p className="text-muted-foreground">{t('subtitle')}</p>
        </div>

        <DocumentUpload onUploadComplete={fetchDocuments} />

        <Card>
          <CardHeader>
            <CardTitle>{t('myDocuments')}</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="text-center py-8">Loading...</div>
            ) : documents.length === 0 ? (
              <p className="text-muted-foreground text-center py-8">
                {t('noDocuments')}
              </p>
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>File Name</TableHead>
                      <TableHead>Size</TableHead>
                      <TableHead>Uploaded</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {documents.map((doc) => (
                      <TableRow key={doc.id}>
                        <TableCell className="font-medium">
                          {doc.originalFileName}
                        </TableCell>
                        <TableCell>{formatFileSize(doc.sizeBytes)}</TableCell>
                        <TableCell>
                          {new Date(doc.createdAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              doc.status === 'ACTIVE' ? 'success' : 'secondary'
                            }
                          >
                            {doc.status}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDownload(doc.id)}
                            >
                              <Download className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDelete(doc.id)}
                            >
                              <Trash2 className="h-4 w-4" />
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
    </StudentShell>
  );
}

