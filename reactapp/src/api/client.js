// Central API client for all backend calls.
// In dev, CRA proxies /api/... requests to http://localhost:8082 (configured via "proxy" in package.json).
// In production, set REACT_APP_API_BASE_URL to the backend origin.

const API_BASE = process.env.REACT_APP_API_BASE_URL
  ? `${process.env.REACT_APP_API_BASE_URL}/api`
  : '/api'

export function getToken() {
  return localStorage.getItem('edutrack_token')
}

function clearSession() {
  localStorage.removeItem('edutrack_token')
  localStorage.removeItem('edutrack_user')
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }

  let res
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    // Network failure — backend is not reachable
    const err = new Error('Cannot reach the server. Please check your connection or try again later.')
    err.status = 0
    throw err
  }

  // Safely parse response — backend may return empty body on 204, 401, etc.
  const contentType = res.headers.get('content-type') || ''
  const isJson = contentType.includes('application/json')
  let data = null
  if (isJson) {
    try {
      data = await res.json()
    } catch {
      data = null
    }
  }

  if (!res.ok) {
    // 401 on an authenticated call → token expired/invalid, force re-login
    if (res.status === 401 && auth) {
      clearSession()
      window.location.href = '/login'
      return
    }

    // 403 → authorization failure, do NOT clear session, just surface the error
    if (res.status === 403) {
      const err = new Error('You do not have permission to perform this action.')
      err.status = 403
      err.body = data
      throw err
    }

    // 404 → resource not found
    if (res.status === 404) {
      const message = data?.message || 'The requested resource was not found.'
      const err = new Error(message)
      err.status = 404
      err.body = data
      throw err
    }

    // 409 → conflict / duplicate
    if (res.status === 409) {
      const message = data?.message || 'A duplicate or conflicting record already exists.'
      const err = new Error(message)
      err.status = 409
      err.body = data
      throw err
    }

    // 400 → validation / bad request — extract validation field errors if present
    if (res.status === 400) {
      let message = data?.message
      if (!message && data?.errors) {
        // Backend returns { errors: { field: "msg", ... } } for @Valid failures
        message = Object.values(data.errors).join(' | ')
      }
      message = message || 'Invalid request. Please check your input.'
      const err = new Error(message)
      err.status = 400
      err.body = data
      throw err
    }

    // 500+ → generic server error
    if (res.status >= 500) {
      const err = new Error('A server error occurred. Please try again later.')
      err.status = res.status
      err.body = data
      throw err
    }

    // Fallback for other statuses
    const message = data?.message || data?.error || `Request failed (${res.status})`
    const err = new Error(message)
    err.status = res.status
    err.body = data
    throw err
  }

  return data
}

// Download helper — uses fetch with auth header and handles file responses correctly.
// Returns a blob URL, or throws with a clean message on error.
export async function downloadReport(path) {
  const token = getToken()
  let res
  try {
    res = await fetch(`${API_BASE}${path}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
  } catch {
    throw new Error('Cannot reach the server. Please check your connection.')
  }

  if (!res.ok) {
    if (res.status === 401) {
      clearSession()
      window.location.href = '/login'
      return
    }
    if (res.status === 403) throw new Error('You do not have permission to download this report.')
    if (res.status === 404) throw new Error('Report data not found.')
    throw new Error(`Download failed (${res.status}). Please try again.`)
  }

  const blob = await res.blob()
  return URL.createObjectURL(blob)
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
  bulkDeleteStudents: (ids) => request('/students/bulk-delete', { method: 'POST', body: { ids } }),

  // ─── Teachers ────────────────────────────────────────────────────────────
  getTeachers: () => request('/teachers'),
  getTeacherById: (id) => request(`/teachers/${id}`),
  createTeacher: (payload) => request('/auth/register', { method: 'POST', body: { ...payload, role: 'TEACHER' } }),
  updateTeacher: (id, payload) => request(`/teachers/${id}`, { method: 'PUT', body: payload }),
  bulkDeleteTeachers: (ids) => request('/teachers/bulk-delete', { method: 'POST', body: { ids } }),

  // ─── Class Sections ──────────────────────────────────────────────────────
  getSections: () => request('/sections'),
  createSection: (payload) => request('/sections', { method: 'POST', body: payload }),
  updateSection: (id, payload) => request(`/sections/${id}`, { method: 'PUT', body: payload }),
  deleteSection: (id) => request(`/sections/${id}`, { method: 'DELETE' }),
  bulkDeleteSections: (ids) => request('/sections/bulk-delete', { method: 'POST', body: { ids } }),

  // ─── Subjects ────────────────────────────────────────────────────────────
  getSubjects: () => request('/subjects'),
  createSubject: (payload) => request('/subjects', { method: 'POST', body: payload }),
  updateSubject: (id, payload) => request(`/subjects/${id}`, { method: 'PUT', body: payload }),
  deleteSubject: (id) => request(`/subjects/${id}`, { method: 'DELETE' }),
  bulkDeleteSubjects: (ids) => request('/subjects/bulk-delete', { method: 'POST', body: { ids } }),

  // ─── Rooms ───────────────────────────────────────────────────────────────
  getRooms: () => request('/rooms'),
  createRoom: (payload) => request('/rooms', { method: 'POST', body: payload }),
  updateRoom: (id, payload) => request(`/rooms/${id}`, { method: 'PUT', body: payload }),
  deleteRoom: (id) => request(`/rooms/${id}`, { method: 'DELETE' }),
  bulkDeleteRooms: (ids) => request('/rooms/bulk-delete', { method: 'POST', body: { ids } }),

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

  // ─── Reports — paths only (use downloadReport() helper to fetch with auth) ──
  reportPaths: {
    below75: '/reports/attendance/below75',
    attendanceSummary: '/reports/attendance/summary',
    teacherDeployment: '/reports/teacher/deployment',
    leaveSummary: '/reports/leave/summary',
    timetable: '/reports/timetable',
  },

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
