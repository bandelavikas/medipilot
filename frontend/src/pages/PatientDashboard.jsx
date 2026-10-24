import { useCallback, useEffect, useState } from 'react'
import AdherencePanel from '../components/AdherencePanel.jsx'
import DashboardLayout from '../components/DashboardLayout.jsx'
import ContactDoctorCard from '../components/ContactDoctorCard.jsx'
import ReminderNotifications from '../components/ReminderNotifications.jsx'
import DoseCard from '../components/DoseCard.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const REMINDER_MINUTES = 15

function toDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatTime(value) {
  return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function formatDay(value) {
  return new Date(value).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' })
}

function PatientDashboard() {
  const { authRequest } = useAuth()
  const [todayDoses, setTodayDoses] = useState([])
  const [upcomingDoses, setUpcomingDoses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [now, setNow] = useState(() => new Date())
  const [refreshKey, setRefreshKey] = useState(0)

  const loadDoses = useCallback(async () => {
    try {
      const today = new Date()
      const tomorrow = new Date(today)
      tomorrow.setDate(today.getDate() + 1)
      const weekAhead = new Date(today)
      weekAhead.setDate(today.getDate() + 7)

      const [todayData, upcomingData] = await Promise.all([
        authRequest('/api/patient/schedule'),
        authRequest(
          `/api/patient/schedule?from=${toDateString(tomorrow)}&to=${toDateString(weekAhead)}`,
        ),
      ])
      setTodayDoses(todayData)
      setUpcomingDoses(upcomingData)
      setError('')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [authRequest])

  useEffect(() => {
    loadDoses()
    const timer = setInterval(() => {
      setNow(new Date())
      setRefreshKey((key) => key + 1)
      loadDoses()
    }, 60000)
    return () => clearInterval(timer)
  }, [loadDoses])

  async function handleRespond(dose, status) {
    setBusyId(dose.id)
    setError('')
    setMessage('')
    try {
      await authRequest(`/api/patient/schedule/${dose.id}/respond`, {
        method: 'POST',
        body: { status },
      })
      setMessage(
        status === 'TAKEN'
          ? `${dose.medicineName} marked as taken.`
          : `${dose.medicineName} marked as missed.`,
      )
      setNow(new Date())
      setRefreshKey((key) => key + 1)
      await loadDoses()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyId(null)
    }
  }

  const taken = todayDoses.filter((dose) => dose.status === 'TAKEN').length
  const missed = todayDoses.filter((dose) => dose.status === 'MISSED').length
  const pending = todayDoses.filter((dose) => dose.status === 'PENDING').length

  const reminders = todayDoses.filter((dose) => {
    if (dose.status !== 'PENDING') {
      return false
    }
    const minutesAway = (new Date(dose.scheduledAt) - now) / 60000
    return minutesAway <= REMINDER_MINUTES
  })

  return (
    <DashboardLayout title="Patient Dashboard">
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      {reminders.length > 0 && (
        <div className="reminder-banner">
          <strong>Reminder</strong>
          {reminders.map((dose) => {
            const late = new Date(dose.scheduledAt) < now
            return (
              <div key={dose.id}>
                {dose.medicineName} ({dose.dosage}) {late ? 'was due at' : 'is due at'}{' '}
                {formatTime(dose.scheduledAt)}
              </div>
            )
          })}
          <div className="reminder-note">
            A reminder does not record anything. Please press Taken or Missed yourself.
          </div>
        </div>
      )}

      <div className="card">
        <h2>Today&apos;s schedule</h2>

        <div className="summary-row">
          <span className="summary-chip">Taken: {taken}</span>
          <span className="summary-chip">Missed: {missed}</span>
          <span className="summary-chip">Pending: {pending}</span>
        </div>

        {loading && <p>Loading your schedule...</p>}
        {!loading && todayDoses.length === 0 && <p>No doses are scheduled for today.</p>}

        {todayDoses.map((dose) => (
          <DoseCard
            key={dose.id}
            dose={dose}
            now={now}
            busy={busyId === dose.id}
            onRespond={handleRespond}
          />
        ))}
      </div>
            <ReminderNotifications doses={todayDoses} now={now} />
      <ContactDoctorCard />
      <div className="card section-gap">
        <h2>My adherence</h2>
        <AdherencePanel endpoint="/api/patient/adherence" refreshKey={refreshKey} />
      </div>

      <div className="card section-gap">
        <h2>Upcoming (next 7 days)</h2>
        {!loading && upcomingDoses.length === 0 && <p>No upcoming doses.</p>}
        {upcomingDoses.map((dose) => (
          <div key={dose.id} className="upcoming-row">
            <span className="upcoming-when">
              {formatDay(dose.scheduledAt)} · {formatTime(dose.scheduledAt)}
            </span>
            <span>
              {dose.medicineName} ({dose.dosage})
            </span>
          </div>
        ))}
      </div>

      <p className="notice">
        MediPilot tracks your medication schedule. It is not a replacement for professional
        medical advice.
      </p>
    </DashboardLayout>
  )
}

export default PatientDashboard