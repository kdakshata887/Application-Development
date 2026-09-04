import { useState } from 'react'
import { api, downloadReport } from '../api/client'
import { IconDownload } from '../components/Icons'

function ReportCard({ title, description, reportPath }) {
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState('')

  async function download() {
    setError('')
    setDownloading(true)
    try {
      const blobUrl = await downloadReport(reportPath)
      const link = document.createElement('a')
      link.href = blobUrl
      link.download = reportPath.split('/').pop() + '.csv'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(blobUrl)
    } catch (err) {
      setError(err.message)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className="card" style={{
      display: 'flex', flexDirection: 'column', gap: 12, justifyContent: 'space-between'
    }}>
      <div>
        <h3 style={{ margin: '0 0 6px' }}>{title}</h3>
        <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: 13 }}>{description}</p>
        {error && <p style={{ margin: '8px 0 0', color: 'var(--error)', fontSize: 12 }}>{error}</p>}
      </div>
      <button
        className="btn"
        onClick={download}
        disabled={downloading}
        style={{ alignSelf: 'flex-start', opacity: downloading ? 0.6 : 1 }}
      >
        <IconDownload width={16} height={16} />
        {downloading ? 'Downloading…' : 'Download CSV'}
      </button>
    </div>
  )
}

export default function Reports() {
  const { reportPaths } = api

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
          reportPath={reportPaths.below75}
        />
        <ReportCard
          title="Full Attendance Summary"
          description="Complete attendance summary for every student — total days, present, absent, late, and percentage."
          reportPath={reportPaths.attendanceSummary}
        />
        <ReportCard
          title="Teacher Deployment"
          description="Shows which teacher is assigned to which subjects, sections, days, and periods in the current timetable."
          reportPath={reportPaths.teacherDeployment}
        />
        <ReportCard
          title="Leave Summary"
          description="Summary of all teacher leave applications with status, type, duration, and assigned substitutes."
          reportPath={reportPaths.leaveSummary}
        />
        <ReportCard
          title="Full Timetable Export"
          description="Export the complete active timetable sorted by section, showing all subjects, teachers, rooms, days, and periods."
          reportPath={reportPaths.timetable}
        />
      </div>
    </div>
  )
}
