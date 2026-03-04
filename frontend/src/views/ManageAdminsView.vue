<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRestaurants } from '../api/restaurants'
import { getRestaurantUsers, createRestaurantUser, updateRestaurantUser, deleteRestaurantUser, resetRestaurantUserPassword } from '../api/users'
import { setToken } from '../api/client'
import { toast } from '../utils/toast'

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
// When current user is a restaurant user, their userId is the staff (restaurant_user) id
const currentStaffUserId = computed(() => (isPlatformAdmin.value ? null : user.value?.userId ?? null))

const restaurantList = ref([])
const selectedRestaurantId = ref('')
const selectedRestaurantName = ref('')
const list = ref([])
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = 20
const loading = ref(false)
const listLoading = ref(false)
const error = ref('')

const effectiveTenantId = computed(() => {
  if (isPlatformAdmin.value) return selectedRestaurantId.value || null
  return user.value?.tenantId || null
})

async function loadRestaurants() {
  if (!isPlatformAdmin.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await getRestaurants({ page: 0, size: 200 })
    restaurantList.value = res.content || []
    const fromQuery = route.query.restaurant
    if (fromQuery && restaurantList.value.some((r) => r.id === fromQuery)) {
      selectedRestaurantId.value = fromQuery
      const r = restaurantList.value.find((x) => x.id === fromQuery)
      selectedRestaurantName.value = r ? r.name : ''
    } else if (restaurantList.value.length && !selectedRestaurantId.value) {
      selectedRestaurantId.value = restaurantList.value[0].id
      selectedRestaurantName.value = restaurantList.value[0].name
    }
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
      return
    }
    error.value = e.message || 'Failed to load restaurants'
  } finally {
    loading.value = false
  }
}

async function loadStaffList() {
  const tenantId = effectiveTenantId.value
  if (!tenantId) {
    list.value = []
    return
  }
  listLoading.value = true
  error.value = ''
  try {
    const res = await getRestaurantUsers(tenantId, { page: page.value, size: pageSize })
    list.value = res.content || []
    totalPages.value = res.totalPages ?? 0
    totalElements.value = res.totalElements ?? 0
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
      return
    }
    if (e.status === 403) {
      await router.push('/home')
      return
    }
    error.value = e.message || 'Failed to load staff list'
  } finally {
    listLoading.value = false
  }
}

function onSelectRestaurant() {
  const r = restaurantList.value.find((x) => x.id === selectedRestaurantId.value)
  selectedRestaurantName.value = r ? r.name : ''
  page.value = 0
  loadStaffList()
}

watch(selectedRestaurantId, () => {
  if (isPlatformAdmin.value) onSelectRestaurant()
})

const STAFF_ROLES = [
  { value: 1, label: 'Restaurant Admin' },
  { value: 2, label: 'Waiter' },
  { value: 3, label: 'Kitchen' },
]
const addAdminModal = ref({ open: false, username: '', password: '', email: '', role: 1, error: '', loading: false })

function openAddAdmin() {
  addAdminModal.value = { open: true, username: '', password: '', email: '', role: 1, error: '', loading: false }
}

function closeAddAdminModal() {
  addAdminModal.value.open = false
  loadStaffList()
}

async function submitAddAdmin() {
  const m = addAdminModal.value
  const tenantId = effectiveTenantId.value
  if (!tenantId) return
  m.error = ''
  if (!m.username?.trim() || !m.password) {
    m.error = 'Username and password are required (min 6 characters)'
    return
  }
  if (m.password.length < 6) {
    m.error = 'Password must be at least 6 characters'
    return
  }
  m.loading = true
  try {
    await createRestaurantUser(tenantId, {
      username: m.username.trim(),
      password: m.password,
      role: m.role,
      email: m.email?.trim() || undefined,
    })
    toast('Staff added')
    closeAddAdminModal()
  } catch (e) {
    m.error = e.message || (e.status === 403 ? 'Access denied' : 'Failed to create admin')
  } finally {
    m.loading = false
  }
}

function formatDate(s) {
  if (!s) return '-'
  try {
    return new Date(s).toLocaleString()
  } catch (_) {
    return s
  }
}

const editModal = ref({ open: false, id: null, username: '', email: '', role: 1, error: '', loading: false })
const deleteConfirm = ref(null)
const resetPwdModal = ref({ open: false, id: null, username: '', newPassword: '', error: '', loading: false })

function openEdit(u) {
  editModal.value = { open: true, id: u.id, username: u.username, email: u.email || '', role: u.role?.code ?? 1, error: '', loading: false }
}
function closeEditModal() {
  editModal.value.open = false
}
async function submitEdit() {
  const tenantId = effectiveTenantId.value
  if (!tenantId || !editModal.value.id) return
  editModal.value.error = ''
  editModal.value.loading = true
  try {
    await updateRestaurantUser(tenantId, editModal.value.id, {
      email: editModal.value.email?.trim() || undefined,
      role: editModal.value.role,
    })
    toast('Saved')
    closeEditModal()
    await loadStaffList()
  } catch (e) {
    editModal.value.error = e.message || 'Update failed'
  } finally {
    editModal.value.loading = false
  }
}

function askDelete(u) {
  deleteConfirm.value = { id: u.id, username: u.username }
}
function cancelDelete() {
  deleteConfirm.value = null
}
async function confirmDelete() {
  if (!deleteConfirm.value) return
  const tenantId = effectiveTenantId.value
  const { id } = deleteConfirm.value
  deleteConfirm.value = null
  if (!tenantId) return
  try {
    await deleteRestaurantUser(tenantId, id)
    toast('Deleted')
    await loadStaffList()
  } catch (e) {
    error.value = e.message || 'Delete failed'
  }
}

function openResetPwd(u) {
  resetPwdModal.value = { open: true, id: u.id, username: u.username, newPassword: '', error: '', loading: false }
}
function closeResetPwdModal() {
  resetPwdModal.value.open = false
}
async function submitResetPwd() {
  const tenantId = effectiveTenantId.value
  const m = resetPwdModal.value
  if (!tenantId || !m.id) return
  m.error = ''
  if (!m.newPassword || m.newPassword.length < 6) {
    m.error = 'Password must be at least 6 characters'
    return
  }
  m.loading = true
  try {
    await resetRestaurantUserPassword(tenantId, m.id, { newPassword: m.newPassword })
    toast('Password reset')
    closeResetPwdModal()
    await loadStaffList()
  } catch (e) {
    m.error = e.message || 'Reset failed'
  } finally {
    m.loading = false
  }
}

function prevPage() {
  if (page.value <= 0) return
  page.value--
  loadStaffList()
}

function nextPage() {
  if (page.value >= totalPages.value - 1) return
  page.value++
  loadStaffList()
}

onMounted(async () => {
  if (isPlatformAdmin.value) {
    await loadRestaurants()
    if (selectedRestaurantId.value) loadStaffList()
  } else {
    loadStaffList()
  }
})
</script>

<template>
  <div class="manage-admins">
    <header class="page-header">
      <h1>Staff</h1>
      <div class="header-actions">
        <template v-if="isPlatformAdmin">
          <label class="select-label">Restaurant:</label>
          <select
            v-model="selectedRestaurantId"
            class="restaurant-select"
            :disabled="loading"
          >
            <option value="">— Select —</option>
            <option v-for="r in restaurantList" :key="r.id" :value="r.id">{{ r.name }}</option>
          </select>
        </template>
        <template v-else>
          <span class="restaurant-badge">Restaurant: {{ user?.tenantId }}</span>
        </template>
        <button
          type="button"
          class="btn primary"
          :disabled="!effectiveTenantId"
          @click="openAddAdmin"
        >
          Add Staff
        </button>
      </div>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="isPlatformAdmin && loading" class="loading">Loading restaurants...</div>
    <template v-else>
      <div v-if="listLoading" class="loading">Loading staff...</div>
      <template v-else>
        <table v-if="list.length" class="table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Role</th>
              <th>Email</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in list" :key="u.id">
              <td>{{ u.username }}</td>
              <td>{{ u.role?.name ?? '-' }}</td>
              <td>{{ u.email || '-' }}</td>
              <td>{{ formatDate(u.createdAt) }}</td>
              <td class="actions">
                <button type="button" class="btn small" @click="openEdit(u)">Edit</button>
                <button type="button" class="btn small" :disabled="u.id === currentStaffUserId" :title="u.id === currentStaffUserId ? 'Use Profile to change your password' : ''" @click="openResetPwd(u)">Reset password</button>
                <button type="button" class="btn small danger" :disabled="u.id === currentStaffUserId" :title="u.id === currentStaffUserId ? 'Cannot delete yourself' : ''" @click="askDelete(u)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else-if="effectiveTenantId" class="empty">No staff yet. Click &quot;Add Staff&quot; to add one.</p>
        <p v-else-if="isPlatformAdmin" class="empty">Select a restaurant above.</p>
        <div v-if="totalPages > 1" class="pagination">
          <button type="button" class="btn small" :disabled="page <= 0" @click="prevPage">Previous</button>
          <span class="page-info">Page {{ page + 1 }} / {{ totalPages }} ({{ totalElements }} total)</span>
          <button type="button" class="btn small" :disabled="page >= totalPages - 1" @click="nextPage">Next</button>
        </div>
      </template>
    </template>

    <!-- Add Admin Modal -->
    <div v-if="editModal.open" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal">
        <h2>Edit Staff</h2>
        <p class="modal-subtitle">{{ editModal.username }}</p>
        <form @submit.prevent="submitEdit">
          <div class="field">
            <label>Role</label>
            <select v-model.number="editModal.role" class="field-select">
              <option v-for="r in STAFF_ROLES" :key="r.value" :value="r.value">{{ r.label }}</option>
            </select>
          </div>
          <div class="field">
            <label>Email</label>
            <input v-model="editModal.email" type="text" placeholder="Email" />
          </div>
          <p v-if="editModal.error" class="form-error">{{ editModal.error }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeEditModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="editModal.loading">Save</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="deleteConfirm" class="modal-overlay" @click.self="cancelDelete">
      <div class="modal">
        <h2>Confirm delete</h2>
        <p>Delete staff &quot;{{ deleteConfirm.username }}&quot;?</p>
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="cancelDelete">Cancel</button>
          <button type="button" class="btn danger" @click="confirmDelete">Delete</button>
        </div>
      </div>
    </div>

    <div v-if="resetPwdModal.open" class="modal-overlay" @click.self="closeResetPwdModal">
      <div class="modal">
        <h2>Reset password</h2>
        <p class="modal-subtitle">For: {{ resetPwdModal.username }}</p>
        <form @submit.prevent="submitResetPwd">
          <div class="field">
            <label>New password <span class="required">*</span></label>
            <input v-model="resetPwdModal.newPassword" type="password" placeholder="Min 6 characters" />
          </div>
          <p v-if="resetPwdModal.error" class="form-error">{{ resetPwdModal.error }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeResetPwdModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="resetPwdModal.loading">Reset</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="addAdminModal.open" class="modal-overlay" @click.self="closeAddAdminModal">
      <div class="modal">
        <h2>Add Staff</h2>
        <p class="modal-subtitle">
          {{ isPlatformAdmin ? `Restaurant: ${selectedRestaurantName || selectedRestaurantId}` : `Restaurant: ${user?.tenantId}` }}
        </p>
        <form @submit.prevent="submitAddAdmin">
          <div class="field">
            <label>Username <span class="required">*</span></label>
            <input v-model="addAdminModal.username" type="text" placeholder="Username" />
          </div>
          <div class="field">
            <label>Password <span class="required">*</span></label>
            <input v-model="addAdminModal.password" type="password" placeholder="Min 6 characters" />
          </div>
          <div class="field">
            <label>Role</label>
            <select v-model.number="addAdminModal.role" class="field-select">
              <option v-for="r in STAFF_ROLES" :key="r.value" :value="r.value">{{ r.label }}</option>
            </select>
          </div>
          <div class="field">
            <label>Email</label>
            <input v-model="addAdminModal.email" type="text" placeholder="Email" />
          </div>
          <p v-if="addAdminModal.error" class="form-error">{{ addAdminModal.error }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeAddAdminModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="addAdminModal.loading">
              {{ addAdminModal.loading ? 'Creating...' : 'Create' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.manage-admins {
  max-width: 800px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}
.page-header h1 {
  margin: 0;
  font-size: 1.5rem;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.select-label {
  font-size: 0.875rem;
  color: #555;
}
.restaurant-select {
  padding: 0.4rem 0.75rem;
  border-radius: 6px;
  border: 1px solid #ddd;
  font-size: 0.9rem;
  min-width: 180px;
}
.restaurant-badge {
  font-size: 0.9rem;
  color: #555;
}
.error {
  padding: 0.75rem;
  background: #fee;
  color: #c33;
  border-radius: 8px;
  margin-bottom: 1rem;
}
.loading, .empty {
  color: #666;
  text-align: center;
  padding: 2rem;
}
.table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.table th, .table td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}
.table th {
  background: #f5f5f5;
  font-weight: 600;
  font-size: 0.875rem;
}
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.5rem;
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
.btn.primary { background: #646cff; color: #fff; }
.btn.primary:hover:not(:disabled) { background: #535bf2; }
.btn.danger { background: #dc3545; color: #fff; }
.btn.danger:hover:not(:disabled) { background: #c82333; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.table td.actions { white-space: nowrap; }
.table td.actions .btn + .btn { margin-left: 0.35rem; }
.required { color: #c33; }
.btn.secondary { background: #e0e0e0; color: #333; }
.btn.secondary:hover:not(:disabled) { background: #d0d0d0; }
.btn.small { padding: 0.35rem 0.65rem; font-size: 0.8rem; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}
.modal {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  max-width: 420px;
  width: 100%;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.modal h2 {
  margin: 0 0 1rem;
  font-size: 1.25rem;
}
.modal-subtitle {
  margin: -0.5rem 0 1rem;
  font-size: 0.875rem;
  color: #666;
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
.modal .field select.field-select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}
.modal .field select.field-select {
  cursor: pointer;
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
