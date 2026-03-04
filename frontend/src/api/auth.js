import { apiFetch } from './client'

/**
 * @param {{ tenantId: string, username: string, password: string }} body
 * @returns {Promise<{ userId: number, tenantId: string, username: string, role: { code: number, name: string, description: string }, accessToken: string }>}
 */
export function login(body) {
  return apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/**
 * Get current user profile (no token in response).
 * @returns {Promise<{ userId: number, tenantId: string, username: string, role: { code: number, name: string, description: string } }>}
 */
export function getProfile() {
  return apiFetch('/auth/me')
}

/**
 * Change current user's password.
 * @param {{ currentPassword: string, newPassword: string }} body
 * @returns {Promise<void>}
 */
export function changePassword(body) {
  return apiFetch('/auth/change-password', {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}
