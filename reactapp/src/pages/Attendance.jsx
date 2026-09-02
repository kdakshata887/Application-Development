import { useState } from 'react'
import { api } from '../api/client'

const STATUS_OPTIONS = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED']

export default function Attendance() {
  const [form, setForm] = useState({
    studentId: '', date: new Date().toISOString().slice(0, 10),
    status: 'PRESENT', capturedBy: 'MANUAL', modificationReason: '',
  })
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [lookupId, setLookupId] = useState('')
  const [records, setRecords] = useState(null)
  const [lookupError, setLookupError] = useState('')

  async function handleMark(e) {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    setResult(null)
    try {
      const payload = { ...form, studentId: Number(form.studentId) }
      const data = await api.markAttendance(payload)
      setResult(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleLookup(e) {
    e.preventDefault()
    setLookupError('')
    setRecords(null)
    try {
      const data = await api.getAttendanceByStudent(lookupId)
      setRecords(data)
    } catch (err) {
      setLookupError(err.message)
    }
  }

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Attendance</h1>
          <div className="breadcrumb">Home / Attendance</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Mark Attendance</h3>
          {error && <div className="error-banner">{error}</div>}
          {result && (
            <div className="error-banner" style={{ background: 'var(--green-bg)', color: 'var(--green)', borderColor: 'var(--green)' }}>
              Saved — Attendance ID {result.attendanceId}, status {result.status}
            </div>
          )}
          <form onSubmit={handleMark}>
            <label className="label">Student ID</label>
            <input className="input" type="number" required value={form.studentId}
              onChange={(e) => setForm({ ...form, studentId: e.target.value })} />

            <label className="label">Date</label>
            <input className="input" type="date" required value={form.date}
              onChange={(e) => setForm({ ...form, date: e.target.value })} />

            <label className="label">Status</label>
            <select className="input" value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value })}>
              {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>

            <label className="label">Captured By</label>
            <select className="input" value={form.capturedBy}
              onChange={(e) => setForm({ ...form, capturedBy: e.target.value })}>
              <option value="MANUAL">MANUAL</option>
              <option value="BIOMETRIC">BIOMETRIC</option>
            </select>

            {form.capturedBy === 'MANUAL' && (
              <>
                <label className="label">Reason (required for manual entries)</label>
                <input className="input" required value={form.modificationReason}
                  onChange={(e) => setForm({ ...form, modificationReason: e.target.value })} />
              </>
            )}

            <button className="btn" disabled={submitting}>
              {submitting ? 'Saving…' : 'Mark Attendance'}
            </button>
          </form>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Lookup Student Attendance</h3>
          {lookupError && <div className="error-banner">{lookupError}</div>}
          <form onSubmit={handleLookup} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <input className="input" style={{ marginBottom: 0 }} placeholder="Student ID"
              value={lookupId} onChange={(e) => setLookupId(e.target.value)} />
            <button className="btn btn-secondary" type="submit">Search</button>
          </form>

          {records && (
            records.length === 0 ? (
              <div className="empty-state">No attendance records for this student.</div>
            ) : (
              <table>
                <thead><tr><th>Date</th><th>Status</th><th>Captured By</th></tr></thead>
                <tbody>
                  {records.map((r) => (
                    <tr key={r.attendanceId}>
                      <td>{r.date}</td>
                      <td><span className={`badge badge-${r.status?.toLowerCase()}`}>{r.status}</span></td>
                      <td>{r.capturedBy}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
          )}
        </div>
      </div>
    </div>
  )
}
