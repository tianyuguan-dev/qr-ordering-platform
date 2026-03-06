import { getToken, setToken } from './client'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

/**
 * Upload an image file. Returns the public URL of the stored file.
 * @param {File} file - Image file (JPEG, PNG, GIF, WebP; max 5MB)
 * @param {{ prefix?: string }} [options] - Optional prefix (e.g. 'logos', 'dishes')
 * @returns {Promise<{ url: string }>}
 */
export async function uploadImage(file, options = {}) {
  const form = new FormData()
  form.append('file', file)
  if (options.prefix) form.append('prefix', options.prefix)

  const url = `${API_BASE}/upload`
  const token = getToken()
  const headers = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, {
    method: 'POST',
    headers,
    body: form,
  })

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
  return res.json()
}
