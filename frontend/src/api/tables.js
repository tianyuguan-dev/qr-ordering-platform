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

/** @param {string} restaurantId @param {number} tableId - Checkout summary for table (active orders + total) */
export function getCheckoutSummary(restaurantId, tableId) {
  return apiFetch(`${base(restaurantId)}/tables/${tableId}/checkout-summary`)
}

/** @param {string} restaurantId @param {number} tableId - Checkout table (complete/cancel orders, set table available) */
export function checkoutTable(restaurantId, tableId) {
  return apiFetch(`${base(restaurantId)}/tables/${tableId}/checkout`, { method: 'POST' })
}
