<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRestaurants } from '../api/restaurants'
import { getTables, createTable, updateTable, deleteTable, getCheckoutSummary, checkoutTable } from '../api/tables'
import { setToken } from '../api/client'
import { toast } from '../utils/toast'

const STATUS_OPTIONS = [
  { value: 1, label: 'Available' },
  { value: 2, label: 'Occupied' },
  { value: 3, label: 'Reserved' },
]

const router = useRouter()
const route = useRoute()
const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (_) {
    return null
  }
})
const isPlatformAdmin = computed(() => user.value?.roleName === 'PLATFORM_ADMIN')
const isRestaurantAdmin = computed(() => user.value?.roleName === 'RESTAURANT_ADMIN')
const isWaiter = computed(() => user.value?.roleName === 'WAITER')
const canEditTables = computed(() => isPlatformAdmin.value || isRestaurantAdmin.value)

const restaurantList = ref([])
const selectedRestaurantId = ref('')
const effectiveRestaurantId = computed(() => {
  if (isRestaurantAdmin.value || isWaiter.value) return 'me'
  return selectedRestaurantId.value || route.query.restaurant || null
})

const list = ref([])
const loading = ref(false)
const error = ref('')
const modalOpen = ref(false)
const editId = ref(null)
const form = ref({ tableNumber: '', seats: 4, status: 1 })
const formError = ref('')
const saving = ref(false)
const deleteConfirm = ref(null)

const checkoutModalOpen = ref(false)
const checkoutSummary = ref(null)
const checkoutTableId = ref(null)
const checkoutLoading = ref(false)
const checkoutConfirming = ref(false)

const ORDER_STATUS_LABELS = {
  1: 'Created',
  2: 'Confirmed',
  3: 'Preparing',
  4: 'Ready',
  5: 'Served',
  6: 'Completed',
  7: 'Cancelled',
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

async function loadTables() {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  loading.value = true
  error.value = ''
  try {
    list.value = await getTables(rid)
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
      return
    }
    if (e.status === 403) await router.push('/home')
    else error.value = e.message || 'Failed to load tables'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editId.value = null
  form.value = { tableNumber: '', seats: 4, status: 1 }
  formError.value = ''
  modalOpen.value = true
}

function openEdit(t) {
  editId.value = t.id
  form.value = {
    tableNumber: t.tableNumber || '',
    seats: t.seats ?? 4,
    status: t.status ?? 1,
  }
  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editId.value = null
}

async function save() {
  formError.value = ''
  if (!form.value.tableNumber?.trim()) {
    formError.value = 'Table number is required'
    return
  }
  const rid = effectiveRestaurantId.value
  if (!rid) return
  saving.value = true
  try {
    if (editId.value) {
      await updateTable(rid, editId.value, {
        tableNumber: form.value.tableNumber.trim(),
        seats: form.value.seats,
        status: form.value.status,
      })
      toast('Table updated')
    } else {
      await createTable(rid, {
        tableNumber: form.value.tableNumber.trim(),
        seats: form.value.seats,
      })
      toast('Table created')
    }
    closeModal()
    await loadTables()
  } catch (e) {
    formError.value = e.message || 'Save failed'
  } finally {
    saving.value = false
  }
}

function askDelete(t) {
  deleteConfirm.value = t
}

function cancelDelete() {
  deleteConfirm.value = null
}

async function doDelete() {
  if (!deleteConfirm.value) return
  const rid = effectiveRestaurantId.value
  if (!rid) return
  try {
    await deleteTable(rid, deleteConfirm.value.id)
    toast('Table deleted')
    deleteConfirm.value = null
    await loadTables()
  } catch (e) {
    toast(e.message || 'Delete failed', 'error')
  }
}

function statusLabel(code) {
  return STATUS_OPTIONS.find((o) => o.value === code)?.label ?? code
}

const QR_API = 'https://api.qrserver.com/v1/create-qr-code/'
function qrImageUrl(table) {
  const url = table.qrCodeUrl
  if (!url) return ''
  return `${QR_API}?size=120x120&data=${encodeURIComponent(url)}`
}

async function openCheckout(t) {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  checkoutTableId.value = t.id
  checkoutSummary.value = null
  checkoutModalOpen.value = true
  checkoutLoading.value = true
  try {
    checkoutSummary.value = await getCheckoutSummary(rid, t.id)
  } catch (e) {
    toast(e.message || 'Failed to load checkout summary', 'error')
    checkoutModalOpen.value = false
  } finally {
    checkoutLoading.value = false
  }
}

function closeCheckoutModal() {
  checkoutModalOpen.value = false
  checkoutSummary.value = null
  checkoutTableId.value = null
}

async function confirmCheckout() {
  const rid = effectiveRestaurantId.value
  const tid = checkoutTableId.value
  if (!rid || !tid) return
  checkoutConfirming.value = true
  try {
    await checkoutTable(rid, tid)
    toast('Table checked out')
    closeCheckoutModal()
    await loadTables()
  } catch (e) {
    toast(e.message || 'Checkout failed', 'error')
  } finally {
    checkoutConfirming.value = false
  }
}

function orderStatusLabel(code) {
  return ORDER_STATUS_LABELS[code] ?? code
}

function checkoutOutcomeLabel(order) {
  return order.status === 5 ? 'To be completed (paid)' : 'To be cancelled (not charged)'
}

const checkoutAmountToPay = computed(() => {
  const s = checkoutSummary.value
  if (!s?.orders?.length) return '0'
  const sum = s.orders.filter((o) => o.status === 5).reduce((acc, o) => acc + Number(o.totalAmount || 0), 0)
  return sum.toFixed(2)
})

const checkoutCancelledTotal = computed(() => {
  const s = checkoutSummary.value
  if (!s?.orders?.length) return '0'
  const sum = s.orders.filter((o) => o.status !== 5).reduce((acc, o) => acc + Number(o.totalAmount || 0), 0)
  return sum.toFixed(2)
})

onMounted(async () => {
  await loadRestaurants()
  if (effectiveRestaurantId.value) await loadTables()
})

watch(effectiveRestaurantId, (val) => {
  if (val) loadTables()
  else list.value = []
})
</script>

<template>
  <div class="tables-page">
    <h1>Tables</h1>
    <div v-if="isPlatformAdmin" class="restaurant-selector">
      <label>Restaurant:</label>
      <select v-model="selectedRestaurantId" class="restaurant-select">
        <option value="">Select restaurant</option>
        <option v-for="r in restaurantList" :key="r.id" :value="r.id">{{ r.name }}</option>
      </select>
    </div>
    <div v-if="isPlatformAdmin && !effectiveRestaurantId" class="hint">Select a restaurant to manage tables.</div>
    <template v-else>
      <div class="panel">
        <div class="panel-header">
          <span>Dining tables</span>
          <button v-if="canEditTables" type="button" class="btn primary" @click="openCreate">Add Table</button>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <div v-if="loading" class="loading">Loading...</div>
        <ul v-else class="table-list">
          <li v-for="t in list" :key="t.id" class="table-row">
            <div class="table-qr">
              <img v-if="qrImageUrl(t)" :src="qrImageUrl(t)" alt="Table QR" class="qr-img" />
              <span v-else class="qr-placeholder">QR</span>
            </div>
            <span class="table-num">{{ t.tableNumber }}</span>
            <span class="table-seats">{{ t.seats }} seats</span>
            <span class="table-status">{{ statusLabel(t.status) }}</span>
            <div class="row-actions">
              <button type="button" class="btn small primary" @click="openCheckout(t)">Checkout</button>
              <button v-if="canEditTables" type="button" class="btn small secondary" @click="openEdit(t)">Edit</button>
              <button v-if="canEditTables" type="button" class="btn small danger" @click="askDelete(t)">Delete</button>
            </div>
          </li>
          <li v-if="!list.length" class="empty">No tables yet. Add one for customers to select.</li>
        </ul>
      </div>
    </template>

    <div v-if="modalOpen" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <h2>{{ editId ? 'Edit Table' : 'New Table' }}</h2>
        <form @submit.prevent="save">
          <div class="field">
            <label>Table number <span class="required">*</span></label>
            <input v-model="form.tableNumber" type="text" placeholder="e.g. T01, A1" />
          </div>
          <div class="field">
            <label>Seats</label>
            <input v-model.number="form.seats" type="number" min="1" />
          </div>
          <div v-if="editId" class="field">
            <label>Status</label>
            <select v-model.number="form.status" class="field-select">
              <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="deleteConfirm" class="modal-overlay" @click.self="cancelDelete">
      <div class="modal modal-sm">
        <h2>Delete table?</h2>
        <p>Delete table "{{ deleteConfirm.tableNumber }}"?</p>
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="cancelDelete">Cancel</button>
          <button type="button" class="btn danger" @click="doDelete">Delete</button>
        </div>
      </div>
    </div>

    <div v-if="checkoutModalOpen" class="modal-overlay checkout-overlay" @click.self="closeCheckoutModal">
      <div class="modal modal-checkout">
        <div class="checkout-header">
          <span class="checkout-title">Checkout</span>
          <span class="checkout-table-badge">Table {{ checkoutSummary?.tableNumber ?? '—' }}</span>
        </div>
        <div v-if="checkoutLoading" class="checkout-loading">Loading...</div>
        <template v-else-if="checkoutSummary">
          <p v-if="checkoutSummary.orderCount === 0" class="checkout-empty">No pending orders for this table.</p>
          <template v-else>
            <ul class="checkout-order-list">
              <li v-for="o in checkoutSummary.orders" :key="o.id" class="checkout-order-block" :class="o.status === 5 ? 'block-paid' : 'block-cancelled'">
                <div class="checkout-order-header">
                  <span class="co-outcome" :class="o.status === 5 ? 'outcome-paid' : 'outcome-cancelled'">
                    {{ checkoutOutcomeLabel(o) }}
                  </span>
                  <span class="co-amount">¥{{ o.totalAmount }}</span>
                </div>
                <p class="co-order-id">{{ o.orderNumber }}</p>
                <ul v-if="o.items?.length" class="checkout-item-list">
                  <li v-for="(item, idx) in o.items" :key="idx" class="checkout-item-row">
                    <span class="ci-name">{{ item.name }} × {{ item.quantity }}</span>
                    <span class="ci-subtotal">¥{{ item.subtotal }}</span>
                  </li>
                </ul>
              </li>
            </ul>
            <div class="checkout-summary-box">
              <p v-if="Number(checkoutAmountToPay) > 0" class="checkout-total checkout-to-pay">
                <span class="summary-label">Amount to pay</span>
                <span class="summary-value">¥{{ checkoutAmountToPay }}</span>
              </p>
              <p v-if="Number(checkoutCancelledTotal) > 0" class="checkout-total checkout-cancelled">
                <span class="summary-label">Cancelled (not charged)</span>
                <span class="summary-value">¥{{ checkoutCancelledTotal }}</span>
              </p>
            </div>
          </template>
        </template>
        <div class="modal-actions checkout-actions">
          <button type="button" class="btn secondary" @click="closeCheckoutModal">Cancel</button>
          <button
            v-if="checkoutSummary?.orderCount > 0"
            type="button"
            class="btn primary btn-confirm"
            :disabled="checkoutConfirming"
            @click="confirmCheckout"
          >
            {{ checkoutConfirming ? 'Checking out...' : 'Confirm checkout' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tables-page {
  max-width: 560px;
  padding: 1.5rem;
}
.tables-page h1 {
  margin: 0 0 1rem;
  font-size: 1.5rem;
}
.restaurant-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.restaurant-selector label {
  font-weight: 500;
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
.table-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.table-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.6rem 0;
  border-bottom: 1px solid #eee;
}
.table-qr {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 6px;
}
.table-qr .qr-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.table-qr .qr-placeholder {
  font-size: 0.7rem;
  color: #999;
}
.table-num {
  font-weight: 600;
  min-width: 60px;
}
.table-seats {
  color: #666;
  font-size: 0.875rem;
}
.table-status {
  flex: 1;
  font-size: 0.875rem;
}
.row-actions {
  display: flex;
  gap: 0.5rem;
}
.empty {
  color: #666;
  padding: 1.5rem;
  text-align: center;
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
.btn.danger {
  background: #dc3545;
  color: #fff;
}
.btn.small {
  padding: 0.35rem 0.65rem;
  font-size: 0.8rem;
}
.required {
  color: #c33;
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
  max-width: 400px;
  width: 100%;
}
.modal.modal-sm {
  max-width: 360px;
}
.checkout-overlay {
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
}
.modal.modal-checkout {
  max-width: 440px;
  padding: 0;
  border-radius: 14px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}
.checkout-header {
  background: linear-gradient(135deg, #4a5568 0%, #2d3748 100%);
  color: #fff;
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}
.checkout-title {
  font-size: 1.15rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.checkout-table-badge {
  font-size: 0.85rem;
  background: rgba(255, 255, 255, 0.2);
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  font-weight: 500;
}
.checkout-loading,
.checkout-empty {
  padding: 1.5rem 1.25rem;
  color: #718096;
  font-size: 0.9rem;
}
.checkout-order-list {
  list-style: none;
  margin: 0;
  padding: 1rem 1.25rem;
  max-height: 300px;
  overflow-y: auto;
  background: #f8fafc;
}
.checkout-order-block {
  background: #fff;
  border-radius: 10px;
  padding: 0.85rem 1rem;
  margin-bottom: 0.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border-left: 4px solid #cbd5e0;
}
.checkout-order-block:last-child {
  margin-bottom: 0;
}
.checkout-order-block.block-paid {
  border-left-color: #38a169;
}
.checkout-order-block.block-cancelled {
  border-left-color: #e53e3e;
}
.checkout-order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}
.co-outcome {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.25rem 0.6rem;
  border-radius: 6px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.co-outcome.outcome-paid {
  background: #c6f6d5;
  color: #276749;
}
.co-outcome.outcome-cancelled {
  background: #fed7d7;
  color: #c53030;
}
.co-amount {
  font-weight: 700;
  font-size: 1rem;
  color: #2d3748;
}
.co-order-id {
  font-size: 0.7rem;
  color: #a0aec0;
  margin: 0 0 0.5rem;
  font-family: ui-monospace, monospace;
}
.checkout-item-list {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 0.85rem;
  color: #4a5568;
}
.checkout-item-row {
  display: flex;
  justify-content: space-between;
  padding: 0.3rem 0;
  border-top: 1px solid #f1f5f9;
}
.checkout-item-row:first-child {
  border-top: none;
}
.ci-name { }
.ci-subtotal {
  font-weight: 600;
  color: #2d3748;
}
.checkout-summary-box {
  padding: 1rem 1.25rem;
  background: #fff;
  border-top: 1px solid #e2e8f0;
}
.checkout-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0.4rem 0;
  font-size: 0.95rem;
}
.checkout-total:first-child {
  margin-top: 0;
}
.checkout-to-pay {
  font-weight: 700;
  color: #276749;
  font-size: 1.05rem;
}
.checkout-to-pay .summary-value {
  font-size: 1.15rem;
}
.checkout-cancelled {
  color: #718096;
  font-weight: 500;
}
.checkout-cancelled .summary-value {
  text-decoration: line-through;
  color: #a0aec0;
}
.checkout-actions {
  padding: 1rem 1.25rem;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  gap: 0.75rem;
}
.btn-confirm {
  padding: 0.6rem 1.25rem;
  font-weight: 600;
}
.modal h2 {
  margin: 0 0 1rem;
  font-size: 1.25rem;
}
.modal .field {
  margin-bottom: 1rem;
}
.modal .field label {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.875rem;
}
.modal .field input,
.modal .field select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-sizing: border-box;
}
.form-error {
  color: #c33;
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}
</style>
