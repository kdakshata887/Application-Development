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

export default function Sections() {
  const [sections, setSections] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ sectionName: '', gradeLevel: '', classTeacherId: '' })
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
    try { setSections(await api.getSections()) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.createSection({
        sectionName: form.sectionName, gradeLevel: form.gradeLevel,
        classTeacherId: form.classTeacherId ? Number(form.classTeacherId) : undefined,
      })
      setShowForm(false)
      setForm({ sectionName: '', gradeLevel: '', classTeacherId: '' })
      load()
      showToast('Section added successfully')
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  const allIds = sections.map(s => s.sectionId)
  const allSelected = allIds.length > 0 && allIds.every(id => selected.has(id))
  function toggleAll() { setSelected(allSelected ? new Set() : new Set(allIds)) }
  function toggleOne(id) { const n = new Set(selected); n.has(id) ? n.delete(id) : n.add(id); setSelected(n) }

  async function handleBulkDelete() {
    setDeleting(true); setShowConfirm(false)
    try {
      const result = await api.bulkDeleteSections([...selected])
      setSelected(new Set())
      await load()
      if (result.failedCount === 0) showToast(`✓ ${result.deletedCount} section(s) deleted.`)
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
        <div><h1>Class Sections</h1><div className="breadcrumb">Home / Sections</div></div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {selected.size > 0 && (
            <button className="btn btn-outline" style={{ borderColor: 'var(--red)', color: 'var(--red)', display: 'flex', alignItems: 'center', gap: 6 }}
              onClick={() => setShowConfirm(true)} disabled={deleting}>
              <IconTrash width={15} height={15} /> Delete Selected ({selected.size})
            </button>
          )}
          <button className="btn" onClick={() => setShowForm(s => !s)}>
            {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Section</>}
          </button>
        </div>
      </div>

      {showConfirm && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="card" style={{ maxWidth: 420, width: '90%', padding: 28 }}>
            <h3 style={{ marginTop: 0 }}>Confirm Deletion</h3>
            <p style={{ color: 'var(--text-secondary)', margin: '12px 0 24px' }}>
              Are you sure you want to delete <strong>{selected.size}</strong> section(s)?
              Sections with enrolled students or active timetable entries cannot be deleted.
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

      {error && <div className="error-banner" style={{ whiteSpace: 'pre-line' }}>{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Section</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">Section Name</label>
                <input className="input" required value={form.sectionName} placeholder="e.g. 10-A"
                  onChange={e => setForm({ ...form, sectionName: e.target.value })} />
              </div>
              <div>
                <label className="label">Grade Level</label>
                <input className="input" value={form.gradeLevel} placeholder="e.g. 10"
                  onChange={e => setForm({ ...form, gradeLevel: e.target.value })} />
              </div>
              <div>
                <label className="label">Class Teacher ID (optional)</label>
                <input className="input" type="number" value={form.classTeacherId}
                  onChange={e => setForm({ ...form, classTeacherId: e.target.value })} />
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>{submitting ? 'Saving…' : 'Save Section'}</button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : sections.length === 0 ? <div className="empty-state">No sections yet.</div>
          : (
            <>
              <div style={{ marginBottom: 10 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13, color: 'var(--text-secondary)' }}>
                  <input type="checkbox" checked={allSelected} onChange={toggleAll} />
                  Select All ({sections.length})
                </label>
              </div>
              <table>
                <thead><tr><th style={{ width: 36 }}></th><th>ID</th><th>Section Name</th><th>Grade</th><th>Class Teacher</th></tr></thead>
                <tbody>
                  {sections.map(s => (
                    <tr key={s.sectionId} style={{ background: selected.has(s.sectionId) ? 'rgba(99,102,241,.07)' : '' }}>
                      <td><input type="checkbox" checked={selected.has(s.sectionId)} onChange={() => toggleOne(s.sectionId)} /></td>
                      <td><code>{s.sectionId}</code></td>
                      <td>{s.sectionName}</td>
                      <td>{s.gradeLevel || '—'}</td>
                      <td>{s.classTeacher ? s.classTeacher.name : '—'}</td>
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
