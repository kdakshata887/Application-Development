import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus, IconTrash } from '../components/Icons'

export default function Students() {
  const [students, setStudents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({
    username: '', password: '', name: '', admissionNumber: '',
    sectionId: '', dateOfBirth: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [selected, setSelected] = useState(new Set())
  const [showConfirm, setShowConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 4000)
  }

  async function load() {
    setLoading(true); setError('')
    try { setStudents(await api.getStudents()) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.register({ ...form, role: 'STUDENT', sectionId: form.sectionId ? Number(form.sectionId) : undefined })
      setShowForm(false)
      setForm({ username: '', password: '', name: '', admissionNumber: '', sectionId: '', dateOfBirth: '' })
      load()
      showToast('Student added successfully')
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  const allIds = students.map(s => s.studentId)
  const allSelected = allIds.length > 0 && allIds.every(id => selected.has(id))

  function toggleAll() {
    if (allSelected) setSelected(new Set())
    else setSelected(new Set(allIds))
  }

  function toggleOne(id) {
    const next = new Set(selected)
    next.has(id) ? next.delete(id) : next.add(id)
    setSelected(next)
  }

  async function handleBulkDelete() {
    setDeleting(true); setShowConfirm(false)
    try {
      const result = await api.bulkDeleteStudents([...selected])
      setSelected(new Set())
      await load()
      if (result.failedCount === 0) {
        showToast(`✓ ${result.deletedCount} student(s) deleted successfully.`)
      } else if (result.deletedCount === 0) {
        showToast(`Could not delete selected students:\n${result.failed.map(f => f.reason).join('\n')}`, 'error')
      } else {
        showToast(`✓ ${result.deletedCount} deleted. ${result.failedCount} could not be deleted — see details below.`, 'warn')
        setError(result.failed.map(f => `ID ${f.id}: ${f.reason}`).join('\n'))
      }
    } catch (err) {
      showToast('Deletion failed: ' + err.message, 'error')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Students</h1>
          <div className="breadcrumb">Home / Students</div>
        </div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {selected.size > 0 && (
            <button className="btn btn-outline" style={{ borderColor: 'var(--red)', color: 'var(--red)', display: 'flex', alignItems: 'center', gap: 6 }}
              onClick={() => setShowConfirm(true)} disabled={deleting}>
              <IconTrash width={15} height={15} />
              Delete Selected ({selected.size})
            </button>
          )}
          <button className="btn" onClick={() => setShowForm(s => !s)}>
            {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Student</>}
          </button>
        </div>
      </div>

      {toast && (
        <div style={{
          position: 'fixed', top: 20, right: 20, zIndex: 9999,
          background: toast.type === 'error' ? 'var(--red)' : toast.type === 'warn' ? '#b45309' : 'var(--green)',
          color: '#fff', padding: '12px 20px', borderRadius: 10,
          boxShadow: '0 4px 20px rgba(0,0,0,.3)', maxWidth: 380, whiteSpace: 'pre-line', fontSize: 14,
        }}>{toast.msg}</div>
      )}

      {error && <div className="error-banner" style={{ whiteSpace: 'pre-line' }}>{error}</div>}

      {/* Confirmation Dialog */}
      {showConfirm && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="card" style={{ maxWidth: 420, width: '90%', padding: 28 }}>
            <h3 style={{ marginTop: 0 }}>Confirm Deletion</h3>
            <p style={{ color: 'var(--text-secondary)', margin: '12px 0 24px' }}>
              Are you sure you want to delete <strong>{selected.size}</strong> student(s)?
              Students with attendance records cannot be deleted and will be reported separately.
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button className="btn btn-outline" onClick={() => setShowConfirm(false)}>Cancel</button>
              <button className="btn" style={{ background: 'var(--red)', borderColor: 'var(--red)' }}
                onClick={handleBulkDelete} disabled={deleting}>
                {deleting ? 'Deleting…' : 'Yes, Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              {[['Username', 'username'], ['Password', 'password'], ['Full Name', 'name'], ['Admission Number', 'admissionNumber']].map(([label, key]) => (
                <div key={key}>
                  <label className="label">{label}</label>
                  <input className="input" required type={key === 'password' ? 'password' : 'text'}
                    value={form[key]} onChange={e => setForm({ ...form, [key]: e.target.value })} />
                </div>
              ))}
              <div>
                <label className="label">Section ID</label>
                <input className="input" type="number" required value={form.sectionId}
                  onChange={e => setForm({ ...form, sectionId: e.target.value })} />
              </div>
              <div>
                <label className="label">Date of Birth</label>
                <input className="input" type="date" value={form.dateOfBirth}
                  onChange={e => setForm({ ...form, dateOfBirth: e.target.value })} />
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>
              {submitting ? 'Saving…' : 'Save Student'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : students.length === 0 ? <div className="empty-state">No students yet. Add your first one above.</div>
          : (
            <>
              {students.length > 0 && (
                <div style={{ marginBottom: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13, color: 'var(--text-secondary)' }}>
                    <input type="checkbox" checked={allSelected} onChange={toggleAll} />
                    Select All ({students.length})
                  </label>
                </div>
              )}
              <table>
                <thead>
                  <tr>
                    <th style={{ width: 36 }}></th>
                    <th>Admission No.</th>
                    <th>Name</th>
                    <th>Section</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {students.map(s => (
                    <tr key={s.studentId} style={{ background: selected.has(s.studentId) ? 'rgba(99,102,241,.07)' : '' }}>
                      <td>
                        <input type="checkbox" checked={selected.has(s.studentId)}
                          onChange={() => toggleOne(s.studentId)} />
                      </td>
                      <td>{s.admissionNumber}</td>
                      <td>{s.name}</td>
                      <td>{s.section?.sectionName || s.sectionId || '—'}</td>
                      <td><span className={`badge ${s.isActive ? 'badge-active' : 'badge-absent'}`}>{s.isActive ? 'Active' : 'Inactive'}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}
      </div>
    </div>
  )
}
