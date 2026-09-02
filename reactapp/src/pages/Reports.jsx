import { useState } from 'react'
import { api } from '../api/client'
import { getToken } from '../api/client'
import { IconDownload } from '../components/Icons'

function ReportCard({ title, description, endpoint }) {
  function download() {
    const token = getToken()
    const a = document.createElement('a')
    a.href = endpoint
    // Include auth header via XHR since anchor clicks can't set headers
    fetch(endpoint, { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.blob())
      .then(blob => {
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = endpoint.split('/').pop() + '.csv'
        link.click()
        window.URL.revokeObjectURL(url)
      })
      .catch(err => alert('Download failed: ' + err.message))
  }

  return (
    <div className="card" style={{
      display: 'flex', flexDirection: 'column', gap: 12, justifyContent: 'space-between'
    }}>
      <div>
        <h3 style={{ margin: '0 0 6px' }}>{title}</h3>
        <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: 13 }}>{description}</p>
      </div>
      <button className="btn" onClick={download} style={{ alignSelf: 'flex-start' }}>
        <IconDownload width={16} height={16} /> Download CSV
      </button>
    </div>
  )
}

export default function Reports() {
  return (
    <div>
      <div className="topbar">
        <div><h1>Reports</h1><div className="breadcrumb">Home / Reports</div></div>
      </div>

      <div className="reports-info" style={{
        background: 'var(--accent-soft)', border: '1px solid var(--border)', borderRadius: 10,
        padding: '12px 16px', marginBottom: 24, color: 'var(--text-secondary)', fontSize: 13
      }}>
        📊 All reports are generated live from current database data. No caching — always up-to-date.
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 }}>
        <ReportCard
          title="Below 75% Attendance"
          description="List of all students whose attendance is below the 75% threshold. Includes student details and exact percentage."
          endpoint="/api/reports/attendance/below75"
        />
        <ReportCard
          title="Full Attendance Summary"
          description="Complete attendance summary for every student — total days, present, absent, late, and percentage."
          endpoint="/api/reports/attendance/summary"
        />
        <ReportCard
          title="Teacher Deployment"
          description="Shows which teacher is assigned to which subjects, sections, days, and periods in the current timetable."
          endpoint="/api/reports/teacher/deployment"
        />
        <ReportCard
          title="Leave Summary"
          description="Summary of all teacher leave applications with status, type, duration, and assigned substitutes."
          endpoint="/api/reports/leave/summary"
        />
        <ReportCard
          title="Full Timetable Export"
          description="Export the complete active timetable sorted by section, showing all subjects, teachers, rooms, days, and periods."
          endpoint="/api/reports/timetable"
        />
      </div>
    </div>
  )
}
