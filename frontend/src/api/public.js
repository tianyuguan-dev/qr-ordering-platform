/**
 * Public API for customer H5 (no auth required).
 * Uses same API_BASE but no Authorization header.
 */
const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

/** @param {string} restaurantId @returns {Promise<{ name: string, logoUrl: string }>} */
export function getPublicRestaurantInfo(restaurantId) {
  return fetch(`${API_BASE}/public/restaurants/${encodeURIComponent(restaurantId)}/info`).then((res) => {
    if (!res.ok) throw new Error(res.statusText)
    return res.json()
  })
}

/**
 * @param {string} restaurantId
 * @returns {Promise<{ categories: Array<{id, name, sortOrder}>, items: Array<{id, categoryId, name, description, price, imageUrl, allergens}> }>}
 */
export function getPublicMenu(restaurantId) {
  return fetch(`${API_BASE}/public/restaurants/${encodeURIComponent(restaurantId)}/menu`).then((res) => {
    if (!res.ok) throw new Error(res.statusText)
    return res.json()
  })
}

/**
 * @param {string} restaurantId
 * @returns {Promise<Array<{ id, tableNumber, qrCodeUrl, seats, status }>>}
 */
export function getPublicTables(restaurantId) {
  return fetch(`${API_BASE}/public/restaurants/${encodeURIComponent(restaurantId)}/tables`).then((res) => {
    if (!res.ok) throw new Error(res.statusText)
    return res.json()
  })
}

/**
 * @param {string} restaurantId
 * @param {{ tableId: number, items: Array<{ menuItemId: number, quantity: number }>, customerNotes?: string }} body
 * @param {{ idempotencyKey?: string }} [options] - idempotencyKey to prevent duplicate orders on retry
 * @returns {Promise<{ id, orderNumber, tableId, status, totalAmount, items, ... }>}
 */
export function createOrder(restaurantId, body, options = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (options.idempotencyKey) {
    headers['Idempotency-Key'] = options.idempotencyKey
  }
  return fetch(`${API_BASE}/public/restaurants/${encodeURIComponent(restaurantId)}/orders`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  }).then((res) => {
    if (!res.ok) {
      return res.text().then((t) => {
        let msg = t
        try {
          const j = JSON.parse(t)
          msg = j.message || msg
        } catch (_) {}
        throw new Error(msg)
      })
    }
    return res.json()
  })
}
