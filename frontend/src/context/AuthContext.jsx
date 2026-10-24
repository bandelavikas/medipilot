import { createContext, useCallback, useContext, useState } from 'react'
import { apiRequest, clearStoredAuth, loadStoredAuth, saveStoredAuth } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => loadStoredAuth())
  const token = auth ? auth.token : null

  function startSession(data) {
    saveStoredAuth(data)
    setAuth(data)
    return data
  }

  const logout = useCallback(() => {
    clearStoredAuth()
    setAuth(null)
  }, [])

  async function login(email, password) {
    const data = await apiRequest('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    })
    return startSession(data)
  }

  async function register(details) {
    const data = await apiRequest('/api/auth/register', {
      method: 'POST',
      body: details,
    })
    return startSession(data)
  }

  const authRequest = useCallback(
    async (path, options = {}) => {
      try {
        return await apiRequest(path, { ...options, token })
      } catch (err) {
        if (err.status === 401) {
          logout()
        }
        throw err
      }
    },
    [token, logout],
  )

  const value = {
    user: auth,
    token,
    isLoggedIn: Boolean(auth),
    login,
    register,
    logout,
    authRequest,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}