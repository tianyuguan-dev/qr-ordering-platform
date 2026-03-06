<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRestaurants } from '../api/restaurants'
import { getOrders, getOrder, updateOrderStatus } from '../api/orders'
import { setToken } from '../api/client'
import { toast } from '../utils/toast'

function getFilterStorageKey() {
  try {
    const user = JSON.parse(sessionStorage.getItem('user') || 'null')
    if (user && user.userId) return `ordersFilterStatuses_${user.userId}`
  } catch (_) {}
  return 'ordersFilterStatuses'
}

const orderEvents = inject('orderEvents', null)

const STATUS_LIST = [
  { code: 1, label: 'Created' },
  { code: 2, label: 'Confirmed' },
  { code: 3, label: 'Preparing' },
  { code: 4, label: 'Ready' },
  { code: 5, label: 'Served' },
  { code: 6, label: 'Completed' },
  { code: 7, label: 'Cancelled' },
]

function loadSavedFilterStatuses() {
  try {
    const raw = localStorage.getItem(getFilterStorageKey())
    if (!raw) return []
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr)) return []
    const validCodes = STATUS_LIST.map((s) => s.code)
    return arr.filter((c) => typeof c === 'number' && validCodes.includes(c))
  } catch (_) {
    return []
  }
}

function saveFilterStatuses(codes) {
  try {
    localStorage.setItem(getFilterStorageKey(), JSON.stringify(codes))
  } catch (_) {}
}

// Allowed next statuses by state machine (admin / platform admin: all valid transitions)
const NEXT_STATUS_ADMIN = {
  1: [2, 7],
  2: [3, 7],
  3: [4, 7],
  4: [5],
  5: [6],
  6: [],
  7: [],
}
// Waiter: confirm, cancel, serve, complete — no preparing/ready
const NEXT_STATUS_WAITER = {
  1: [2, 7],
  2: [7],
  3: [7],
  4: [5],
  5: [6],
  6: [],
  7: [],
}
// Kitchen: preparing, ready, cancel — no confirm/serve/complete
const NEXT_STATUS_KITCHEN = {
  1: [7],
  2: [3, 7],
  3: [4, 7],
  4: [],
  5: [],
  6: [],
  7: [],
}

const router = useRouter()
const route = useRoute()
const user = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('user') || 'null')
  } catch (_) {
    return null
  }
})
const isPlatformAdmin = computed(() => user.value?.roleName === 'PLATFORM_ADMIN')
const isRestaurantAdmin = computed(() => user.value?.roleName === 'RESTAURANT_ADMIN')
const isWaiterOrKitchen = computed(() => user.value?.roleName === 'WAITER' || user.value?.roleName === 'KITCHEN')

const restaurantList = ref([])
const selectedRestaurantId = ref('')
const effectiveRestaurantId = computed(() => {
  if (isRestaurantAdmin.value || isWaiterOrKitchen.value) return 'me'
  return selectedRestaurantId.value || route.query.restaurant || null
})

const pageData = ref({ content: [], totalElements: 0, totalPages: 0 })
const loading = ref(false)
const error = ref('')
const filterStatuses = ref(loadSavedFilterStatuses())
const statusDropdownOpen = ref(false)
const currentPage = ref(0)
const pageSize = 20

const detailOrderId = ref(null)
const detailOrder = ref(null)
const detailLoading = ref(false)

function statusLabel(code) {
  return STATUS_LIST.find((s) => s.code === code)?.label ?? code
}

function nextStatusMap() {
  const role = user.value?.roleName
  if (role === 'WAITER') return NEXT_STATUS_WAITER
  if (role === 'KITCHEN') return NEXT_STATUS_KITCHEN
  return NEXT_STATUS_ADMIN
}

function nextStatuses(code) {
  const map = nextStatusMap()
  return (map[code] || []).map((c) => STATUS_LIST.find((s) => s.code === c)).filter(Boolean)
}

async function loadRestaurants() {
  if (!isPlatformAdmin.value) return
  try {
    const res = await getRestaurants({ page: 0, size: 500 })
    restaurantList.value = res.content || []
    if (route.query.restaurant && restaurantList.value.some((r) => r.id === route.query.restaurant)) {
      selectedRestaurantId.value = route.query.restaurant
    } else if (restaurantList.value.length && !selectedRestaurantId.value) {
      selectedRestaurantId.value = restaurantList.value[0].id
    }
  } catch (_) {}
}

async function loadOrders() {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  loading.value = true
  error.value = ''
  try {
    const res = await getOrders(rid, {
      status: filterStatuses.value.length ? filterStatuses.value : undefined,
      page: currentPage.value,
      size: pageSize,
    })
    pageData.value = res
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      sessionStorage.removeItem('user')
      await router.push('/login')
      return
    }
    if (e.status === 403) await router.push('/home')
    else error.value = e.message || 'Failed to load orders'
  } finally {
    loading.value = false
  }
}

async function changeStatus(order, newCode) {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  try {
    await updateOrderStatus(rid, order.id, { status: newCode })
    toast('Status updated')
    await loadOrders()
  } catch (e) {
    if (e.status === 409) {
      toast('Order was updated elsewhere. Refreshing.', 'error')
      await loadOrders()
    } else {
      toast(e.message || 'Update failed', 'error')
    }
  }
}

async function openDetail(order) {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  detailOrderId.value = order.id
  detailOrder.value = null
  detailLoading.value = true
  try {
    detailOrder.value = await getOrder(rid, order.id)
  } catch (e) {
    toast(e.message || 'Failed to load order', 'error')
    detailOrderId.value = null
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailOrderId.value = null
  detailOrder.value = null
}

const statusDropdownRef = ref(null)
function onDocumentClick(e) {
  if (statusDropdownRef.value && !statusDropdownRef.value.contains(e.target)) {
    closeStatusDropdown()
  }
}

onMounted(async () => {
  document.addEventListener('click', onDocumentClick)
  await loadRestaurants()
  if (effectiveRestaurantId.value) {
    await loadOrders()
  }
  if (orderEvents && isPlatformAdmin.value) {
    orderEvents.setRestaurantIdForSse(effectiveRestaurantId.value)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  if (orderEvents && isPlatformAdmin.value) {
    orderEvents.setRestaurantIdForSse(null)
  }
})

watch(effectiveRestaurantId, (val) => {
  if (val) {
    currentPage.value = 0
    loadOrders()
    if (orderEvents && isPlatformAdmin.value) {
      orderEvents.setRestaurantIdForSse(val)
    }
  } else {
    if (orderEvents && isPlatformAdmin.value) {
      orderEvents.setRestaurantIdForSse(null)
    }
    pageData.value = { content: [], totalElements: 0, totalPages: 0 }
  }
})

watch(
  () => orderEvents?.lastOrderEvent?.value,
  (ev) => {
    if (ev && effectiveRestaurantId.value) {
      loadOrders()
    }
  },
  { deep: true }
)

function toggleStatus(code) {
  const idx = filterStatuses.value.indexOf(code)
  if (idx === -1) filterStatuses.value = [...filterStatuses.value, code]
  else filterStatuses.value = filterStatuses.value.filter((c) => c !== code)
  saveFilterStatuses(filterStatuses.value)
}

function selectAllStatuses() {
  filterStatuses.value = STATUS_LIST.map((s) => s.code)
  saveFilterStatuses(filterStatuses.value)
}

function clearStatusFilter() {
  filterStatuses.value = []
  saveFilterStatuses(filterStatuses.value)
}

const isAllSelected = computed(() => filterStatuses.value.length === STATUS_LIST.length)
const statusFilterLabel = computed(() => {
  if (filterStatuses.value.length === 0) return 'All statuses'
  if (filterStatuses.value.length === STATUS_LIST.length) return 'All statuses'
  return filterStatuses.value
    .map((c) => STATUS_LIST.find((s) => s.code === c)?.label ?? c)
    .join(', ')
})

function closeStatusDropdown() {
  statusDropdownOpen.value = false
}

watch(filterStatuses, () => {
  currentPage.value = 0
  loadOrders()
}, { deep: true })
</script>

<template>
  <div class="orders-page">
    <h1>Orders</h1>
    <div v-if="isPlatformAdmin" class="restaurant-selector">
      <label>Restaurant:</label>
      <select v-model="selectedRestaurantId" class="restaurant-select">
        <option value="">Select restaurant</option>
        <option v-for="r in restaurantList" :key="r.id" :value="r.id">{{ r.name }}</option>
      </select>
    </div>
    <div v-if="isPlatformAdmin && !effectiveRestaurantId" class="hint">Select a restaurant to view orders.</div>
    <template v-else>
      <div class="panel">
        <div class="panel-header">
          <span>Order list</span>
          <div class="filter" ref="statusDropdownRef">
            <label>Status:</label>
            <div class="status-dropdown">
              <button type="button" class="status-dropdown-trigger" @click.stop="statusDropdownOpen = !statusDropdownOpen" :aria-expanded="statusDropdownOpen">
                {{ statusFilterLabel }}
                <span class="status-dropdown-arrow">▼</span>
              </button>
              <div v-show="statusDropdownOpen" class="status-dropdown-panel">
                <div class="status-dropdown-actions">
                  <button type="button" class="status-dropdown-btn" @click.stop="selectAllStatuses">{{ isAllSelected ? '✓ ' : '' }}Select all</button>
                  <button type="button" class="status-dropdown-btn" @click.stop="clearStatusFilter">Clear</button>
                </div>
                <div class="status-dropdown-list">
                  <label v-for="s in STATUS_LIST" :key="s.code" class="status-dropdown-item">
                    <input type="checkbox" :checked="filterStatuses.includes(s.code)" @change="toggleStatus(s.code)" />
                    <span>{{ s.label }}</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <div v-if="loading" class="loading">Loading...</div>
        <template v-else>
          <ul class="order-list">
            <li v-for="o in pageData.content" :key="o.id" class="order-row">
              <div class="order-main">
                <span class="order-num">#{{ o.orderNumber }}</span>
                <span class="order-table">Table {{ o.tableNumber || o.tableId }}</span>
                <span class="order-status" :class="'status-' + o.status">{{ statusLabel(o.status) }}</span>
                <span class="order-amount">¥{{ o.totalAmount }}</span>
                <span class="order-time">{{ o.createdAt ? new Date(o.createdAt).toLocaleString() : '' }}</span>
              </div>
              <div class="order-actions">
                <button type="button" class="btn small secondary" @click="openDetail(o)">Detail</button>
                <template v-for="next in nextStatuses(o.status)" :key="next.code">
                  <button
                    type="button"
                    class="btn small primary"
                    @click="changeStatus(o, next.code)"
                  >
                    → {{ next.label }}
                  </button>
                </template>
              </div>
            </li>
            <li v-if="!pageData.content?.length" class="empty">No orders.</li>
          </ul>
          <div v-if="pageData.totalPages > 1" class="pagination">
            <button
              type="button"
              class="btn small secondary"
              :disabled="currentPage === 0"
              @click="currentPage--; loadOrders()"
            >
              Previous
            </button>
            <span class="page-info">Page {{ currentPage + 1 }} of {{ pageData.totalPages }}</span>
            <button
              type="button"
              class="btn small secondary"
              :disabled="currentPage >= pageData.totalPages - 1"
              @click="currentPage++; loadOrders()"
            >
              Next
            </button>
          </div>
        </template>
      </div>
    </template>

    <div v-if="detailOrderId" class="modal-overlay" @click.self="closeDetail">
      <div class="modal modal-order-detail">
        <h2>Order detail</h2>
        <div v-if="detailLoading" class="detail-loading">Loading...</div>
        <template v-else-if="detailOrder">
          <div class="detail-meta">
            <p><strong>Order #</strong> {{ detailOrder.orderNumber }}</p>
            <p><strong>Table</strong> {{ detailOrder.tableNumber || detailOrder.tableId }}</p>
            <p><strong>Status</strong> <span :class="'order-status status-' + detailOrder.status">{{ statusLabel(detailOrder.status) }}</span></p>
            <p><strong>Created</strong> {{ detailOrder.createdAt ? new Date(detailOrder.createdAt).toLocaleString() : '' }}</p>
            <p v-if="detailOrder.customerNotes"><strong>Notes</strong> {{ detailOrder.customerNotes }}</p>
          </div>
          <ul class="detail-items">
            <li v-for="(item, idx) in detailOrder.items" :key="idx" class="detail-item-row">
              <span class="di-name">{{ item.name }} × {{ item.quantity }}</span>
              <span class="di-subtotal">¥{{ item.subtotal }}</span>
            </li>
          </ul>
          <p class="detail-total">Total: ¥{{ detailOrder.totalAmount }}</p>
        </template>
        <div class="modal-actions">
          <button type="button" class="btn primary" @click="closeDetail">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 960px;
  padding: 1.5rem;
}
.orders-page h1 {
  margin: 0 0 1rem;
  font-size: 1.5rem;
}
.restaurant-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.restaurant-select {
  padding: 0.4rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  min-width: 220px;
}
.hint {
  color: #666;
  margin-bottom: 1rem;
}
.panel {
  background: #fff;
  border-radius: 8px;
  padding: 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.filter-select {
  padding: 0.35rem 0.6rem;
  border: 1px solid #ddd;
  border-radius: 6px;
}
.status-dropdown {
  position: relative;
}
.status-dropdown-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 180px;
  padding: 0.4rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  font-size: 0.875rem;
  text-align: left;
  cursor: pointer;
  color: #213547;
}
.status-dropdown-trigger:hover {
  border-color: #646cff;
}
.status-dropdown-arrow {
  margin-left: auto;
  font-size: 0.65rem;
  opacity: 0.7;
}
.status-dropdown-panel {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 2px;
  min-width: 200px;
  padding: 0.5rem 0;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  z-index: 50;
}
.status-dropdown-actions {
  display: flex;
  gap: 0.5rem;
  padding: 0 0.5rem 0.5rem;
  border-bottom: 1px solid #eee;
  margin-bottom: 0.5rem;
}
.status-dropdown-btn {
  padding: 0.25rem 0.5rem;
  font-size: 0.8rem;
  border: none;
  background: #f0f2f5;
  border-radius: 4px;
  cursor: pointer;
  color: #213547;
}
.status-dropdown-btn:hover {
  background: #e0e0e0;
}
.status-dropdown-list {
  max-height: 220px;
  overflow-y: auto;
}
.status-dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.75rem;
  font-size: 0.875rem;
  cursor: pointer;
}
.status-dropdown-item:hover {
  background: #f5f5f5;
}
.status-dropdown-item input {
  margin: 0;
}
.error {
  color: #c33;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}
.loading {
  color: #666;
  padding: 1.5rem;
}
.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid #eee;
  flex-wrap: nowrap;
}
.order-main {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: nowrap;
  min-width: 0;
  flex: 1;
}
.order-num {
  font-weight: 600;
  font-size: 0.9rem;
}
.order-table {
  color: #555;
  font-size: 0.875rem;
}
.order-status {
  font-size: 0.8rem;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  background: #e8e8e8;
  color: #333;
}
.order-status.status-1 { background: #e3f2fd; color: #1565c0; }
.order-status.status-2 { background: #fff3e0; color: #e65100; }
.order-status.status-3 { background: #fce4ec; color: #c2185b; }
.order-status.status-4 { background: #e8f5e9; color: #2e7d32; }
.order-status.status-5 { background: #e0f2f1; color: #00695c; }
.order-status.status-6 { background: #f5f5f5; color: #616161; }
.order-status.status-7 { background: #ffebee; color: #c62828; }
.order-amount {
  font-weight: 600;
  color: #333;
}
.order-time {
  font-size: 0.8rem;
  color: #888;
  white-space: nowrap;
}
.order-actions {
  display: flex;
  gap: 0.5rem;
  flex-shrink: 0;
}
.empty {
  color: #666;
  padding: 1.5rem;
  text-align: center;
}
.pagination {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}
.page-info {
  font-size: 0.875rem;
  color: #666;
}
.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
}
.btn.primary {
  background: #646cff;
  color: #fff;
}
.btn.secondary {
  background: #e0e0e0;
  color: #333;
}
.btn.small {
  padding: 0.35rem 0.65rem;
  font-size: 0.8rem;
}
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 1rem;
}
.modal {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  max-width: 420px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}
.modal h2 {
  margin: 0 0 1rem;
  font-size: 1.25rem;
}
.detail-loading {
  padding: 1rem;
  color: #666;
}
.detail-meta {
  margin-bottom: 1rem;
  font-size: 0.9rem;
}
.detail-meta p {
  margin: 0.25rem 0;
}
.detail-items {
  list-style: none;
  margin: 0 0 1rem;
  padding: 0;
  border-top: 1px solid #eee;
  padding-top: 0.5rem;
}
.detail-item-row {
  display: flex;
  justify-content: space-between;
  padding: 0.35rem 0;
  font-size: 0.9rem;
}
.di-name { color: #333; }
.di-subtotal { font-weight: 500; }
.detail-total {
  margin: 0 0 1rem;
  font-weight: 700;
  font-size: 1.05rem;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}
</style>
