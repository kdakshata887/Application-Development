import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import {
  IconDashboard, IconStudents, IconTeachers, IconAttendance, IconTimetable,
  IconLeave, IconLogout, IconSun, IconMoon, IconSlate, IconChart, IconReports,
  IconNotification, IconHoliday, IconRoom, IconSubject, IconParent, IconSection, IconAdmin,
} from './Icons'

// Role-based navigation — each item specifies which roles can see it.
// If `roles` is absent, all authenticated users can see it.
const NAV = [
  {
    label: 'Core',
    items: [
      { to: '/dashboard', label: 'Dashboard', end: true, icon: IconDashboard },
    ]
  },
  {
    label: 'Academic',
    roles: ['ADMIN', 'PRINCIPAL', 'TEACHER', 'CLASS_TEACHER'],
    items: [
      { to: '/dashboard/students', label: 'Students', icon: IconStudents },
      { to: '/dashboard/teachers', label: 'Teachers', icon: IconTeachers, roles: ['ADMIN', 'PRINCIPAL'] },
      { to: '/dashboard/sections', label: 'Sections', icon: IconSection, roles: ['ADMIN', 'PRINCIPAL'] },
      { to: '/dashboard/subjects', label: 'Subjects', icon: IconSubject, roles: ['ADMIN', 'PRINCIPAL'] },
      { to: '/dashboard/rooms', label: 'Rooms', icon: IconRoom, roles: ['ADMIN', 'PRINCIPAL'] },
    ]
  },
  {
    label: 'Scheduling',
    items: [
      { to: '/dashboard/timetable', label: 'Timetable', icon: IconTimetable },
      { to: '/dashboard/timetable-generate', label: 'Generate Timetable', icon: IconDashboard, roles: ['ADMIN', 'PRINCIPAL'] },
    ]
  },
  {
    label: 'Attendance',
    items: [
      { to: '/dashboard/attendance', label: 'Attendance', icon: IconAttendance },
    ]
  },
  {
    label: 'HR',
    items: [
      { to: '/dashboard/leaves', label: 'Leave Management', icon: IconLeave },
      { to: '/dashboard/holidays', label: 'Holidays', icon: IconHoliday, roles: ['ADMIN', 'PRINCIPAL'] },
    ]
  },
  {
    label: 'Reports',
    roles: ['ADMIN', 'PRINCIPAL', 'CLASS_TEACHER'],
    items: [
      { to: '/dashboard/analytics', label: 'Analytics', icon: IconChart },
      { to: '/dashboard/reports', label: 'Reports', icon: IconReports },
    ]
  },
  {
    label: 'Parent',
    roles: ['PARENT'],
    items: [
      { to: '/dashboard/parent', label: 'My Children', icon: IconParent },
    ]
  },
  {
    label: 'Admin',
    roles: ['ADMIN'],
    items: [
      { to: '/dashboard/notifications', label: 'Notifications', icon: IconNotification },
      { to: '/dashboard/holidays', label: 'Holidays', icon: IconHoliday },
    ]
  },
]

function initials(name = '') {
  return name.slice(0, 2).toUpperCase()
}

function canSee(item, userRole) {
  if (!item.roles) return true
  return item.roles.includes(userRole)
}

export default function DashboardLayout() {
  const { user, logout } = useAuth()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()
  const userRole = user?.role || ''

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

        {NAV.map(group => {
          const visibleItems = group.items.filter(item => canSee(item, userRole))
          if (visibleItems.length === 0) return null
          if (group.roles && !group.roles.includes(userRole)) return null
          return (
            <div key={group.label}>
              <div className="sidebar-section-label">{group.label}</div>
              {visibleItems.map((item) => (
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
            </div>
          )
        })}

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

          <div className="sidebar-link" onClick={handleLogout} style={{ cursor: 'pointer' }}>
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

