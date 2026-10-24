import { useCallback, useEffect, useState } from 'react'
import DashboardLayout from '../components/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const ROLE_FILTERS = [
  { value: '', label: 'All roles' },
  { value: 'DOCTOR', label: 'Doctors' },
  { value: 'PATIENT', label: 'Patients' },
  { value: 'ADMIN', label: 'Admins' },
]

function formatDate(value) {
  return new Date(value).toLocaleDateString([], {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

function AdminDashboard() {
  const { authRequest, user } = useAuth()
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [roleFilter, setRoleFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    try {
      const query = roleFilter ? `?role=${roleFilter}` : ''
      const [statsData, usersData] = await Promise.all([
        authRequest('/api/admin/stats'),
        authRequest(`/api/admin/users${query}`),
      ])
      setStats(statsData)
      setUsers(usersData)
      setError('')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [authRequest, roleFilter])

  useEffect(() => {
    load()
  }, [load])

  async function toggleActive(account) {
    const turningOff = account.active

    if (
      turningOff &&
      !window.confirm(
        `Disable the account for ${account.fullName}? They will be signed out and cannot log in until you enable it again.`,
      )
    ) {
      return
    }

    setBusyId(account.id)
    setError('')
    setMessage('')
    try {
      await authRequest(`/api/admin/users/${account.id}/status`, {
        method: 'PATCH',
        body: { active: !account.active },
      })
      setMessage(`${account.fullName} is now ${turningOff ? 'disabled' : 'active'}.`)
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyId(null)
    }
  }

  const cards = stats
    ? [
        { label: 'Total accounts', value: stats.totalUsers },
        { label: 'Doctors', value: stats.doctors },
        { label: 'Patients', value: stats.patients },
        { label: 'Admins', value: stats.admins },
        { label: 'Disabled accounts', value: stats.disabledUsers },
        { label: 'Active prescriptions', value: stats.activePrescriptions },
      ]
    : []

  return (
    <DashboardLayout title="Admin Dashboard">
      {error && <div className="alert alert-error">{error}</div>}
      {message && <div className="alert alert-success">{message}</div>}

      <div className="stat-grid">
        {cards.map((card) => (
          <div key={card.label} className="stat-card">
            <div className="stat-number">{card.value}</div>
            <div className="stat-label">{card.label}</div>
          </div>
        ))}
      </div>

      <div className="card section-gap">
        <div className="toolbar-row">
          <h2>User accounts</h2>
          <select
            aria-label="Filter by role"
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
          >
            {ROLE_FILTERS.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </div>

        {loading && <p>Loading accounts...</p>}
        {!loading && users.length === 0 && <p>No accounts found.</p>}

        {users.length > 0 && (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Joined</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {users.map((account) => {
                  const isSelf = user.userId === account.id
                  return (
                    <tr key={account.id}>
                      <td>{account.fullName}</td>
                      <td>{account.email}</td>
                      <td>
                        <span className="role-badge">{account.role}</span>
                      </td>
                      <td>
                        <span
                          className={`status-pill ${account.active ? 'status-active' : 'status-disabled'}`}
                        >
                          {account.active ? 'Active' : 'Disabled'}
                        </span>
                      </td>
                      <td>{formatDate(account.createdAt)}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          disabled={busyId === account.id || isSelf}
                          title={isSelf ? 'You cannot disable your own account' : ''}
                          onClick={() => toggleActive(account)}
                        >
                          {account.active ? 'Disable' : 'Enable'}
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <p className="notice">
        Administrators manage accounts only. Medical information is never shown on this screen.
      </p>
    </DashboardLayout>
  )
}

export default AdminDashboard