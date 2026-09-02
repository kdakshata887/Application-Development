import { useEffect, useState } from 'react'
import { api } from '../api/client'

export default function ParentPortal() {
  const [children, setChildren] = useState([])
  const [selectedChild, setSelectedChild] = useState(null)
  const [attendance, setAttendance] = useState([])
  const [percentage, setPercentage] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      try {
        const c = await api.getMyChildren()
        setChildren(c)
        if (c.length > 0) {
          setSelectedChild(c[0])
          await loadChildData(c[0].studentId)
        }
      } catch (err) { setError(err.message) }
      finally { setLoading(false) }
    }
    load()
  }, [])

  async function loadChildData(studentId) {
    try {
      const [att, pct] = await Promise.all([
        api.getChildAttendance(studentId),
        api.getChildAttendancePercentage(studentId),
      ])
      setAttendance(att)
      setPercentage(pct)
    } catch (err) { setError(err.message) }
  }

  async function switchChild(child) {
    setSelectedChild(child)
    setAttendance([]); setPercentage(null)
    await loadChildData(child.studentId)
  }

  const statusColor = { PRESENT: 'var(--green)', ABSENT: 'var(--red)', LATE: 'var(--amber)' }
  const pctValue = percentage?.attendancePercentage ?? 0
  const pctColor = pctValue >= 90 ? 'var(--green)' : pctValue >= 75 ? 'var(--amber)' : 'var(--red)'

  return (
    <div>
      <div className="topbar">
        <div><h1>Parent Portal</h1><div className="breadcrumb">My Children & Attendance</div></div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p> : (
        children.length === 0 ? (
          <div className="card">
            <div className="empty-state">No children linked to your account. Please contact the administrator.</div>
          </div>
        ) : (
          <>
            {/* Child selector tabs */}
            {children.length > 1 && (
              <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                {children.map(c => (
                  <button key={c.studentId}
                    className={`btn ${selectedChild?.studentId === c.studentId ? '' : 'btn-outline'}`}
                    onClick={() => switchChild(c)}>
                    {c.name}
                  </button>
                ))}
              </div>
            )}

            {selectedChild && (
              <>
                {/* Child info + percentage */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 20 }}>
                  <div className="card">
                    <h3 style={{ marginTop: 0 }}>Student Information</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                      {[
                        ['Name', selectedChild.name],
                        ['Admission No.', selectedChild.admissionNumber],
                        ['Section', selectedChild.section?.sectionName || '—'],
                        ['Date of Birth', selectedChild.dateOfBirth || '—'],
                      ].map(([label, val]) => (
                        <div key={label} style={{ display: 'flex', gap: 12 }}>
                          <span style={{ color: 'var(--text-secondary)', minWidth: 110, fontSize: 13 }}>{label}</span>
                          <span style={{ fontWeight: 600 }}>{val}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                  {percentage && (
                    <div className="card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                      <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 8 }}>Attendance Percentage</div>
                      <div style={{ fontSize: 56, fontWeight: 800, color: pctColor, lineHeight: 1 }}>{pctValue}%</div>
                      <div style={{ fontSize: 12, color: 'var(--text-faint)', marginTop: 8 }}>
                        {percentage.presentDays} present / {percentage.totalRecordedDays} days recorded
                      </div>
                      {pctValue < 75 && (
                        <div style={{ marginTop: 12, padding: '6px 12px', background: 'var(--red-soft)', color: 'var(--red)', borderRadius: 8, fontSize: 12 }}>
                          ⚠ Below 75% threshold — may face attendance shortage
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Attendance records */}
                <div className="card">
                  <h3 style={{ marginTop: 0 }}>Attendance Records ({attendance.length} entries)</h3>
                  {attendance.length === 0 ? <div className="empty-state">No attendance records found.</div> : (
                    <table>
                      <thead><tr><th>Date</th><th>Status</th><th>Subject</th><th>Marked By</th></tr></thead>
                      <tbody>
                        {[...attendance].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 30).map(a => (
                          <tr key={a.attendanceId}>
                            <td>{a.date}</td>
                            <td><span style={{ color: statusColor[a.status], fontWeight: 700 }}>{a.status}</span></td>
                            <td>{a.subject?.subjectName || '—'}</td>
                            <td style={{ color: 'var(--text-secondary)' }}>{a.capturedBy}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                  {attendance.length > 30 && (
                    <div style={{ textAlign: 'center', color: 'var(--text-faint)', fontSize: 12, marginTop: 8 }}>
                      Showing last 30 of {attendance.length} records
                    </div>
                  )}
                </div>
              </>
            )}
          </>
        )
      )}
    </div>
  )
}
