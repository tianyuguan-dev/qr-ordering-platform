import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import LoginView from './LoginView.vue'

vi.mock('../api/auth', () => ({ login: vi.fn() }))
vi.mock('../api/client', () => ({ setToken: vi.fn() }))

import { login } from '../api/auth'
import { setToken } from '../api/client'

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div/>' } },
      { path: '/home', component: { template: '<div/>' } },
      { path: '/login', component: LoginView },
    ],
  })
}

async function mountLogin() {
  const router = makeRouter()
  await router.push('/login')
  const wrapper = mount(LoginView, {
    global: { plugins: [router] },
  })
  return { wrapper, router }
}

describe('LoginView', () => {
  beforeEach(() => {
    vi.mocked(login).mockClear()
    vi.mocked(setToken).mockClear()
    sessionStorage.clear()
  })

  // ------------------------------------------------------------------ rendering

  describe('rendering', () => {
    it('renders tenantId, username and password inputs', async () => {
      const { wrapper } = await mountLogin()

      expect(wrapper.find('input#tenantId').exists()).toBe(true)
      expect(wrapper.find('input#username').exists()).toBe(true)
      expect(wrapper.find('input#password').exists()).toBe(true)
    })

    it('renders a Sign in submit button', async () => {
      const { wrapper } = await mountLogin()

      const btn = wrapper.find('button[type="submit"]')
      expect(btn.exists()).toBe(true)
      expect(btn.text()).toContain('Sign in')
    })

    it('defaults tenantId to PLATFORM', async () => {
      const { wrapper } = await mountLogin()

      expect(wrapper.find('input#tenantId').element.value).toBe('PLATFORM')
    })
  })

  // ------------------------------------------------------------------ validation

  describe('validation', () => {
    it('shows error when submitting with empty username', async () => {
      const { wrapper } = await mountLogin()

      await wrapper.find('input#tenantId').setValue('PLATFORM')
      // leave username empty
      await wrapper.find('input#password').setValue('secret')
      await wrapper.find('form').trigger('submit')

      expect(wrapper.find('.error').text()).toContain('Please enter')
      expect(login).not.toHaveBeenCalled()
    })

    it('shows error when submitting with empty password', async () => {
      const { wrapper } = await mountLogin()

      await wrapper.find('input#tenantId').setValue('PLATFORM')
      await wrapper.find('input#username').setValue('admin')
      // leave password empty
      await wrapper.find('form').trigger('submit')

      expect(wrapper.find('.error').text()).toContain('Please enter')
      expect(login).not.toHaveBeenCalled()
    })
  })

  // ------------------------------------------------------------------ success

  describe('successful login', () => {
    it('calls login API with tenantId, username and password', async () => {
      vi.mocked(login).mockResolvedValue({
        accessToken: 'tok-123',
        userId: 1,
        tenantId: 'PLATFORM',
        username: 'admin',
        role: { code: 0, name: 'PLATFORM_ADMIN' },
      })
      const { wrapper } = await mountLogin()

      await wrapper.find('input#tenantId').setValue('PLATFORM')
      await wrapper.find('input#username').setValue('admin')
      await wrapper.find('input#password').setValue('admin123')
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(login).toHaveBeenCalledWith({
        tenantId: 'PLATFORM',
        username: 'admin',
        password: 'admin123',
      })
    })

    it('stores token and user in sessionStorage then navigates to /home', async () => {
      const fakeUser = {
        accessToken: 'tok-xyz',
        userId: 42,
        tenantId: 'PLATFORM',
        username: 'admin',
        role: { code: 0, name: 'PLATFORM_ADMIN' },
      }
      vi.mocked(login).mockResolvedValue(fakeUser)
      const { wrapper, router } = await mountLogin()

      await wrapper.find('input#tenantId').setValue('PLATFORM')
      await wrapper.find('input#username').setValue('admin')
      await wrapper.find('input#password').setValue('admin123')
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(setToken).toHaveBeenCalledWith('tok-xyz')
      const stored = JSON.parse(sessionStorage.getItem('user'))
      expect(stored.userId).toBe(42)
      expect(stored.username).toBe('admin')
      expect(router.currentRoute.value.path).toBe('/home')
    })
  })

  // ------------------------------------------------------------------ failure

  describe('failed login', () => {
    it('shows API error message on 401', async () => {
      const err = new Error('Invalid username or password')
      err.status = 401
      vi.mocked(login).mockRejectedValue(err)
      const { wrapper } = await mountLogin()

      await wrapper.find('input#tenantId').setValue('PLATFORM')
      await wrapper.find('input#username').setValue('admin')
      await wrapper.find('input#password').setValue('wrong')
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(wrapper.find('.error').text()).toBe('Invalid username or password')
    })

    it('shows generic error for non-401 failures', async () => {
      vi.mocked(login).mockRejectedValue(new Error('Network error'))
      const { wrapper } = await mountLogin()

      await wrapper.find('input#tenantId').setValue('PLATFORM')
      await wrapper.find('input#username').setValue('admin')
      await wrapper.find('input#password').setValue('pw')
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(wrapper.find('.error').text()).toBe('Network error')
    })

    it('re-enables submit button after failure', async () => {
      vi.mocked(login).mockRejectedValue(new Error('fail'))
      const { wrapper } = await mountLogin()

      await wrapper.find('input#tenantId').setValue('PLATFORM')
      await wrapper.find('input#username').setValue('admin')
      await wrapper.find('input#password').setValue('pw')
      await wrapper.find('form').trigger('submit')
      await flushPromises()

      expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeUndefined()
    })
  })
})
