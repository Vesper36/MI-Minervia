import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Stepper } from '@/components/ui/stepper'

describe('Stepper', () => {
  const steps = [
    { id: 'step1', label: 'Step 1' },
    { id: 'step2', label: 'Step 2' },
    { id: 'step3', label: 'Step 3' },
  ]

  it('renders all steps', () => {
    render(<Stepper steps={steps} currentStep="step1" />)

    expect(screen.getByText('Step 1')).toBeInTheDocument()
    expect(screen.getByText('Step 2')).toBeInTheDocument()
    expect(screen.getByText('Step 3')).toBeInTheDocument()
  })

  it('highlights current step', () => {
    render(<Stepper steps={steps} currentStep="step2" />)

    const step2 = screen.getByText('Step 2')
    expect(step2.closest('div')).toHaveClass('text-primary')
  })

  it('marks completed steps', () => {
    render(<Stepper steps={steps} currentStep="step3" />)

    const step1Container = screen.getByText('Step 1').closest('div')
    expect(step1Container).toHaveClass('text-primary')
  })

  it('shows step numbers', () => {
    render(<Stepper steps={steps} currentStep="step1" />)

    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('applies custom className', () => {
    const { container } = render(
      <Stepper steps={steps} currentStep="step1" className="custom-stepper" />
    )

    expect(container.firstChild).toHaveClass('custom-stepper')
  })

  it('handles single step', () => {
    const singleStep = [{ id: 'only', label: 'Only Step' }]
    render(<Stepper steps={singleStep} currentStep="only" />)

    expect(screen.getByText('Only Step')).toBeInTheDocument()
  })

  it('handles empty steps array', () => {
    const { container } = render(<Stepper steps={[]} currentStep="" />)
    expect(container.firstChild).toBeInTheDocument()
  })
})
