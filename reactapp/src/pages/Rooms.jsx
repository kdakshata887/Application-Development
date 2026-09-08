import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus, IconTrash } from '../components/Icons'

const ROOM_TYPES = ['CLASSROOM', 'LABORATORY', 'LIBRARY', 'AUDITORIUM', 'COMPUTER_LAB', 'OTHER']

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

export default function Rooms() {
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [toast, setToast] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ roomName: '', capacity: '', roomType: 'CLASSROOM' })
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
    try { setRooms(await api.getRooms()) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function handleAdd(e) {
    e.preventDefault(); setSubmitting(true); setError('')
    try {
      await api.createRoom({ ...form, capacity: Number(form.capacity) })
      setShowForm(false); setForm({ roomName: '', capacity: '', roomType: 'CLASSROOM' })
      load(); showToast('Room added successfully')
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  const allIds = rooms.map(r => r.roomId)
  const allSelected = allIds.length > 0 && allIds.every(id => selected.has(id))
  function toggleAll() { setSelected(allSelected ? new Set() : new Set(allIds)) }
  function toggleOne(id) { const n = new Set(selected); n.has(id) ? n.delete(id) : n.add(id); setSelected(n) }

  async function handleBulkDelete() {
    setDeleting(true); setShowConfirm(false)
    try {
      const result = await api.bulkDeleteRooms([...selected])
      setSelected(new Set()); await load()
      if (result.failedCount === 0) showToast(`✓ ${result.deletedCount} room(s) deleted.`)
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
        <div><h1>Rooms</h1><div className="breadcrumb">Home / Rooms</div></div>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          {selected.size > 0 && (
            <button className="btn btn-outline" style={{ borderColor: 'var(--red)', color: 'var(--red)', display: 'flex', alignItems: 'center', gap: 6 }}
              onClick={() => setShowConfirm(true)} disabled={deleting}>
              <IconTrash width={15} height={15} /> Delete Selected ({selected.size})
            </button>
          )}
          <button className="btn" onClick={() => setShowForm(s => !s)}>
            {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Room</>}
          </button>
        </div>
      </div>

      {showConfirm && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.5)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="card" style={{ maxWidth: 420, width: '90%', padding: 28 }}>
            <h3 style={{ marginTop: 0 }}>Confirm Deletion</h3>
            <p style={{ color: 'var(--text-secondary)', margin: '12px 0 24px' }}>
              Are you sure you want to delete <strong>{selected.size}</strong> room(s)?
              Rooms assigned to active timetable slots cannot be deleted.
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
          <h3 style={{ marginTop: 0 }}>Add Room</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">Room Name</label>
                <input className="input" required value={form.roomName} placeholder="e.g. Room 101"
                  onChange={e => setForm({ ...form, roomName: e.target.value })} />
              </div>
              <div>
                <label className="label">Capacity</label>
                <input className="input" type="number" min={1} value={form.capacity}
                  onChange={e => setForm({ ...form, capacity: e.target.value })} />
              </div>
              <div>
                <label className="label">Room Type</label>
                <select className="input" value={form.roomType} onChange={e => setForm({ ...form, roomType: e.target.value })}>
                  {ROOM_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>{submitting ? 'Saving…' : 'Save Room'}</button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : rooms.length === 0 ? <div className="empty-state">No rooms yet.</div>
          : (
            <>
              <div style={{ marginBottom: 10 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13, color: 'var(--text-secondary)' }}>
                  <input type="checkbox" checked={allSelected} onChange={toggleAll} />
                  Select All ({rooms.length})
                </label>
              </div>
              <table>
                <thead><tr><th style={{ width: 36 }}></th><th>ID</th><th>Room Name</th><th>Capacity</th><th>Type</th></tr></thead>
                <tbody>
                  {rooms.map(r => (
                    <tr key={r.roomId} style={{ background: selected.has(r.roomId) ? 'rgba(99,102,241,.07)' : '' }}>
                      <td><input type="checkbox" checked={selected.has(r.roomId)} onChange={() => toggleOne(r.roomId)} /></td>
                      <td><code>{r.roomId}</code></td>
                      <td>{r.roomName}</td>
                      <td>{r.capacity || '—'}</td>
                      <td><span className="badge badge-pending">{r.roomType}</span></td>
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
