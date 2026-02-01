// Minervia Institute - Academic Structure Data
// This file contains all faculties, departments, and programs

export interface Program {
  id: string;
  name: string;
  degree: 'BSc' | 'BA' | 'MSc' | 'MA' | 'LLB' | 'LLM' | 'MD' | 'PhD';
  duration: number; // years
  credits: number; // ECTS
  language: 'English' | 'Polish' | 'English/Polish';
  tuitionEU: number; // EUR per year
  tuitionNonEU: number;
  description: string;
  careers: string[];
}

export interface Faculty {
  id: string;
  name: string;
  shortName: string;
  slug: string;
  description: string;
  dean: string;
  email: string;
  phone: string;
  location: string;
  programs: Program[];
  researchAreas: string[];
}

export const faculties: Faculty[] = [
  {
    id: 'fcsm',
    name: 'Faculty of Computer Science and Mathematics',
    shortName: 'FCSM',
    slug: 'computer-science',
    description: 'The Faculty of Computer Science and Mathematics offers cutting-edge programs in computing, data science, and mathematical sciences. Our graduates are highly sought after by leading technology companies worldwide.',
    dean: 'Prof. Dr. Tomasz Kowalczyk',
    email: 'fcsm@minervia.edu.pl',
    phone: '+48 12 345 67 01',
    location: 'Building A, Main Campus',
    researchAreas: ['Artificial Intelligence', 'Cybersecurity', 'Data Analytics', 'Software Engineering', 'Applied Mathematics'],
    programs: [
      {
        id: 'cs-bsc',
        name: 'Computer Science',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3500,
        tuitionNonEU: 5500,
        description: 'A comprehensive program covering programming, algorithms, databases, and software development.',
        careers: ['Software Developer', 'Systems Analyst', 'Database Administrator', 'IT Consultant']
      },
      {
        id: 'cs-msc',
        name: 'Computer Science',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 4000,
        tuitionNonEU: 6500,
        description: 'Advanced studies in computer science with specializations in AI, security, or distributed systems.',
        careers: ['Senior Software Engineer', 'Research Scientist', 'Technical Lead', 'CTO']
      },
      {
        id: 'ds-bsc',
        name: 'Data Science and Analytics',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3800,
        tuitionNonEU: 5800,
        description: 'Learn to extract insights from data using statistics, machine learning, and visualization.',
        careers: ['Data Analyst', 'Business Intelligence Analyst', 'Data Engineer', 'Quantitative Analyst']
      },
      {
        id: 'ds-msc',
        name: 'Data Science and Analytics',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 4500,
        tuitionNonEU: 7000,
        description: 'Advanced data science with focus on big data, deep learning, and predictive analytics.',
        careers: ['Data Scientist', 'Machine Learning Engineer', 'AI Researcher', 'Chief Data Officer']
      },
      {
        id: 'cyber-bsc',
        name: 'Cybersecurity',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 4000,
        tuitionNonEU: 6000,
        description: 'Comprehensive training in network security, cryptography, and ethical hacking.',
        careers: ['Security Analyst', 'Penetration Tester', 'Security Consultant', 'SOC Analyst']
      },
      {
        id: 'ai-msc',
        name: 'Artificial Intelligence',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 5000,
        tuitionNonEU: 8000,
        description: 'Specialized program in machine learning, neural networks, and intelligent systems.',
        careers: ['AI Engineer', 'ML Researcher', 'Robotics Engineer', 'NLP Specialist']
      }
    ]
  },
  {
    id: 'fbe',
    name: 'Faculty of Business and Economics',
    shortName: 'FBE',
    slug: 'business',
    description: 'The Faculty of Business and Economics prepares future business leaders with a strong foundation in management, finance, and international business practices.',
    dean: 'Prof. Dr. Anna Wisniewska',
    email: 'fbe@minervia.edu.pl',
    phone: '+48 12 345 67 02',
    location: 'Building B, Main Campus',
    researchAreas: ['International Business', 'Financial Markets', 'Marketing Strategy', 'Entrepreneurship', 'Sustainable Business'],
    programs: [
      {
        id: 'ba-bsc',
        name: 'Business Administration',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3200,
        tuitionNonEU: 5000,
        description: 'Foundational business education covering management, marketing, finance, and operations.',
        careers: ['Business Analyst', 'Project Manager', 'Marketing Coordinator', 'Operations Manager']
      },
      {
        id: 'ba-msc',
        name: 'Business Administration (MBA)',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 6000,
        tuitionNonEU: 9000,
        description: 'Executive-level business education for aspiring leaders and entrepreneurs.',
        careers: ['CEO', 'Managing Director', 'Strategy Consultant', 'Entrepreneur']
      },
      {
        id: 'ib-bsc',
        name: 'International Business',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3500,
        tuitionNonEU: 5500,
        description: 'Focus on global trade, cross-cultural management, and international markets.',
        careers: ['International Trade Specialist', 'Export Manager', 'Global Account Manager', 'Diplomat']
      },
      {
        id: 'fin-bsc',
        name: 'Finance and Accounting',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3500,
        tuitionNonEU: 5500,
        description: 'Comprehensive training in financial analysis, accounting principles, and investment.',
        careers: ['Financial Analyst', 'Accountant', 'Auditor', 'Investment Banker']
      },
      {
        id: 'fin-msc',
        name: 'Finance and Investment',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 5000,
        tuitionNonEU: 7500,
        description: 'Advanced finance with focus on portfolio management and financial engineering.',
        careers: ['Portfolio Manager', 'Risk Analyst', 'CFO', 'Hedge Fund Manager']
      },
      {
        id: 'mkt-bsc',
        name: 'Marketing and Management',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3200,
        tuitionNonEU: 5000,
        description: 'Strategic marketing, brand management, and digital marketing techniques.',
        careers: ['Marketing Manager', 'Brand Manager', 'Digital Marketing Specialist', 'PR Manager']
      },
      {
        id: 'econ-bsc',
        name: 'Economics',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3000,
        tuitionNonEU: 4800,
        description: 'Micro and macroeconomics, econometrics, and economic policy analysis.',
        careers: ['Economist', 'Policy Analyst', 'Research Analyst', 'Economic Consultant']
      }
    ]
  },
  {
    id: 'feng',
    name: 'Faculty of Engineering',
    shortName: 'FENG',
    slug: 'engineering',
    description: 'The Faculty of Engineering provides hands-on training in various engineering disciplines with modern laboratories and industry partnerships.',
    dean: 'Prof. Dr. Marek Jankowski',
    email: 'feng@minervia.edu.pl',
    phone: '+48 12 345 67 03',
    location: 'Engineering Complex, East Campus',
    researchAreas: ['Renewable Energy', 'Robotics', 'Structural Engineering', 'Environmental Technology', 'Biomedical Devices'],
    programs: [
      {
        id: 'me-bsc',
        name: 'Mechanical Engineering',
        degree: 'BSc',
        duration: 3.5,
        credits: 210,
        language: 'English',
        tuitionEU: 4000,
        tuitionNonEU: 6000,
        description: 'Design, analysis, and manufacturing of mechanical systems and machines.',
        careers: ['Mechanical Engineer', 'Design Engineer', 'Manufacturing Engineer', 'R&D Engineer']
      },
      {
        id: 'me-msc',
        name: 'Mechanical Engineering',
        degree: 'MSc',
        duration: 1.5,
        credits: 90,
        language: 'English',
        tuitionEU: 4500,
        tuitionNonEU: 7000,
        description: 'Advanced mechanical engineering with specialization options.',
        careers: ['Senior Engineer', 'Project Lead', 'Technical Director', 'Research Engineer']
      },
      {
        id: 'ee-bsc',
        name: 'Electrical Engineering',
        degree: 'BSc',
        duration: 3.5,
        credits: 210,
        language: 'English',
        tuitionEU: 4000,
        tuitionNonEU: 6000,
        description: 'Electrical systems, power electronics, and control systems.',
        careers: ['Electrical Engineer', 'Power Systems Engineer', 'Control Engineer', 'Electronics Designer']
      },
      {
        id: 'ce-bsc',
        name: 'Civil Engineering',
        degree: 'BSc',
        duration: 4,
        credits: 240,
        language: 'English/Polish',
        tuitionEU: 3800,
        tuitionNonEU: 5800,
        description: 'Structural design, construction management, and infrastructure development.',
        careers: ['Civil Engineer', 'Structural Engineer', 'Construction Manager', 'Urban Planner']
      },
      {
        id: 'env-bsc',
        name: 'Environmental Engineering',
        degree: 'BSc',
        duration: 3.5,
        credits: 210,
        language: 'English',
        tuitionEU: 3800,
        tuitionNonEU: 5800,
        description: 'Water treatment, waste management, and environmental protection.',
        careers: ['Environmental Engineer', 'Sustainability Consultant', 'Water Resources Engineer', 'EHS Manager']
      },
      {
        id: 'bme-msc',
        name: 'Biomedical Engineering',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 5000,
        tuitionNonEU: 7500,
        description: 'Medical devices, biomechanics, and healthcare technology.',
        careers: ['Biomedical Engineer', 'Medical Device Designer', 'Clinical Engineer', 'Healthcare Technology Consultant']
      }
    ]
  },
  {
    id: 'fmhs',
    name: 'Faculty of Medicine and Health Sciences',
    shortName: 'FMHS',
    slug: 'medicine',
    description: 'The Faculty of Medicine and Health Sciences offers rigorous medical education with clinical training at partner hospitals throughout Poland.',
    dean: 'Prof. Dr. hab. med. Katarzyna Lewandowska',
    email: 'fmhs@minervia.edu.pl',
    phone: '+48 12 345 67 04',
    location: 'Medical Campus',
    researchAreas: ['Clinical Medicine', 'Public Health', 'Pharmaceutical Sciences', 'Nursing Research', 'Medical Technology'],
    programs: [
      {
        id: 'med-md',
        name: 'Medicine',
        degree: 'MD',
        duration: 6,
        credits: 360,
        language: 'English',
        tuitionEU: 12000,
        tuitionNonEU: 14000,
        description: 'Six-year medical program leading to MD degree with clinical rotations.',
        careers: ['Physician', 'Surgeon', 'Medical Specialist', 'Medical Researcher']
      },
      {
        id: 'nurs-bsc',
        name: 'Nursing',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English/Polish',
        tuitionEU: 3500,
        tuitionNonEU: 5000,
        description: 'Comprehensive nursing education with clinical practice.',
        careers: ['Registered Nurse', 'Clinical Nurse', 'Nurse Educator', 'Healthcare Administrator']
      },
      {
        id: 'pharm-msc',
        name: 'Pharmacy',
        degree: 'MSc',
        duration: 5,
        credits: 300,
        language: 'English',
        tuitionEU: 8000,
        tuitionNonEU: 10000,
        description: 'Pharmaceutical sciences, drug development, and clinical pharmacy.',
        careers: ['Pharmacist', 'Pharmaceutical Researcher', 'Clinical Pharmacist', 'Regulatory Affairs Specialist']
      },
      {
        id: 'ph-bsc',
        name: 'Public Health',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3200,
        tuitionNonEU: 5000,
        description: 'Population health, epidemiology, and health policy.',
        careers: ['Public Health Officer', 'Epidemiologist', 'Health Policy Analyst', 'NGO Health Coordinator']
      },
      {
        id: 'ph-msc',
        name: 'Public Health',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 4000,
        tuitionNonEU: 6000,
        description: 'Advanced public health with focus on global health challenges.',
        careers: ['Public Health Director', 'WHO Consultant', 'Health Program Manager', 'Research Director']
      }
    ]
  },
  {
    id: 'fla',
    name: 'Faculty of Law and Administration',
    shortName: 'FLA',
    slug: 'law',
    description: 'The Faculty of Law and Administration provides comprehensive legal education with emphasis on European and international law.',
    dean: 'Prof. Dr. hab. Pawel Mazur',
    email: 'fla@minervia.edu.pl',
    phone: '+48 12 345 67 05',
    location: 'Building C, Main Campus',
    researchAreas: ['European Union Law', 'International Law', 'Human Rights', 'Commercial Law', 'Public Administration'],
    programs: [
      {
        id: 'law-llb',
        name: 'Law',
        degree: 'LLB',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 4000,
        tuitionNonEU: 6000,
        description: 'Foundational legal education covering civil, criminal, and constitutional law.',
        careers: ['Legal Assistant', 'Paralegal', 'Compliance Officer', 'Legal Researcher']
      },
      {
        id: 'law-llm',
        name: 'Law',
        degree: 'LLM',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 5000,
        tuitionNonEU: 7500,
        description: 'Advanced legal studies with specialization options.',
        careers: ['Lawyer', 'Legal Counsel', 'Judge', 'Legal Academic']
      },
      {
        id: 'eulaw-llm',
        name: 'European Law',
        degree: 'LLM',
        duration: 1,
        credits: 60,
        language: 'English',
        tuitionEU: 5500,
        tuitionNonEU: 8000,
        description: 'Specialized program in EU law, institutions, and regulations.',
        careers: ['EU Legal Advisor', 'Policy Officer', 'EU Affairs Consultant', 'International Lawyer']
      },
      {
        id: 'pa-bsc',
        name: 'Public Administration',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English/Polish',
        tuitionEU: 3000,
        tuitionNonEU: 4500,
        description: 'Government administration, public policy, and management.',
        careers: ['Civil Servant', 'Policy Analyst', 'Government Administrator', 'NGO Manager']
      },
      {
        id: 'ir-bsc',
        name: 'International Relations',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3200,
        tuitionNonEU: 5000,
        description: 'Global politics, diplomacy, and international organizations.',
        careers: ['Diplomat', 'International Affairs Analyst', 'NGO Coordinator', 'Foreign Correspondent']
      },
      {
        id: 'ir-msc',
        name: 'International Relations',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 4000,
        tuitionNonEU: 6000,
        description: 'Advanced international relations with focus on security and diplomacy.',
        careers: ['Senior Diplomat', 'Security Analyst', 'International Consultant', 'UN Officer']
      }
    ]
  },
  {
    id: 'fah',
    name: 'Faculty of Arts and Humanities',
    shortName: 'FAH',
    slug: 'arts-humanities',
    description: 'The Faculty of Arts and Humanities offers programs in languages, psychology, history, and cultural studies in an international environment.',
    dean: 'Prof. Dr. hab. Ewa Kaminska',
    email: 'fah@minervia.edu.pl',
    phone: '+48 12 345 67 06',
    location: 'Humanities Building, Main Campus',
    researchAreas: ['Applied Linguistics', 'Clinical Psychology', 'European History', 'Cultural Heritage', 'Philosophy of Mind'],
    programs: [
      {
        id: 'eng-ba',
        name: 'English Philology',
        degree: 'BA',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 2800,
        tuitionNonEU: 4500,
        description: 'English language, literature, and linguistics.',
        careers: ['Translator', 'English Teacher', 'Editor', 'Content Writer']
      },
      {
        id: 'eng-ma',
        name: 'English Philology',
        degree: 'MA',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 3500,
        tuitionNonEU: 5500,
        description: 'Advanced English studies with specialization in translation or teaching.',
        careers: ['Senior Translator', 'University Lecturer', 'Publishing Editor', 'Language Consultant']
      },
      {
        id: 'psy-bsc',
        name: 'Psychology',
        degree: 'BSc',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 3500,
        tuitionNonEU: 5500,
        description: 'Foundational psychology covering cognitive, social, and developmental psychology.',
        careers: ['Research Assistant', 'HR Specialist', 'Counselor Assistant', 'Market Researcher']
      },
      {
        id: 'psy-msc',
        name: 'Psychology',
        degree: 'MSc',
        duration: 2,
        credits: 120,
        language: 'English',
        tuitionEU: 4500,
        tuitionNonEU: 7000,
        description: 'Advanced psychology with clinical or organizational specialization.',
        careers: ['Clinical Psychologist', 'Organizational Psychologist', 'Therapist', 'Research Psychologist']
      },
      {
        id: 'hist-ba',
        name: 'History',
        degree: 'BA',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 2500,
        tuitionNonEU: 4000,
        description: 'European and world history with focus on modern period.',
        careers: ['Historian', 'Archivist', 'Museum Curator', 'History Teacher']
      },
      {
        id: 'phil-ba',
        name: 'Philosophy',
        degree: 'BA',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 2500,
        tuitionNonEU: 4000,
        description: 'Western philosophy, ethics, and critical thinking.',
        careers: ['Ethics Consultant', 'Writer', 'Academic Researcher', 'Policy Advisor']
      },
      {
        id: 'cult-ba',
        name: 'Cultural Studies',
        degree: 'BA',
        duration: 3,
        credits: 180,
        language: 'English',
        tuitionEU: 2800,
        tuitionNonEU: 4500,
        description: 'Interdisciplinary study of culture, media, and society.',
        careers: ['Cultural Manager', 'Media Analyst', 'Event Coordinator', 'Arts Administrator']
      }
    ]
  }
];

export function getFacultyBySlug(slug: string): Faculty | undefined {
  return faculties.find(f => f.slug === slug);
}

export function getProgramById(id: string): { program: Program; faculty: Faculty } | undefined {
  for (const faculty of faculties) {
    const program = faculty.programs.find(p => p.id === id);
    if (program) {
      return { program, faculty };
    }
  }
  return undefined;
}

export function getAllPrograms(): { program: Program; faculty: Faculty }[] {
  const result: { program: Program; faculty: Faculty }[] = [];
  for (const faculty of faculties) {
    for (const program of faculty.programs) {
      result.push({ program, faculty });
    }
  }
  return result;
}

export function getProgramsByDegree(degree: Program['degree']): { program: Program; faculty: Faculty }[] {
  return getAllPrograms().filter(({ program }) => program.degree === degree);
}
