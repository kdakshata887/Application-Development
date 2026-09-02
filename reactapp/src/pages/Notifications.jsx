import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { IconRefresh } from '../components/Icons'

export default function Notifications() {
  const { user } = useAuth()
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setLoading(true); setError('')
    try {
      const data = user?.role === 'ADMIN'
        ? await api.getAllNotifications()
        : await api.getMyNotifications(user?.userId)
      setNotifications(data || [])
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  async function markRead(id) {
    try {
      await api.markNotificationRead(id)
      setNotifications(prev => prev.map(n => n.notificationId === id ? { ...n, status: 'READ' } : n))
    } catch (err) { setError(err.message) }
  }

  const statusColor = { SENT: 'badge-pending', READ: 'badge-active', FAILED: 'badge-absent', PENDING: 'badge-late' }
  const channelColor = { EMAIL: 'badge-active', SMS: 'badge-late', IN_APP: 'badge-pending', PUSH: 'badge-absent' }

  const unread = notifications.filter(n => n.status !== 'READ').length

  return (
    <div>
      <div className="topbar">
        <div>
          <h1>Notifications {unread > 0 && <span style={{ fontSize: 14, fontWeight: 600, color: 'var(--accent)', marginLeft: 8 }}>({unread} unread)</span>}</h1>
          <div className="breadcrumb">Home / Notifications</div>
        </div>
        <button className="btn btn-outline" onClick={load}><IconRefresh width={16} height={16} /> Refresh</button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="card">
        {loading ? <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
          : notifications.length === 0 ? <div className="empty-state">No notifications.</div>
          : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
              {notifications.map(n => (
                <div key={n.notificationId} style={{
                  padding: '14px 16px',
                  borderBottom: '1px solid var(--border-soft)',
                  background: n.status !== 'READ' ? 'var(--accent-soft)' : 'transparent',
                  display: 'flex', alignItems: 'flex-start', gap: 12,
                  transition: 'background 0.2s'
                }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 4, flexWrap: 'wrap' }}>
                      <span className={`badge ${statusColor[n.status] || 'badge-pending'}`}>{n.status}</span>
                      {n.channel && <span className={`badge ${channelColor[n.channel] || 'badge-pending'}`}>{n.channel}</span>}
                      {n.notificationType && <span style={{ fontSize: 11, color: 'var(--text-faint)' }}>{n.notificationType}</span>}
                    </div>
                    <p style={{ margin: '0 0 4px', fontSize: 14 }}>{n.message}</p>
                    {n.recipient && <div style={{ fontSize: 12, color: 'var(--text-faint)' }}>To: {n.recipient.username} · {n.sentAt ? new Date(n.sentAt).toLocaleString() : ''}</div>}
                  </div>
                  {n.status !== 'READ' && (
                    <button className="btn btn-outline" style={{ fontSize: 12, padding: '4px 10px' }}
                      onClick={() => markRead(n.notificationId)}>
                      Mark Read
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
      </div>
    </div>
  )
}
