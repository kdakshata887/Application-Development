import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'

const TEACHER_ROLES = ['TEACHER', 'CLASS_TEACHER']
const ADMIN_ROLES = ['ADMIN', 'PRINCIPAL']

export default function Leaves() {
  const { user } = useAuth()
  const isTeacher = TEACHER_ROLES.includes(user?.role)
  const isAdmin = ADMIN_ROLES.includes(user?.role)

  const [pending, setPending] = useState([])
  const [myLeaves, setMyLeaves] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [form, setForm] = useState({
    teacherId: '', leaveType: 'SL', fromDate: '', toDate: '', reason: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [applyResult, setApplyResult] = useState(null)

  async function loadData() {
    setLoading(true)
    setError('')
    try {
      if (isAdmin) {
        const data = await api.getPendingLeaves()
        setPending(data)
      }
      // Teachers can view their own leaves
      if (isTeacher && form.teacherId) {
        const data = await api.getLeavesByTeacher(form.teacherId)
        setMyLeaves(data)
      }
    } catch (err) {
      // 403 errors on pending leaves are expected for teacher role — suppress silently
      if (!err.message?.includes('403') && !err.message?.includes('Access Denied')) {
        setError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleApply(e) {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    setApplyResult(null)
    try {
      const payload = { ...form, teacherId: Number(form.teacherId) }
      const data = await api.applyLeave(payload)
      setApplyResult(data)
      loadData()
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
      loadData()
    } catch (err) {
      setError(err.message)
    }
  }

  async function handleReject(leaveId) {
    setError('')
    try {
      await api.rejectLeave(leaveId, { approverUserId: user.userId })
      loadData()
    } catch (err) {
      setError(err.message)
    }
  }

  const statusBadge = (s) => {
    const cls = s === 'APPROVED' ? 'badge-active' : s === 'REJECTED' ? 'badge-absent' : 'badge-pending'
    return <span className={`badge ${cls}`}>{s}</span>
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

      <div style={{ display: 'grid', gridTemplateColumns: isAdmin ? '1fr 1fr' : '1fr', gap: 20 }}>
        {/* Apply form — visible to teachers and admins */}
        {(isTeacher || isAdmin) && (
          <div className="card">
            <h3 style={{ marginTop: 0 }}>Apply for Leave</h3>
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

              <label className="label">Reason <span style={{ color: 'var(--red)' }}>*</span></label>
              <input className="input" required value={form.reason}
                onChange={(e) => setForm({ ...form, reason: e.target.value })} />

              <button className="btn" disabled={submitting}>
                {submitting ? 'Submitting…' : 'Apply for Leave'}
              </button>
            </form>
          </div>
        )}

        {/* Pending Approvals — ONLY visible to ADMIN / PRINCIPAL */}
        {isAdmin && (
          <div className="card">
            <h3 style={{ marginTop: 0 }}>Pending Approvals</h3>
            {loading ? (
              <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
            ) : pending.length === 0 ? (
              <div className="empty-state">No pending leave requests.</div>
            ) : (
              <table>
                <thead><tr><th>Teacher</th><th>Type</th><th>Dates</th><th>Status</th><th></th></tr></thead>
                <tbody>
                  {pending.map((l) => (
                    <tr key={l.leaveId}>
                      <td>{l.teacher?.name || '—'}</td>
                      <td>{l.leaveType}</td>
                      <td>{l.fromDate} → {l.toDate}</td>
                      <td>{statusBadge(l.status)}</td>
                      <td style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary" style={{ padding: '5px 10px', fontSize: 12 }}
                          onClick={() => handleApprove(l.leaveId)}>
                          Approve
                        </button>
                        <button className="btn btn-outline" style={{ padding: '5px 10px', fontSize: 12, borderColor: 'var(--red)', color: 'var(--red)' }}
                          onClick={() => handleReject(l.leaveId)}>
                          Reject
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
