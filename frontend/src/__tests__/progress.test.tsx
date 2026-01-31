import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Progress } from '@/components/ui/progress'

describe('Progress', () => {
  it('renders with default value of 0', () => {
    render(<Progress />)
    const progressBar = screen.getByRole('progressbar')
    expect(progressBar).toBeInTheDocument()
  })

  it('displays correct progress percentage', () => {
    render(<Progress value={50} />)
    const indicator = document.querySelector('[data-state]')
    expect(indicator).toHaveStyle({ transform: 'translateX(-50%)' })
  })

  it('handles 0% progress', () => {
    render(<Progress value={0} />)
    const indicator = document.querySelector('[data-state]')
    expect(indicator).toHaveStyle({ transform: 'translateX(-100%)' })
  })

  it('handles 100% progress', () => {
    render(<Progress value={100} />)
    const indicator = document.querySelector('[data-state]')
    expect(indicator).toHaveStyle({ transform: 'translateX(-0%)' })
  })

  it('clamps values above 100', () => {
    render(<Progress value={150} />)
    const progressBar = screen.getByRole('progressbar')
    expect(progressBar).toBeInTheDocument()
  })

  it('clamps negative values to 0', () => {
    render(<Progress value={-10} />)
    const progressBar = screen.getByRole('progressbar')
    expect(progressBar).toBeInTheDocument()
  })

  it('applies custom className', () => {
    render(<Progress className="custom-class" value={50} />)
    const progressBar = screen.getByRole('progressbar')
    expect(progressBar).toHaveClass('custom-class')
  })
})
