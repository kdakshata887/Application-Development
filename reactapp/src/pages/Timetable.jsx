import { useState, useEffect } from 'react'
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
                <label className="label">Section</label>
                <AsyncSelect
                  fetcher={api.getSections}
                  value={form.sectionId}
                  onChange={v => setForm({ ...form, sectionId: v })}
                  getKey={s => s.sectionId}
                  getLabel={s => `${s.sectionName}${s.className ? ` (${s.className})` : ''}`}
                  placeholder="Select section…"
                  required
                />
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
                <label className="label">Subject</label>
                <AsyncSelect
                  fetcher={api.getSubjects}
                  value={form.subjectId}
                  onChange={v => setForm({ ...form, subjectId: v })}
                  getKey={s => s.subjectId}
                  getLabel={s => `${s.subjectName}${s.subjectCode ? ` (${s.subjectCode})` : ''}`}
                  placeholder="Select subject…"
                  required
                />
              </div>
              <div>
                <label className="label">Teacher</label>
                <AsyncSelect
                  fetcher={api.getTeachers}
                  value={form.teacherId}
                  onChange={v => setForm({ ...form, teacherId: v })}
                  getKey={t => t.teacherId}
                  getLabel={t => `${t.name}${t.employeeId ? ` (${t.employeeId})` : ''}`}
                  placeholder="Select teacher…"
                  required
                />
              </div>
              <div>
                <label className="label">Room</label>
                <AsyncSelect
                  fetcher={api.getRooms}
                  value={form.roomId}
                  onChange={v => setForm({ ...form, roomId: v })}
                  getKey={r => r.roomId}
                  getLabel={r => `${r.roomName} (${r.roomType})`}
                  placeholder="Select room…"
                  required
                />
              </div>
            </div>
            <div style={{ marginTop: 12 }}>
              <label className="label">Effective From</label>
              <input className="input" type="date" value={form.effectiveFrom}
                onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })} />
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>
              {submitting ? 'Creating…' : 'Create Entry'}
            </button>
          </form>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>View Section Timetable</h3>
          <form onSubmit={handleLoad} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <div style={{ flex: 1 }}>
              <AsyncSelect
                fetcher={api.getSections}
                value={sectionId}
                onChange={v => setSectionId(v)}
                getKey={s => s.sectionId}
                getLabel={s => `${s.sectionName}${s.className ? ` (${s.className})` : ''}`}
                placeholder="Select section…"
              />
            </div>
            <button className="btn" type="submit" style={{ marginBottom: 0 }}>Load</button>
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
                      <td>{t.subject?.subjectName || t.subjectId}</td>
                      <td>{t.teacher?.name || t.teacherId}</td>
                      <td>{t.room?.roomName || t.roomId}</td>
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

// ── Reusable lazy-loading select ──────────────────────────────────────────────
// Fetches options once on mount, shows a loading state,
// then renders a <select> with the provided key/label mappers.
function AsyncSelect({ fetcher, value, onChange, getKey, getLabel, placeholder, required, style }) {
  const [options, setOptions] = useState(null)
  const [loadErr, setLoadErr] = useState('')

  useEffect(() => {
    let cancelled = false
    fetcher()
      .then(data => { if (!cancelled) setOptions(data) })
      .catch(err => { if (!cancelled) setLoadErr(err.message) })
    return () => { cancelled = true }
  }, [])  // eslint-disable-line react-hooks/exhaustive-deps

  if (loadErr) return <div className="input" style={{ color: 'var(--red)', ...style }}>⚠ {loadErr}</div>
  if (!options) return <div className="input" style={{ color: 'var(--text-secondary)', ...style }}>Loading…</div>

  return (
    <select
      className="input"
      value={value}
      required={required}
      style={style}
      onChange={e => onChange(e.target.value)}
    >
      <option value="">{placeholder || '— Select —'}</option>
      {options.map(opt => (
        <option key={getKey(opt)} value={getKey(opt)}>
          {getLabel(opt)}
        </option>
      ))}
    </select>
  )
}
