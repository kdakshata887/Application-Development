// Small hand-drawn icon set (no external icon package needed —
// keeps the project dependency-free and installable offline).
const base = { width: 18, height: 18, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' }

export const IconDashboard = (p) => (
  <svg {...base} {...p}><rect x="3" y="3" width="7" height="9" rx="1.5" /><rect x="14" y="3" width="7" height="5" rx="1.5" /><rect x="14" y="12" width="7" height="9" rx="1.5" /><rect x="3" y="16" width="7" height="5" rx="1.5" /></svg>
)
export const IconStudents = (p) => (
  <svg {...base} {...p}><circle cx="12" cy="8" r="3.2" /><path d="M5 20c0-3.5 3-6 7-6s7 2.5 7 6" /></svg>
)
export const IconAttendance = (p) => (
  <svg {...base} {...p}><rect x="3.5" y="4.5" width="17" height="16" rx="2" /><path d="M8 3v3M16 3v3M3.5 9.5h17" /><path d="M8.5 14l2 2 4-4" /></svg>
)
export const IconTimetable = (p) => (
  <svg {...base} {...p}><rect x="3.5" y="4.5" width="17" height="16" rx="2" /><path d="M3.5 10h17M9 4.5V21" /></svg>
)
export const IconLeave = (p) => (
  <svg {...base} {...p}><path d="M4 12c0-4.4 3.6-8 8-8s8 3.6 8 8-3.6 8-8 8" /><path d="M4 12l3-2M4 12l3 2" /><path d="M12 8v4l3 2" /></svg>
)
export const IconLogout = (p) => (
  <svg {...base} {...p}><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" /><path d="M16 17l5-5-5-5" /><path d="M21 12H9" /></svg>
)
export const IconSun = (p) => (
  <svg {...base} {...p}><circle cx="12" cy="12" r="4.2" /><path d="M12 2v2.4M12 19.6V22M4.9 4.9l1.7 1.7M17.4 17.4l1.7 1.7M2 12h2.4M19.6 12H22M4.9 19.1l1.7-1.7M17.4 6.6l1.7-1.7" /></svg>
)
export const IconMoon = (p) => (
  <svg {...base} {...p}><path d="M20 14.5A8.5 8.5 0 119.5 4a7 7 0 0010.5 10.5z" /></svg>
)
export const IconSlate = (p) => (
  <svg {...base} {...p}><circle cx="12" cy="12" r="9" /><path d="M12 3a9 9 0 000 18 9 9 0 010-18z" fill="currentColor" stroke="none" /></svg>
)
export const IconPlus = (p) => (
  <svg {...base} {...p}><path d="M12 5v14M5 12h14" /></svg>
)
export const IconSearch = (p) => (
  <svg {...base} {...p}><circle cx="11" cy="11" r="7" /><path d="M21 21l-4.3-4.3" /></svg>
)
export const IconCheck = (p) => (
  <svg {...base} {...p}><path d="M5 12l5 5 9-11" /></svg>
)
