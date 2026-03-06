import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch } from './client'
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  getMenuItems,
  getAllMenuItems,
  createMenuItem,
  updateMenuItem,
  deleteMenuItem,
  updateMenuItemStatus,
} from './menu'

vi.mock('./client', () => ({ apiFetch: vi.fn() }))

describe('menu API', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockClear()
    vi.mocked(apiFetch).mockResolvedValue({})
  })

  // ------------------------------------------------------------------ categories

  describe('getCategories', () => {
    it('calls GET /restaurants/{id}/categories', async () => {
      await getCategories('me')
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/categories')
    })
  })

  describe('createCategory', () => {
    it('calls POST with body', async () => {
      await createCategory('me', { name: 'Drinks', sortOrder: 2 })
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/categories', {
        method: 'POST',
        body: JSON.stringify({ name: 'Drinks', sortOrder: 2 }),
      })
    })
  })

  describe('updateCategory', () => {
    it('calls PUT /restaurants/{id}/categories/{catId} with body', async () => {
      await updateCategory('me', 3, { name: 'Beverages' })
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/categories/3', {
        method: 'PUT',
        body: JSON.stringify({ name: 'Beverages' }),
      })
    })
  })

  describe('deleteCategory', () => {
    it('calls DELETE /restaurants/{id}/categories/{catId}', async () => {
      await deleteCategory('me', 3)
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/categories/3', { method: 'DELETE' })
    })
  })

  // ------------------------------------------------------------------ menu items

  describe('getMenuItems', () => {
    it('calls /restaurants/{id}/menu-items with no query string when params empty', async () => {
      await getMenuItems('me', {})
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/menu-items')
    })

    it('appends categoryId and status when provided', async () => {
      await getMenuItems('me', { categoryId: 5, status: 1 })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).toContain('categoryId=5')
      expect(url).toContain('status=1')
    })

    it('appends page and size', async () => {
      await getMenuItems('me', { page: 0, size: 20 })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).toContain('page=0')
      expect(url).toContain('size=20')
    })
  })

  describe('getAllMenuItems', () => {
    it('calls GET /restaurants/{id}/menu-items/all', async () => {
      await getAllMenuItems('REST-001')
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/REST-001/menu-items/all')
    })
  })

  describe('createMenuItem', () => {
    it('calls POST /restaurants/{id}/menu-items with body', async () => {
      const body = { name: 'Ramen', price: 12.5, categoryId: 1 }
      await createMenuItem('me', body)

      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/menu-items', {
        method: 'POST',
        body: JSON.stringify(body),
      })
    })
  })

  describe('updateMenuItem', () => {
    it('calls PUT /restaurants/{id}/menu-items/{itemId} with body', async () => {
      const body = { price: 15 }
      await updateMenuItem('me', 42, body)

      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/menu-items/42', {
        method: 'PUT',
        body: JSON.stringify(body),
      })
    })
  })

  describe('deleteMenuItem', () => {
    it('calls DELETE /restaurants/{id}/menu-items/{itemId}', async () => {
      await deleteMenuItem('me', 42)
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/menu-items/42', { method: 'DELETE' })
    })
  })

  describe('updateMenuItemStatus', () => {
    it('calls PATCH /restaurants/{id}/menu-items/{itemId}/status with status body', async () => {
      await updateMenuItemStatus('me', 42, { status: 2 })

      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/menu-items/42/status', {
        method: 'PATCH',
        body: JSON.stringify({ status: 2 }),
      })
    })
  })
})
