function formatTime(value) {
  return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function DoseCard({ dose, now, busy, onRespond }) {
  const overdue = dose.status === 'PENDING' && new Date(dose.scheduledAt) < now
  const label = overdue ? 'Overdue' : dose.status.charAt(0) + dose.status.slice(1).toLowerCase()
  const statusClass = overdue ? 'overdue' : dose.status.toLowerCase()

  return (
    <div className="dose-card">
      <div className="dose-time">{formatTime(dose.scheduledAt)}</div>

      <div className="dose-info">
        <div className="dose-name">{dose.medicineName}</div>
        <div className="dose-detail">{dose.dosage}</div>
        {dose.instructions && <div className="dose-instructions">{dose.instructions}</div>}
      </div>

      <div className="dose-actions">
        <span className={`status-pill status-${statusClass}`}>{label}</span>
        {dose.canMarkTaken && (
          <button
            type="button"
            className="btn btn-primary"
            disabled={busy}
            onClick={() => onRespond(dose, 'TAKEN')}
          >
            Taken
          </button>
        )}
        {dose.canMarkMissed && (
          <button
            type="button"
            className="btn btn-secondary"
            disabled={busy}
            onClick={() => onRespond(dose, 'MISSED')}
          >
            Missed
          </button>
        )}
      </div>
    </div>
  )
}

export default DoseCard