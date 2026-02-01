import { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Admissions - Minervia Institute',
  description: 'Admission requirements, deadlines, and application process for Minervia Institute.',
};

const admissionRounds = [
  {
    id: 'early',
    name: 'Early Admission',
    deadline: 'November 30, 2025',
    notification: 'December 20, 2025',
    status: 'closed',
    description: 'For highly qualified candidates seeking early decision.',
  },
  {
    id: 'regular-1',
    name: 'Regular Round 1',
    deadline: 'January 15, 2026',
    notification: 'February 28, 2026',
    status: 'open',
    description: 'Main admission round with full program availability.',
  },
  {
    id: 'regular-2',
    name: 'Regular Round 2',
    deadline: 'March 31, 2026',
    notification: 'April 30, 2026',
    status: 'upcoming',
    description: 'Second round for remaining program seats.',
  },
  {
    id: 'late',
    name: 'Late Admission',
    deadline: 'June 15, 2026',
    notification: 'June 30, 2026',
    status: 'upcoming',
    description: 'Final round for available positions only.',
  },
];

const requirements = {
  undergraduate: [
    { item: 'Secondary School Certificate', description: 'Matura or equivalent with minimum 60% average' },
    { item: 'Language Proficiency', description: 'IELTS 6.0 / TOEFL 80 / B2 Certificate for English programs' },
    { item: 'Motivation Letter', description: '500-800 words explaining your academic interests' },
    { item: 'CV/Resume', description: 'Academic and extracurricular achievements' },
    { item: 'Passport Photo', description: 'Recent color photograph (3.5x4.5cm)' },
    { item: 'ID Document', description: 'Valid passport or national ID copy' },
  ],
  graduate: [
    { item: "Bachelor's Degree", description: 'From accredited institution with minimum GPA 3.0/4.0' },
    { item: 'Language Proficiency', description: 'IELTS 6.5 / TOEFL 90 / C1 Certificate for English programs' },
    { item: 'Academic Transcripts', description: 'Official transcripts from all previous institutions' },
    { item: 'Letters of Recommendation', description: '2 academic or professional references' },
    { item: 'Research Proposal', description: 'For research-based programs (2000-3000 words)' },
    { item: 'Professional Experience', description: 'CV with relevant work experience (if applicable)' },
  ],
};

const tuitionFees = [
  { program: 'Undergraduate (EU/EEA)', annual: '4,500 - 6,000', currency: 'EUR' },
  { program: 'Undergraduate (Non-EU)', annual: '8,000 - 12,000', currency: 'EUR' },
  { program: 'Graduate (EU/EEA)', annual: '5,500 - 8,000', currency: 'EUR' },
  { program: 'Graduate (Non-EU)', annual: '10,000 - 15,000', currency: 'EUR' },
  { program: 'Doctoral Programs', annual: '3,000 - 5,000', currency: 'EUR' },
];

const programQuotas = [
  { faculty: 'Engineering & Technology', undergraduate: 180, graduate: 90, total: 270 },
  { faculty: 'Business & Economics', undergraduate: 200, graduate: 120, total: 320 },
  { faculty: 'Arts & Humanities', undergraduate: 150, graduate: 60, total: 210 },
  { faculty: 'Natural Sciences', undergraduate: 120, graduate: 80, total: 200 },
  { faculty: 'Law & Social Sciences', undergraduate: 160, graduate: 70, total: 230 },
  { faculty: 'Medicine & Health', undergraduate: 100, graduate: 50, total: 150 },
];

export default function AdmissionsPage({ params: { locale } }: Props) {
  const totalQuota = programQuotas.reduce((sum, p) => sum + p.total, 0);

  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Header */}
        <div className="text-center mb-16">
          <h1 className="text-4xl font-bold tracking-tight mb-4">Admissions 2025/2026</h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
            Join our diverse academic community. Applications are now open for the 2025/2026 academic year.
          </p>
        </div>

        {/* Admission Rounds */}
        <div className="mb-16">
          <h2 className="text-2xl font-bold mb-6">Application Deadlines</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {admissionRounds.map((round) => (
              <Card key={round.id} className="border-0 shadow-md">
                <CardHeader className="pb-2">
                  <div className="flex items-center justify-between mb-2">
                    <Badge
                      variant={round.status === 'open' ? 'default' : 'secondary'}
                      className={round.status === 'open' ? 'bg-green-600' : ''}
                    >
                      {round.status === 'open' ? 'Open' : round.status === 'closed' ? 'Closed' : 'Upcoming'}
                    </Badge>
                  </div>
                  <CardTitle className="text-lg">{round.name}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground mb-3">{round.description}</p>
                  <div className="space-y-1 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Deadline:</span>
                      <span className="font-medium">{round.deadline}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Decision:</span>
                      <span className="font-medium">{round.notification}</span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        {/* Enrollment Quotas */}
        <div className="mb-16">
          <h2 className="text-2xl font-bold mb-6">Enrollment Plan 2025/2026</h2>
          <Card className="border-0 shadow-md overflow-hidden">
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="text-left p-4 font-semibold">Faculty</th>
                      <th className="text-center p-4 font-semibold">Undergraduate</th>
                      <th className="text-center p-4 font-semibold">Graduate</th>
                      <th className="text-center p-4 font-semibold">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {programQuotas.map((quota, index) => (
                      <tr key={index} className="border-t">
                        <td className="p-4">{quota.faculty}</td>
                        <td className="p-4 text-center">{quota.undergraduate}</td>
                        <td className="p-4 text-center">{quota.graduate}</td>
                        <td className="p-4 text-center font-medium">{quota.total}</td>
                      </tr>
                    ))}
                    <tr className="border-t bg-slate-50 font-bold">
                      <td className="p-4">Total Enrollment</td>
                      <td className="p-4 text-center">{programQuotas.reduce((s, p) => s + p.undergraduate, 0)}</td>
                      <td className="p-4 text-center">{programQuotas.reduce((s, p) => s + p.graduate, 0)}</td>
                      <td className="p-4 text-center">{totalQuota}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
          <p className="text-sm text-muted-foreground mt-4">
            * Quotas are subject to change based on program capacity and applicant qualifications.
            International students: 30% of total enrollment reserved.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-8">
            {/* Undergraduate Requirements */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle>Undergraduate Admission Requirements</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {requirements.undergraduate.map((req, index) => (
                    <div key={index} className="flex gap-3">
                      <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                        <span className="text-xs font-medium">{index + 1}</span>
                      </div>
                      <div>
                        <div className="font-medium">{req.item}</div>
                        <div className="text-sm text-muted-foreground">{req.description}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Graduate Requirements */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle>Graduate Admission Requirements</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {requirements.graduate.map((req, index) => (
                    <div key={index} className="flex gap-3">
                      <div className="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                        <span className="text-xs font-medium">{index + 1}</span>
                      </div>
                      <div>
                        <div className="font-medium">{req.item}</div>
                        <div className="text-sm text-muted-foreground">{req.description}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Application Process */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle>Application Process</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex gap-4">
                    <div className="w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center flex-shrink-0">
                      1
                    </div>
                    <div>
                      <div className="font-medium">Create Account</div>
                      <div className="text-sm text-muted-foreground">
                        Register on our application portal with your email address.
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-4">
                    <div className="w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center flex-shrink-0">
                      2
                    </div>
                    <div>
                      <div className="font-medium">Complete Application Form</div>
                      <div className="text-sm text-muted-foreground">
                        Fill in personal details, educational background, and program preferences.
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-4">
                    <div className="w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center flex-shrink-0">
                      3
                    </div>
                    <div>
                      <div className="font-medium">Upload Documents</div>
                      <div className="text-sm text-muted-foreground">
                        Submit all required documents in PDF format (max 5MB each).
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-4">
                    <div className="w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center flex-shrink-0">
                      4
                    </div>
                    <div>
                      <div className="font-medium">Pay Application Fee</div>
                      <div className="text-sm text-muted-foreground">
                        EUR 50 (non-refundable) via bank transfer or online payment.
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-4">
                    <div className="w-8 h-8 rounded-full bg-slate-900 text-white flex items-center justify-center flex-shrink-0">
                      5
                    </div>
                    <div>
                      <div className="font-medium">Await Decision</div>
                      <div className="text-sm text-muted-foreground">
                        Receive admission decision via email within the notification period.
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Tuition Fees */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">Tuition Fees (Annual)</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {tuitionFees.map((fee, index) => (
                    <div key={index} className="flex justify-between text-sm">
                      <span className="text-muted-foreground">{fee.program}</span>
                      <span className="font-medium">{fee.currency} {fee.annual}</span>
                    </div>
                  ))}
                </div>
                <p className="text-xs text-muted-foreground mt-4">
                  * Fees vary by program. Scholarships available for qualified candidates.
                </p>
              </CardContent>
            </Card>

            {/* Contact */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">Admissions Office</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div>
                  <div className="font-medium">Email</div>
                  <a href="mailto:admissions@minervia.edu.pl" className="text-slate-900 hover:underline">
                    admissions@minervia.edu.pl
                  </a>
                </div>
                <div>
                  <div className="font-medium">Phone</div>
                  <a href="tel:+48123456700" className="text-slate-900 hover:underline">
                    +48 12 345 67 00
                  </a>
                </div>
                <div>
                  <div className="font-medium">Office Hours</div>
                  <div className="text-muted-foreground">Mon-Fri: 9:00 - 16:00 CET</div>
                </div>
                <div>
                  <div className="font-medium">Location</div>
                  <div className="text-muted-foreground">Main Building, Room 102</div>
                </div>
              </CardContent>
            </Card>

            {/* Apply CTA */}
            <Card className="border-0 shadow-md bg-slate-900 text-white">
              <CardContent className="p-6 text-center">
                <h3 className="font-bold mb-2">Ready to Apply?</h3>
                <p className="text-sm text-slate-300 mb-4">
                  Start your application today and take the first step towards your future.
                </p>
                <Button asChild className="w-full bg-white text-slate-900 hover:bg-slate-100">
                  <Link href={`/${locale}/apply`}>Start Application</Link>
                </Button>
              </CardContent>
            </Card>

            {/* Important Dates */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">Important Dates</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Semester Start</span>
                  <span className="font-medium">Oct 1, 2026</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Orientation Week</span>
                  <span className="font-medium">Sep 23-27, 2026</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Registration</span>
                  <span className="font-medium">Sep 15-20, 2026</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Visa Deadline</span>
                  <span className="font-medium">Jul 15, 2026</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
