import { Metadata } from 'next';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Contact Us - Minervia Institute',
  description: 'Get in touch with Minervia Institute. Contact our admissions, academic, or administrative offices.',
};

const departments = [
  {
    name: 'Admissions Office',
    email: 'admissions@minervia.edu.pl',
    phone: '+48 12 345 67 89',
    hours: 'Mon-Fri: 9:00 - 17:00',
  },
  {
    name: 'International Office',
    email: 'international@minervia.edu.pl',
    phone: '+48 12 345 67 90',
    hours: 'Mon-Fri: 9:00 - 16:00',
  },
  {
    name: 'Student Services',
    email: 'students@minervia.edu.pl',
    phone: '+48 12 345 67 91',
    hours: 'Mon-Fri: 8:00 - 18:00',
  },
  {
    name: 'General Inquiries',
    email: 'info@minervia.edu.pl',
    phone: '+48 12 345 67 00',
    hours: 'Mon-Fri: 8:00 - 17:00',
  },
];

export default function ContactPage({ params: { locale } }: Props) {
  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Header */}
        <div className="text-center mb-16">
          <h1 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
            Contact Us
          </h1>
          <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
            We are here to help. Reach out to us with any questions about admissions,
            programs, or campus life.
          </p>
        </div>

        <div className="grid lg:grid-cols-2 gap-12">
          {/* Contact Form */}
          <div>
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle>Send us a Message</CardTitle>
              </CardHeader>
              <CardContent>
                <form className="space-y-4">
                  <div className="grid sm:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="firstName">First Name</Label>
                      <Input id="firstName" placeholder="John" />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="lastName">Last Name</Label>
                      <Input id="lastName" placeholder="Doe" />
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input id="email" type="email" placeholder="john.doe@example.com" />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="subject">Subject</Label>
                    <Input id="subject" placeholder="How can we help?" />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="message">Message</Label>
                    <textarea
                      id="message"
                      className="flex min-h-[120px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      placeholder="Your message..."
                    />
                  </div>
                  <Button type="submit" className="w-full">
                    Send Message
                  </Button>
                </form>
              </CardContent>
            </Card>
          </div>

          {/* Contact Information */}
          <div className="space-y-6">
            {/* Address */}
            <Card className="border-0 shadow-md">
              <CardContent className="pt-6">
                <h3 className="font-semibold mb-4">Campus Address</h3>
                <div className="text-muted-foreground space-y-1">
                  <p className="font-medium text-foreground">Minervia Institute</p>
                  <p>ul. Akademicka 1</p>
                  <p>31-120 Krakow, Poland</p>
                </div>
                <div className="mt-4 h-48 bg-slate-100 rounded-lg flex items-center justify-center">
                  <span className="text-muted-foreground text-sm">Map placeholder</span>
                </div>
              </CardContent>
            </Card>

            {/* Departments */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">Department Contacts</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {departments.map((dept) => (
                  <div key={dept.name} className="pb-4 border-b last:border-0 last:pb-0">
                    <h4 className="font-medium mb-1">{dept.name}</h4>
                    <div className="text-sm text-muted-foreground space-y-0.5">
                      <p>
                        <a href={`mailto:${dept.email}`} className="hover:text-primary">
                          {dept.email}
                        </a>
                      </p>
                      <p>{dept.phone}</p>
                      <p>{dept.hours}</p>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
