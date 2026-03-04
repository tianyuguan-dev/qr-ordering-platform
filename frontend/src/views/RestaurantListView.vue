<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRestaurants, createRestaurant, updateRestaurant, deleteRestaurant } from '../api/restaurants'
import { setToken } from '../api/client'
import { toast } from '../utils/toast'

const STATUS_OPTIONS = [
  { value: 1, label: 'ACTIVE' },
  { value: 2, label: 'SUSPENDED' },
  { value: 3, label: 'INACTIVE' },
]

const router = useRouter()
const list = ref([])
const totalElements = ref(0)
const totalPages = ref(0)
const page = ref(0)
const size = 20
const statusFilter = ref(null) // null = all, 1/2/3 = ACTIVE/SUSPENDED/INACTIVE
const searchName = ref('')
const loading = ref(false)
const error = ref('')
const modalOpen = ref(false)
const modalEdit = ref(null) // editing restaurant id or null for create
const form = ref({ name: '', description: '', logoUrl: '', address: '', phone: '' })
const formError = ref('')
const submitLoading = ref(false)
const deleteConfirm = ref(null) // { id, name }

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (_) {
    return null
  }
})
const isPlatformAdmin = computed(() => user.value?.roleName === 'PLATFORM_ADMIN')

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const res = await getRestaurants({
      page: page.value,
      size,
      status: statusFilter.value ?? undefined,
      name: searchName.value?.trim() || undefined,
    })
    list.value = res.content || []
    totalElements.value = res.totalElements ?? 0
    totalPages.value = res.totalPages ?? 0
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
      return
    }
    error.value = e.message || 'Failed to load list'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  modalEdit.value = null
  form.value = { name: '', description: '', logoUrl: '', address: '', phone: '' }
  formError.value = ''
  modalOpen.value = true
}

function openEdit(r) {
  modalEdit.value = r.id
  form.value = {
    name: r.name || '',
    description: r.description || '',
    logoUrl: r.logoUrl || '',
    address: r.address || '',
    phone: r.phone || '',
    status: r.status?.code ?? 1,
  }
  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  modalEdit.value = null
}

async function submitForm() {
  formError.value = ''
  if (!form.value.name?.trim()) {
    formError.value = 'Please enter restaurant name'
    return
  }
  submitLoading.value = true
  try {
    if (modalEdit.value) {
      await updateRestaurant(modalEdit.value, {
        name: form.value.name.trim(),
        description: form.value.description?.trim() || undefined,
        logoUrl: form.value.logoUrl?.trim() || undefined,
        address: form.value.address?.trim() || undefined,
        phone: form.value.phone?.trim() || undefined,
        status: form.value.status,
      })
    } else {
      await createRestaurant({
        name: form.value.name.trim(),
        description: form.value.description?.trim() || undefined,
        logoUrl: form.value.logoUrl?.trim() || undefined,
        address: form.value.address?.trim() || undefined,
        phone: form.value.phone?.trim() || undefined,
      })
    }
    toast(modalEdit.value ? 'Updated' : 'Created')
    closeModal()
    await loadList()
  } catch (e) {
    formError.value = e.message || (e.status === 403 ? 'Access denied' : 'Operation failed')
  } finally {
    submitLoading.value = false
  }
}

function askDelete(r) {
  deleteConfirm.value = { id: r.id, name: r.name }
}

function cancelDelete() {
  deleteConfirm.value = null
}

async function confirmDelete() {
  if (!deleteConfirm.value) return
  const { id } = deleteConfirm.value
  deleteConfirm.value = null
  try {
    await deleteRestaurant(id)
    toast('Deleted')
    await loadList()
  } catch (e) {
    error.value = e.message || (e.status === 403 ? 'Access denied' : 'Delete failed')
  }
}

function goToStaff(r) {
  router.push({ path: '/staff', query: { restaurant: r.id } })
}

function setStatus(s) {
  statusFilter.value = s
  page.value = 0
  loadList()
}

function onSearch() {
  page.value = 0
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="restaurant-list">
    <header class="page-header">
      <h1>Restaurant Management</h1>
      <div class="header-actions">
        <button v-if="isPlatformAdmin" type="button" class="btn primary" @click="openCreate">
          Add Restaurant
        </button>
      </div>
    </header>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="isPlatformAdmin" class="filters">
      <div class="status-tabs">
        <button type="button" class="tab" :class="{ active: statusFilter === null }" @click="setStatus(null)">All</button>
        <button type="button" class="tab" :class="{ active: statusFilter === 1 }" @click="setStatus(1)">ACTIVE</button>
        <button type="button" class="tab" :class="{ active: statusFilter === 2 }" @click="setStatus(2)">SUSPENDED</button>
        <button type="button" class="tab" :class="{ active: statusFilter === 3 }" @click="setStatus(3)">INACTIVE</button>
      </div>
      <div class="search-row">
        <input v-model="searchName" type="text" placeholder="Search by name..." class="search-input" @keyup.enter="onSearch" />
        <button type="button" class="btn small" @click="onSearch">Search</button>
      </div>
    </div>

    <div v-if="loading" class="loading">Loading...</div>
    <template v-else>
      <table class="table" v-if="list.length">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Status</th>
            <th>Address</th>
            <th>Phone</th>
            <th v-if="isPlatformAdmin">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in list" :key="r.id">
            <td>{{ r.id }}</td>
            <td>{{ r.name }}</td>
            <td>{{ r.status?.name ?? '-' }}</td>
            <td>{{ r.address || '-' }}</td>
            <td>{{ r.phone || '-' }}</td>
            <td v-if="isPlatformAdmin" class="actions">
              <button type="button" class="btn small" @click="openEdit(r)">Edit</button>
              <button type="button" class="btn small" @click="goToStaff(r)">Staff</button>
              <button type="button" class="btn small danger" @click="askDelete(r)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">No restaurants yet.</p>

      <div v-if="totalPages > 1" class="pagination">
        <button
          type="button"
          class="btn small"
          :disabled="page <= 0"
          @click="page--; loadList()"
        >
          Previous
        </button>
        <span class="page-info">Page {{ page + 1 }} / {{ totalPages }} ({{ totalElements }} total)</span>
        <button
          type="button"
          class="btn small"
          :disabled="page >= totalPages - 1"
          @click="page++; loadList()"
        >
          Next
        </button>
      </div>
    </template>

    <!-- Create/Edit Modal -->
    <div v-if="modalOpen" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <h2>{{ modalEdit ? 'Edit Restaurant' : 'Add Restaurant' }}</h2>
        <form @submit.prevent="submitForm">
          <div class="field">
            <label>Name <span class="required">*</span></label>
            <input v-model="form.name" type="text" placeholder="Restaurant name" />
          </div>
          <div class="field">
            <label>Description</label>
            <textarea v-model="form.description" rows="2" placeholder="Description"></textarea>
          </div>
          <div class="field">
            <label>Logo URL</label>
            <input v-model="form.logoUrl" type="text" placeholder="https://..." />
          </div>
          <div class="field">
            <label>Address</label>
            <input v-model="form.address" type="text" placeholder="Address" />
          </div>
          <div class="field">
            <label>Phone</label>
            <input v-model="form.phone" type="text" placeholder="+64 9 123 4567" />
          </div>
          <div v-if="modalEdit" class="field">
            <label>Status</label>
            <select v-model.number="form.status" class="field-select">
              <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="submitLoading">
              {{ submitLoading ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete Confirm -->
    <div v-if="deleteConfirm" class="modal-overlay" @click.self="cancelDelete">
      <div class="modal modal-sm">
        <h2>Confirm delete</h2>
        <p>Delete restaurant "{{ deleteConfirm.name }}"?</p>
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="cancelDelete">Cancel</button>
          <button type="button" class="btn danger" @click="confirmDelete">Delete</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.restaurant-list {
  max-width: 1000px;
  margin: 0 auto;
  padding: 1.5rem;
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
}
.user-info {
  font-size: 0.875rem;
  color: #666;
}
.filters {
  margin-bottom: 1rem;
}
.status-tabs {
  display: flex;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
}
.tab {
  padding: 0.4rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  font-size: 0.875rem;
  cursor: pointer;
}
.tab:hover { background: #f5f5f5; }
.tab.active {
  background: #646cff;
  color: #fff;
  border-color: #646cff;
}
.search-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}
.search-input {
  padding: 0.4rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
  width: 220px;
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
.table td.actions {
  white-space: nowrap;
}
.table td.actions .btn + .btn {
  margin-left: 0.5rem;
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
.btn.secondary { background: #e0e0e0; color: #333; }
.btn.secondary:hover:not(:disabled) { background: #d0d0d0; }
.btn.danger { background: #dc3545; color: #fff; }
.btn.danger:hover:not(:disabled) { background: #c82333; }
.btn.small { padding: 0.35rem 0.65rem; font-size: 0.8rem; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.required { color: #c33; }

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
  width: 100%;
  max-width: 440px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal.modal-sm { max-width: 360px; }
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
  font-weight: 500;
}
.modal .field input, .modal .field textarea, .modal .field select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  box-sizing: border-box;
}
.modal .field select.field-select {
  cursor: pointer;
}
.modal-subtitle {
  margin: -0.5rem 0 1rem;
  font-size: 0.875rem;
  color: #666;
}
.form-error {
  margin: 0 0 1rem;
  padding: 0.5rem;
  background: #fee;
  color: #c33;
  border-radius: 6px;
  font-size: 0.875rem;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.25rem;
}
</style>
