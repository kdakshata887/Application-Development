import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconRefresh, IconDownload, IconChart } from '../components/Icons'

export default function Analytics() {
  const [overview, setOverview] = useState(null)
  const [sectionData, setSectionData] = useState([])
  const [dayData, setDayData] = useState([])
  const [teacherWorkload, setTeacherWorkload] = useState([])
  const [utilization, setUtilization] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true); setError('')
    try {
      const [ov, sec, day, tw, util] = await Promise.all([
        api.getAttendanceOverview().catch(() => null),
        api.getAttendanceBySection().catch(() => []),
        api.getAttendanceByDayOfWeek().catch(() => []),
        api.getTeacherWorkload().catch(() => []),
        api.getTimetableUtilization().catch(() => null),
      ])
      setOverview(ov); setSectionData(sec); setDayData(day)
      setTeacherWorkload(tw); setUtilization(util)
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const statColor = (pct) => pct >= 90 ? 'var(--green)' : pct >= 75 ? 'var(--amber)' : 'var(--red)'

  return (
    <div>
      <div className="topbar">
        <div><h1>Analytics</h1><div className="breadcrumb">Home / Analytics</div></div>
        <button className="btn btn-outline" onClick={load}><IconRefresh width={16} height={16} /> Refresh</button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? <p style={{ color: 'var(--text-secondary)', padding: 24 }}>Loading analytics…</p> : (
        <>
          {/* Attendance Overview */}
          {overview && (
            <div className="stats-grid" style={{ marginBottom: 24 }}>
              <div className="stat-card">
                <div className="stat-label">Total Students</div>
                <div className="stat-value">{overview.totalStudents}</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Avg Attendance</div>
                <div className="stat-value" style={{ color: statColor(overview.averageAttendancePercent) }}>
                  {overview.averageAttendancePercent}%
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Below 75% Alert</div>
                <div className="stat-value" style={{ color: 'var(--red)' }}>{overview.below75Count}</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Above Threshold</div>
                <div className="stat-value" style={{ color: 'var(--green)' }}>{overview.above75Count}</div>
              </div>
            </div>
          )}

          {/* Timetable Utilization */}
          {utilization && (
            <div className="card" style={{ marginBottom: 24 }}>
              <h3 style={{ marginTop: 0 }}>Timetable Utilization</h3>
              <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
                <div>
                  <div className="stat-label">Filled Slots</div>
                  <div className="stat-value">{utilization.filledSlots}</div>
                </div>
                <div>
                  <div className="stat-label">Sections with Timetable</div>
                  <div className="stat-value">{utilization.sectionsWithTimetable} / {utilization.totalSections}</div>
                </div>
                <div>
                  <div className="stat-label">Utilization</div>
                  <div className="stat-value" style={{ color: statColor(utilization.utilizationPercent) }}>
                    {utilization.utilizationPercent}%
                  </div>
                </div>
              </div>
              {/* Simple utilization bar */}
              <div style={{ marginTop: 12, height: 8, background: 'var(--bg-app)', borderRadius: 4, overflow: 'hidden' }}>
                <div style={{
                  height: '100%', width: `${utilization.utilizationPercent}%`,
                  background: 'linear-gradient(90deg, var(--accent), var(--green))', borderRadius: 4,
                  transition: 'width 0.8s ease'
                }} />
              </div>
            </div>
          )}

          {/* Section Attendance */}
          {sectionData.length > 0 && (
            <div className="card" style={{ marginBottom: 24 }}>
              <h3 style={{ marginTop: 0 }}>Attendance by Section</h3>
              <table>
                <thead><tr><th>Section</th><th>Students</th><th>Avg Attendance</th><th>Visual</th></tr></thead>
                <tbody>
                  {sectionData.map(s => (
                    <tr key={s.sectionId}>
                      <td>{s.sectionName}</td>
                      <td>{s.studentCount}</td>
                      <td style={{ color: statColor(s.averageAttendance), fontWeight: 600 }}>{s.averageAttendance}%</td>
                      <td style={{ minWidth: 120 }}>
                        <div style={{ height: 6, background: 'var(--bg-app)', borderRadius: 3, overflow: 'hidden' }}>
                          <div style={{
                            height: '100%', width: `${s.averageAttendance}%`,
                            background: statColor(s.averageAttendance), borderRadius: 3
                          }} />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Day of Week */}
          {dayData.length > 0 && (
            <div className="card" style={{ marginBottom: 24 }}>
              <h3 style={{ marginTop: 0 }}>Attendance by Day of Week</h3>
              <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', height: 120, padding: '0 4px' }}>
                {dayData.filter(d => d.dayOfWeek !== 'SATURDAY' && d.dayOfWeek !== 'SUNDAY').map(d => (
                  <div key={d.dayOfWeek} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                    <span style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 600 }}>{d.attendancePercent || 0}%</span>
                    <div style={{
                      width: '100%', background: 'var(--accent)', borderRadius: '4px 4px 0 0',
                      height: `${Math.max((d.attendancePercent || 0) * 0.8, 4)}px`,
                      opacity: 0.85, transition: 'height 0.5s ease'
                    }} />
                    <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>{d.dayOfWeek.slice(0,3)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Teacher Workload */}
          {teacherWorkload.length > 0 && (
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Teacher Workload</h3>
              <table>
                <thead><tr><th>Teacher</th><th>Employee ID</th><th>Scheduled Periods/Week</th><th>Subjects</th></tr></thead>
                <tbody>
                  {teacherWorkload.map(t => (
                    <tr key={t.teacherId}>
                      <td>{t.teacherName}</td>
                      <td><code>{t.employeeId}</code></td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span style={{ fontWeight: 700, color: t.scheduledPeriodsPerWeek > 30 ? 'var(--red)' : 'var(--text-primary)' }}>
                            {t.scheduledPeriodsPerWeek}
                          </span>
                          <div style={{ height: 5, width: 60, background: 'var(--bg-app)', borderRadius: 3, overflow: 'hidden' }}>
                            <div style={{
                              height: '100%', width: `${Math.min((t.scheduledPeriodsPerWeek / 40) * 100, 100)}%`,
                              background: 'var(--accent)', borderRadius: 3
                            }} />
                          </div>
                        </div>
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>{(t.subjectsAssigned || []).join(', ') || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  )
}
