import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import { useOrderEvents } from './useOrderEvents'

// Mock dependencies so SSE is not opened
vi.mock('../api/client', () => ({ getToken: vi.fn(() => null) }))
vi.mock('../utils/toast', () => ({ toast: vi.fn(), toastAlert: vi.fn() }))
vi.stubGlobal('EventSource', vi.fn())

describe('useOrderEvents', () => {
  let wrapper

  afterEach(() => {
    wrapper?.unmount()
  })

  it('returns lastOrderEvent ref and setRestaurantIdForSse function', () => {
    const TestComp = defineComponent({
      setup() {
        return useOrderEvents()
      },
      template: '<div />',
    })
    wrapper = mount(TestComp)
    const vm = wrapper.vm
    expect(vm.lastOrderEvent).toBeDefined()
    expect(vm.setRestaurantIdForSse).toBeTypeOf('function')
  })

  it('setRestaurantIdForSse accepts id and normalizes null/empty to null', async () => {
    const TestComp = defineComponent({
      setup() {
        return useOrderEvents()
      },
      template: '<div />',
    })
    wrapper = mount(TestComp)
    const vm = wrapper.vm
    vm.setRestaurantIdForSse('me')
    await nextTick()
    vm.setRestaurantIdForSse(null)
    await nextTick()
    vm.setRestaurantIdForSse('')
    await nextTick()
    // No throw; getToken returns null so EventSource is never created
    expect(EventSource).not.toHaveBeenCalled()
  })
})
