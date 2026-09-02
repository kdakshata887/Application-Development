import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import {
  IconDashboard, IconStudents, IconAttendance, IconTimetable,
  IconLeave, IconLogout, IconSun, IconMoon, IconSlate,
} from './Icons'

const NAV = [
  { to: '/dashboard', label: 'Dashboard', end: true, icon: IconDashboard },
  { to: '/dashboard/students', label: 'Students', icon: IconStudents },
  { to: '/dashboard/attendance', label: 'Attendance', icon: IconAttendance },
  { to: '/dashboard/timetable', label: 'Timetable', icon: IconTimetable },
  { to: '/dashboard/leaves', label: 'Leave Management', icon: IconLeave },
]

function initials(name = '') {
  return name.slice(0, 2).toUpperCase()
}

export default function DashboardLayout() {
  const { user, logout } = useAuth()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <span className="mark">E</span>
          EduTrack
        </div>

        <div className="sidebar-section-label">Modules</div>
        {NAV.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => 'sidebar-link' + (isActive ? ' active' : '')}
          >
            <item.icon />
            {item.label}
          </NavLink>
        ))}

        <div className="sidebar-footer">
          <div className="theme-switch">
            <button
              className={theme === 'midnight' ? 'active' : ''}
              onClick={() => setTheme('midnight')}
              title="Midnight theme"
            >
              <IconMoon width={15} height={15} />
            </button>
            <button
              className={theme === 'daylight' ? 'active' : ''}
              onClick={() => setTheme('daylight')}
              title="Daylight theme"
            >
              <IconSun width={15} height={15} />
            </button>
            <button
              className={theme === 'slate' ? 'active' : ''}
              onClick={() => setTheme('slate')}
              title="Slate theme"
            >
              <IconSlate width={15} height={15} />
            </button>
          </div>

          <div className="user-chip">
            <div className="avatar">{initials(user?.username)}</div>
            <div className="meta">
              <div className="name">{user?.username}</div>
              <div className="role">{user?.role}</div>
            </div>
          </div>

          <div className="sidebar-link" onClick={handleLogout}>
            <IconLogout />
            Logout
          </div>
        </div>
      </aside>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  )
}
