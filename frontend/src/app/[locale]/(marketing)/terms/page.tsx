import { Metadata } from 'next';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Terms of Service - Minervia Institute',
  description: 'Terms of service and conditions for using Minervia Institute services.',
};

export default function TermsPage({ params: { locale } }: Props) {
  return (
    <div className="py-16 md:py-24">
      <div className="container max-w-4xl">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold tracking-tight mb-4">Terms of Service</h1>
          <p className="text-muted-foreground">Last updated: January 31, 2026</p>
        </div>

        <div className="space-y-8">
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>1. Acceptance of Terms</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>
                By accessing and using the Minervia Institute website and services, you accept and agree to be bound
                by these Terms of Service. If you do not agree to these terms, please do not use our services.
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>2. Application Process</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <h3 className="text-lg font-semibold mt-4 mb-2">2.1 Application Submission</h3>
              <p>
                All applications must be submitted through our official online portal. You must provide accurate,
                complete, and truthful information. False or misleading information may result in application
                rejection or enrollment cancellation.
              </p>

              <h3 className="text-lg font-semibold mt-4 mb-2">2.2 Application Fee</h3>
              <p>
                A non-refundable application fee of EUR 50 is required for all applications. Payment instructions
                will be sent to your registered email address after application submission.
              </p>

              <h3 className="text-lg font-semibold mt-4 mb-2">2.3 Video Interview</h3>
              <p>
                All applicants are required to complete an online video interview as part of the admission process.
                This ensures the authenticity of applications and allows us to assess your communication skills and
                motivation. Interview scheduling details will be provided after document verification.
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>3. Admission Decisions</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>
                Admission decisions are made by the Admissions Committee and are final. We reserve the right to
                accept or reject any application at our sole discretion. Admission offers are conditional upon:
              </p>
              <ul className="list-disc pl-6 space-y-1">
                <li>Verification of all submitted documents</li>
                <li>Successful completion of video interview</li>
                <li>Payment of tuition deposit</li>
                <li>Meeting visa requirements (for international students)</li>
              </ul>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>4. Student Email and System Access</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <h3 className="text-lg font-semibold mt-4 mb-2">4.1 EDU Email Issuance</h3>
              <p>
                To prevent misuse and ensure authenticity, institutional email addresses (@minervia.edu.pl) are
                issued only after successful in-person enrollment at our campus. Online admission and video
                interview completion do not automatically grant EDU email access.
              </p>

              <h3 className="text-lg font-semibold mt-4 mb-2">4.2 Limited System Access</h3>
              <p>
                After passing the video interview but before physical enrollment, you may access the student
                portal using your application email. Limited features include:
              </p>
              <ul className="list-disc pl-6 space-y-1">
                <li>View course schedules and academic calendar</li>
                <li>Access pre-enrollment information</li>
                <li>Submit required documentation</li>
              </ul>
              <p className="mt-2">
                Full student privileges (library access, course registration, grade viewing) are activated
                only after physical enrollment verification.
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>5. Intellectual Property</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>
                All content on this website, including text, graphics, logos, and software, is the property of
                Minervia Institute and protected by copyright laws. Unauthorized use is prohibited.
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>6. Limitation of Liability</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>
                Minervia Institute is not liable for any indirect, incidental, or consequential damages arising
                from the use of our website or services. We make no warranties about the accuracy or completeness
                of information provided.
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>7. Changes to Terms</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>
                We reserve the right to modify these terms at any time. Changes will be posted on this page with
                an updated revision date. Continued use of our services constitutes acceptance of modified terms.
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>8. Contact Information</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>For questions about these Terms of Service:</p>
              <p className="mt-2">
                <strong>Minervia Institute</strong><br />
                Email: legal@minervia.edu.pl<br />
                Phone: +48 12 345 67 00<br />
                Address: ul. Akademicka 1, 31-120 Kraków, Poland
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
