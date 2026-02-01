import { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { faculties, getFacultyBySlug } from '@/lib/academic-data';

interface Props {
  params: { locale: string; slug: string };
}

export async function generateStaticParams() {
  return faculties.map((faculty) => ({
    slug: faculty.slug,
  }));
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const faculty = getFacultyBySlug(params.slug);
  if (!faculty) return { title: 'Faculty Not Found' };

  return {
    title: `${faculty.name} - Minervia Institute`,
    description: faculty.description,
  };
}

export default function FacultyDetailPage({ params: { locale, slug } }: Props) {
  const faculty = getFacultyBySlug(slug);

  if (!faculty) {
    notFound();
  }

  const bachelorPrograms = faculty.programs.filter(p => ['BSc', 'BA', 'LLB'].includes(p.degree));
  const masterPrograms = faculty.programs.filter(p => ['MSc', 'MA', 'LLM', 'MD'].includes(p.degree));

  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Breadcrumb */}
        <nav className="text-sm text-muted-foreground mb-8">
          <Link href={`/${locale}`} className="hover:text-slate-900">Home</Link>
          {' / '}
          <Link href={`/${locale}/faculties`} className="hover:text-slate-900">Faculties</Link>
          {' / '}
          <span className="text-slate-900">{faculty.shortName}</span>
        </nav>

        {/* Header */}
        <div className="mb-12">
          <div className="flex items-center gap-4 mb-4">
            <div className="w-16 h-16 rounded-lg bg-slate-900 text-white flex items-center justify-center">
              <span className="text-lg font-bold">{faculty.shortName}</span>
            </div>
            <div>
              <h1 className="text-3xl font-bold">{faculty.name}</h1>
              <p className="text-muted-foreground">{faculty.programs.length} Academic Programs</p>
            </div>
          </div>
          <p className="text-lg text-muted-foreground max-w-3xl">
            {faculty.description}
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-8">
            {/* Bachelor Programs */}
            {bachelorPrograms.length > 0 && (
              <div>
                <h2 className="text-xl font-bold mb-4">Undergraduate Programs</h2>
                <div className="space-y-4">
                  {bachelorPrograms.map((program) => (
                    <Card key={program.id} className="border-0 shadow-sm">
                      <CardContent className="p-4">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <h3 className="font-semibold">{program.name}</h3>
                              <Badge variant="secondary">{program.degree}</Badge>
                            </div>
                            <p className="text-sm text-muted-foreground mb-2">
                              {program.description}
                            </p>
                            <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                              <span>{program.duration} years</span>
                              <span>{program.credits} ECTS</span>
                              <span>{program.language}</span>
                              <span>From EUR {program.tuitionEU.toLocaleString()}/year</span>
                            </div>
                          </div>
                          <Button asChild size="sm" variant="outline">
                            <Link href={`/${locale}/apply`}>Apply</Link>
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            )}

            {/* Master Programs */}
            {masterPrograms.length > 0 && (
              <div>
                <h2 className="text-xl font-bold mb-4">Graduate Programs</h2>
                <div className="space-y-4">
                  {masterPrograms.map((program) => (
                    <Card key={program.id} className="border-0 shadow-sm">
                      <CardContent className="p-4">
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <h3 className="font-semibold">{program.name}</h3>
                              <Badge variant="secondary">{program.degree}</Badge>
                            </div>
                            <p className="text-sm text-muted-foreground mb-2">
                              {program.description}
                            </p>
                            <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
                              <span>{program.duration} years</span>
                              <span>{program.credits} ECTS</span>
                              <span>{program.language}</span>
                              <span>From EUR {program.tuitionEU.toLocaleString()}/year</span>
                            </div>
                          </div>
                          <Button asChild size="sm" variant="outline">
                            <Link href={`/${locale}/apply`}>Apply</Link>
                          </Button>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            )}

            {/* Research Areas */}
            <div>
              <h2 className="text-xl font-bold mb-4">Research Areas</h2>
              <div className="flex flex-wrap gap-2">
                {faculty.researchAreas.map((area) => (
                  <Badge key={area} variant="outline" className="text-sm">
                    {area}
                  </Badge>
                ))}
              </div>
            </div>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Contact Card */}
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">Contact Information</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div>
                  <div className="font-medium">Dean</div>
                  <div className="text-muted-foreground">{faculty.dean}</div>
                </div>
                <div>
                  <div className="font-medium">Email</div>
                  <a href={`mailto:${faculty.email}`} className="text-slate-900 hover:underline">
                    {faculty.email}
                  </a>
                </div>
                <div>
                  <div className="font-medium">Phone</div>
                  <a href={`tel:${faculty.phone}`} className="text-slate-900 hover:underline">
                    {faculty.phone}
                  </a>
                </div>
                <div>
                  <div className="font-medium">Location</div>
                  <div className="text-muted-foreground">{faculty.location}</div>
                </div>
              </CardContent>
            </Card>

            {/* Apply CTA */}
            <Card className="border-0 shadow-md bg-slate-900 text-white">
              <CardContent className="p-6 text-center">
                <h3 className="font-bold mb-2">Ready to Apply?</h3>
                <p className="text-sm text-slate-300 mb-4">
                  Start your application today and join our academic community.
                </p>
                <Button asChild className="w-full bg-white text-slate-900 hover:bg-slate-100">
                  <Link href={`/${locale}/apply`}>Apply Now</Link>
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
