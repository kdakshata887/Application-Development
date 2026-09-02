// Central API client for all backend calls.
// In dev, requests go to "/api/..." and vite.config.js proxies them to
// http://localhost:8081, so this file never needs to know the port.

function getToken() {
  return localStorage.getItem('edutrack_token')
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const isJson = res.headers.get('content-type')?.includes('application/json')
  const data = isJson ? await res.json() : null

  if (!res.ok) {
    const message = data?.message || data?.error || `Request failed (${res.status})`
    const err = new Error(message)
    err.status = res.status
    err.body = data
    throw err
  }
  return data
}

export const api = {
  // ─── Auth ────────────────────────────────────────────────────────────────
  login: (username, password) =>
    request('/auth/login', { method: 'POST', body: { username, password }, auth: false }),
  register: (payload) =>
    request('/auth/register', { method: 'POST', body: payload }),

  // ─── Students ────────────────────────────────────────────────────────────
  getStudents: () => request('/students'),
  getStudentById: (id) => request(`/students/${id}`),
  createStudent: (payload) => request('/students', { method: 'POST', body: payload }),
  updateStudent: (id, payload) => request(`/students/${id}`, { method: 'PUT', body: payload }),
  deleteStudent: (id) => request(`/students/${id}`, { method: 'DELETE' }),

  // ─── Teachers ────────────────────────────────────────────────────────────
  getTeachers: () => request('/teachers'),
  getTeacherById: (id) => request(`/teachers/${id}`),
  createTeacher: (payload) => request('/auth/register', { method: 'POST', body: { ...payload, role: 'TEACHER' } }),

  // ─── Class Sections ──────────────────────────────────────────────────────
  getSections: () => request('/sections'),
  createSection: (payload) => request('/sections', { method: 'POST', body: payload }),
  updateSection: (id, payload) => request(`/sections/${id}`, { method: 'PUT', body: payload }),
  deleteSection: (id) => request(`/sections/${id}`, { method: 'DELETE' }),

  // ─── Subjects ────────────────────────────────────────────────────────────
  getSubjects: () => request('/subjects'),
  createSubject: (payload) => request('/subjects', { method: 'POST', body: payload }),
  updateSubject: (id, payload) => request(`/subjects/${id}`, { method: 'PUT', body: payload }),
  deleteSubject: (id) => request(`/subjects/${id}`, { method: 'DELETE' }),

  // ─── Rooms ───────────────────────────────────────────────────────────────
  getRooms: () => request('/rooms'),
  createRoom: (payload) => request('/rooms', { method: 'POST', body: payload }),
  updateRoom: (id, payload) => request(`/rooms/${id}`, { method: 'PUT', body: payload }),
  deleteRoom: (id) => request(`/rooms/${id}`, { method: 'DELETE' }),

  // ─── Timetable ───────────────────────────────────────────────────────────
  getAllTimetables: () => request('/timetables'),
  getTimetableBySection: (sectionId) => request(`/timetables/section/${sectionId}`),
  getTimetableByTeacher: (teacherId) => request(`/timetables/teacher/${teacherId}`),
  createTimetable: (payload) => request('/timetables', { method: 'POST', body: payload }),
  updateTimetable: (id, payload) => request(`/timetables/${id}`, { method: 'PUT', body: payload }),
  deleteTimetable: (id) => request(`/timetables/${id}`, { method: 'DELETE' }),

  // ─── Timetable Generation ─────────────────────────────────────────────────
  generateTimetableForSection: (sectionId, workingDays, periodsPerDay) =>
    request(`/timetables/generate/${sectionId}?workingDays=${workingDays}&periodsPerDay=${periodsPerDay}`, { method: 'POST' }),
  generateTimetableForAll: (workingDays, periodsPerDay) =>
    request(`/timetables/generate?workingDays=${workingDays}&periodsPerDay=${periodsPerDay}`, { method: 'POST' }),
  getClashReport: () => request('/timetables/clashes'),

  // ─── Subject-Teacher Mappings ─────────────────────────────────────────────
  getMappings: () => request('/subject-teacher-mappings'),
  getMappingsBySection: (sectionId) => request(`/subject-teacher-mappings/section/${sectionId}`),
  createMapping: (payload) => request('/subject-teacher-mappings', { method: 'POST', body: payload }),
  deleteMapping: (id) => request(`/subject-teacher-mappings/${id}`, { method: 'DELETE' }),

  // ─── Attendance ──────────────────────────────────────────────────────────
  markAttendance: (payload) => request('/attendance/mark', { method: 'POST', body: payload }),
  overrideAttendance: (id, payload) => request(`/attendance/${id}/override`, { method: 'PUT', body: payload }),
  getAttendanceByStudent: (studentId) => request(`/attendance/student/${studentId}`),
  getAttendanceByStudentRange: (studentId, from, to) =>
    request(`/attendance/student/${studentId}/range?from=${from}&to=${to}`),
  getAttendanceBySectionDate: (sectionId, date) =>
    request(`/attendance/section/${sectionId}?date=${date}`),
  getAttendancePercentage: (studentId) => request(`/attendance/student/${studentId}/percentage`),
  getBelow75: () => request('/attendance/below-75'),

  // ─── Leaves ──────────────────────────────────────────────────────────────
  applyLeave: (payload) => request('/leaves', { method: 'POST', body: payload }),
  getPendingLeaves: () => request('/leaves/pending'),
  getAllLeaves: () => request('/leaves'),
  getLeavesByTeacher: (teacherId) => request(`/leaves/teacher/${teacherId}`),
  approveLeave: (leaveId, payload) =>
    request(`/leaves/${leaveId}/approve`, { method: 'PUT', body: payload }),
  rejectLeave: (leaveId, payload) =>
    request(`/leaves/${leaveId}/reject`, { method: 'PUT', body: payload }),
  getSubstituteTeachers: (teacherId, fromDate, toDate) =>
    request(`/leaves/substitutes?teacherId=${teacherId}&fromDate=${fromDate}&toDate=${toDate}`),

  // ─── Holidays ────────────────────────────────────────────────────────────
  getHolidays: () => request('/holidays'),
  createHoliday: (payload) => request('/holidays', { method: 'POST', body: payload }),
  updateHoliday: (id, payload) => request(`/holidays/${id}`, { method: 'PUT', body: payload }),
  deleteHoliday: (id) => request(`/holidays/${id}`, { method: 'DELETE' }),

  // ─── Academic Years ───────────────────────────────────────────────────────
  getAcademicYears: () => request('/academic-years'),
  getActiveAcademicYear: () => request('/academic-years/active'),
  createAcademicYear: (payload) => request('/academic-years', { method: 'POST', body: payload }),
  activateAcademicYear: (id) => request(`/academic-years/${id}/activate`, { method: 'PUT' }),

  // ─── Biometric Devices ────────────────────────────────────────────────────
  getDevices: () => request('/devices'),
  createDevice: (payload) => request('/devices', { method: 'POST', body: payload }),
  syncDevice: (deviceId) => request(`/devices/${deviceId}/sync`, { method: 'POST' }),
  getDeviceHealth: (deviceId) => request(`/devices/${deviceId}/health`),

  // ─── Notifications ───────────────────────────────────────────────────────
  getMyNotifications: (userId) => request(`/notifications/user/${userId}`),
  markNotificationRead: (id) => request(`/notifications/${id}/read`, { method: 'PUT' }),
  getAllNotifications: () => request('/notifications'),

  // ─── Analytics ───────────────────────────────────────────────────────────
  getAttendanceOverview: () => request('/analytics/attendance/overview'),
  getAttendanceBySection: () => request('/analytics/attendance/by-section'),
  getAttendanceByDayOfWeek: () => request('/analytics/attendance/by-day-of-week'),
  getTeacherWorkload: () => request('/analytics/teacher-workload'),
  getTimetableUtilization: () => request('/analytics/timetable/utilization'),

  // ─── Reports ─────────────────────────────────────────────────────────────
  downloadBelow75Report: () => '/api/reports/attendance/below75',
  downloadAttendanceSummary: () => '/api/reports/attendance/summary',
  downloadTeacherDeployment: () => '/api/reports/teacher/deployment',
  downloadLeaveSummary: () => '/api/reports/leave/summary',
  downloadTimetable: () => '/api/reports/timetable',

  // ─── Users (Admin) ───────────────────────────────────────────────────────
  getAllUsers: () => request('/users'),
  getUserById: (id) => request(`/users/${id}`),
  updateUser: (id, payload) => request(`/users/${id}`, { method: 'PUT', body: payload }),
  deactivateUser: (id) => request(`/users/${id}/deactivate`, { method: 'PUT' }),

  // ─── Parent Portal ───────────────────────────────────────────────────────
  getMyChildren: () => request('/parent/children'),
  getChildAttendance: (studentId) => request(`/parent/children/${studentId}/attendance`),
  getChildAttendancePercentage: (studentId) => request(`/parent/children/${studentId}/percentage`),
  getParentNotifications: () => request('/parent/notifications'),
}

export { getToken }

