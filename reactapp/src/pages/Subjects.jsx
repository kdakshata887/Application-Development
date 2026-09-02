import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus } from '../components/Icons'

export default function Subjects() {
  const [subjects, setSubjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ subjectName: '', subjectCode: '', isLab: false })
  const [submitting, setSubmitting] = useState(false)

  async function load() {
    setLoading(true); setError('')
    try { setSubjects(await api.getSubjects()) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.createSubject(form)
      setShowForm(false)
      setForm({ subjectName: '', subjectCode: '', isLab: false })
      load()
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  return (
    <div>
      <div className="topbar">
        <div><h1>Subjects</h1><div className="breadcrumb">Home / Subjects</div></div>
        <button className="btn" onClick={() => setShowForm(s => !s)}>
          {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Subject</>}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Subject</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: 12, alignItems: 'end' }}>
              <div>
                <label className="label">Subject Name</label>
                <input className="input" required value={form.subjectName}
                  placeholder="e.g. Mathematics"
                  onChange={e => setForm({ ...form, subjectName: e.target.value })} />
              </div>
              <div>
                <label className="label">Subject Code</label>
                <input className="input" value={form.subjectCode}
                  placeholder="e.g. MATH-10"
                  onChange={e => setForm({ ...form, subjectCode: e.target.value })} />
              </div>
              <div style={{ paddingBottom: 14 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input type="checkbox" checked={form.isLab}
                    onChange={e => setForm({ ...form, isLab: e.target.checked })} />
                  <span className="label" style={{ margin: 0 }}>Lab Subject</span>
                </label>
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>
              {submitting ? 'Saving…' : 'Save Subject'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : subjects.length === 0 ? <div className="empty-state">No subjects yet.</div>
          : (
            <table>
              <thead><tr><th>ID</th><th>Subject Name</th><th>Code</th><th>Type</th></tr></thead>
              <tbody>
                {subjects.map(s => (
                  <tr key={s.subjectId}>
                    <td><code>{s.subjectId}</code></td>
                    <td>{s.subjectName}</td>
                    <td><code>{s.subjectCode || '—'}</code></td>
                    <td><span className={`badge ${s.isLab ? 'badge-late' : 'badge-active'}`}>{s.isLab ? 'Lab' : 'Theory'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
      </div>
    </div>
  )
}
