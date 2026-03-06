import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch } from './client'
import { getOrders, updateOrderStatus } from './orders'

vi.mock('./client', () => ({ apiFetch: vi.fn() }))

describe('orders API', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockClear()
    vi.mocked(apiFetch).mockResolvedValue({})
  })

  describe('getOrders', () => {
    it('builds URL with restaurant id when params empty', async () => {
      await getOrders('me', {})
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/orders')
    })

    it('appends status as multiple query params when status is array', async () => {
      await getOrders('me', { status: [1, 4] })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).toContain('/restaurants/me/orders')
      expect(url).toMatch(/status=1.*status=4|status=4.*status=1/)
    })

    it('appends page and size', async () => {
      await getOrders('tenant-1', { page: 1, size: 10 })
      const url = vi.mocked(apiFetch).mock.calls[0][0]
      expect(url).toContain('page=1')
      expect(url).toContain('size=10')
    })
  })

  describe('updateOrderStatus', () => {
    it('calls PATCH with status in body', async () => {
      await updateOrderStatus('me', 100, { status: 2 })
      expect(apiFetch).toHaveBeenCalledWith('/restaurants/me/orders/100/status', {
        method: 'PATCH',
        body: JSON.stringify({ status: 2 }),
      })
    })
  })
})
