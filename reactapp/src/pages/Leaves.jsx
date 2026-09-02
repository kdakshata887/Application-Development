import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function Leaves() {
  const { user } = useAuth()
  const [pending, setPending] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [form, setForm] = useState({
    teacherId: '', leaveType: 'SL', fromDate: '', toDate: '', reason: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [applyResult, setApplyResult] = useState(null)

  async function loadPending() {
    setLoading(true)
    setError('')
    try {
      const data = await api.getPendingLeaves()
      setPending(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadPending() }, [])

  async function handleApply(e) {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    setApplyResult(null)
    try {
      const payload = { ...form, teacherId: Number(form.teacherId) }
      const data = await api.applyLeave(payload)
      setApplyResult(data)
      loadPending()
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleApprove(leaveId) {
    setError('')
    try {
      await api.approveLeave(leaveId, { approverUserId: user.userId, substituteTeacherId: null })
      loadPending()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Leave Management</h1>
          <div className="breadcrumb">Home / Leave Management</div>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Apply for Leave <span style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 400 }}>(Teacher token required)</span></h3>
          {applyResult && (
            <div className="error-banner" style={{ background: 'var(--green-bg)', color: 'var(--green)', borderColor: 'var(--green)' }}>
              Applied — Leave ID {applyResult.leaveId}, status {applyResult.status}
            </div>
          )}
          <form onSubmit={handleApply}>
            <label className="label">Teacher ID</label>
            <input className="input" type="number" required value={form.teacherId}
              onChange={(e) => setForm({ ...form, teacherId: e.target.value })} />

            <label className="label">Leave Type</label>
            <select className="input" value={form.leaveType}
              onChange={(e) => setForm({ ...form, leaveType: e.target.value })}>
              {['CL', 'SL', 'PL', 'COMP_OFF', 'ON_DUTY'].map((t) => <option key={t} value={t}>{t}</option>)}
            </select>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label className="label">From Date</label>
                <input className="input" type="date" required value={form.fromDate}
                  onChange={(e) => setForm({ ...form, fromDate: e.target.value })} />
              </div>
              <div>
                <label className="label">To Date</label>
                <input className="input" type="date" required value={form.toDate}
                  onChange={(e) => setForm({ ...form, toDate: e.target.value })} />
              </div>
            </div>

            <label className="label">Reason</label>
            <input className="input" required value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })} />

            <button className="btn" disabled={submitting}>
              {submitting ? 'Submitting…' : 'Apply for Leave'}
            </button>
          </form>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Pending Approvals <span style={{ fontSize: 11, color: 'var(--text-secondary)', fontWeight: 400 }}>(Admin token required to approve)</span></h3>
          {loading ? (
            <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          ) : pending.length === 0 ? (
            <div className="empty-state">No pending leave requests.</div>
          ) : (
            <table>
              <thead><tr><th>Type</th><th>Dates</th><th>Status</th><th></th></tr></thead>
              <tbody>
                {pending.map((l) => (
                  <tr key={l.leaveId}>
                    <td>{l.leaveType}</td>
                    <td>{l.fromDate} → {l.toDate}</td>
                    <td><span className="badge badge-pending">{l.status}</span></td>
                    <td>
                      <button className="btn btn-secondary" style={{ padding: '5px 10px', fontSize: 12 }}
                        onClick={() => handleApprove(l.leaveId)}>
                        Approve
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  )
}
