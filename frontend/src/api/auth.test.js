import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch } from './client'
import { login, getProfile, changePassword } from './auth'

vi.mock('./client', () => ({ apiFetch: vi.fn() }))

describe('auth API', () => {
  beforeEach(() => {
    vi.mocked(apiFetch).mockClear()
    vi.mocked(apiFetch).mockResolvedValue({})
  })

  describe('login', () => {
    it('calls POST /auth/login with serialized body', async () => {
      const body = { tenantId: 'REST-001', username: 'admin', password: 'secret' }
      await login(body)

      expect(apiFetch).toHaveBeenCalledWith('/auth/login', {
        method: 'POST',
        body: JSON.stringify(body),
      })
    })

    it('calls exactly once', async () => {
      await login({ tenantId: 'PLATFORM', username: 'admin', password: 'pw' })
      expect(apiFetch).toHaveBeenCalledTimes(1)
    })
  })

  describe('getProfile', () => {
    it('calls GET /auth/me with no options', async () => {
      await getProfile()
      expect(apiFetch).toHaveBeenCalledWith('/auth/me')
    })
  })

  describe('changePassword', () => {
    it('calls PUT /auth/change-password with body', async () => {
      const body = { currentPassword: 'old', newPassword: 'new' }
      await changePassword(body)

      expect(apiFetch).toHaveBeenCalledWith('/auth/change-password', {
        method: 'PUT',
        body: JSON.stringify(body),
      })
    })
  })
})
