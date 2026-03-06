const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

export function getToken() {
  return sessionStorage.getItem('accessToken')
}

export function setToken(token) {
  if (token) sessionStorage.setItem('accessToken', token)
  else sessionStorage.removeItem('accessToken')
}

/**
 * @param {string} path - e.g. '/auth/login' or '/restaurants'
 * @param {RequestInit} [options]
 * @returns {Promise<any>} - JSON or undefined for 204
 */
export async function apiFetch(path, options = {}) {
  const url = path.startsWith('http') ? path : `${API_BASE}${path}`
  const token = getToken()
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, { ...options, headers })
  if (!res.ok) {
    if (res.status === 401) {
      setToken(null)
      sessionStorage.removeItem('user')
      window.location.href = '/login'
    }
    let message = await res.text()
    try {
      const j = JSON.parse(message)
      message = j.message || message
    } catch (_) {}
    const err = new Error(message)
    err.status = res.status
    throw err
  }
  if (res.status === 204) return undefined
  return res.json()
}
