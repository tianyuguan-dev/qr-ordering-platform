import { apiFetch } from './client'

/**
 * @param {{ page?: number, size?: number }} [params]
 * @returns {Promise<{ content: any[], totalElements: number, totalPages: number, number: number, size: number }>}
 */
export function getRestaurants(params = {}) {
  const sp = new URLSearchParams()
  if (params.page != null) sp.set('page', String(params.page))
  if (params.size != null) sp.set('size', String(params.size))
  const q = sp.toString()
  return apiFetch(`/restaurants${q ? `?${q}` : ''}`)
}

/**
 * @param {string} id
 * @returns {Promise<any>}
 */
export function getRestaurant(id) {
  return apiFetch(`/restaurants/${encodeURIComponent(id)}`)
}

/**
 * @param {{ name: string, description?: string, logoUrl?: string, address?: string, phone?: string }} body
 * @returns {Promise<any>}
 */
export function createRestaurant(body) {
  return apiFetch('/restaurants', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/**
 * @param {string} id
 * @param {{ name?: string, description?: string, logoUrl?: string, address?: string, phone?: string, status?: number }} body
 * @returns {Promise<any>}
 */
export function updateRestaurant(id, body) {
  return apiFetch(`/restaurants/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

/**
 * @param {string} id
 * @returns {Promise<void>}
 */
export function deleteRestaurant(id) {
  return apiFetch(`/restaurants/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
