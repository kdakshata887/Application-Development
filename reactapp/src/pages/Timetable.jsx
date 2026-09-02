import { useState } from 'react'
import { api } from '../api/client'

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

export default function Timetable() {
  const [sectionId, setSectionId] = useState('')
  const [entries, setEntries] = useState(null)
  const [error, setError] = useState('')

  const [form, setForm] = useState({
    sectionId: '', day: 'MON', period: 1, subjectId: '', teacherId: '', roomId: '',
    effectiveFrom: new Date().toISOString().slice(0, 10),
  })
  const [submitting, setSubmitting] = useState(false)
  const [createResult, setCreateResult] = useState(null)

  async function handleLoad(e) {
    e.preventDefault()
    setError('')
    setEntries(null)
    try {
      const data = await api.getTimetableBySection(sectionId)
      setEntries(data)
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleCreate(e) {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    setCreateResult(null)
    try {
      const payload = {
        ...form,
        sectionId: Number(form.sectionId),
        period: Number(form.period),
        subjectId: Number(form.subjectId),
        teacherId: Number(form.teacherId),
        roomId: Number(form.roomId),
      }
      const data = await api.createTimetable(payload)
      setCreateResult(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Timetable</h1>
          <div className="breadcrumb">Home / Timetable</div>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Create Timetable Entry</h3>
          {createResult && (
            <div className="error-banner" style={{ background: 'var(--green-bg)', color: 'var(--green)', borderColor: 'var(--green)' }}>
              Created — Timetable ID {createResult.timetableId}
            </div>
          )}
          <form onSubmit={handleCreate}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">Section ID</label>
                <input className="input" type="number" required value={form.sectionId}
                  onChange={(e) => setForm({ ...form, sectionId: e.target.value })} />
              </div>
              <div>
                <label className="label">Day</label>
                <select className="input" value={form.day}
                  onChange={(e) => setForm({ ...form, day: e.target.value })}>
                  {DAYS.map((d) => <option key={d} value={d}>{d}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Period (1–8)</label>
                <input className="input" type="number" min={1} max={8} required value={form.period}
                  onChange={(e) => setForm({ ...form, period: e.target.value })} />
              </div>
              <div>
                <label className="label">Subject ID</label>
                <input className="input" type="number" required value={form.subjectId}
                  onChange={(e) => setForm({ ...form, subjectId: e.target.value })} />
              </div>
              <div>
                <label className="label">Teacher ID</label>
                <input className="input" type="number" required value={form.teacherId}
                  onChange={(e) => setForm({ ...form, teacherId: e.target.value })} />
              </div>
              <div>
                <label className="label">Room ID</label>
                <input className="input" type="number" required value={form.roomId}
                  onChange={(e) => setForm({ ...form, roomId: e.target.value })} />
              </div>
            </div>
            <label className="label">Effective From</label>
            <input className="input" type="date" value={form.effectiveFrom}
              onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} />
            <button className="btn" disabled={submitting}>
              {submitting ? 'Creating…' : 'Create Entry'}
            </button>
          </form>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>View Section Timetable</h3>
          <form onSubmit={handleLoad} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <input className="input" style={{ marginBottom: 0 }} placeholder="Section ID"
              value={sectionId} onChange={(e) => setSectionId(e.target.value)} />
            <button className="btn btn-secondary" type="submit">Load</button>
          </form>

          {entries && (
            entries.length === 0 ? (
              <div className="empty-state">No timetable entries for this section yet.</div>
            ) : (
              <table>
                <thead><tr><th>Day</th><th>Period</th><th>Subject</th><th>Teacher</th><th>Room</th></tr></thead>
                <tbody>
                  {entries.map((t) => (
                    <tr key={t.timetableId}>
                      <td>{t.day}</td>
                      <td>{t.period}</td>
                      <td>{t.subjectId}</td>
                      <td>{t.teacherId}</td>
                      <td>{t.roomId}</td>
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
