import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch } from './client'
import {
  getRestaurants,
  getMyRestaurant,
  getRestaurant,
  createRestaurant,
  updateRestaurant,
  deleteRestaurant,
} from './restaurants'

vi.mock('./client', () => ({ apiFetch: vi.fn() }))

describe('restaurants API', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockClear()
    vi.mocked(apiFetch).mockResolvedValue({})
  })

  describe('getRestaurants', () => {
    it('calls /restaurants with no query string when params are empty', async () => {
      await getRestaurants({})
      expect(apiFetch).toHaveBeenCalledWith('/restaurants')
    })

    it('calls /restaurants with no query string when no params given', async () => {
      await getRestaurants()
      expect(apiFetch).toHaveBeenCalledWith('/restaurants')
    })

    it('appends page and size', async () => {
      await getRestaurants({ page: 2, size: 5 })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).toContain('page=2')
      expect(url).toContain('size=5')
    })

    it('appends status when provided', async () => {
      await getRestaurants({ status: 1 })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).toContain('status=1')
    })

    it('appends trimmed name when non-empty', async () => {
      await getRestaurants({ name: '  Ramen  ' })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).toContain('name=Ramen')
    })

    it('omits name param when name is whitespace-only', async () => {
      await getRestaurants({ name: '   ' })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).not.toContain('name=')
    })
  })

  describe('getMyRestaurant', () => {
    it('calls GET /restaurants/me', async () => {
      await getMyRestaurant()
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me')
    })
  })

  describe('getRestaurant', () => {
    it('calls GET /restaurants/{id}', async () => {
      await getRestaurant('REST-001')
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/REST-001')
    })
  })

  describe('createRestaurant', () => {
    it('calls POST /restaurants with body', async () => {
      const body = { name: 'Sushi Bar', address: '123 Main St' }
      await createRestaurant(body)

      expect(apiFetch).toHaveBeenCalledWith('/restaurants', {
        method: 'POST',
        body: JSON.stringify(body),
      })
    })
  })

  describe('updateRestaurant', () => {
    it('calls PUT /restaurants/{id} with body', async () => {
      const body = { name: 'Updated Name' }
      await updateRestaurant('REST-001', body)

      expect(apiFetch).toHaveBeenCalledWith('/restaurants/REST-001', {
        method: 'PUT',
        body: JSON.stringify(body),
      })
    })
  })

  describe('deleteRestaurant', () => {
    it('calls DELETE /restaurants/{id}', async () => {
      await deleteRestaurant('REST-001')

      expect(apiFetch).toHaveBeenCalledWith('/restaurants/REST-001', { method: 'DELETE' })
    })
  })
})
