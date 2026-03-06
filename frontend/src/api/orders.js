import { apiFetch } from './client'

const base = (restaurantId) => `/restaurants/${encodeURIComponent(restaurantId)}`

/**
 * @param {string} restaurantId
 * @param {{ status?: number | number[], page?: number, size?: number }} [params] - status: single code or array of codes for multi-select filter
 */
export function getOrders(restaurantId, params = {}) {
  const q = new URLSearchParams()
  if (params.status != null) {
    const statuses = Array.isArray(params.status) ? params.status : [params.status]
    statuses.forEach((s) => q.append('status', s))
  }
  if (params.page != null) q.set('page', params.page)
  if (params.size != null) q.set('size', params.size)
  const qs = q.toString()
  return apiFetch(`${base(restaurantId)}/orders${qs ? '?' + qs : ''}`)
}

/** @param {string} restaurantId @param {number} orderId */
export function getOrder(restaurantId, orderId) {
  return apiFetch(`${base(restaurantId)}/orders/${orderId}`)
}

/**
 * @param {string} restaurantId
 * @param {number} orderId
 * @param {{ status: number }} body
 */
export function updateOrderStatus(restaurantId, orderId, body) {
  return apiFetch(`${base(restaurantId)}/orders/${orderId}/status`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  })
}
