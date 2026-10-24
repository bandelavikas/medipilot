import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import CallButtons from './CallButtons.jsx'

function ContactDoctorCard() {
  const { authRequest } = useAuth()
  const [doctor, setDoctor] = useState(null)
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const data = await authRequest('/api/patient/my-doctor')
        if (!cancelled) {
          setDoctor(data)
        }
      } catch (err) {
        if (!cancelled) {
          setMessage(err.status === 404 ? 'No doctor has been assigned to you yet.' : err.message)
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
  }, [authRequest])

  return (
    <div className="card section-gap">
      <h2>Contact my doctor</h2>
      {loading && <p>Loading...</p>}
      {message && <p className="hint">{message}</p>}
      {doctor && (
        <>
          <p className="rx-meta">
            {doctor.fullName}
            {doctor.specialization ? ` · ${doctor.specialization}` : ''}
          </p>
          <CallButtons peerUserId={doctor.userId} peerName={doctor.fullName} />
        </>
      )}
    </div>
  )
}

export default ContactDoctorCard