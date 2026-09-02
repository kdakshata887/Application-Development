# School Timetable and Attendance Management System — Backend

Spring Boot 3 / Java 17 REST API implementing the SRS and database design supplied
(`SRS_06_School_Timetable_and_Attendance_Management_System.docx`,
`DATABASE_DESIGN_School_Timetable_Attendance.pdf`), matched to the EduTrack
frontend mockups.

## Tech stack

- Java 17, Spring Boot 3.2.5
- Spring Web, Spring Data JPA, Spring Security
- JWT authentication (jjwt) with role-based access control
- BCrypt password hashing
- MySQL (default) — H2 in-memory profile available for a zero-setup run
- Lombok, Bean Validation

## Package layout (`com.examly.springapp`)

```
model        -> JPA entities (User, Student, Teacher, ClassSection, Subject, Room,
                Timetable, Attendance, LeaveApplication, BiometricDevice,
                HolidayCalendar, Notification + enums)
repository   -> Spring Data JPA repositories for every entity
service      -> Service interfaces + ValidationUtil
service/impl -> Service implementations (business rules, clash detection,
                attendance override, leave/substitution workflow, lockout policy)
controller   -> REST controllers, one per entity + AuthController
dto          -> Request/response payloads (Login, Register, Timetable, Attendance,
                Leave, ApiResponse)
security     -> JwtUtil, JwtAuthenticationFilter, UserPrincipal,
                CustomUserDetailsService
config       -> SecurityConfig (JWT filter chain, CORS, BCrypt), DataSeeder
exception    -> InvalidNameException, InvalidPhoneException,
                TimetableClashException, AttendanceOverrideException,
                ResourceNotFoundException, DuplicateResourceException,
                AccountLockedException, InvalidCredentialsException,
                GlobalExceptionHandler
```

## Getting started in VS Code

1. Install the **Extension Pack for Java** and **Spring Boot Extension Pack**.
2. Open this folder in VS Code.
3. Create the database (MySQL default):
   ```sql
   CREATE DATABASE school_timetable_attendance_db;
   ```
   Edit `src/main/resources/application.properties` with your MySQL
   username/password, **or** comment the MySQL block and uncomment the H2
   block for an instant in-memory database (no install needed).
4. Run `SpringappApplication.java` (Run ▶ in VS Code, or `mvn spring-boot:run`).
5. The API starts on `http://localhost:8080`.

A default admin account is seeded automatically on first run:
```
username: admin
password: Admin@123
```

## Authentication flow

1. `POST /api/auth/register` — register a user (STUDENT/TEACHER/PARENT/
   CLASS_TEACHER/PRINCIPAL/ADMIN). Student/Teacher registrations also create
   the linked `Student`/`Teacher` profile row.
2. `POST /api/auth/login` — returns a JWT (`Authorization: Bearer <token>`).
   Token lifetime follows FR2: 8h student/parent, 12h teacher, 24h admin/principal.
3. Every other endpoint requires the bearer token; access is enforced per role
   with `@PreAuthorize` (FR3 — Role-Based Access Control).
4. Progressive lockout (FR2): 5 failed attempts → 15 min lock, 10 → 30 min,
   15 → 24 hours.

## Key endpoints

| Area | Endpoint | Notes |
|---|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` | public |
| Users | `/api/users/**` | admin/principal manage all users |
| Students | `/api/students/**` | includes `/section/{id}`, `/parent/{id}` |
| Teachers | `/api/teachers/**`, `/api/teachers/{id}/substitutes?day=&period=` | substitute suggestion engine (FR6) |
| Sections | `/api/sections/**` | class-section CRUD |
| Subjects | `/api/subjects/**` | subject CRUD |
| Rooms | `/api/rooms/**` | room CRUD |
| Timetable | `/api/timetables/**` | clash-detected create/update (FR4), version increments on edit |
| Attendance | `POST /api/attendance/mark`, `PUT /api/attendance/{id}/override`, `/student/{id}`, `/student/{id}/percentage`, `/below-75` | manual override requires `modificationReason` (FR10), percentage & below-75% list (FR9) |
| Leave | `POST /api/leaves`, `PUT /api/leaves/{id}/approve`, `/reject`, `/pending` | principal approval + substitute assignment (FR6) |
| Devices | `/api/devices/**` | biometric device health/status (FR5) |
| Holidays | `/api/holidays/**`, `/year/{academicYear}` | academic calendar (FR12) |
| Notifications | `POST /api/notifications`, `/user/{id}` | absence/below-75/leave/announcement channels (FR7) |

## Custom exceptions (FR10)

`InvalidNameException`, `InvalidPhoneException`, `TimetableClashException`,
`AttendanceOverrideException` are thrown with the exact user-facing messages
specified in the SRS and mapped to proper HTTP status codes by
`GlobalExceptionHandler` — no stack traces are ever exposed to clients.

## Notes / extension points

- `NotificationServiceImpl` has a clearly marked integration point for wiring
  a real SMS gateway / email service / push provider.
- MIS/DISE XML export, WebSocket live dashboards, and the constraint-solver
  timetable auto-generator are intentionally left as extension points — the
  clash-detection rules they depend on (teacher/section/room double-booking)
  are already implemented in `TimetableServiceImpl` and can be built on
  directly.
- Attendance data model supports the 30-day freeze / audit-trail rules in the
  SRS; wire a scheduled job against `Attendance.modifiedAt` if you need to
  enforce the freeze window automatically.
