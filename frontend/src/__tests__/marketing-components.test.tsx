import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'

vi.mock('next-intl', () => ({
  useTranslations: (namespace: string) => {
    const translations: Record<string, Record<string, string>> = {
      'Marketing.nav': {
        home: 'Home',
        about: 'About',
        programs: 'Programs',
        admissions: 'Admissions',
        login: 'Student Login',
        admin: 'Admin Portal',
      },
      'Marketing.footer': {
        contact: 'Contact Us',
        address: '123 Education Lane, Knowledge City',
        email: 'contact@minervia.edu',
        phone: '+1 (555) 123-4567',
        copyright: '(c) {year} Minervia Institute. All rights reserved.',
      },
      Common: {
        siteName: 'Minervia Institute',
        footer: 'Minervia Institute Educational Platform',
      },
    }
    return (key: string, params?: Record<string, unknown>) => {
      const value = translations[namespace]?.[key] || key
      if (params) {
        return Object.entries(params).reduce(
          (acc, [k, v]) => acc.replace(`{${k}}`, String(v)),
          value
        )
      }
      return value
    }
  },
}))

vi.mock('next/navigation', () => ({
  useParams: () => ({ locale: 'en' }),
  usePathname: () => '/en',
}))

vi.mock('next/link', () => ({
  default: ({ children, href }: { children: React.ReactNode; href: string }) => (
    <a href={href}>{children}</a>
  ),
}))

import { MarketingNavbar } from '@/components/marketing/navbar'
import { MarketingFooter } from '@/components/marketing/footer'
import { MarketingLayout } from '@/components/marketing/layout'

describe('MarketingNavbar', () => {
  it('renders site name', () => {
    render(<MarketingNavbar />)
    expect(screen.getByText('Minervia Institute')).toBeInTheDocument()
  })

  it('renders navigation links', () => {
    render(<MarketingNavbar />)
    expect(screen.getByText('Home')).toBeInTheDocument()
    expect(screen.getByText('About')).toBeInTheDocument()
    expect(screen.getByText('Programs')).toBeInTheDocument()
    expect(screen.getByText('Admissions')).toBeInTheDocument()
  })

  it('renders login links', () => {
    render(<MarketingNavbar />)
    expect(screen.getByText('Student Login')).toBeInTheDocument()
    expect(screen.getByText('Admin Portal')).toBeInTheDocument()
  })

  it('has correct href for home link', () => {
    render(<MarketingNavbar />)
    const homeLink = screen.getByText('Home').closest('a')
    expect(homeLink).toHaveAttribute('href', '/en')
  })

  it('has correct href for about link', () => {
    render(<MarketingNavbar />)
    const aboutLink = screen.getByText('About').closest('a')
    expect(aboutLink).toHaveAttribute('href', '/en/about')
  })

  it('has correct href for programs link', () => {
    render(<MarketingNavbar />)
    const programsLink = screen.getByText('Programs').closest('a')
    expect(programsLink).toHaveAttribute('href', '/en/programs')
  })

  it('has correct href for admissions link', () => {
    render(<MarketingNavbar />)
    const admissionsLink = screen.getByText('Admissions').closest('a')
    expect(admissionsLink).toHaveAttribute('href', '/en/admissions')
  })

  it('has correct href for student login', () => {
    render(<MarketingNavbar />)
    const loginLink = screen.getByText('Student Login').closest('a')
    expect(loginLink).toHaveAttribute('href', '/en/portal/login')
  })

  it('has correct href for admin portal', () => {
    render(<MarketingNavbar />)
    const adminLink = screen.getByText('Admin Portal').closest('a')
    expect(adminLink).toHaveAttribute('href', '/en/admin/login')
  })
})

describe('MarketingFooter', () => {
  it('renders site name', () => {
    render(<MarketingFooter />)
    expect(screen.getByText('Minervia Institute')).toBeInTheDocument()
  })

  it('renders contact section', () => {
    render(<MarketingFooter />)
    expect(screen.getByText('Contact Us')).toBeInTheDocument()
  })

  it('renders address', () => {
    render(<MarketingFooter />)
    expect(screen.getByText('123 Education Lane, Knowledge City')).toBeInTheDocument()
  })

  it('renders email', () => {
    render(<MarketingFooter />)
    expect(screen.getByText('contact@minervia.edu')).toBeInTheDocument()
  })

  it('renders phone', () => {
    render(<MarketingFooter />)
    expect(screen.getByText('+1 (555) 123-4567')).toBeInTheDocument()
  })

  it('renders copyright with current year', () => {
    render(<MarketingFooter />)
    const currentYear = new Date().getFullYear()
    expect(screen.getByText(new RegExp(`${currentYear}`))).toBeInTheDocument()
  })
})

describe('MarketingLayout', () => {
  it('renders children', () => {
    render(
      <MarketingLayout>
        <div data-testid="test-content">Test Content</div>
      </MarketingLayout>
    )
    expect(screen.getByTestId('test-content')).toBeInTheDocument()
  })

  it('renders navbar', () => {
    render(
      <MarketingLayout>
        <div>Content</div>
      </MarketingLayout>
    )
    expect(screen.getAllByText('Minervia Institute').length).toBeGreaterThan(0)
    expect(screen.getByText('Home')).toBeInTheDocument()
  })

  it('renders footer', () => {
    render(
      <MarketingLayout>
        <div>Content</div>
      </MarketingLayout>
    )
    expect(screen.getByText('Contact Us')).toBeInTheDocument()
  })

  it('has correct structure with flex layout', () => {
    const { container } = render(
      <MarketingLayout>
        <div>Content</div>
      </MarketingLayout>
    )
    const wrapper = container.firstChild as HTMLElement
    expect(wrapper).toHaveClass('flex', 'min-h-screen', 'flex-col')
  })
})
