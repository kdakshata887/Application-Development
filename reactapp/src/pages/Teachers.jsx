import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus, IconTrash } from '../components/Icons'

export default function Teachers() {
  const [teachers, setTeachers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({
    username: '', password: '', name: '', employeeId: '',
    qualification: '', subjectSpecialization: '', email: '', mobileNumber: '',
  })
  const [submitting, setSubmitting] = useState(false)

  async function load() {
    setLoading(true); setError('')
    try { setTeachers(await api.getTeachers()) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.register({ ...form, role: 'TEACHER' })
      setShowForm(false)
      setForm({ username: '', password: '', name: '', employeeId: '', qualification: '', subjectSpecialization: '', email: '', mobileNumber: '' })
      load()
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Teachers</h1>
          <div className="breadcrumb">Home / Teachers</div>
        </div>
        <button className="btn" onClick={() => setShowForm(s => !s)}>
          {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Teacher</>}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Teacher</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              {[
                ['Username', 'username', 'text'], ['Password', 'password', 'password'],
                ['Full Name', 'name', 'text'], ['Employee ID', 'employeeId', 'text'],
                ['Qualification', 'qualification', 'text'], ['Subject Specialization', 'subjectSpecialization', 'text'],
                ['Email', 'email', 'email'], ['Mobile', 'mobileNumber', 'tel'],
              ].map(([label, key, type]) => (
                <div key={key}>
                  <label className="label">{label}</label>
                  <input className="input" type={type} value={form[key]}
                    required={['username','password','name','employeeId'].includes(key)}
                    onChange={e => setForm({ ...form, [key]: e.target.value })} />
                </div>
              ))}
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>
              {submitting ? 'Saving…' : 'Save Teacher'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : teachers.length === 0 ? <div className="empty-state">No teachers yet. Add your first one above.</div>
          : (
            <table>
              <thead><tr><th>Employee ID</th><th>Name</th><th>Specialization</th><th>Status</th></tr></thead>
              <tbody>
                {teachers.map(t => (
                  <tr key={t.teacherId}>
                    <td><code>{t.employeeId}</code></td>
                    <td>{t.name}</td>
                    <td>{t.subjectSpecialization || '—'}</td>
                    <td><span className={`badge ${t.isActive ? 'badge-active' : 'badge-absent'}`}>{t.isActive ? 'Active' : 'Inactive'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
      </div>
    </div>
  )
}
