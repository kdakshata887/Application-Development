import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus } from '../components/Icons'

export default function Students() {
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({
    username: '', password: '', name: '', admissionNumber: '',
    sectionId: '', dateOfBirth: '',
  })
  const [submitting, setSubmitting] = useState(false)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const data = await api.getStudents()
      setStudents(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await api.register({
        ...form,
        role: 'STUDENT',
        sectionId: form.sectionId ? Number(form.sectionId) : undefined,
      })
      setShowForm(false)
      setForm({ username: '', password: '', name: '', admissionNumber: '', sectionId: '', dateOfBirth: '' })
      load()
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
          <h1>Students</h1>
          <div className="breadcrumb">Home / Students</div>
        </div>
        <button className="btn" onClick={() => setShowForm((s) => !s)}>
          {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Student</>}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">Username</label>
                <input className="input" required value={form.username}
                  onChange={(e) => setForm({ ...form, username: e.target.value })} />
              </div>
              <div>
                <label className="label">Password</label>
                <input className="input" type="password" required value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })} />
              </div>
              <div>
                <label className="label">Full Name</label>
                <input className="input" required value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div>
                <label className="label">Admission Number</label>
                <input className="input" required value={form.admissionNumber}
                  onChange={(e) => setForm({ ...form, admissionNumber: e.target.value })} />
              </div>
              <div>
                <label className="label">Section ID</label>
                <input className="input" type="number" required value={form.sectionId}
                  onChange={(e) => setForm({ ...form, sectionId: e.target.value })} />
              </div>
              <div>
                <label className="label">Date of Birth</label>
                <input className="input" type="date" value={form.dateOfBirth}
                  onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
              </div>
            </div>
            <button className="btn" disabled={submitting}>
              {submitting ? 'Saving…' : 'Save Student'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? (
          <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
        ) : students.length === 0 ? (
          <div className="empty-state">No students yet. Add your first one above.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Admission No.</th>
                <th>Name</th>
                <th>Section</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.studentId}>
                  <td>{s.admissionNumber}</td>
                  <td>{s.name}</td>
                  <td>{s.sectionId}</td>
                  <td><span className="badge badge-active">{s.isActive ? 'Active' : 'Inactive'}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
