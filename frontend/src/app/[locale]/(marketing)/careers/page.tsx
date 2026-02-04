import { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Careers - Minervia Institute',
  description: 'Join our team. Explore academic and administrative positions at Minervia Institute.',
};

const openPositions = [
  {
    id: 'prof-cs-001',
    title: 'Associate Professor of Computer Science',
    department: 'Faculty of Computer Science and Mathematics',
    type: 'Academic',
    contract: 'Full-time, Permanent',
    deadline: 'March 15, 2026',
    description: 'We seek an experienced researcher in AI/ML to lead our machine learning research group.',
  },
  {
    id: 'prof-econ-002',
    title: 'Assistant Professor of Economics',
    department: 'Faculty of Business and Economics',
    type: 'Academic',
    contract: 'Full-time, Tenure-track',
    deadline: 'April 1, 2026',
    description: 'Teaching and research position in microeconomics and behavioral economics.',
  },
  {
    id: 'lect-eng-003',
    title: 'Senior Lecturer in English',
    department: 'Language Center',
    type: 'Academic',
    contract: 'Full-time, Fixed-term (3 years)',
    deadline: 'February 28, 2026',
    description: 'Academic English instruction for undergraduate and graduate students.',
  },
  {
    id: 'admin-it-004',
    title: 'IT Systems Administrator',
    department: 'IT Services',
    type: 'Administrative',
    contract: 'Full-time, Permanent',
    deadline: 'March 1, 2026',
    description: 'Manage campus network infrastructure and student information systems.',
  },
  {
    id: 'admin-lib-005',
    title: 'Digital Resources Librarian',
    department: 'University Library',
    type: 'Administrative',
    contract: 'Full-time, Permanent',
    deadline: 'March 10, 2026',
    description: 'Manage digital collections and support research data management.',
  },
];

const applicationProcess = [
  {
    step: 1,
    title: 'Submit Application',
    description: 'Complete the online application form with your CV, cover letter, and supporting documents.',
  },
  {
    step: 2,
    title: 'Initial Review',
    description: 'Our HR team reviews applications and shortlists candidates within 2-3 weeks.',
  },
  {
    step: 3,
    title: 'Interview',
    description: 'Shortlisted candidates are invited for interviews (online or on-campus).',
  },
  {
    step: 4,
    title: 'Presentation',
    description: 'Academic candidates present a research talk or teaching demonstration.',
  },
  {
    step: 5,
    title: 'Decision',
    description: 'Final decisions are communicated within 4 weeks of the interview.',
  },
];

const benefits = [
  {
    title: 'Competitive Compensation',
    description: 'Salary packages aligned with European academic standards, plus performance bonuses.',
  },
  {
    title: 'Research Funding',
    description: 'Access to internal grants and support for external funding applications.',
  },
  {
    title: 'Professional Development',
    description: 'Conference attendance, sabbatical opportunities, and continuous learning programs.',
  },
  {
    title: 'Work-Life Balance',
    description: 'Flexible working arrangements and generous vacation allowances.',
  },
  {
    title: 'Health Benefits',
    description: 'Comprehensive health insurance for employees and their families.',
  },
  {
    title: 'International Environment',
    description: 'Collaborate with colleagues and students from over 40 countries.',
  },
];

export default function CareersPage({ params: { locale } }: Props) {
  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Hero Section */}
        <div className="text-center mb-16">
          <h1 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
            Join Our Team
          </h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
            Build your academic career at Minervia Institute. We are looking for passionate
            educators and researchers to join our growing community.
          </p>
        </div>

        {/* Open Positions */}
        <section className="mb-20">
          <h2 className="text-2xl font-bold mb-8">Open Positions</h2>
          <div className="grid gap-6">
            {openPositions.map((position) => (
              <Card key={position.id} className="border shadow-sm hover:shadow-md transition-shadow">
                <CardHeader>
                  <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
                    <div>
                      <CardTitle className="text-xl mb-2">{position.title}</CardTitle>
                      <p className="text-muted-foreground">{position.department}</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Badge variant={position.type === 'Academic' ? 'default' : 'secondary'}>
                        {position.type}
                      </Badge>
                      <Badge variant="outline">{position.contract}</Badge>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <p className="text-muted-foreground mb-4">{position.description}</p>
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                    <p className="text-sm text-muted-foreground">
                      Application deadline: <span className="font-medium text-foreground">{position.deadline}</span>
                    </p>
                    <Button asChild>
                      <Link href={`/${locale}/careers/${position.id}`}>View Details</Link>
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        {/* Application Process */}
        <section className="mb-20">
          <h2 className="text-2xl font-bold mb-8">Application Process</h2>
          <div className="grid md:grid-cols-5 gap-4">
            {applicationProcess.map((item) => (
              <Card key={item.step} className="border-0 shadow-md text-center">
                <CardContent className="pt-6">
                  <div className="w-10 h-10 rounded-full bg-primary text-primary-foreground flex items-center justify-center mx-auto mb-4 font-bold">
                    {item.step}
                  </div>
                  <h3 className="font-semibold mb-2">{item.title}</h3>
                  <p className="text-sm text-muted-foreground">{item.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        {/* Benefits */}
        <section className="mb-20">
          <h2 className="text-2xl font-bold mb-8">Why Work With Us</h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {benefits.map((benefit) => (
              <Card key={benefit.title} className="border-0 shadow-md">
                <CardContent className="pt-6">
                  <h3 className="font-semibold mb-2">{benefit.title}</h3>
                  <p className="text-sm text-muted-foreground">{benefit.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        {/* Contact Section */}
        <section>
          <Card className="border-0 shadow-lg bg-slate-50">
            <CardContent className="py-12 text-center">
              <h2 className="text-2xl font-bold mb-4">Have Questions?</h2>
              <p className="text-muted-foreground mb-6 max-w-2xl mx-auto">
                Our Human Resources team is happy to answer any questions about working at Minervia Institute.
              </p>
              <div className="space-y-2 text-sm text-muted-foreground mb-6">
                <p>Email: <a href="mailto:careers@minervia.edu.pl" className="text-primary hover:underline">careers@minervia.edu.pl</a></p>
                <p>Phone: +48 12 345 67 10</p>
              </div>
              <Button variant="outline" asChild>
                <Link href={`/${locale}/contact`}>Contact Us</Link>
              </Button>
            </CardContent>
          </Card>
        </section>
      </div>
    </div>
  );
}
