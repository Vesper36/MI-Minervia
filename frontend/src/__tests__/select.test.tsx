import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { Select, SelectTrigger, SelectContent, SelectItem, SelectValue } from '@/components/ui/select'

describe('Select', () => {
  it('renders trigger with placeholder', () => {
    render(
      <Select>
        <SelectTrigger>
          <SelectValue placeholder="Select option" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="a">Option A</SelectItem>
          <SelectItem value="b">Option B</SelectItem>
        </SelectContent>
      </Select>
    )

    expect(screen.getByText('Select option')).toBeInTheDocument()
  })

  it('opens dropdown on click', async () => {
    render(
      <Select>
        <SelectTrigger>
          <SelectValue placeholder="Select" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="a">Option A</SelectItem>
        </SelectContent>
      </Select>
    )

    fireEvent.click(screen.getByRole('combobox'))

    await waitFor(() => {
      expect(screen.getByText('Option A')).toBeVisible()
    })
  })

  it('selects item on click', async () => {
    const onValueChange = vi.fn()

    render(
      <Select onValueChange={onValueChange}>
        <SelectTrigger>
          <SelectValue placeholder="Select" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="a">Option A</SelectItem>
          <SelectItem value="b">Option B</SelectItem>
        </SelectContent>
      </Select>
    )

    fireEvent.click(screen.getByRole('combobox'))

    await waitFor(() => {
      fireEvent.click(screen.getByText('Option A'))
    })

    expect(onValueChange).toHaveBeenCalledWith('a')
  })

  it('displays selected value', () => {
    render(
      <Select value="b">
        <SelectTrigger>
          <SelectValue placeholder="Select" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="a">Option A</SelectItem>
          <SelectItem value="b">Option B</SelectItem>
        </SelectContent>
      </Select>
    )

    expect(screen.getByText('Option B')).toBeInTheDocument()
  })

  it('can be disabled', () => {
    render(
      <Select disabled>
        <SelectTrigger>
          <SelectValue placeholder="Select" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="a">Option A</SelectItem>
        </SelectContent>
      </Select>
    )

    const trigger = screen.getByRole('combobox')
    expect(trigger).toHaveAttribute('aria-disabled', 'true')
  })

  it('closes on outside click', async () => {
    render(
      <div>
        <Select>
          <SelectTrigger>
            <SelectValue placeholder="Select" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="a">Option A</SelectItem>
          </SelectContent>
        </Select>
        <button>Outside</button>
      </div>
    )

    fireEvent.click(screen.getByRole('combobox'))

    await waitFor(() => {
      expect(screen.getByText('Option A')).toBeVisible()
    })

    fireEvent.click(screen.getByText('Outside'))

    await waitFor(() => {
      expect(screen.queryByText('Option A')).not.toBeVisible()
    })
  })
})
