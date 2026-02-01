import { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { faculties } from '@/lib/academic-data';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Faculties - Minervia Institute',
  description: 'Explore our six faculties offering over 35 academic programs.',
};

export default function FacultiesPage({ params: { locale } }: Props) {
  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Header */}
        <div className="text-center mb-16">
          <h1 className="text-4xl font-bold tracking-tight mb-4">Our Faculties</h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
            Minervia Institute comprises six faculties offering over 35 undergraduate, graduate,
            and doctoral programs across diverse fields of study.
          </p>
        </div>

        {/* Faculty Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {faculties.map((faculty) => (
            <Card key={faculty.id} className="border-0 shadow-md hover:shadow-lg transition-shadow">
              <CardHeader>
                <div className="w-12 h-12 rounded-lg bg-slate-900 text-white flex items-center justify-center mb-4">
                  <span className="text-sm font-bold">{faculty.shortName}</span>
                </div>
                <CardTitle className="text-lg">{faculty.name}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-sm mb-4 line-clamp-3">
                  {faculty.description}
                </p>
                <div className="flex items-center justify-between text-sm text-muted-foreground mb-4">
                  <span>{faculty.programs.length} Programs</span>
                  <span>Dean: {faculty.dean.split(' ').slice(-1)[0]}</span>
                </div>
                <Button asChild variant="outline" className="w-full">
                  <Link href={`/${locale}/faculties/${faculty.slug}`}>
                    View Faculty
                  </Link>
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Stats */}
        <div className="mt-16 py-12 bg-slate-50 rounded-lg">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
            <div>
              <div className="text-3xl font-bold text-slate-900">6</div>
              <div className="text-sm text-muted-foreground">Faculties</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-slate-900">35+</div>
              <div className="text-sm text-muted-foreground">Programs</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-slate-900">450+</div>
              <div className="text-sm text-muted-foreground">Faculty Members</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-slate-900">8,500+</div>
              <div className="text-sm text-muted-foreground">Students</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
