import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch } from './client'
import {
  getTables,
  createTable,
  updateTable,
  deleteTable,
  getCheckoutSummary,
  checkoutTable,
} from './tables'

vi.mock('./client', () => ({ apiFetch: vi.fn() }))

describe('tables API', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockClear()
    vi.mocked(apiFetch).mockResolvedValue({})
  })

  describe('getTables', () => {
    it('calls GET /restaurants/{id}/tables', async () => {
      await getTables('me')
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/tables')
    })
  })

  describe('createTable', () => {
    it('calls POST /restaurants/{id}/tables with body', async () => {
      const body = { tableNumber: 'T01', seats: 4 }
      await createTable('me', body)

      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/tables', {
        method: 'POST',
        body: JSON.stringify(body),
      })
    })
  })

  describe('updateTable', () => {
    it('calls PUT /restaurants/{id}/tables/{tableId} with body', async () => {
      const body = { tableNumber: 'T02', status: 2 }
      await updateTable('me', 5, body)

      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/tables/5', {
        method: 'PUT',
        body: JSON.stringify(body),
      })
    })
  })

  describe('deleteTable', () => {
    it('calls DELETE /restaurants/{id}/tables/{tableId}', async () => {
      await deleteTable('me', 5)
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/tables/5', { method: 'DELETE' })
    })
  })

  describe('getCheckoutSummary', () => {
    it('calls GET /restaurants/{id}/tables/{tableId}/checkout-summary', async () => {
      await getCheckoutSummary('me', 3)
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/tables/3/checkout-summary')
    })
  })

  describe('checkoutTable', () => {
    it('calls POST /restaurants/{id}/tables/{tableId}/checkout', async () => {
      await checkoutTable('me', 3)
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/tables/3/checkout', { method: 'POST' })
    })
  })
})
