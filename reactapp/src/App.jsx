import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Students from './pages/Students'
import Teachers from './pages/Teachers'
import Sections from './pages/Sections'
import Subjects from './pages/Subjects'
import Rooms from './pages/Rooms'
import Attendance from './pages/Attendance'
import Timetable from './pages/Timetable'
import TimetableGenerate from './pages/TimetableGenerate'
import Leaves from './pages/Leaves'
import Analytics from './pages/Analytics'
import Reports from './pages/Reports'
import ParentPortal from './pages/ParentPortal'
import Holidays from './pages/Holidays'
import Notifications from './pages/Notifications'
import DashboardLayout from './components/DashboardLayout'

function ProtectedRoute({ children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="students" element={<Students />} />
        <Route path="teachers" element={<Teachers />} />
        <Route path="sections" element={<Sections />} />
        <Route path="subjects" element={<Subjects />} />
        <Route path="rooms" element={<Rooms />} />
        <Route path="attendance" element={<Attendance />} />
        <Route path="timetable" element={<Timetable />} />
        <Route path="timetable-generate" element={<TimetableGenerate />} />
        <Route path="leaves" element={<Leaves />} />
        <Route path="analytics" element={<Analytics />} />
        <Route path="reports" element={<Reports />} />
        <Route path="parent" element={<ParentPortal />} />
        <Route path="holidays" element={<Holidays />} />
        <Route path="notifications" element={<Notifications />} />
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}

