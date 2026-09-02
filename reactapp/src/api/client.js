// Central place for every backend call.
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
  login: (username, password) =>
    request('/auth/login', { method: 'POST', body: { username, password }, auth: false }),
  register: (payload) =>
    request('/auth/register', { method: 'POST', body: payload }),

  getStudents: () => request('/students'),
  createStudent: (payload) => request('/students', { method: 'POST', body: payload }),

  getTeachers: () => request('/teachers'),

  getSections: () => request('/sections'),
  createSection: (payload) => request('/sections', { method: 'POST', body: payload }),

  getSubjects: () => request('/subjects'),
  createSubject: (payload) => request('/subjects', { method: 'POST', body: payload }),

  getRooms: () => request('/rooms'),
  createRoom: (payload) => request('/rooms', { method: 'POST', body: payload }),

  getTimetableBySection: (sectionId) => request(`/timetables/section/${sectionId}`),
  createTimetable: (payload) => request('/timetables', { method: 'POST', body: payload }),

  markAttendance: (payload) => request('/attendance/mark', { method: 'POST', body: payload }),
  getAttendanceByStudent: (studentId) => request(`/attendance/student/${studentId}`),
  getAttendancePercentage: (studentId) => request(`/attendance/student/${studentId}/percentage`),
  getBelow75: () => request('/attendance/below-75'),

  applyLeave: (payload) => request('/leaves', { method: 'POST', body: payload }),
  getPendingLeaves: () => request('/leaves/pending'),
  approveLeave: (leaveId, payload) =>
    request(`/leaves/${leaveId}/approve`, { method: 'PUT', body: payload }),
}

export { getToken }
