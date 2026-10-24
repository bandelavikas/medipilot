import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import DashboardLayout from '../components/DashboardLayout.jsx'
import CallButtons from '../components/CallButtons.jsx'
import AdherencePanel from '../components/AdherencePanel.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const FREQUENCIES = [
  { value: 'ONCE_DAILY', label: 'Once a day', times: ['08:00'] },
  { value: 'TWICE_DAILY', label: 'Twice a day', times: ['08:00', '20:00'] },
  { value: 'THREE_TIMES_DAILY', label: 'Three times a day', times: ['08:00', '14:00', '20:00'] },
  {
    value: 'FOUR_TIMES_DAILY',
    label: 'Four times a day',
    times: ['08:00', '12:00', '16:00', '20:00'],
  },
]

function toDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function addDays(date, days) {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + days)
  return copy
}

function formatDate(value) {
  return new Date(`${value}T00:00:00`).toLocaleDateString([], {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

function frequencyLabel(value) {
  const found = FREQUENCIES.find((item) => item.value === value)
  return found ? found.label : value
}

function emptyForm() {
  const today = new Date()
  return {
    medicineName: '',
    dosage: '',
    frequency: 'TWICE_DAILY',
    times: [...FREQUENCIES[1].times],
    startDate: toDateString(today),
    endDate: toDateString(addDays(today, 6)),
    instructions: '',
    notes: '',
  }
}

function PatientDetailPage() {
  const { patientId } = useParams()
  const { authRequest } = useAuth()
  const [patient, setPatient] = useState(null)
  const [prescriptions, setPrescriptions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    try {
      const patients = await authRequest('/api/doctor/patients')
      const found = patients.find((item) => String(item.patientId) === patientId)
      if (!found) {
        setPatient(null)
        setError('Patient not found in your list')
        return
      }
      setPatient(found)
      const list = await authRequest(`/api/doctor/patients/${patientId}/prescriptions`)
      setPrescriptions(list)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [authRequest, patientId])

  useEffect(() => {
    load()
  }, [load])

  function handleField(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  function handleFrequency(event) {
    const value = event.target.value
    const option = FREQUENCIES.find((item) => item.value === value)
    setForm((previous) => ({ ...previous, frequency: value, times: [...option.times] }))
  }

  function handleTime(index, value) {
    setForm((previous) => ({
      ...previous,
      times: previous.times.map((time, position) => (position === index ? value : time)),
    }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setMessage('')

    if (form.times.some((time) => !time)) {
      setError('Please fill in every dose time.')
      return
    }
    if (form.endDate < form.startDate) {
      setError('The end date cannot be before the start date.')
      return
    }

    setSaving(true)
    try {
      await authRequest('/api/doctor/prescriptions', {
        method: 'POST',
        body: {
          patientId: Number(patientId),
          medicineName: form.medicineName.trim(),
          dosage: form.dosage.trim(),
          frequency: form.frequency,
          administrationTimes: form.times,
          startDate: form.startDate,
          endDate: form.endDate,
          instructions: form.instructions.trim() || null,
          notes: form.notes.trim() || null,
        },
      })
      setMessage('Prescription saved and the dose schedule was created.')
      setForm(emptyForm())
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }
  async function handleCancel(item) {
    const confirmed = window.confirm(
      `Cancel ${item.medicineName}? Future doses will be removed. Doses already answered stay in the history.`,
    )
    if (!confirmed) {
      return
    }
    setError('')
    setMessage('')
    try {
      await authRequest(`/api/doctor/prescriptions/${item.id}/cancel`, { method: 'POST' })
      setMessage(`${item.medicineName} was cancelled and its future doses were removed.`)
      await load()
    } catch (err) {
      setError(err.message)
    }
  }
  return (
    <DashboardLayout title={patient ? patient.fullName : 'Patient'}>
      <Link className="back-link" to="/doctor">
        &larr; Back to my patients
      </Link>

      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}
      {loading && <p>Loading...</p>}

      {patient && (
        <>
          <div className="card">
            <h2>Patient details</h2>
            <p className="rx-meta">
              {patient.email} · Phone: {patient.phone || '-'} · Date of birth:{' '}
              {patient.dateOfBirth ? formatDate(patient.dateOfBirth) : '-'}
            </p>            <CallButtons peerUserId={patient.userId} peerName={patient.fullName} />
          </div>
          <div className="card section-gap">
            <h2>Adherence</h2>
            <AdherencePanel endpoint={`/api/doctor/patients/${patientId}/adherence`} />
          </div>
          <div className="card section-gap">
            <h2>New prescription</h2>
            <p className="hint">
              You decide the medicine, dosage and timing. MediPilot only creates the reminder
              schedule from what you enter.
            </p>

            <form onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-field">
                  <label htmlFor="medicineName">Medicine name</label>
                  <input
                    id="medicineName"
                    name="medicineName"
                    value={form.medicineName}
                    onChange={handleField}
                    maxLength={150}
                    required
                  />
                </div>

                <div className="form-field">
                  <label htmlFor="dosage">Dosage</label>
                  <input
                    id="dosage"
                    name="dosage"
                    value={form.dosage}
                    onChange={handleField}
                    maxLength={100}
                    placeholder="for example: 1 tablet"
                    required
                  />
                </div>

                <div className="form-field">
                  <label htmlFor="frequency">Frequency</label>
                  <select
                    id="frequency"
                    name="frequency"
                    value={form.frequency}
                    onChange={handleFrequency}
                  >
                    {FREQUENCIES.map((item) => (
                      <option key={item.value} value={item.value}>
                        {item.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-field">
                  <label htmlFor="startDate">Start date</label>
                  <input
                    id="startDate"
                    name="startDate"
                    type="date"
                    value={form.startDate}
                    onChange={handleField}
                    required
                  />
                </div>

                <div className="form-field">
                  <label htmlFor="endDate">End date</label>
                  <input
                    id="endDate"
                    name="endDate"
                    type="date"
                    value={form.endDate}
                    onChange={handleField}
                    required
                  />
                </div>

                <div className="form-field form-field-wide">
                  <label>Dose times</label>
                  <div className="time-row">
                    {form.times.map((time, index) => (
                      <input
                        key={index}
                        type="time"
                        aria-label={`Dose time ${index + 1}`}
                        value={time}
                        onChange={(e) => handleTime(index, e.target.value)}
                        required
                      />
                    ))}
                  </div>
                </div>

                <div className="form-field form-field-wide">
                  <label htmlFor="instructions">Instructions (optional)</label>
                  <input
                    id="instructions"
                    name="instructions"
                    value={form.instructions}
                    onChange={handleField}
                    maxLength={500}
                    placeholder="for example: take after food"
                  />
                </div>

                <div className="form-field form-field-wide">
                  <label htmlFor="notes">Notes (optional)</label>
                  <textarea
                    id="notes"
                    name="notes"
                    rows={2}
                    value={form.notes}
                    onChange={handleField}
                    maxLength={1000}
                  />
                </div>
              </div>

              <div className="form-actions">
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Save prescription'}
                </button>
              </div>
            </form>
          </div>

          <div className="card section-gap">
            <h2>Prescriptions</h2>
            {prescriptions.length === 0 && <p>No prescriptions yet for this patient.</p>}

            {prescriptions.map((item) => (
              <div key={item.id} className="rx-card">
                <div className="rx-title">
                  <span>
                    {item.medicineName} ({item.dosage})
                  </span>
                  <span className={`status-pill status-${item.status.toLowerCase()}`}>
                    {item.status}
                  </span>
                </div>
                <div className="rx-meta">
                  {frequencyLabel(item.frequency)} at{' '}
                  {item.administrationTimes.map((time) => time.slice(0, 5)).join(', ')}
                </div>
                <div className="rx-meta">
                  {formatDate(item.startDate)} to {formatDate(item.endDate)}
                </div>
                {item.instructions && <div className="rx-meta">Instructions: {item.instructions}</div>}
                {item.notes && <div className="rx-meta">Notes: {item.notes}</div>}
                                {item.status === 'ACTIVE' && (
                  <button
                    type="button"
                    className="btn btn-secondary rx-cancel"
                    onClick={() => handleCancel(item)}
                  >
                    Cancel prescription
                  </button>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </DashboardLayout>
  )
}

export default PatientDetailPage