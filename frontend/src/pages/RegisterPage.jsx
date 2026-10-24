import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const HOME_BY_ROLE = {
  DOCTOR: '/doctor',
  PATIENT: '/patient',
  ADMIN: '/admin',
}

const EMPTY_FORM = {
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
  role: 'PATIENT',
  phone: '',
  specialization: '',
  dateOfBirth: '',
}

function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    const payload = {
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      password: form.password,
      role: form.role,
      phone: form.phone.trim() || null,
      specialization: form.role === 'DOCTOR' ? form.specialization.trim() || null : null,
      dateOfBirth: form.role === 'PATIENT' ? form.dateOfBirth || null : null,
    }

    setLoading(true)
    try {
      const data = await register(payload)
      navigate(HOME_BY_ROLE[data.role] || '/login', { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Create your account</h1>
        <p className="subtitle">Join MediPilot as a doctor or a patient.</p>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          <label htmlFor="role">I am a</label>
          <select id="role" name="role" value={form.role} onChange={handleChange}>
            <option value="PATIENT">Patient</option>
            <option value="DOCTOR">Doctor</option>
          </select>

          <label htmlFor="fullName">Full name</label>
          <input
            id="fullName"
            name="fullName"
            type="text"
            value={form.fullName}
            onChange={handleChange}
            maxLength={100}
            required
          />

          <label htmlFor="email">Email</label>
          <input
            id="email"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            autoComplete="email"
            required
          />

          <label htmlFor="password">Password (at least 8 characters)</label>
          <input
            id="password"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            autoComplete="new-password"
            required
          />

          <label htmlFor="confirmPassword">Confirm password</label>
          <input
            id="confirmPassword"
            name="confirmPassword"
            type="password"
            value={form.confirmPassword}
            onChange={handleChange}
            autoComplete="new-password"
            required
          />

          <label htmlFor="phone">Phone (optional)</label>
          <input
            id="phone"
            name="phone"
            type="tel"
            value={form.phone}
            onChange={handleChange}
            maxLength={20}
          />

          {form.role === 'DOCTOR' && (
            <>
              <label htmlFor="specialization">Specialization (optional)</label>
              <input
                id="specialization"
                name="specialization"
                type="text"
                value={form.specialization}
                onChange={handleChange}
                maxLength={100}
              />
            </>
          )}

          {form.role === 'PATIENT' && (
            <>
              <label htmlFor="dateOfBirth">Date of birth (optional)</label>
              <input
                id="dateOfBirth"
                name="dateOfBirth"
                type="date"
                value={form.dateOfBirth}
                onChange={handleChange}
              />
            </>
          )}

          <button type="submit" disabled={loading}>
            {loading ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p className="switch-link">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
        <p className="notice">
          Please use fictional details while MediPilot is being developed and demonstrated.
        </p>
      </div>
    </div>
  )
}

export default RegisterPage