# Proposal: 官网真实性增强与申请系统

## Context

用户反馈官网需要更强的真实性，主要问题：
1. 学生登录链接到了真实注册页面（应该隐蔽）
2. 专业数量太少（只有4个）
3. 缺少完整的申请流程
4. 缺少排课、招生计划等内容
5. 校园生活内容不够丰富

## 核心原则

**真实入口 vs 模拟入口**
- 真实入口：注册码注册系统 → 完全隐蔽，不在官网任何地方出现
- 模拟入口：官网申请系统 → 完整专业流程，但永远不会通过审核

## 需求分析

### 1. 登录系统重构

| 入口 | 类型 | 位置 | 功能 |
|------|------|------|------|
| 学生门户 | 模拟 | 官网导航 | 展示用，登录后显示"系统维护中" |
| 教职工门户 | 模拟 | 官网导航 | 展示用，登录后显示"系统维护中" |
| 真实注册 | 真实 | 隐蔽URL | 注册码注册，实际功能 |

### 2. 院系与专业结构

参考波兰真实大学结构，设计6个学院，每个学院4-6个专业：

**Faculty of Computer Science and Mathematics**
- Computer Science (BSc, MSc)
- Data Science and Analytics (BSc, MSc)
- Cybersecurity (BSc)
- Software Engineering (MSc)
- Artificial Intelligence (MSc)

**Faculty of Business and Economics**
- Business Administration (BSc, MSc)
- International Business (BSc)
- Finance and Accounting (BSc, MSc)
- Marketing and Management (BSc)
- Economics (BSc, MSc)

**Faculty of Engineering**
- Mechanical Engineering (BSc, MSc)
- Electrical Engineering (BSc, MSc)
- Civil Engineering (BSc)
- Environmental Engineering (BSc)
- Biomedical Engineering (MSc)

**Faculty of Medicine and Health Sciences**
- Medicine (MD, 6-year program)
- Nursing (BSc)
- Pharmacy (MSc)
- Public Health (BSc, MSc)

**Faculty of Law and Administration**
- Law (LLB, LLM)
- European Law (LLM)
- Public Administration (BSc)
- International Relations (BSc, MSc)

**Faculty of Arts and Humanities**
- English Philology (BA, MA)
- Psychology (BSc, MSc)
- History (BA, MA)
- Philosophy (BA)
- Cultural Studies (BA)

### 3. 申请流程设计

**Step 1: Account Creation**
- Email address
- Password
- Email verification

**Step 2: Personal Information**
- Full name (as in passport)
- Date of birth
- Place of birth
- Nationality
- Gender
- Passport/ID number
- Address (permanent and correspondence)
- Phone number
- Emergency contact

**Step 3: Educational Background**
- Previous education level
- School/University name
- Country of education
- Graduation date
- GPA/Grades
- Upload: Transcripts, Diploma

**Step 4: Language Proficiency**
- Native language
- English proficiency (IELTS/TOEFL/Cambridge)
- Polish proficiency (if applicable)
- Upload: Language certificates

**Step 5: Program Selection**
- Degree level (Bachelor/Master/Doctoral)
- Faculty
- Program (1st and 2nd choice)
- Study mode (Full-time/Part-time)
- Start semester

**Step 6: Supporting Documents**
- Motivation letter
- CV/Resume
- Passport photo
- Passport copy
- Medical certificate (for Medicine)
- Portfolio (for Arts)

**Step 7: Declaration & Submit**
- Terms and conditions
- Data processing consent
- Declaration of authenticity
- Application fee payment (simulated)

**Final: Application Submitted**
- Application reference number
- "Under review" status
- Email confirmation sent
- "We will contact you within 4-6 weeks"

### 4. 新增页面

| 页面 | 路径 | 内容 |
|------|------|------|
| 学院列表 | /faculties | 6个学院介绍 |
| 学院详情 | /faculties/[slug] | 专业列表、教授、研究 |
| 专业详情 | /programs/[slug] | 课程设置、入学要求、学费 |
| 招生计划 | /admissions/plan | 招生人数、时间表、要求 |
| 学期日历 | /academic-calendar | 详细学期安排 |
| 课程表示例 | /academics/schedule | 示例课程表 |
| 教职工招聘 | /careers | 招聘信息 |
| 学生门户登录 | /student-portal | 模拟登录页 |
| 教职工门户登录 | /staff-portal | 模拟登录页 |
| 在线申请 | /apply | 完整申请流程 |

### 5. 时间线一致性

**学校历史**
- 1998: 成立
- 2003: 首批国际学生
- 2008: 研究中心
- 2012: Erasmus+
- 2018: 20周年
- 2024: 数字化

**学年安排 (2025-2026)**
- Fall 2025: Oct 1 - Feb 7
- Spring 2026: Feb 17 - Jun 26
- Summer 2026: Jul 1 - Aug 31

**招生周期**
- Fall intake: Application deadline June 30
- Spring intake: Application deadline November 30

## 实施优先级

1. **P0**: 移除真实注册入口链接
2. **P0**: 创建模拟学生/教职工门户登录
3. **P1**: 扩展专业和学院结构
4. **P1**: 创建完整申请流程
5. **P2**: 添加招生计划、课程表等详细内容
6. **P2**: 丰富校园生活内容
7. **P3**: 教职工招聘页面
