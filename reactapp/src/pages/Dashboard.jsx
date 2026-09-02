import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { api } from '../api/client'

export default function Dashboard() {
  const { user } = useAuth()
  const [studentCount, setStudentCount] = useState(null)
  const [teacherCount, setTeacherCount] = useState(null)
  const [below75, setBelow75] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      try {
        const [students, teachers, low] = await Promise.allSettled([
          api.getStudents(),
          api.getTeachers(),
          api.getBelow75(),
        ])
        if (students.status === 'fulfilled') setStudentCount(students.value.length)
        if (teachers.status === 'fulfilled') setTeacherCount(teachers.value.length)
        if (low.status === 'fulfilled') setBelow75(low.value.length)
      } catch (err) {
        setError(err.message)
      }
    }
    load()
  }, [])

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Welcome, {user?.username}</h1>
          <div className="breadcrumb">Home / Dashboard · Role: {user?.role}</div>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="stat-grid">
        <div className="stat-card">
          <div className="label">Total Students</div>
          <div className="value">{studentCount ?? '—'}</div>
        </div>
        <div className="stat-card">
          <div className="label">Total Teachers</div>
          <div className="value">{teacherCount ?? '—'}</div>
        </div>
        <div className="stat-card">
          <div className="label">Below 75% Attendance</div>
          <div className="value">{below75 ?? '—'}</div>
        </div>
      </div>

      <div className="card">
        <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: 13.5 }}>
          This dashboard pulls live data directly from your Spring Boot backend
          (<code>/api/students</code>, <code>/api/teachers</code>, <code>/api/attendance/below-75</code>).
          Use the sidebar to manage students, mark attendance, build the timetable, and handle leave requests.
        </p>
      </div>
    </div>
  )
}
