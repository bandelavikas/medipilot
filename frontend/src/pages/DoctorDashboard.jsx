import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardLayout from '../components/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(`${value}T00:00:00`).toLocaleDateString([], {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

function DoctorDashboard() {
  const { authRequest } = useAuth()
  const [patients, setPatients] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [email, setEmail] = useState('')
  const [adding, setAdding] = useState(false)

  const loadPatients = useCallback(async () => {
    try {
      const data = await authRequest('/api/doctor/patients')
      setPatients(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [authRequest])

  useEffect(() => {
    loadPatients()
  }, [loadPatients])

  async function handleAdd(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    setAdding(true)
    try {
      const patient = await authRequest('/api/doctor/patients', {
        method: 'POST',
        body: { email: email.trim() },
      })
      setMessage(`${patient.fullName} was added to your patient list.`)
      setEmail('')
      await loadPatients()
    } catch (err) {
      setError(err.message)
    } finally {
      setAdding(false)
    }
  }

  return (
    <DashboardLayout title="Doctor Dashboard">
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-number">{patients.length}</div>
          <div className="stat-label">Assigned patients</div>
        </div>
      </div>

      <div className="card section-gap">
        <h2>Add a patient</h2>
        <p className="hint">
          Enter the email the patient used to register. A patient who already has a doctor
          cannot be added.
        </p>
        <form className="inline-form" onSubmit={handleAdd}>
          <input
            type="email"
            placeholder="patient@example.com"
            aria-label="Patient email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <button type="submit" className="btn btn-primary" disabled={adding}>
            {adding ? 'Adding...' : 'Add patient'}
          </button>
        </form>
      </div>

      <div className="card section-gap">
        <h2>My patients</h2>
        {loading && <p>Loading patients...</p>}
        {!loading && patients.length === 0 && <p>You have no patients yet. Add one above.</p>}

        {patients.length > 0 && (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Date of birth</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {patients.map((patient) => (
                  <tr key={patient.patientId}>
                    <td>{patient.fullName}</td>
                    <td>{patient.email}</td>
                    <td>{patient.phone || '-'}</td>
                    <td>{formatDate(patient.dateOfBirth)}</td>
                    <td>
                      <Link to={`/doctor/patients/${patient.patientId}`}>Prescriptions</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <p className="notice">
        MediPilot tracks medication schedules. It does not diagnose conditions or replace
        professional medical judgement.
      </p>
    </DashboardLayout>
  )
}

export default DoctorDashboard