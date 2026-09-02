import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { IconRefresh, IconPlus } from '../components/Icons'

export default function TimetableGenerate() {
  const [sections, setSections] = useState([])
  const [mappings, setMappings] = useState([])
  const [subjects, setSubjects] = useState([])
  const [teachers, setTeachers] = useState([])
  const [clashes, setClashes] = useState(null)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [genConfig, setGenConfig] = useState({ sectionId: '', workingDays: 'MON,TUE,WED,THU,FRI', periodsPerDay: 8 })
  const [mapForm, setMapForm] = useState({ sectionId: '', subjectId: '', teacherId: '', periodsPerWeek: 5 })
  const [mapSubmitting, setMapSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function loadBase() {
    try {
      const [s, sub, t, m] = await Promise.all([
        api.getSections(), api.getSubjects(), api.getTeachers(), api.getMappings()
      ])
      setSections(s); setSubjects(sub); setTeachers(t); setMappings(m)
    } catch (err) { setError(err.message) }
  }

  async function loadClashes() {
    try { setClashes(await api.getClashReport()) }
    catch (err) { setClashes({ clashes: [], clashCount: 0, error: err.message }) }
  }

  useEffect(() => { loadBase(); loadClashes() }, [])

  async function generate() {
    if (!genConfig.sectionId && !window.confirm('Generate timetables for ALL sections? Existing entries will be cleared.')) return
    setLoading(true); setResult(null); setError('')
    try {
      const res = genConfig.sectionId
        ? await api.generateTimetableForSection(genConfig.sectionId, genConfig.workingDays, genConfig.periodsPerDay)
        : await api.generateTimetableForAll(genConfig.workingDays, genConfig.periodsPerDay)
      setResult(res)
      await loadClashes()
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  async function addMapping(e) {
    e.preventDefault(); setMapSubmitting(true); setError('')
    try {
      await api.createMapping({
        sectionId: Number(mapForm.sectionId), subjectId: Number(mapForm.subjectId),
        teacherId: Number(mapForm.teacherId), periodsPerWeek: Number(mapForm.periodsPerWeek)
      })
      setMapForm({ sectionId: '', subjectId: '', teacherId: '', periodsPerWeek: 5 })
      const m = await api.getMappings(); setMappings(m)
    } catch (err) { setError(err.message) }
    finally { setMapSubmitting(false) }
  }

  async function deleteMapping(id) {
    try { await api.deleteMapping(id); const m = await api.getMappings(); setMappings(m) }
    catch (err) { setError(err.message) }
  }

  const sectionName = (id) => sections.find(s => s.sectionId === id)?.sectionName || id
  const subjectName = (id) => subjects.find(s => s.subjectId === id)?.subjectName || id
  const teacherName = (id) => teachers.find(t => t.teacherId === id)?.name || id

  return (
    <div>
      <div className="topbar">
        <div><h1>Timetable Generator</h1><div className="breadcrumb">Home / Timetable / Generate</div></div>
        <button className="btn btn-outline" onClick={() => { loadBase(); loadClashes() }}>
          <IconRefresh width={16} height={16} /> Refresh
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Clash Report */}
      {clashes && (
        <div className={`card ${clashes.hasClashes ? 'border-red' : 'border-green'}`} style={{ marginBottom: 20 }}>
          <h3 style={{ marginTop: 0, color: clashes.hasClashes ? 'var(--red)' : 'var(--green)' }}>
            {clashes.hasClashes ? `⚠ ${clashes.clashCount} Clash(es) Detected` : '✓ No Clashes Detected'}
          </h3>
          {clashes.hasClashes && (
            <ul style={{ margin: 0, paddingLeft: 20, color: 'var(--text-secondary)' }}>
              {clashes.clashes.map((c, i) => <li key={i}>{c}</li>)}
            </ul>
          )}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        {/* Generator */}
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Generate Timetable</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div>
              <label className="label">Section (blank = All Sections)</label>
              <select className="input" value={genConfig.sectionId}
                onChange={e => setGenConfig({ ...genConfig, sectionId: e.target.value })}>
                <option value="">— All Sections —</option>
                {sections.map(s => <option key={s.sectionId} value={s.sectionId}>{s.sectionName}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Working Days</label>
              <input className="input" value={genConfig.workingDays}
                placeholder="MON,TUE,WED,THU,FRI"
                onChange={e => setGenConfig({ ...genConfig, workingDays: e.target.value })} />
            </div>
            <div>
              <label className="label">Periods per Day</label>
              <input className="input" type="number" min={1} max={10} value={genConfig.periodsPerDay}
                onChange={e => setGenConfig({ ...genConfig, periodsPerDay: Number(e.target.value) })} />
            </div>
            <button className="btn" onClick={generate} disabled={loading}>
              {loading ? 'Generating…' : '⚡ Generate Timetable'}
            </button>
          </div>
          {result && (
            <div style={{
              marginTop: 16, padding: 12, borderRadius: 8,
              background: result.success ? 'var(--green-soft)' : 'var(--red-soft)',
              color: result.success ? 'var(--green)' : 'var(--red)'
            }}>
              <strong>{result.success ? '✓' : '✗'} {result.message}</strong>
              {result.entriesCreated > 0 && <p style={{ margin: '4px 0 0', fontSize: 13 }}>Created {result.entriesCreated} entries</p>}
              {result.violations?.length > 0 && (
                <ul style={{ margin: '8px 0 0', paddingLeft: 20, fontSize: 13 }}>
                  {result.violations.map((v, i) => <li key={i}>{v}</li>)}
                </ul>
              )}
              {result.warnings?.length > 0 && (
                <div style={{ marginTop: 8, color: 'var(--amber)', fontSize: 12 }}>
                  {result.warnings.map((w, i) => <div key={i}>⚠ {w}</div>)}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Mapping Form */}
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Add Subject-Teacher Mapping</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginTop: 0 }}>
            Define which teacher teaches which subject in which section, and how many periods per week.
          </p>
          <form onSubmit={addMapping} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div>
              <label className="label">Section</label>
              <select className="input" required value={mapForm.sectionId}
                onChange={e => setMapForm({ ...mapForm, sectionId: e.target.value })}>
                <option value="">Select section…</option>
                {sections.map(s => <option key={s.sectionId} value={s.sectionId}>{s.sectionName}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Subject</label>
              <select className="input" required value={mapForm.subjectId}
                onChange={e => setMapForm({ ...mapForm, subjectId: e.target.value })}>
                <option value="">Select subject…</option>
                {subjects.map(s => <option key={s.subjectId} value={s.subjectId}>{s.subjectName}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Teacher</label>
              <select className="input" required value={mapForm.teacherId}
                onChange={e => setMapForm({ ...mapForm, teacherId: e.target.value })}>
                <option value="">Select teacher…</option>
                {teachers.map(t => <option key={t.teacherId} value={t.teacherId}>{t.name}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Periods per Week</label>
              <input className="input" type="number" min={1} max={8} value={mapForm.periodsPerWeek}
                onChange={e => setMapForm({ ...mapForm, periodsPerWeek: e.target.value })} />
            </div>
            <button className="btn" disabled={mapSubmitting}>
              {mapSubmitting ? 'Saving…' : <><IconPlus width={15} height={15} /> Add Mapping</>}
            </button>
          </form>
        </div>
      </div>

      {/* Existing Mappings */}
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Current Mappings ({mappings.length})</h3>
        {mappings.length === 0 ? <div className="empty-state">No mappings defined yet.</div> : (
          <table>
            <thead><tr><th>Section</th><th>Subject</th><th>Teacher</th><th>Periods/Week</th><th></th></tr></thead>
            <tbody>
              {mappings.map(m => (
                <tr key={m.mappingId}>
                  <td>{m.section?.sectionName || sectionName(m.section?.sectionId)}</td>
                  <td>{m.subject?.subjectName || subjectName(m.subject?.subjectId)}</td>
                  <td>{m.teacher?.name || teacherName(m.teacher?.teacherId)}</td>
                  <td style={{ fontWeight: 700 }}>{m.periodsPerWeek}</td>
                  <td>
                    <button className="btn-icon-danger" onClick={() => deleteMapping(m.mappingId)}>×</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
