const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const STORAGE_KEY = 'medipilot_auth'

export function loadStoredAuth() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function saveStoredAuth(auth) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
}

export function clearStoredAuth() {
  sessionStorage.removeItem(STORAGE_KEY)
}

export async function apiRequest(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new Error('Cannot reach the server. Please check that the backend is running.')
  }

  let data = null
  const text = await response.text()
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!response.ok) {
    const message = (data && (data.message || data.error)) || `Request failed (${response.status})`
    const error = new Error(message)
    error.status = response.status
    throw error
  }

  return data
}