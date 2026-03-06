import { ref, watch, onUnmounted } from 'vue'
import { getToken } from '../api/client'
import { toast } from '../utils/toast'
import { toastAlert } from '../utils/toast'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'

// Shared state: which restaurant id to use for SSE (set by Layout or OrdersView)
const sseRestaurantId = ref(null)

// Single connection state
let eventSource = null
let currentRestaurantId = null

// Last event for subscribers (e.g. OrdersView refreshes list)
const lastOrderEvent = ref(null)

function getRole() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    return user?.roleName || null
  } catch (_) {
    return null
  }
}

function connect(rid) {
  const token = getToken()
  if (!rid || !token) return
  if (eventSource && currentRestaurantId === rid) return

  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  currentRestaurantId = rid
  const path = rid === 'me' ? '/sse/subscribe/me' : `/sse/subscribe/${encodeURIComponent(rid)}`
  const url = `${API_BASE}${path}?token=${encodeURIComponent(token)}`
  eventSource = new EventSource(url)

  eventSource.addEventListener('connected', () => {
    toast('Real-time updates connected')
  })

  eventSource.addEventListener('ORDER_CREATED', (e) => {
    const role = getRole()
    lastOrderEvent.value = { type: 'ORDER_CREATED', data: parseData(e.data) }
    if (role === 'WAITER' || role === 'RESTAURANT_ADMIN') {
      toastAlert('New order needs confirmation', 'alert')
    }
  })

  eventSource.addEventListener('ORDER_STATUS_CHANGED', (e) => {
    const data = parseData(e.data)
    lastOrderEvent.value = { type: 'ORDER_STATUS_CHANGED', data }
    const role = getRole()
    const newStatus = data?.newStatus
    if (role === 'KITCHEN' && newStatus === 2) {
      toastAlert('Order confirmed, please prepare', 'warning')
    } else if ((role === 'WAITER' || role === 'RESTAURANT_ADMIN') && newStatus === 4) {
      toastAlert('Order ready, please serve', 'alert')
    }
  })

  eventSource.onerror = () => {
    if (eventSource?.readyState === EventSource.CLOSED) return
    eventSource?.close()
    eventSource = null
    currentRestaurantId = null
    toast('Real-time connection lost, reconnecting…', 'error')
    setTimeout(() => {
      if (sseRestaurantId.value) connect(sseRestaurantId.value)
    }, 3000)
  }
}

function disconnect() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  currentRestaurantId = null
}

function parseData(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (_) {
    return null
  }
}

/**
 * Composable for global order SSE and role-based alert toasts.
 * - Layout should call setRestaurantIdForSse('me') for WAITER/KITCHEN/RESTAURANT_ADMIN.
 * - OrdersView should call setRestaurantIdForSse(effectiveRestaurantId) for PLATFORM_ADMIN.
 * Returns { lastOrderEvent, setRestaurantIdForSse }.
 */
export function useOrderEvents() {
  const stopWatch = watch(
    sseRestaurantId,
    (rid) => {
      if (rid) connect(rid)
      else disconnect()
    },
    { immediate: true }
  )

  onUnmounted(() => {
    stopWatch()
    // Only disconnect when the component that "owns" the connection unmounts.
    // For Layout we don't disconnect on unmount (user might navigate). We disconnect when sseRestaurantId becomes null.
  })

  return {
    lastOrderEvent,
    setRestaurantIdForSse(id) {
      sseRestaurantId.value = id || null
    },
  }
}
