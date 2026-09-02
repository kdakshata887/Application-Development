import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus, IconTrash } from '../components/Icons'

export default function Sections() {
  const [sections, setSections] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ sectionName: '', gradeLevel: '', classTeacherId: '' })
  const [submitting, setSubmitting] = useState(false)

  async function load() {
    setLoading(true); setError('')
    try { setSections(await api.getSections()) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.createSection({
        sectionName: form.sectionName,
        gradeLevel: form.gradeLevel,
        classTeacherId: form.classTeacherId ? Number(form.classTeacherId) : undefined,
      })
      setShowForm(false)
      setForm({ sectionName: '', gradeLevel: '', classTeacherId: '' })
      load()
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Class Sections</h1>
          <div className="breadcrumb">Home / Sections</div>
        </div>
        <button className="btn" onClick={() => setShowForm(s => !s)}>
          {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Section</>}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Section</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">Section Name</label>
                <input className="input" required value={form.sectionName}
                  placeholder="e.g. 10-A"
                  onChange={e => setForm({ ...form, sectionName: e.target.value })} />
              </div>
              <div>
                <label className="label">Grade Level</label>
                <input className="input" value={form.gradeLevel}
                  placeholder="e.g. 10"
                  onChange={e => setForm({ ...form, gradeLevel: e.target.value })} />
              </div>
              <div>
                <label className="label">Class Teacher ID (optional)</label>
                <input className="input" type="number" value={form.classTeacherId}
                  onChange={e => setForm({ ...form, classTeacherId: e.target.value })} />
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>
              {submitting ? 'Saving…' : 'Save Section'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : sections.length === 0 ? <div className="empty-state">No sections yet.</div>
          : (
            <table>
              <thead><tr><th>ID</th><th>Section Name</th><th>Grade</th><th>Class Teacher</th></tr></thead>
              <tbody>
                {sections.map(s => (
                  <tr key={s.sectionId}>
                    <td><code>{s.sectionId}</code></td>
                    <td>{s.sectionName}</td>
                    <td>{s.gradeLevel || '—'}</td>
                    <td>{s.classTeacher ? s.classTeacher.name : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
      </div>
    </div>
  )
}
