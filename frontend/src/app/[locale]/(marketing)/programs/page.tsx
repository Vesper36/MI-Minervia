import { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { getAllPrograms, faculties } from '@/lib/academic-data';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Academic Programs - Minervia Institute',
  description: 'Explore over 35 undergraduate, graduate, and doctoral programs across six faculties.',
};

export default function ProgramsPage({ params: { locale } }: Props) {
  const allPrograms = getAllPrograms();

  const undergraduatePrograms = allPrograms.filter(
    ({ program }) => ['BSc', 'BA', 'LLB'].includes(program.degree)
  );
  const graduatePrograms = allPrograms.filter(
    ({ program }) => ['MSc', 'MA', 'LLM', 'MBA'].includes(program.degree)
  );
  const professionalPrograms = allPrograms.filter(
    ({ program }) => ['MD', 'PhD'].includes(program.degree)
  );

  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Header */}
        <div className="text-center mb-16">
          <h1 className="text-4xl md:text-5xl font-bold tracking-tight mb-4">
            Academic Programs
          </h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
            Minervia Institute offers over 35 programs across six faculties, providing
            world-class education in English and Polish.
          </p>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-16">
          <Card className="border-0 shadow-sm text-center">
            <CardContent className="pt-6">
              <div className="text-3xl font-bold text-primary">{allPrograms.length}</div>
              <div className="text-sm text-muted-foreground">Total Programs</div>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm text-center">
            <CardContent className="pt-6">
              <div className="text-3xl font-bold text-primary">{undergraduatePrograms.length}</div>
              <div className="text-sm text-muted-foreground">Undergraduate</div>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm text-center">
            <CardContent className="pt-6">
              <div className="text-3xl font-bold text-primary">{graduatePrograms.length}</div>
              <div className="text-sm text-muted-foreground">Graduate</div>
            </CardContent>
          </Card>
          <Card className="border-0 shadow-sm text-center">
            <CardContent className="pt-6">
              <div className="text-3xl font-bold text-primary">{faculties.length}</div>
              <div className="text-sm text-muted-foreground">Faculties</div>
            </CardContent>
          </Card>
        </div>

        {/* Undergraduate Programs */}
        <section className="mb-16">
          <h2 className="text-2xl font-bold mb-6">Undergraduate Programs</h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {undergraduatePrograms.map(({ program, faculty }) => (
              <Card key={program.id} className="border shadow-sm hover:shadow-md transition-shadow">
                <CardHeader className="pb-2">
                  <div className="flex items-start justify-between gap-2">
                    <CardTitle className="text-lg">{program.name}</CardTitle>
                    <Badge variant="secondary">{program.degree}</Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{faculty.name}</p>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground mb-4 line-clamp-2">
                    {program.description}
                  </p>
                  <div className="grid grid-cols-2 gap-2 text-sm mb-4">
                    <div>
                      <span className="text-muted-foreground">Duration:</span>{' '}
                      <span className="font-medium">{program.duration} years</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Credits:</span>{' '}
                      <span className="font-medium">{program.credits} ECTS</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Language:</span>{' '}
                      <span className="font-medium">{program.language}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Tuition:</span>{' '}
                      <span className="font-medium">EUR {program.tuitionEU.toLocaleString()}</span>
                    </div>
                  </div>
                  <Button asChild variant="outline" size="sm" className="w-full">
                    <Link href={`/${locale}/faculties/${faculty.slug}`}>View Details</Link>
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        {/* Graduate Programs */}
        <section className="mb-16">
          <h2 className="text-2xl font-bold mb-6">Graduate Programs</h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {graduatePrograms.map(({ program, faculty }) => (
              <Card key={program.id} className="border shadow-sm hover:shadow-md transition-shadow">
                <CardHeader className="pb-2">
                  <div className="flex items-start justify-between gap-2">
                    <CardTitle className="text-lg">{program.name}</CardTitle>
                    <Badge>{program.degree}</Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{faculty.name}</p>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground mb-4 line-clamp-2">
                    {program.description}
                  </p>
                  <div className="grid grid-cols-2 gap-2 text-sm mb-4">
                    <div>
                      <span className="text-muted-foreground">Duration:</span>{' '}
                      <span className="font-medium">{program.duration} years</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Credits:</span>{' '}
                      <span className="font-medium">{program.credits} ECTS</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Language:</span>{' '}
                      <span className="font-medium">{program.language}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Tuition:</span>{' '}
                      <span className="font-medium">EUR {program.tuitionEU.toLocaleString()}</span>
                    </div>
                  </div>
                  <Button asChild variant="outline" size="sm" className="w-full">
                    <Link href={`/${locale}/faculties/${faculty.slug}`}>View Details</Link>
                  </Button>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>

        {/* Professional Programs */}
        {professionalPrograms.length > 0 && (
          <section className="mb-16">
            <h2 className="text-2xl font-bold mb-6">Professional Programs</h2>
            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
              {professionalPrograms.map(({ program, faculty }) => (
                <Card key={program.id} className="border shadow-sm hover:shadow-md transition-shadow">
                  <CardHeader className="pb-2">
                    <div className="flex items-start justify-between gap-2">
                      <CardTitle className="text-lg">{program.name}</CardTitle>
                      <Badge variant="destructive">{program.degree}</Badge>
                    </div>
                    <p className="text-sm text-muted-foreground">{faculty.name}</p>
                  </CardHeader>
                  <CardContent>
                    <p className="text-sm text-muted-foreground mb-4 line-clamp-2">
                      {program.description}
                    </p>
                    <div className="grid grid-cols-2 gap-2 text-sm mb-4">
                      <div>
                        <span className="text-muted-foreground">Duration:</span>{' '}
                        <span className="font-medium">{program.duration} years</span>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Credits:</span>{' '}
                        <span className="font-medium">{program.credits} ECTS</span>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Language:</span>{' '}
                        <span className="font-medium">{program.language}</span>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Tuition:</span>{' '}
                        <span className="font-medium">EUR {program.tuitionEU.toLocaleString()}</span>
                      </div>
                    </div>
                    <Button asChild variant="outline" size="sm" className="w-full">
                      <Link href={`/${locale}/faculties/${faculty.slug}`}>View Details</Link>
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </section>
        )}

        {/* CTA */}
        <section>
          <Card className="border-0 shadow-lg bg-slate-900 text-white">
            <CardContent className="py-12 text-center">
              <h2 className="text-2xl font-bold mb-4">Ready to Start Your Journey?</h2>
              <p className="mb-6 opacity-90 max-w-2xl mx-auto">
                Apply now to join our diverse community of students from over 50 countries.
              </p>
              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <Button asChild size="lg" variant="secondary">
                  <Link href={`/${locale}/apply`}>Apply Now</Link>
                </Button>
                <Button asChild size="lg" variant="outline" className="border-white text-white hover:bg-white/10">
                  <Link href={`/${locale}/admissions`}>Admission Requirements</Link>
                </Button>
              </div>
            </CardContent>
          </Card>
        </section>
      </div>
    </div>
  );
}
