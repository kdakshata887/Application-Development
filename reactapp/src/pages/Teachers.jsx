import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus, IconTrash } from '../components/Icons'

function Toast({ toast }) {
  if (!toast) return null
  const bg = toast.type === 'error' ? 'var(--red)' : toast.type === 'warn' ? '#b45309' : 'var(--green)'
  return (
    <div style={{
      position: 'fixed', top: 20, right: 20, zIndex: 9999,
      background: bg, color: '#fff', padding: '12px 20px', borderRadius: 10,
      boxShadow: '0 4px 20px rgba(0,0,0,.3)', maxWidth: 380, whiteSpace: 'pre-line', fontSize: 14,
    }}>{toast.msg}</div>
  )
}

function ConfirmDialog({ count, onConfirm, onCancel, deleting, noun = 'record' }) {
  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div className="card" style={{ maxWidth: 420, width: '90%', padding: 28 }}>
        <h3 style={{ marginTop: 0 }}>Confirm Deletion</h3>
        <p style={{ color: 'var(--text-secondary)', margin: '12px 0 24px' }}>
          Are you sure you want to delete <strong>{count}</strong> {noun}(s)?
          {noun === 'teacher' && ' Teachers with active timetable assignments cannot be deleted.'}
          {noun === 'section' && ' Sections with enrolled students or active timetable entries cannot be deleted.'}
          {noun === 'subject' && ' Subjects used in active timetable entries cannot be deleted.'}
          {noun === 'room' && ' Rooms assigned to active timetable slots cannot be deleted.'}
        </p>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn btn-outline" onClick={onCancel}>Cancel</button>
          <button className="btn" style={{ background: 'var(--red)', borderColor: 'var(--red)' }}
            onClick={onConfirm} disabled={deleting}>
            {deleting ? 'Deleting…' : 'Yes, Delete'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function Teachers() {
  const [teachers, setTeachers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({
    username: '', password: '', name: '', employeeId: '',
    qualification: '', subjectSpecialization: '', email: '', mobileNumber: '',
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
      showToast('Teacher added successfully')
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  const allIds = teachers.map(t => t.teacherId)
  const allSelected = allIds.length > 0 && allIds.every(id => selected.has(id))
  function toggleAll() { setSelected(allSelected ? new Set() : new Set(allIds)) }
  function toggleOne(id) { const n = new Set(selected); n.has(id) ? n.delete(id) : n.add(id); setSelected(n) }

  async function handleBulkDelete() {
    setDeleting(true); setShowConfirm(false)
    try {
      const result = await api.bulkDeleteTeachers([...selected])
      setSelected(new Set())
      await load()
      if (result.failedCount === 0) showToast(`✓ ${result.deletedCount} teacher(s) deleted.`)
      else if (result.deletedCount === 0) showToast(result.failed.map(f => f.reason).join('\n'), 'error')
      else {
        showToast(`✓ ${result.deletedCount} deleted. ${result.failedCount} failed.`, 'warn')
        setError(result.failed.map(f => `ID ${f.id}: ${f.reason}`).join('\n'))
      }
    } catch (err) { showToast('Deletion failed: ' + err.message, 'error') }
    finally { setDeleting(false) }
  }

  return (
    <div>
      <Toast toast={toast} />
      <div className="topbar">
        <div><h1>Teachers</h1><div className="breadcrumb">Home / Teachers</div></div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {selected.size > 0 && (
            <button className="btn btn-outline" style={{ borderColor: 'var(--red)', color: 'var(--red)', display: 'flex', alignItems: 'center', gap: 6 }}
              onClick={() => setShowConfirm(true)} disabled={deleting}>
              <IconTrash width={15} height={15} /> Delete Selected ({selected.size})
            </button>
          )}
          <button className="btn" onClick={() => setShowForm(s => !s)}>
            {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Teacher</>}
          </button>
        </div>
      </div>

      {showConfirm && <ConfirmDialog count={selected.size} onConfirm={handleBulkDelete} onCancel={() => setShowConfirm(false)} deleting={deleting} noun="teacher" />}
      {error && <div className="error-banner" style={{ whiteSpace: 'pre-line' }}>{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Teacher</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              {[['Username', 'username', 'text'], ['Password', 'password', 'password'],
                ['Full Name', 'name', 'text'], ['Employee ID', 'employeeId', 'text'],
                ['Qualification', 'qualification', 'text'], ['Subject Specialization', 'subjectSpecialization', 'text'],
                ['Email', 'email', 'email'], ['Mobile', 'mobileNumber', 'tel']].map(([label, key, type]) => (
                <div key={key}>
                  <label className="label">{label}</label>
                  <input className="input" type={type} value={form[key]}
                    required={['username', 'password', 'name', 'employeeId'].includes(key)}
                    onChange={e => setForm({ ...form, [key]: e.target.value })} />
                </div>
              ))}
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>{submitting ? 'Saving…' : 'Save Teacher'}</button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : teachers.length === 0 ? <div className="empty-state">No teachers yet.</div>
          : (
            <>
              <div style={{ marginBottom: 10 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13, color: 'var(--text-secondary)' }}>
                  <input type="checkbox" checked={allSelected} onChange={toggleAll} />
                  Select All ({teachers.length})
                </label>
              </div>
              <table>
                <thead><tr><th style={{ width: 36 }}></th><th>Employee ID</th><th>Name</th><th>Specialization</th><th>Status</th></tr></thead>
                <tbody>
                  {teachers.map(t => (
                    <tr key={t.teacherId} style={{ background: selected.has(t.teacherId) ? 'rgba(99,102,241,.07)' : '' }}>
                      <td><input type="checkbox" checked={selected.has(t.teacherId)} onChange={() => toggleOne(t.teacherId)} /></td>
                      <td><code>{t.employeeId}</code></td>
                      <td>{t.name}</td>
                      <td>{t.subjectSpecialization || '—'}</td>
                      <td><span className={`badge ${t.isActive ? 'badge-active' : 'badge-absent'}`}>{t.isActive ? 'Active' : 'Inactive'}</span></td>
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
