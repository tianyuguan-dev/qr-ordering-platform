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
 * Update restaurant staff (email, role).
 * @param {string} tenantId
 * @param {number} userId
 * @param {{ email?: string, role?: number }} body
 */
export function updateRestaurantUser(tenantId, userId, body) {
  return apiFetch(`/restaurants/${encodeURIComponent(tenantId)}/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

/**
 * Delete restaurant staff. Cannot delete self.
 * @param {string} tenantId
 * @param {number} userId
 */
export function deleteRestaurantUser(tenantId, userId) {
  return apiFetch(`/restaurants/${encodeURIComponent(tenantId)}/users/${userId}`, { method: 'DELETE' })
}

/**
 * Reset restaurant staff password. Cannot reset own.
 * @param {string} tenantId
 * @param {number} userId
 * @param {{ newPassword: string }} body
 */
export function resetRestaurantUserPassword(tenantId, userId, body) {
  return apiFetch(`/restaurants/${encodeURIComponent(tenantId)}/users/${userId}/reset-password`, {
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

/**
 * Update platform admin (email only).
 * @param {number} id
 * @param {{ email?: string }} body
 */
export function updatePlatformAdmin(id, body) {
  return apiFetch(`/platform-admins/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

/**
 * Delete platform admin. Cannot delete self or last admin.
 * @param {number} id
 */
export function deletePlatformAdmin(id) {
  return apiFetch(`/platform-admins/${id}`, { method: 'DELETE' })
}

/**
 * Reset another platform admin's password.
 * @param {number} id
 * @param {{ newPassword: string }} body
 */
export function resetPlatformAdminPassword(id, body) {
  return apiFetch(`/platform-admins/${id}/reset-password`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
