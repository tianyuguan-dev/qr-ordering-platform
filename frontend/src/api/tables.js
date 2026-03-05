import { apiFetch } from './client'

const base = (restaurantId) => `/restaurants/${encodeURIComponent(restaurantId)}`

/** @param {string} restaurantId */
export function getTables(restaurantId) {
  return apiFetch(`${base(restaurantId)}/tables`)
}

/**
 * @param {string} restaurantId
 * @param {{ tableNumber: string, qrCodeUrl?: string, seats?: number }} body
 */
export function createTable(restaurantId, body) {
  return apiFetch(`${base(restaurantId)}/tables`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/**
 * @param {string} restaurantId
 * @param {number} id
 * @param {{ tableNumber?: string, qrCodeUrl?: string, seats?: number, status?: number }} body
 */
export function updateTable(restaurantId, id, body) {
  return apiFetch(`${base(restaurantId)}/tables/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

/** @param {string} restaurantId @param {number} id */
export function deleteTable(restaurantId, id) {
  return apiFetch(`${base(restaurantId)}/tables/${id}`, { method: 'DELETE' })
}
