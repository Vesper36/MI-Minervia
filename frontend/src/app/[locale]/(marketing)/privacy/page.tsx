import { Metadata } from 'next';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Privacy Policy - Minervia Institute',
  description: 'Privacy policy and data protection information for Minervia Institute.',
};

export default function PrivacyPage({ params: { locale } }: Props) {
  return (
    <div className="py-16 md:py-24">
      <div className="container max-w-4xl">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold tracking-tight mb-4">Privacy Policy</h1>
          <p className="text-muted-foreground">Last updated: January 31, 2026</p>
        </div>

        <div className="space-y-8">
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>1. Introduction</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>
                Minervia Institute (&quot;we&quot;, &quot;our&quot;, or &quot;us&quot;) is committed to protecting your privacy and personal data.
                This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you
                visit our website or apply to our institution.
              </p>
              <p>
                This policy complies with the General Data Protection Regulation (GDPR) (EU) 2016/679 and the Polish
                Personal Data Protection Act.
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>2. Data Controller</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p><strong>Minervia Institute</strong></p>
              <p>ul. Akademicka 1<br />31-120 Kraków, Poland</p>
              <p>Email: privacy@minervia.edu.pl<br />Phone: +48 12 345 67 00</p>
              <p>
                Data Protection Officer: dpo@minervia.edu.pl
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>3. Information We Collect</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <h3 className="text-lg font-semibold mt-4 mb-2">3.1 Personal Information</h3>
              <p>When you apply to Minervia Institute, we collect:</p>
              <ul className="list-disc pl-6 space-y-1">
                <li>Full name, date of birth, nationality</li>
                <li>Contact information (email, phone, address)</li>
                <li>Passport or national ID number</li>
                <li>Educational background and transcripts</li>
                <li>Language proficiency certificates</li>
                <li>Motivation letter and CV</li>
                <li>Photograph for identification purposes</li>
              </ul>

              <h3 className="text-lg font-semibold mt-4 mb-2">3.2 Technical Information</h3>
              <p>We automatically collect:</p>
              <ul className="list-disc pl-6 space-y-1">
                <li>IP address and browser type</li>
                <li>Device information and operating system</li>
                <li>Pages visited and time spent on site</li>
                <li>Cookies and similar tracking technologies</li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>4. How We Use Your Information</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>We use your personal data for the following purposes:</p>
              <ul className="list-disc pl-6 space-y-1">
                <li><strong>Application Processing:</strong> To evaluate your admission application</li>
                <li><strong>Communication:</strong> To send you updates about your application status</li>
                <li><strong>Student Services:</strong> To provide educational services if you enroll</li>
                <li><strong>Legal Compliance:</strong> To comply with legal and regulatory requirements</li>
                <li><strong>Statistical Analysis:</strong> To improve our admission process (anonymized data)</li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>5. Legal Basis for Processing</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>Under GDPR, we process your data based on:</p>
              <ul className="list-disc pl-6 space-y-1">
                <li><strong>Consent:</strong> You provide explicit consent when submitting your application</li>
                <li><strong>Contract:</strong> Processing is necessary for enrollment and educational services</li>
                <li><strong>Legal Obligation:</strong> We must comply with education and immigration laws</li>
                <li><strong>Legitimate Interest:</strong> To maintain academic records and quality standards</li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>6. Data Retention</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>We retain your personal data for the following periods:</p>
              <ul className="list-disc pl-6 space-y-1">
                <li><strong>Application Data:</strong> 3 years after application decision</li>
                <li><strong>Student Records:</strong> 50 years after graduation (legal requirement)</li>
                <li><strong>Financial Records:</strong> 10 years (tax law requirement)</li>
                <li><strong>Website Analytics:</strong> 26 months maximum</li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>7. Your Rights Under GDPR</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>You have the following rights regarding your personal data:</p>
              <ul className="list-disc pl-6 space-y-1">
                <li><strong>Right to Access:</strong> Request a copy of your personal data</li>
                <li><strong>Right to Rectification:</strong> Correct inaccurate or incomplete data</li>
                <li><strong>Right to Erasure:</strong> Request deletion of your data (with limitations)</li>
                <li><strong>Right to Restriction:</strong> Limit how we process your data</li>
                <li><strong>Right to Data Portability:</strong> Receive your data in a structured format</li>
                <li><strong>Right to Object:</strong> Object to processing based on legitimate interests</li>
                <li><strong>Right to Withdraw Consent:</strong> Withdraw consent at any time</li>
              </ul>
              <p className="mt-4">
                To exercise these rights, contact our Data Protection Officer at dpo@minervia.edu.pl
              </p>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>8. Data Security</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>We implement appropriate technical and organizational measures to protect your data:</p>
              <ul className="list-disc pl-6 space-y-1">
                <li>SSL/TLS encryption for data transmission</li>
                <li>Encrypted storage of sensitive information</li>
                <li>Access controls and authentication systems</li>
                <li>Regular security audits and updates</li>
                <li>Staff training on data protection</li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>9. International Data Transfers</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>
                Your data is primarily stored and processed within the European Economic Area (EEA). If we transfer
                data outside the EEA, we ensure adequate protection through:
              </p>
              <ul className="list-disc pl-6 space-y-1">
                <li>EU Standard Contractual Clauses</li>
                <li>Adequacy decisions by the European Commission</li>
                <li>Appropriate safeguards as required by GDPR</li>
              </ul>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>10. Contact Information</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-slate max-w-none">
              <p>For privacy-related inquiries or to exercise your rights:</p>
              <p className="mt-2">
                <strong>Data Protection Officer</strong><br />
                Email: dpo@minervia.edu.pl<br />
                Phone: +48 12 345 67 00<br />
                Address: ul. Akademicka 1, 31-120 Kraków, Poland
              </p>
              <p className="mt-4">
                You also have the right to lodge a complaint with the Polish supervisory authority:
              </p>
              <p className="mt-2">
                <strong>Urząd Ochrony Danych Osobowych (UODO)</strong><br />
                ul. Stawki 2, 00-193 Warszawa, Poland<br />
                Website: uodo.gov.pl
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
