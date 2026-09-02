import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconPlus } from '../components/Icons'

const ROOM_TYPES = ['CLASSROOM', 'LABORATORY', 'LIBRARY', 'AUDITORIUM', 'COMPUTER_LAB', 'OTHER']

export default function Rooms() {
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ roomName: '', capacity: '', roomType: 'CLASSROOM' })
  const [submitting, setSubmitting] = useState(false)

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
      setShowForm(false)
      setForm({ roomName: '', capacity: '', roomType: 'CLASSROOM' })
      load()
    } catch (err) { setError(err.message) }
    finally { setSubmitting(false) }
  }

  return (
    <div>
      <div className="topbar">
        <div><h1>Rooms</h1><div className="breadcrumb">Home / Rooms</div></div>
        <button className="btn" onClick={() => setShowForm(s => !s)}>
          {showForm ? 'Cancel' : <><IconPlus width={16} height={16} /> Add Room</>}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>Add Room</h3>
          <form onSubmit={handleAdd}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">Room Name</label>
                <input className="input" required value={form.roomName}
                  placeholder="e.g. Room 101"
                  onChange={e => setForm({ ...form, roomName: e.target.value })} />
              </div>
              <div>
                <label className="label">Capacity</label>
                <input className="input" type="number" min={1} value={form.capacity}
                  onChange={e => setForm({ ...form, capacity: e.target.value })} />
              </div>
              <div>
                <label className="label">Room Type</label>
                <select className="input" value={form.roomType}
                  onChange={e => setForm({ ...form, roomType: e.target.value })}>
                  {ROOM_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
            </div>
            <button className="btn" disabled={submitting} style={{ marginTop: 12 }}>
              {submitting ? 'Saving…' : 'Save Room'}
            </button>
          </form>
        </div>
      )}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : rooms.length === 0 ? <div className="empty-state">No rooms yet.</div>
          : (
            <table>
              <thead><tr><th>ID</th><th>Room Name</th><th>Capacity</th><th>Type</th></tr></thead>
              <tbody>
                {rooms.map(r => (
                  <tr key={r.roomId}>
                    <td><code>{r.roomId}</code></td>
                    <td>{r.roomName}</td>
                    <td>{r.capacity || '—'}</td>
                    <td><span className="badge badge-pending">{r.roomType}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
      </div>
    </div>
  )
}
