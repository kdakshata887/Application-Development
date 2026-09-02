# EduTrack Frontend

A React + Vite frontend for the EduTrack School Timetable & Attendance backend.
Connects to your real Spring Boot API — no mock data.

## Requirements
- Node.js 18+ (check with `node -v`)
- Your Spring Boot backend running on `http://localhost:8081`

## Setup

```
npm install
npm run dev
```

Open the URL it prints (usually `http://localhost:5173`).

## How it connects to the backend

`vite.config.js` proxies every `/api/...` request to `http://localhost:8081`
during development. So the frontend code just calls things like
`/api/students`, and Vite quietly forwards them to your real backend —
no CORS issues, no hardcoded ports scattered through the code.

If your backend runs on a different port, change the `target` value in
`vite.config.js`.

## Login

Use your real backend credentials, e.g. `admin` / `Admin@123` (or whatever
you registered). The JWT is stored in `localStorage` and attached to every
subsequent request automatically.

## Pages included

- **Login** — real authentication against `/api/auth/login`
- **Dashboard** — live counts from `/api/students`, `/api/teachers`, `/api/attendance/below-75`
- **Students** — list + register new students (`/api/students`, `/api/auth/register`)
- **Attendance** — mark attendance and look up a student's records
- **Timetable** — create entries and view a section's weekly schedule
- **Leave Management** — apply for leave (teacher) and approve (admin)

Note: some actions require a specific role's token (e.g. only a TEACHER can
apply for leave, only ADMIN/PRINCIPAL can approve it) — the backend enforces
this via `@PreAuthorize`, so log in as the appropriate role when testing
each action.
