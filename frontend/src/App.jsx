import { Link, Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import DoctorDashboard from './pages/DoctorDashboard.jsx'
import PatientDetailPage from './pages/PatientDetailPage.jsx'
import PatientDashboard from './pages/PatientDashboard.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import CallOverlay from './components/CallOverlay.jsx'

function NotFoundPage() {
  return (
    <div style={{ padding: '2rem' }}>
      <h1>Page not found</h1>
      <Link to="/login">Go to login</Link>
    </div>
  )
}

function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/doctor"
          element={
            <ProtectedRoute allowedRole="DOCTOR">
              <DoctorDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/doctor/patients/:patientId"
          element={
            <ProtectedRoute allowedRole="DOCTOR">
              <PatientDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/patient"
          element={
            <ProtectedRoute allowedRole="PATIENT">
              <PatientDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRole="ADMIN">
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      <CallOverlay />
    </>
  )
}

export default App