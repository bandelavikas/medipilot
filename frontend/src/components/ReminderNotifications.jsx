import { useEffect, useRef, useState } from 'react'

const NOTIFIED_KEY = 'medipilot_notified_doses'
const UPCOMING_MINUTES = 15
const LATE_MINUTES = 30

function notificationsSupported() {
  return typeof window !== 'undefined' && 'Notification' in window
}

function loadNotified() {
  try {
    return new Set(JSON.parse(sessionStorage.getItem(NOTIFIED_KEY) || '[]'))
  } catch {
    return new Set()
  }
}

function saveNotified(ids) {
  try {
    sessionStorage.setItem(NOTIFIED_KEY, JSON.stringify([...ids]))
  } catch {
    // Storage can be unavailable. Reminders still work, they just might repeat.
  }
}

function ReminderNotifications({ doses, now }) {
  const [permission, setPermission] = useState(() =>
    notificationsSupported() ? Notification.permission : 'unsupported',
  )
  const notifiedRef = useRef(loadNotified())

  useEffect(() => {
    if (permission !== 'granted') {
      return
    }

    doses.forEach((dose) => {
      if (dose.status !== 'PENDING' || notifiedRef.current.has(dose.id)) {
        return
      }

      const minutesAway = (new Date(dose.scheduledAt) - now) / 60000
      if (minutesAway > UPCOMING_MINUTES || minutesAway < -LATE_MINUTES) {
        return
      }

      const time = new Date(dose.scheduledAt).toLocaleTimeString([], {
        hour: '2-digit',
        minute: '2-digit',
      })

      try {
        new Notification('Time for your medicine', {
          body: `${dose.medicineName} (${dose.dosage}) at ${time}. Open MediPilot and press Taken or Missed yourself.`,
          tag: `dose-${dose.id}`,
        })
      } catch {
        // Some browsers refuse to show a notification. Ignore it.
      }

      notifiedRef.current.add(dose.id)
      saveNotified(notifiedRef.current)
    })
  }, [doses, now, permission])

  async function turnOn() {
    const result = await Notification.requestPermission()
    setPermission(result)
  }

  return (
    <div className="card section-gap">
      <h2>Browser reminders</h2>

      {permission === 'unsupported' && (
        <p className="hint">This browser does not support notifications.</p>
      )}

      {permission === 'default' && (
        <>
          <p className="hint">
            Get a pop-up when a dose is due, while MediPilot is open in a browser tab. A
            reminder never records anything: you still press Taken or Missed yourself.
          </p>
          <button type="button" className="btn btn-primary" onClick={turnOn}>
            Turn on browser reminders
          </button>
        </>
      )}

      {permission === 'granted' && (
        <p className="hint">
          Browser reminders are on. You will get a pop-up when a dose is due, while MediPilot is
          open in a browser tab.
        </p>
      )}

      {permission === 'denied' && (
        <p className="hint">
          Notifications are blocked for this site. To turn them on, click the lock icon next to the
          address bar and allow Notifications, then reload the page.
        </p>
      )}
    </div>
  )
}

export default ReminderNotifications