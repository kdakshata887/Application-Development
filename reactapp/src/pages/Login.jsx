import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { IconSun, IconMoon, IconSlate } from '../components/Icons'

export default function Login() {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/dashboard')
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-shell">
      <div className="login-hero">
        <div style={{ fontSize: 19, fontWeight: 800, display: 'flex', alignItems: 'center', gap: 9 }}>
          <span style={{
            width: 32, height: 32, borderRadius: 9, background: 'rgba(255,255,255,0.18)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16,
          }}>E</span>
          EduTrack
        </div>

        <div>
          <h1 style={{ fontSize: 38, lineHeight: 1.15, maxWidth: 420, marginBottom: 14 }}>
            Empowering schools with smart management
          </h1>
          <p style={{ opacity: 0.88, maxWidth: 400, fontSize: 15, lineHeight: 1.6 }}>
            Track attendance, manage timetables, and analyze performance — all in one platform.
          </p>
        </div>

        <div style={{ display: 'flex', gap: 40, fontSize: 13 }}>
          <div><strong style={{ fontSize: 24, display: 'block', fontWeight: 800 }}>500+</strong><span style={{ opacity: 0.75 }}>Schools</span></div>
          <div><strong style={{ fontSize: 24, display: 'block', fontWeight: 800 }}>120K+</strong><span style={{ opacity: 0.75 }}>Students</span></div>
          <div><strong style={{ fontSize: 24, display: 'block', fontWeight: 800 }}>99.9%</strong><span style={{ opacity: 0.75 }}>Uptime</span></div>
        </div>
      </div>

      <div className="login-form-panel">
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 24 }}>
          <div className="theme-switch" style={{ width: 120 }}>
            <button className={theme === 'midnight' ? 'active' : ''} onClick={() => setTheme('midnight')} title="Midnight">
              <IconMoon width={15} height={15} />
            </button>
            <button className={theme === 'daylight' ? 'active' : ''} onClick={() => setTheme('daylight')} title="Daylight">
              <IconSun width={15} height={15} />
            </button>
            <button className={theme === 'slate' ? 'active' : ''} onClick={() => setTheme('slate')} title="Slate">
              <IconSlate width={15} height={15} />
            </button>
          </div>
        </div>

        <h2 style={{ marginTop: 0, marginBottom: 4, fontSize: 24 }}>Welcome back</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13.5, marginTop: 0, marginBottom: 24 }}>
          Sign in to access your dashboard
        </p>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={handleSubmit}>
          <label className="label">Username</label>
          <input
            className="input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="admin"
            autoFocus
          />
          <label className="label">Password</label>
          <input
            className="input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter your password"
          />
          <button className="btn" style={{ width: '100%', justifyContent: 'center', marginTop: 6, padding: '12px 18px' }} disabled={loading}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>
        </form>

        <p style={{ fontSize: 11.5, color: 'var(--text-faint)', marginTop: 22, textAlign: 'center' }}>
          Default admin: <code>admin</code> / <code>Admin@123</code>
        </p>
      </div>
    </div>
  )
}
