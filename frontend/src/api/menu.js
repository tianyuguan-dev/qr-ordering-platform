import { apiFetch } from './client'

const base = (restaurantId) => `/restaurants/${encodeURIComponent(restaurantId)}`

// ---------- Categories ----------

/** @param {string} restaurantId - "me" or restaurant id */
export function getCategories(restaurantId) {
  return apiFetch(`${base(restaurantId)}/categories`)
}

/**
 * @param {string} restaurantId
 * @param {{ name: string, sortOrder?: number }} body
 */
export function createCategory(restaurantId, body) {
  return apiFetch(`${base(restaurantId)}/categories`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/**
 * @param {string} restaurantId
 * @param {number} id
 * @param {{ name?: string, sortOrder?: number }} body
 */
export function updateCategory(restaurantId, id, body) {
  return apiFetch(`${base(restaurantId)}/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

/** @param {string} restaurantId @param {number} id */
export function deleteCategory(restaurantId, id) {
  return apiFetch(`${base(restaurantId)}/categories/${id}`, { method: 'DELETE' })
}

// ---------- Menu items ----------

/**
 * @param {string} restaurantId
 * @param {{ page?: number, size?: number, categoryId?: number, status?: number }} [params]
 */
export function getMenuItems(restaurantId, params = {}) {
  const sp = new URLSearchParams()
  if (params.page != null) sp.set('page', String(params.page))
  if (params.size != null) sp.set('size', String(params.size))
  if (params.categoryId != null) sp.set('categoryId', String(params.categoryId))
  if (params.status != null) sp.set('status', String(params.status))
  const q = sp.toString()
  return apiFetch(`${base(restaurantId)}/menu-items${q ? `?${q}` : ''}`)
}

/** @param {string} restaurantId */
export function getAllMenuItems(restaurantId) {
  return apiFetch(`${base(restaurantId)}/menu-items/all`)
}

/** @param {string} restaurantId @param {number} id */
export function getMenuItem(restaurantId, id) {
  return apiFetch(`${base(restaurantId)}/menu-items/${id}`)
}

/**
 * @param {string} restaurantId
 * @param {{ categoryId?: number, name: string, description?: string, price: number, imageUrl?: string, allergens?: string }} body
 */
export function createMenuItem(restaurantId, body) {
  return apiFetch(`${base(restaurantId)}/menu-items`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

/**
 * @param {string} restaurantId
 * @param {number} id
 * @param {{ categoryId?: number, name?: string, description?: string, price?: number, imageUrl?: string, allergens?: string, status?: number }} body
 */
export function updateMenuItem(restaurantId, id, body) {
  return apiFetch(`${base(restaurantId)}/menu-items/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

/** @param {string} restaurantId @param {number} id */
export function deleteMenuItem(restaurantId, id) {
  return apiFetch(`${base(restaurantId)}/menu-items/${id}`, { method: 'DELETE' })
}
