import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus, IconTrash, IconEdit } from '../components/Icons'

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

export default function Subjects() {
  const [subjects, setSubjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ subjectName: '', subjectCode: '', isLab: false })
  const [submitting, setSubmitting] = useState(false)
  const [selected, setSelected] = useState(new Set())
  const [showConfirm, setShowConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)

  // Edit state
  const [editSubject, setEditSubject] = useState(null)
  const [editForm, setEditForm] = useState({ subjectName: '', subjectCode: '', isLab: false })
  const [editSubmitting, setEditSubmitting] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 4000)
  }

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
      setShowForm(false); setForm({ subjectName: '', subjectCode: '', isLab: false })
      load(); showToast('Subject added successfully')
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  const allIds = subjects.map(s => s.subjectId)
  const allSelected = allIds.length > 0 && allIds.every(id => selected.has(id))
  function toggleAll() { setSelected(allSelected ? new Set() : new Set(allIds)) }
  function toggleOne(id) { const n = new Set(selected); n.has(id) ? n.delete(id) : n.add(id); setSelected(n) }

  function openEdit(subject) {
    setEditSubject(subject)
    setEditForm({ subjectName: subject.subjectName || '', subjectCode: subject.subjectCode || '', isLab: subject.isLab ?? false })
  }
  function closeEdit() { setEditSubject(null) }

  async function handleEdit(e) {
    e.preventDefault(); setEditSubmitting(true); setError('')
    try {
      await api.updateSubject(editSubject.subjectId, editForm)
      closeEdit(); load(); showToast('Subject updated successfully')
    } catch (err) { setError(err.message) }
    finally { setEditSubmitting(false) }
  }

  async function handleBulkDelete() {
    setDeleting(true); setShowConfirm(false)
    try {
      const result = await api.bulkDeleteSubjects([...selected])
      setSelected(new Set()); await load()
      if (result.failedCount === 0) showToast(`✓ ${result.deletedCount} subject(s) deleted.`)
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
        <div><h1>Subjects</h1><div className="breadcrumb">Home / Subjects</div></div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {selected.size > 0 && (
            <button className="btn btn-outline" style={{ borderColor: 'var(--red)', color: 'var(--red)', display: 'flex', alignItems: 'center', gap: 6 }}
              onClick={() => setShowConfirm(true)} disabled={deleting}>
              <IconTrash width={15} height={15} /> Delete Selected ({selected.size})
            </button>
          )}
          <button className="btn" onClick={() => setShowForm(s => !s)}>
            {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Subject</>}
          </button>
        </div>
      </div>

      {showConfirm && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="card" style={{ maxWidth: 420, width: '90%', padding: 28 }}>
            <h3 style={{ marginTop: 0 }}>Confirm Deletion</h3>
            <p style={{ color: 'var(--text-secondary)', margin: '12px 0 24px' }}>
              Are you sure you want to delete <strong>{selected.size}</strong> subject(s)?
              Subjects used in active timetable entries cannot be deleted.
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
              <button className="btn btn-outline" onClick={() => setShowConfirm(false)}>Cancel</button>
              <button className="btn" style={{ background: 'var(--red)', borderColor: 'var(--red)' }} onClick={handleBulkDelete} disabled={deleting}>
                {deleting ? 'Deleting…' : 'Yes, Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {editSubject && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.55)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="card" style={{ maxWidth: 480, width: '92%', padding: 28 }}>
            <h3 style={{ marginTop: 0 }}>Edit Subject — <code style={{ fontSize: 14 }}>{editSubject.subjectName}</code></h3>
            <form onSubmit={handleEdit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: 12, alignItems: 'end' }}>
                <div>
                  <label className="label">Subject Name</label>
                  <input className="input" required value={editForm.subjectName}
                    onChange={e => setEditForm({ ...editForm, subjectName: e.target.value })} />
                </div>
                <div>
                  <label className="label">Subject Code</label>
                  <input className="input" value={editForm.subjectCode}
                    onChange={e => setEditForm({ ...editForm, subjectCode: e.target.value })} />
                </div>
                <div style={{ paddingBottom: 14 }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                    <input type="checkbox" checked={editForm.isLab} onChange={e => setEditForm({ ...editForm, isLab: e.target.checked })} />
                    <span className="label" style={{ margin: 0 }}>Lab Subject</span>
                  </label>
                </div>
              </div>
              {error && <div className="error-banner" style={{ marginTop: 10 }}>{error}</div>}
              <div style={{ display: 'flex', gap: 10, marginTop: 18, justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-outline" onClick={closeEdit}>Cancel</button>
                <button type="submit" className="btn" disabled={editSubmitting}>
                  {editSubmitting ? 'Saving…' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {error && !editSubject && <div className="error-banner" style={{ whiteSpace: 'pre-line' }}>{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Subject</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: 12, alignItems: 'end' }}>
              <div>
                <label className="label">Subject Name</label>
                <input className="input" required value={form.subjectName} placeholder="e.g. Mathematics"
                  onChange={e => setForm({ ...form, subjectName: e.target.value })} />
              </div>
              <div>
                <label className="label">Subject Code</label>
                <input className="input" value={form.subjectCode} placeholder="e.g. MATH-10"
                  onChange={e => setForm({ ...form, subjectCode: e.target.value })} />
              </div>
              <div style={{ paddingBottom: 14 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input type="checkbox" checked={form.isLab} onChange={e => setForm({ ...form, isLab: e.target.checked })} />
                  <span className="label" style={{ margin: 0 }}>Lab Subject</span>
                </label>
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>{submitting ? 'Saving…' : 'Save Subject'}</button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : subjects.length === 0 ? <div className="empty-state">No subjects yet.</div>
          : (
            <>
              <div style={{ marginBottom: 10 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13, color: 'var(--text-secondary)' }}>
                  <input type="checkbox" checked={allSelected} onChange={toggleAll} />
                  Select All ({subjects.length})
                </label>
              </div>
              <table>
                <thead><tr><th style={{ width: 36 }}></th><th>ID</th><th>Subject Name</th><th>Code</th><th>Type</th><th style={{ width: 70 }}>Actions</th></tr></thead>
                <tbody>
                  {subjects.map(s => (
                    <tr key={s.subjectId} style={{ background: selected.has(s.subjectId) ? 'rgba(99,102,241,.07)' : '' }}>
                      <td><input type="checkbox" checked={selected.has(s.subjectId)} onChange={() => toggleOne(s.subjectId)} /></td>
                      <td><code>{s.subjectId}</code></td>
                      <td>{s.subjectName}</td>
                      <td><code>{s.subjectCode || '—'}</code></td>
                      <td><span className={`badge ${s.isLab ? 'badge-late' : 'badge-active'}`}>{s.isLab ? 'Lab' : 'Theory'}</span></td>
                      <td>
                        <button onClick={() => openEdit(s)} title="Edit subject"
                          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--indigo)', padding: '4px 6px', borderRadius: 6, display: 'inline-flex', alignItems: 'center' }}>
                          <IconEdit width={15} height={15} />
                        </button>
                      </td>
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
