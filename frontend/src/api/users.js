import { apiFetch } from './client'

/**
 * List restaurant staff users with pagination.
 * @param {string} tenantId - Restaurant ID
 * @param {{ page?: number, size?: number }} [params]
 * @returns {Promise<{ content: Array<{ id: number, username: string, role: { code, name }, email?: string, createdAt: string }>, totalElements: number, totalPages: number }>}
 */
export function getRestaurantUsers(tenantId, params = {}) {
  const sp = new URLSearchParams()
  if (params.page != null) sp.set('page', String(params.page))
  if (params.size != null) sp.set('size', String(params.size))
  const qs = sp.toString()
  return apiFetch(`/restaurants/${encodeURIComponent(tenantId)}/users${qs ? '?' + qs : ''}`)
}

/**
 * Create a restaurant staff user (admin, waiter, or kitchen).
 * @param {string} tenantId - Restaurant ID
 * @param {{ username: string, password: string, role: number, email?: string }} body
 * @returns {Promise<any>}
 */
export function createRestaurantUser(tenantId, body) {
  return apiFetch(`/restaurants/${encodeURIComponent(tenantId)}/users`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/**
 * List all platform administrators.
 * @returns {Promise<Array<{ id: number, username: string, email?: string, createdAt: string }>>}
 */
export function getPlatformAdmins() {
  return apiFetch('/platform-admins')
}

/**
 * Create a platform administrator.
 * @param {{ username: string, password: string, email?: string }} body
 * @returns {Promise<any>}
 */
export function createPlatformAdmin(body) {
  return apiFetch('/platform-admins', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
