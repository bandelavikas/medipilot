import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

function DashboardLayout({ title, children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="brand">MediPilot</div>
        <div className="header-user">
          <span className="user-name">{user.fullName}</span>
          <span className="role-badge">{user.role}</span>
          <button type="button" className="logout-button" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </header>
      <main className="dashboard-content">
        <h1>{title}</h1>
        {children}
      </main>
    </div>
  )
}

export default DashboardLayout