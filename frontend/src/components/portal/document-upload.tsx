'use client';

import { useState, useRef } from 'react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { apiClient } from '@/lib/api-client';
import { Upload, File, X, CheckCircle, AlertCircle } from 'lucide-react';

interface DocumentUploadProps {
  onUploadComplete?: () => void;
}

interface UploadState {
  status: 'idle' | 'uploading' | 'success' | 'error';
  progress: number;
  fileName: string | null;
  error: string | null;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/png',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
];

export function DocumentUpload({ onUploadComplete }: DocumentUploadProps) {
  const [uploadState, setUploadState] = useState<UploadState>({
    status: 'idle',
    progress: 0,
    fileName: null,
    error: null,
  });
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = (file: File): string | null => {
    if (file.size > MAX_FILE_SIZE) {
      return 'File size exceeds 10MB limit';
    }
    if (!ALLOWED_TYPES.includes(file.type)) {
      return 'File type not allowed. Please upload PDF, JPG, PNG, or DOC files';
    }
    return null;
  };

  const handleFileUpload = async (file: File) => {
    const validationError = validateFile(file);
    if (validationError) {
      setUploadState({
        status: 'error',
        progress: 0,
        fileName: file.name,
        error: validationError,
      });
      return;
    }

    setUploadState({
      status: 'uploading',
      progress: 0,
      fileName: file.name,
      error: null,
    });

    try {
      // Step 1: Initialize upload
      const initResponse = await apiClient.post<{
        documentId: number;
        uploadUrl: string;
        objectKey: string;
      }>('/api/student/documents', {
        fileName: file.name,
        contentType: file.type,
        sizeBytes: file.size,
      });

      if (!initResponse.success || !initResponse.data) {
        throw new Error('Failed to initialize upload');
      }

      const { documentId, uploadUrl } = initResponse.data;

      // Step 2: Upload to R2
      const uploadResponse = await fetch(uploadUrl, {
        method: 'PUT',
        body: file,
        headers: {
          'Content-Type': file.type,
        },
      });

      if (!uploadResponse.ok) {
        throw new Error('Failed to upload file');
      }

      setUploadState((prev) => ({ ...prev, progress: 50 }));

      // Step 3: Complete upload
      const completeResponse = await apiClient.post(
        `/api/student/documents/${documentId}/complete`,
        { success: true }
      );

      if (!completeResponse.success) {
        throw new Error('Failed to complete upload');
      }

      setUploadState({
        status: 'success',
        progress: 100,
        fileName: file.name,
        error: null,
      });

      if (onUploadComplete) {
        onUploadComplete();
      }

      // Reset after 2 seconds
      setTimeout(() => {
        setUploadState({
          status: 'idle',
          progress: 0,
          fileName: null,
          error: null,
        });
      }, 2000);
    } catch (error) {
      setUploadState({
        status: 'error',
        progress: 0,
        fileName: file.name,
        error: error instanceof Error ? error.message : 'Upload failed',
      });
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileUpload(file);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleFileUpload(file);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
  };

  const handleReset = () => {
    setUploadState({
      status: 'idle',
      progress: 0,
      fileName: null,
      error: null,
    });
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <Card>
      <CardContent className="pt-6">
        <div
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-gray-400 transition-colors"
        >
          <input
            ref={fileInputRef}
            type="file"
            onChange={handleFileSelect}
            accept={ALLOWED_TYPES.join(',')}
            className="hidden"
          />

          {uploadState.status === 'idle' && (
            <div className="space-y-4">
              <Upload className="h-12 w-12 mx-auto text-gray-400" />
              <div>
                <p className="text-lg font-medium">Upload Document</p>
                <p className="text-sm text-muted-foreground mt-1">
                  Drag and drop or click to select
                </p>
                <p className="text-xs text-muted-foreground mt-2">
                  PDF, JPG, PNG, DOC (max 10MB)
                </p>
              </div>
              <Button onClick={() => fileInputRef.current?.click()}>
                Select File
              </Button>
            </div>
          )}

          {uploadState.status === 'uploading' && (
            <div className="space-y-4">
              <File className="h-12 w-12 mx-auto text-blue-500" />
              <div>
                <p className="text-lg font-medium">Uploading...</p>
                <p className="text-sm text-muted-foreground mt-1">
                  {uploadState.fileName}
                </p>
              </div>
              <Progress value={uploadState.progress} className="w-full" />
            </div>
          )}

          {uploadState.status === 'success' && (
            <div className="space-y-4">
              <CheckCircle className="h-12 w-12 mx-auto text-green-500" />
              <div>
                <p className="text-lg font-medium text-green-600">Upload Successful</p>
                <p className="text-sm text-muted-foreground mt-1">
                  {uploadState.fileName}
                </p>
              </div>
            </div>
          )}

          {uploadState.status === 'error' && (
            <div className="space-y-4">
              <AlertCircle className="h-12 w-12 mx-auto text-red-500" />
              <div>
                <p className="text-lg font-medium text-red-600">Upload Failed</p>
                <p className="text-sm text-muted-foreground mt-1">
                  {uploadState.fileName}
                </p>
                <p className="text-sm text-red-500 mt-2">{uploadState.error}</p>
              </div>
              <Button onClick={handleReset} variant="outline">
                Try Again
              </Button>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
