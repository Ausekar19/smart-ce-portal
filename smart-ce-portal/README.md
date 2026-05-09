# Smart CE Portal

**Continuous Evaluation MCQ Exam System**  
Fergusson College, Pune — Estd. 1885

---

## Tech Stack

| Layer      | Technology                             |
|------------|----------------------------------------|
| Backend    | Spring Boot 3.2 (Java 17)             |
| Security   | Spring Security 6 (BCrypt)            |
| ORM        | Spring Data JPA + Hibernate           |
| Database   | MySQL 8+                              |
| Templates  | Thymeleaf 3 + Bootstrap 5.3           |
| Reports    | Apache POI (Excel export)             |
| Build      | Maven                                 |

---

## Quick Start

### 1. Prerequisites

- Java 17+
- MySQL 8+
- Maven 3.8+  (or use Spring Tool Suite's built-in Maven)

### 2. Database Setup

```sql
CREATE DATABASE smart_ce_portal;
```

> Hibernate will auto-create all tables on first run (`ddl-auto=update`).

### 3. Configure Database Credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smart_ce_portal?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Kolkata
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

### 4. Import in Spring Tool Suite

1. **File → Import → Maven → Existing Maven Projects**
2. Browse to the extracted `smart-ce-portal` folder
3. Click **Finish** – STS will download dependencies automatically
4. Right-click project → **Run As → Spring Boot App**
5. Open `http://localhost:8080`

### 5. Default Admin Login

| Field    | Value     |
|----------|-----------|
| Username | `admin`   |
| Password | `admin123`|

> Created automatically on first startup by `DataInitializer.java`.

---

## User Roles & Flows

```
Login Page
├── Student Register  → /register/student
├── Teacher Register  → /register/teacher
└── Login            → redirects by role:
    ├── STUDENT  → /student/dashboard
    ├── TEACHER  → /teacher/dashboard
    └── ADMIN    → /admin/dashboard
```

---

## Key Features

### Students
- **Dashboard** shows:
  - Upcoming tests with scheduled date, time, duration, marks
  - Completed tests with date given, score, status
- **Exam Interface**:
  - Question navigator sidebar
  - Countdown timer (auto-submits when time ends)
  - **Tab switch prevention** — each switch is recorded via POST to server
  - Warning modal shown after each switch
  - Auto-submit after N violations (configurable, default = 3)
  - Right-click, copy-paste, keyboard shortcuts all blocked

### Teachers
- Create / Edit / Delete tests with scheduled date+time
- Add MCQ questions (A/B/C/D + correct answer + marks)
- View results table for each test
- Export results as Excel (.xlsx) with tab-switch counts

### Admin
- View all students, teachers, tests
- Enable / disable / delete users
- Add teacher accounts directly

---

## Configuration

`application.properties` key settings:

```properties
# Max tab switches before auto-submit
app.exam.max-tab-switches=3

# Server port
server.port=8080
```

---

## Project Structure

```
src/main/java/com/fergusson/ceportal/
├── config/
│   ├── SecurityConfig.java        ← Spring Security rules
│   └── DataInitializer.java       ← Seeds default admin
├── controller/
│   ├── AuthController.java        ← Login, Register pages
│   ├── StudentController.java     ← Dashboard, Exam, Tab-switch API
│   ├── TeacherController.java     ← Tests, Questions, Results
│   └── AdminController.java       ← User management
├── model/
│   ├── User.java
│   ├── ExamTest.java
│   ├── Question.java
│   ├── TestAttempt.java           ← Tracks tab switches
│   └── StudentAnswer.java
├── repository/                    ← Spring Data JPA interfaces
├── service/
│   ├── ExamService.java           ← Core business logic + tab-switch
│   ├── UserService.java
│   ├── ExcelExportService.java    ← Apache POI report
│   └── CustomUserDetailsService.java

src/main/resources/
├── templates/
│   ├── layout.html                ← Shared navbar/footer fragments
│   ├── login.html
│   ├── register-student.html
│   ├── register-teacher.html
│   ├── student/
│   │   ├── dashboard.html         ← Upcoming + past tests
│   │   ├── exam.html              ← Exam UI + tab prevention JS
│   │   └── result.html
│   ├── teacher/
│   │   ├── dashboard.html
│   │   ├── create-test.html
│   │   ├── questions.html
│   │   └── results.html
│   └── admin/
│       ├── dashboard.html
│       ├── users.html
│       └── new-teacher.html
├── static/css/style.css
└── application.properties
```

---

## Tab Switch Prevention — How It Works

1. JS listens to `document.visibilitychange`
2. When tab goes hidden → POST to `/student/exam/{attemptId}/tabswitch`
3. Server increments `TestAttempt.tabSwitchCount` in DB
4. If count ≥ `maxAllowed` → `autoSubmitted = true`, exam scored immediately
5. Front-end shows warning modal with remaining count
6. All violations saved to DB → visible in teacher's results table + Excel export

---

## Points to Remember (from requirements)

1. **Tab switching is blocked at the browser AND server level** — even if JS is bypassed, the server auto-submits on the Nth call.
2. **Exam schedule** is shown to students immediately after login — upcoming tests sorted by date, past tests with scores.
3. **Teacher login & registration** have dedicated buttons on the login page (separate from student).
4. **Teacher dashboard** is fully independent from admin — teachers manage their own tests and questions.
