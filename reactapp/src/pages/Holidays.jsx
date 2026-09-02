import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus, IconTrash } from '../components/Icons'

const HOLIDAY_TYPES = ['PUBLIC_HOLIDAY', 'SCHOOL_HOLIDAY', 'EXAM_HOLIDAY', 'EMERGENCY_CLOSURE']

export default function Holidays() {
  const [holidays, setHolidays] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ holidayName: '', date: '', holidayType: 'PUBLIC_HOLIDAY', description: '' })
  const [submitting, setSubmitting] = useState(false)

  async function load() {
    setLoading(true); setError('')
    try { setHolidays(await api.getHolidays()) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.createHoliday(form)
      setShowForm(false)
      setForm({ holidayName: '', date: '', holidayType: 'PUBLIC_HOLIDAY', description: '' })
      load()
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  async function handleDelete(id) {
    if (!window.confirm('Delete this holiday?')) return
    try { await api.deleteHoliday(id); load() }
    catch (err) { setError(err.message) }
  }

  const typeColor = {
    PUBLIC_HOLIDAY: 'badge-active', SCHOOL_HOLIDAY: 'badge-pending',
    EXAM_HOLIDAY: 'badge-late', EMERGENCY_CLOSURE: 'badge-absent'
  }

  return (
    <div>
      <div className="topbar">
        <div><h1>Holiday Calendar</h1><div className="breadcrumb">Home / Holidays</div></div>
        <button className="btn" onClick={() => setShowForm(s => !s)}>
          {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Holiday</>}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Holiday</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">Holiday Name</label>
                <input className="input" required value={form.holidayName}
                  placeholder="e.g. Republic Day"
                  onChange={e => setForm({ ...form, holidayName: e.target.value })} />
              </div>
              <div>
                <label className="label">Date</label>
                <input className="input" type="date" required value={form.date}
                  onChange={e => setForm({ ...form, date: e.target.value })} />
              </div>
              <div>
                <label className="label">Type</label>
                <select className="input" value={form.holidayType}
                  onChange={e => setForm({ ...form, holidayType: e.target.value })}>
                  {HOLIDAY_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
                </select>
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                <label className="label">Description (optional)</label>
                <input className="input" value={form.description}
                  onChange={e => setForm({ ...form, description: e.target.value })} />
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>
              {submitting ? 'Saving…' : 'Save Holiday'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : holidays.length === 0 ? <div className="empty-state">No holidays defined. Add school/public holidays above.</div>
          : (
            <table>
              <thead><tr><th>Date</th><th>Holiday</th><th>Type</th><th>Description</th><th></th></tr></thead>
              <tbody>
                {[...holidays].sort((a, b) => a.date.localeCompare(b.date)).map(h => (
                  <tr key={h.holidayId}>
                    <td style={{ fontWeight: 600 }}>{h.date}</td>
                    <td>{h.holidayName}</td>
                    <td><span className={`badge ${typeColor[h.holidayType] || 'badge-pending'}`}>{h.holidayType?.replace(/_/g, ' ')}</span></td>
                    <td style={{ color: 'var(--text-secondary)' }}>{h.description || '—'}</td>
                    <td><button className="btn-icon-danger" onClick={() => handleDelete(h.holidayId)}>×</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
      </div>
    </div>
  )
}
