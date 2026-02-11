import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'

const mockApiClient = {
  post: vi.fn(),
}

vi.mock('@/lib/api-client', () => ({
  apiClient: mockApiClient,
}))

vi.mock('lucide-react', () => ({
  Upload: () => <span data-testid="icon-upload" />,
  File: () => <span data-testid="icon-file" />,
  X: () => <span data-testid="icon-x" />,
  CheckCircle: () => <span data-testid="icon-check" />,
  AlertCircle: () => <span data-testid="icon-alert" />,
}))

import { DocumentUpload } from '@/components/portal/document-upload'

describe('DocumentUpload', () => {
  const mockOnUploadComplete = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
    global.fetch = vi.fn()
  })

  it('renders upload area in idle state', () => {
    render(<DocumentUpload onUploadComplete={mockOnUploadComplete} />)

    expect(screen.getByText('Upload Document')).toBeInTheDocument()
    expect(screen.getByText('Drag and drop or click to select')).toBeInTheDocument()
    expect(screen.getByText('PDF, JPG, PNG, DOC (max 10MB)')).toBeInTheDocument()
    expect(screen.getByTestId('icon-upload')).toBeInTheDocument()
  })

  it('renders select file button', () => {
    render(<DocumentUpload onUploadComplete={mockOnUploadComplete} />)

    const button = screen.getByText('Select File')
    expect(button).toBeInTheDocument()
  })

  it('has hidden file input', () => {
    const { container } = render(<DocumentUpload onUploadComplete={mockOnUploadComplete} />)

    const fileInput = container.querySelector('input[type="file"]')
    expect(fileInput).toBeInTheDocument()
    expect(fileInput).toHaveClass('hidden')
  })

  it('shows error when file size exceeds limit', async () => {
    render(<DocumentUpload onUploadComplete={mockOnUploadComplete} />)

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    const largeFile = new File(['x'.repeat(11 * 1024 * 1024)], 'large.pdf', {
      type: 'application/pdf',
    })

    Object.defineProperty(fileInput, 'files', {
      value: [largeFile],
      writable: false,
    })

    fireEvent.change(fileInput)

    await waitFor(() => {
      expect(screen.getByText('File size exceeds 10MB limit')).toBeInTheDocument()
      expect(screen.getByTestId('icon-alert')).toBeInTheDocument()
    })
  })

  it('shows error when file type is not allowed', async () => {
    render(<DocumentUpload onUploadComplete={mockOnUploadComplete} />)

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    const invalidFile = new File(['content'], 'file.exe', {
      type: 'application/x-msdownload',
    })

    Object.defineProperty(fileInput, 'files', {
      value: [invalidFile],
      writable: false,
    })

    fireEvent.change(fileInput)

    await waitFor(() => {
      expect(
        screen.getByText('File type not allowed. Please upload PDF, JPG, PNG, or DOC files')
      ).toBeInTheDocument()
    })
  })
})
