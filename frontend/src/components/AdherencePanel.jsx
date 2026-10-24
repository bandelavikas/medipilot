import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { addDays, formatDateTime, formatShortDay, toDateString } from '../utils/dates.js'

const TIME_OF_DAY_LABELS = {
  MORNING: 'Morning (05:00 to 11:59)',
  AFTERNOON: 'Afternoon (12:00 to 16:59)',
  EVENING: 'Evening (17:00 to 20:59)',
  NIGHT: 'Night (21:00 to 04:59)',
}

function lastDays(count) {
  const today = new Date()
  return {
    from: toDateString(addDays(today, -(count - 1))),
    to: toDateString(today),
  }
}

function AdherenceChart({ daily }) {
  const maxTotal = Math.max(
    1,
    ...daily.map((day) => day.taken + day.missed + day.awaitingResponse + day.upcoming),
  )

  return (
    <div>
      <div className="chart">
        {daily.map((day) => {
          const parts = [
            { key: 'taken', value: day.taken },
            { key: 'missed', value: day.missed },
            { key: 'awaiting', value: day.awaitingResponse },
            { key: 'upcoming', value: day.upcoming },
          ]
          return (
            <div
              key={day.date}
              className="chart-col"
              title={`${day.date}: taken ${day.taken}, missed ${day.missed}, awaiting response ${day.awaitingResponse}, upcoming ${day.upcoming}`}
            >
              <div className="chart-bar">
                {parts.map(
                  (part) =>
                    part.value > 0 && (
                      <div
                        key={part.key}
                        className={`chart-seg seg-${part.key}`}
                        style={{ height: `${(part.value / maxTotal) * 100}%` }}
                      />
                    ),
                )}
              </div>
              <div className="chart-label">{formatShortDay(day.date)}</div>
            </div>
          )
        })}
      </div>

      <div className="chart-legend">
        <span>
          <i className="legend-dot seg-taken" /> Taken
        </span>
        <span>
          <i className="legend-dot seg-missed" /> Missed
        </span>
        <span>
          <i className="legend-dot seg-awaiting" /> Awaiting response
        </span>
        <span>
          <i className="legend-dot seg-upcoming" /> Upcoming
        </span>
      </div>
    </div>
  )
}

function AdherencePanel({ endpoint, refreshKey = 0 }) {
  const { authRequest } = useAuth()
  const [range, setRange] = useState(() => lastDays(7))
  const [draft, setDraft] = useState(() => lastDays(7))
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const data = await authRequest(`${endpoint}?from=${range.from}&to=${range.to}`)
        if (!cancelled) {
          setReport(data)
          setError('')
        }
      } catch (err) {
        if (!cancelled) {
          setError(err.message)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [authRequest, endpoint, range, refreshKey])

  function applyDraft() {
    if (!draft.from || !draft.to) {
      setError('Please choose both dates.')
      return
    }
    if (draft.to < draft.from) {
      setError('The end date cannot be before the start date.')
      return
    }
    setError('')
    setRange({ ...draft })
  }

  function applyQuick(count) {
    const next = lastDays(count)
    setError('')
    setDraft(next)
    setRange(next)
  }

  return (
    <div>
      <div className="range-row">
        <label>
          From
          <input
            type="date"
            value={draft.from}
            onChange={(e) => setDraft({ ...draft, from: e.target.value })}
          />
        </label>
        <label>
          To
          <input
            type="date"
            value={draft.to}
            onChange={(e) => setDraft({ ...draft, to: e.target.value })}
          />
        </label>
        <button type="button" className="btn btn-primary" onClick={applyDraft}>
          Apply
        </button>
        <button type="button" className="btn btn-secondary" onClick={() => applyQuick(7)}>
          Last 7 days
        </button>
        <button type="button" className="btn btn-secondary" onClick={() => applyQuick(30)}>
          Last 30 days
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p>Loading adherence...</p>}

      {report && (
        <>
          <div className="adherence-percent">
            {report.adherencePercent === null ? 'No data yet' : `${report.adherencePercent}%`}
          </div>
          <div className="adherence-sub">
            {formatShortDay(report.from)} to {formatShortDay(report.to)} · Taken ÷ (Taken + Missed),
            counting only doses that were answered
          </div>

          <div className="summary-row">
            <span className="summary-chip">Taken: {report.taken}</span>
            <span className="summary-chip">Missed: {report.missed}</span>
            <span className="summary-chip">Awaiting response: {report.awaitingResponse}</span>
            <span className="summary-chip">Upcoming: {report.upcoming}</span>
          </div>

          {report.awaitingResponse > 0 && (
            <div className="alert alert-warning">
              {report.awaitingResponse} dose(s) are past their time with no answer yet. They are
              not counted as missed until the patient records them.
            </div>
          )}

          <h3>Doses per day</h3>
          <AdherenceChart daily={report.daily} />

          {report.byMedicine.length > 0 && (
            <>
              <h3>By medicine</h3>
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Medicine</th>
                      <th>Taken</th>
                      <th>Missed</th>
                      <th>Awaiting</th>
                      <th>Adherence</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.byMedicine.map((item) => (
                      <tr key={item.medicineName}>
                        <td>{item.medicineName}</td>
                        <td>{item.taken}</td>
                        <td>{item.missed}</td>
                        <td>{item.awaitingResponse}</td>
                        <td>{item.adherencePercent === null ? '-' : `${item.adherencePercent}%`}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}

          <h3>Missed doses by time of day</h3>
          {report.missed === 0 ? (
            <p className="hint">No missed doses in this period.</p>
          ) : (
            <ul className="plain-list">
              {Object.entries(report.missedByTimeOfDay).map(([part, count]) => (
                <li key={part}>
                  {TIME_OF_DAY_LABELS[part]}: <strong>{count}</strong>
                </li>
              ))}
            </ul>
          )}

          {report.recentMissed.length > 0 && (
            <>
              <h3>Most recent missed doses</h3>
              <ul className="plain-list">
                {report.recentMissed.map((item, index) => (
                  <li key={index}>
                    {formatDateTime(item.scheduledAt)} · {item.medicineName} ({item.dosage})
                  </li>
                ))}
              </ul>
            </>
          )}

          <p className="notice">
            These figures only track the answers recorded in MediPilot. They do not show clinical
            outcomes and are not medical advice.
          </p>
        </>
      )}
    </div>
  )
}

export default AdherencePanel