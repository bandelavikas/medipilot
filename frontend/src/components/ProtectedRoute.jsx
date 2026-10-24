import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

const HOME_BY_ROLE = {
  DOCTOR: '/doctor',
  PATIENT: '/patient',
  ADMIN: '/admin',
}

function ProtectedRoute({ allowedRole, children }) {
  const { isLoggedIn, user } = useAuth()

  if (!isLoggedIn) {
    return <Navigate to="/login" replace />
  }

  if (allowedRole && user.role !== allowedRole) {
    return <Navigate to={HOME_BY_ROLE[user.role] || '/login'} replace />
  }

  return children
}

export default ProtectedRoute