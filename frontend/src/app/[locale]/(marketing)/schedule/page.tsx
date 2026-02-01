import { Metadata } from 'next';
import Link from 'next/link';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

interface Props {
  params: { locale: string };
}

export const metadata: Metadata = {
  title: 'Academic Schedule - Minervia Institute',
  description: 'Academic calendar, semester schedule, and course timetables for Minervia Institute.',
};

const academicCalendar = [
  {
    semester: 'Winter Semester 2025/2026',
    period: 'October 1, 2025 - February 14, 2026',
    events: [
      { date: 'Sep 15-20', event: 'Student Registration', type: 'admin' },
      { date: 'Sep 23-27', event: 'Orientation Week', type: 'orientation' },
      { date: 'Oct 1', event: 'Classes Begin', type: 'academic' },
      { date: 'Nov 1', event: "All Saints' Day (Holiday)", type: 'holiday' },
      { date: 'Nov 11', event: 'Independence Day (Holiday)', type: 'holiday' },
      { date: 'Dec 23 - Jan 6', event: 'Winter Break', type: 'break' },
      { date: 'Jan 20 - Feb 7', event: 'Examination Period', type: 'exam' },
      { date: 'Feb 8-14', event: 'Semester Break', type: 'break' },
    ],
  },
  {
    semester: 'Summer Semester 2025/2026',
    period: 'February 17 - June 30, 2026',
    events: [
      { date: 'Feb 17', event: 'Classes Begin', type: 'academic' },
      { date: 'Apr 13-21', event: 'Easter Break', type: 'break' },
      { date: 'May 1', event: 'Labour Day (Holiday)', type: 'holiday' },
      { date: 'May 3', event: 'Constitution Day (Holiday)', type: 'holiday' },
      { date: 'Jun 8-28', event: 'Examination Period', type: 'exam' },
      { date: 'Jun 30', event: 'Semester Ends', type: 'academic' },
      { date: 'Jul 1 - Sep 30', event: 'Summer Vacation', type: 'break' },
    ],
  },
];

const sampleSchedule = {
  program: 'BSc Computer Science - Year 1, Semester 1',
  courses: [
    {
      code: 'CS101',
      name: 'Introduction to Programming',
      credits: 6,
      schedule: [
        { day: 'Monday', time: '08:00-09:30', type: 'Lecture', room: 'A-101' },
        { day: 'Wednesday', time: '10:00-11:30', type: 'Lab', room: 'Lab-3' },
      ],
      instructor: 'Dr. Anna Kowalska',
    },
    {
      code: 'CS102',
      name: 'Discrete Mathematics',
      credits: 5,
      schedule: [
        { day: 'Tuesday', time: '08:00-09:30', type: 'Lecture', room: 'B-201' },
        { day: 'Thursday', time: '08:00-09:30', type: 'Tutorial', room: 'B-105' },
      ],
      instructor: 'Prof. Marek Nowak',
    },
    {
      code: 'CS103',
      name: 'Computer Architecture',
      credits: 5,
      schedule: [
        { day: 'Monday', time: '10:00-11:30', type: 'Lecture', room: 'A-102' },
        { day: 'Friday', time: '10:00-11:30', type: 'Lab', room: 'Lab-1' },
      ],
      instructor: 'Dr. Piotr Wisniewski',
    },
    {
      code: 'MATH101',
      name: 'Calculus I',
      credits: 6,
      schedule: [
        { day: 'Tuesday', time: '10:00-11:30', type: 'Lecture', room: 'C-301' },
        { day: 'Thursday', time: '10:00-11:30', type: 'Tutorial', room: 'C-105' },
      ],
      instructor: 'Prof. Ewa Mazur',
    },
    {
      code: 'LANG101',
      name: 'Academic English',
      credits: 3,
      schedule: [
        { day: 'Wednesday', time: '14:00-15:30', type: 'Seminar', room: 'D-201' },
      ],
      instructor: 'Dr. Sarah Johnson',
    },
    {
      code: 'PHY101',
      name: 'Physics for Engineers',
      credits: 5,
      schedule: [
        { day: 'Friday', time: '08:00-09:30', type: 'Lecture', room: 'A-201' },
        { day: 'Friday', time: '12:00-13:30', type: 'Lab', room: 'Physics Lab' },
      ],
      instructor: 'Dr. Tomasz Krol',
    },
  ],
};

const weeklyTimetable = [
  { time: '08:00-09:30', mon: 'CS101 Lec', tue: 'CS102 Lec', wed: '', thu: 'CS102 Tut', fri: 'PHY101 Lec' },
  { time: '10:00-11:30', mon: 'CS103 Lec', tue: 'MATH101 Lec', wed: 'CS101 Lab', thu: 'MATH101 Tut', fri: 'CS103 Lab' },
  { time: '12:00-13:30', mon: '', tue: '', wed: '', thu: '', fri: 'PHY101 Lab' },
  { time: '14:00-15:30', mon: '', tue: '', wed: 'LANG101 Sem', thu: '', fri: '' },
];

const classTypes = [
  { type: 'Lecture', abbr: 'Lec', description: 'Traditional lecture format with professor presentation' },
  { type: 'Tutorial', abbr: 'Tut', description: 'Small group problem-solving sessions' },
  { type: 'Laboratory', abbr: 'Lab', description: 'Hands-on practical work in specialized facilities' },
  { type: 'Seminar', abbr: 'Sem', description: 'Discussion-based classes with student participation' },
];

export default function SchedulePage({ params: { locale } }: Props) {
  const totalCredits = sampleSchedule.courses.reduce((sum, c) => sum + c.credits, 0);

  return (
    <div className="py-16 md:py-24">
      <div className="container">
        {/* Header */}
        <div className="text-center mb-16">
          <h1 className="text-4xl font-bold tracking-tight mb-4">Academic Schedule</h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
            Academic calendar, course schedules, and important dates for the 2025/2026 academic year.
          </p>
        </div>

        {/* Academic Calendar */}
        <div className="mb-16">
          <h2 className="text-2xl font-bold mb-6">Academic Calendar 2025/2026</h2>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {academicCalendar.map((semester) => (
              <Card key={semester.semester} className="border-0 shadow-md">
                <CardHeader>
                  <CardTitle className="text-lg">{semester.semester}</CardTitle>
                  <p className="text-sm text-muted-foreground">{semester.period}</p>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {semester.events.map((event, index) => (
                      <div key={index} className="flex items-center gap-3 text-sm">
                        <Badge
                          variant="outline"
                          className={
                            event.type === 'exam' ? 'border-red-300 text-red-700' :
                            event.type === 'holiday' ? 'border-green-300 text-green-700' :
                            event.type === 'break' ? 'border-blue-300 text-blue-700' :
                            event.type === 'academic' ? 'border-slate-300 text-slate-700' :
                            'border-amber-300 text-amber-700'
                          }
                        >
                          {event.date}
                        </Badge>
                        <span>{event.event}</span>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        {/* Sample Course Schedule */}
        <div className="mb-16">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold">Sample Course Schedule</h2>
              <p className="text-muted-foreground">{sampleSchedule.program}</p>
            </div>
            <Badge variant="secondary" className="text-sm">
              {totalCredits} ECTS Total
            </Badge>
          </div>

          {/* Weekly Timetable */}
          <Card className="border-0 shadow-md mb-6 overflow-hidden">
            <CardHeader>
              <CardTitle className="text-lg">Weekly Timetable</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="p-3 text-left font-semibold border-r">Time</th>
                      <th className="p-3 text-center font-semibold">Monday</th>
                      <th className="p-3 text-center font-semibold">Tuesday</th>
                      <th className="p-3 text-center font-semibold">Wednesday</th>
                      <th className="p-3 text-center font-semibold">Thursday</th>
                      <th className="p-3 text-center font-semibold">Friday</th>
                    </tr>
                  </thead>
                  <tbody>
                    {weeklyTimetable.map((row, index) => (
                      <tr key={index} className="border-t">
                        <td className="p-3 font-medium border-r bg-slate-50">{row.time}</td>
                        <td className="p-3 text-center">{row.mon && <Badge variant="outline">{row.mon}</Badge>}</td>
                        <td className="p-3 text-center">{row.tue && <Badge variant="outline">{row.tue}</Badge>}</td>
                        <td className="p-3 text-center">{row.wed && <Badge variant="outline">{row.wed}</Badge>}</td>
                        <td className="p-3 text-center">{row.thu && <Badge variant="outline">{row.thu}</Badge>}</td>
                        <td className="p-3 text-center">{row.fri && <Badge variant="outline">{row.fri}</Badge>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>

          {/* Course Details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {sampleSchedule.courses.map((course) => (
              <Card key={course.code} className="border-0 shadow-sm">
                <CardContent className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <Badge variant="secondary">{course.code}</Badge>
                        <span className="text-xs text-muted-foreground">{course.credits} ECTS</span>
                      </div>
                      <h3 className="font-semibold mt-1">{course.name}</h3>
                    </div>
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">{course.instructor}</p>
                  <div className="space-y-1">
                    {course.schedule.map((slot, index) => (
                      <div key={index} className="text-xs flex items-center gap-2">
                        <span className="font-medium w-20">{slot.day}</span>
                        <span className="text-muted-foreground">{slot.time}</span>
                        <Badge variant="outline" className="text-xs">{slot.type}</Badge>
                        <span className="text-muted-foreground">{slot.room}</span>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        {/* Class Types Legend */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">Class Types</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {classTypes.map((type) => (
                    <div key={type.type} className="flex gap-3">
                      <Badge variant="outline" className="h-fit">{type.abbr}</Badge>
                      <div>
                        <div className="font-medium text-sm">{type.type}</div>
                        <div className="text-xs text-muted-foreground">{type.description}</div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            <Card className="border-0 shadow-md">
              <CardHeader>
                <CardTitle className="text-lg">Schedule Information</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div>
                  <div className="font-medium">Class Duration</div>
                  <div className="text-muted-foreground">90 minutes per session</div>
                </div>
                <div>
                  <div className="font-medium">Break Between Classes</div>
                  <div className="text-muted-foreground">15-30 minutes</div>
                </div>
                <div>
                  <div className="font-medium">Attendance Policy</div>
                  <div className="text-muted-foreground">Minimum 80% required</div>
                </div>
                <div>
                  <div className="font-medium">Schedule Changes</div>
                  <div className="text-muted-foreground">Announced via student portal</div>
                </div>
              </CardContent>
            </Card>

            <Card className="border-0 shadow-md bg-slate-900 text-white">
              <CardContent className="p-6 text-center">
                <h3 className="font-bold mb-2">View Full Schedule</h3>
                <p className="text-sm text-slate-300 mb-4">
                  Access your personalized schedule through the student portal.
                </p>
                <Button asChild className="w-full bg-white text-slate-900 hover:bg-slate-100">
                  <Link href={`/${locale}/student-portal`}>Student Portal</Link>
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
