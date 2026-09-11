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

export default function Sections() {
  const [sections, setSections] = useState([])
  const [teachers, setTeachers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ sectionName: '', className: '', classTeacherId: '' })
  const [submitting, setSubmitting] = useState(false)
  const [selected, setSelected] = useState(new Set())
  const [showConfirm, setShowConfirm] = useState(false)
  const [deleting, setDeleting] = useState(false)

  // Edit state
  const [editSection, setEditSection] = useState(null)
  const [editForm, setEditForm] = useState({ sectionName: '', className: '', classTeacherId: '' })
  const [editSubmitting, setEditSubmitting] = useState(false)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 4000)
  }

  async function load() {
    setLoading(true); setError('')
    try {
      const [secs, tchs] = await Promise.all([api.getSections(), api.getTeachers()])
      setSections(secs)
      setTeachers(tchs)
    }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.createSection({
        sectionName: form.sectionName,
        className: form.className,
        classTeacher: form.classTeacherId ? { teacherId: Number(form.classTeacherId) } : undefined,
      })
      setShowForm(false)
      setForm({ sectionName: '', className: '', classTeacherId: '' })
      load()
      showToast('Section added successfully')
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  const allIds = sections.map(s => s.sectionId)
  const allSelected = allIds.length > 0 && allIds.every(id => selected.has(id))
  function toggleAll() { setSelected(allSelected ? new Set() : new Set(allIds)) }
  function toggleOne(id) { const n = new Set(selected); n.has(id) ? n.delete(id) : n.add(id); setSelected(n) }

  function openEdit(section) {
    setEditSection(section)
    setEditForm({ sectionName: section.sectionName || '', className: section.className || '', classTeacherId: section.classTeacher?.teacherId ?? '' })
  }
  function closeEdit() { setEditSection(null) }

  async function handleEdit(e) {
    e.preventDefault(); setEditSubmitting(true); setError('')
    try {
      await api.updateSection(editSection.sectionId, {
        sectionName: editForm.sectionName,
        className: editForm.className,
        classTeacher: editForm.classTeacherId ? { teacherId: Number(editForm.classTeacherId) } : undefined,
      })
      closeEdit(); load(); showToast('Section updated successfully')
    } catch (err) { setError(err.message) }
    finally { setEditSubmitting(false) }
  }

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

      {/* Edit Modal */}
      {editSection && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.55)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="card" style={{ maxWidth: 480, width: '92%', padding: 28 }}>
            <h3 style={{ marginTop: 0 }}>Edit Section — <code style={{ fontSize: 14 }}>{editSection.sectionName}</code></h3>
            <form onSubmit={handleEdit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
                <div>
                  <label className="label">Section Name</label>
                  <input className="input" required value={editForm.sectionName}
                    onChange={e => setEditForm({ ...editForm, sectionName: e.target.value })} />
                </div>
                <div>
                   <label className="label">Class Name (e.g. Grade 10)</label>
                   <input className="input" value={editForm.className}
                     onChange={e => setEditForm({ ...editForm, className: e.target.value })} />
                </div>
                <div>
                  <label className="label">Class Teacher <span style={{ color: 'var(--text-secondary)', fontWeight: 400 }}>(optional)</span></label>
                  <select className="input" value={editForm.classTeacherId}
                    onChange={e => setEditForm({ ...editForm, classTeacherId: e.target.value })}>
                    <option value="">— None —</option>
                    {teachers.map(t => (
                      <option key={t.teacherId} value={t.teacherId}>
                        {t.name} {t.employeeId ? `(${t.employeeId})` : ''}
                      </option>
                    ))}
                  </select>
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

      {error && !editSection && <div className="error-banner" style={{ whiteSpace: 'pre-line' }}>{error}</div>}

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
                <label className="label">Class Name (e.g. Grade 10)</label>
                <input className="input" value={form.className} placeholder="e.g. Grade 10"
                  onChange={e => setForm({ ...form, className: e.target.value })} />
              </div>
              <div>
                <label className="label">Class Teacher <span style={{ color: 'var(--text-secondary)', fontWeight: 400 }}>(optional)</span></label>
                <select className="input" value={form.classTeacherId}
                  onChange={e => setForm({ ...form, classTeacherId: e.target.value })}>
                  <option value="">— None —</option>
                  {teachers.map(t => (
                    <option key={t.teacherId} value={t.teacherId}>
                      {t.name} {t.employeeId ? `(${t.employeeId})` : ''}
                    </option>
                  ))}
                </select>
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
                <thead><tr><th style={{ width: 36 }}></th><th>ID</th><th>Section Name</th><th>Class Name</th><th>Class Teacher</th><th style={{ width: 70 }}>Actions</th></tr></thead>
                <tbody>
                  {sections.map(s => (
                    <tr key={s.sectionId} style={{ background: selected.has(s.sectionId) ? 'rgba(99,102,241,.07)' : '' }}>
                      <td><input type="checkbox" checked={selected.has(s.sectionId)} onChange={() => toggleOne(s.sectionId)} /></td>
                      <td><code>{s.sectionId}</code></td>
                      <td>{s.sectionName}</td>
                      <td>{s.className || '—'}</td>
                      <td>{s.classTeacher ? s.classTeacher.name : '—'}</td>
                      <td>
                        <button onClick={() => openEdit(s)} title="Edit section"
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
