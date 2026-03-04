<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPlatformAdmins, createPlatformAdmin, updatePlatformAdmin, deletePlatformAdmin, resetPlatformAdminPassword } from '../api/users'
import { setToken } from '../api/client'
import { toast } from '../utils/toast'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const error = ref('')
const modalOpen = ref(false)
const form = ref({ username: '', password: '', email: '' })
const formError = ref('')
const submitLoading = ref(false)

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (_) {
    return null
  }
})
const currentUserId = computed(() => user.value?.userId ?? null)

const editModal = ref({ open: false, id: null, email: '', error: '', loading: false })
const deleteConfirm = ref(null)
const resetPwdModal = ref({ open: false, id: null, username: '', newPassword: '', error: '', loading: false })

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    list.value = await getPlatformAdmins()
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
    error.value = e.message || 'Failed to load list'
  } finally {
    loading.value = false
  }
}

function openModal() {
  form.value = { username: '', password: '', email: '' }
  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
}

async function submitForm() {
  formError.value = ''
  if (!form.value.username?.trim() || !form.value.password) {
    formError.value = 'Username and password are required (min 6 characters)'
    return
  }
  if (form.value.password.length < 6) {
    formError.value = 'Password must be at least 6 characters'
    return
  }
  submitLoading.value = true
  try {
    await createPlatformAdmin({
      username: form.value.username.trim(),
      password: form.value.password,
      email: form.value.email?.trim() || undefined,
    })
    toast('Created')
    closeModal()
    await loadList()
  } catch (e) {
    formError.value = e.message || (e.status === 403 ? 'Access denied' : 'Failed to create')
  } finally {
    submitLoading.value = false
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

function openEdit(a) {
  editModal.value = { open: true, id: a.id, email: a.email || '', error: '', loading: false }
}
function closeEditModal() {
  editModal.value.open = false
}
async function submitEdit() {
  editModal.value.error = ''
  editModal.value.loading = true
  try {
    await updatePlatformAdmin(editModal.value.id, { email: editModal.value.email?.trim() || undefined })
    toast('Saved')
    closeEditModal()
    await loadList()
  } catch (e) {
    editModal.value.error = e.message || 'Update failed'
  } finally {
    editModal.value.loading = false
  }
}

function askDelete(a) {
  deleteConfirm.value = { id: a.id, username: a.username }
}
function cancelDelete() {
  deleteConfirm.value = null
}
async function confirmDelete() {
  if (!deleteConfirm.value) return
  const { id } = deleteConfirm.value
  deleteConfirm.value = null
  try {
    await deletePlatformAdmin(id)
    toast('Deleted')
    await loadList()
  } catch (e) {
    error.value = e.message || 'Delete failed'
  }
}

function openResetPwd(a) {
  resetPwdModal.value = { open: true, id: a.id, username: a.username, newPassword: '', error: '', loading: false }
}
function closeResetPwdModal() {
  resetPwdModal.value.open = false
}
async function submitResetPwd() {
  const m = resetPwdModal.value
  m.error = ''
  if (!m.newPassword || m.newPassword.length < 6) {
    m.error = 'Password must be at least 6 characters'
    return
  }
  m.loading = true
  try {
    await resetPlatformAdminPassword(m.id, { newPassword: m.newPassword })
    toast('Password reset')
    closeResetPwdModal()
    await loadList()
  } catch (e) {
    m.error = e.message || 'Reset failed'
  } finally {
    m.loading = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="platform-admins">
    <header class="page-header">
      <h1>Platform Admins</h1>
      <button type="button" class="btn primary" @click="openModal">Add Platform Admin</button>
    </header>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="loading">Loading...</div>
    <template v-else>
      <table v-if="list.length" class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
            <th>Created at</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in list" :key="a.id">
            <td>{{ a.id }}</td>
            <td>{{ a.username }}</td>
            <td>{{ a.email || '-' }}</td>
            <td>{{ formatDate(a.createdAt) }}</td>
            <td class="actions">
              <button type="button" class="btn small" @click="openEdit(a)">Edit</button>
              <button type="button" class="btn small" :disabled="a.id === currentUserId" @click="openResetPwd(a)" :title="a.id === currentUserId ? 'Use Profile to change your password' : ''">Reset password</button>
              <button type="button" class="btn small danger" :disabled="a.id === currentUserId" @click="askDelete(a)" :title="a.id === currentUserId ? 'Cannot delete yourself' : ''">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">No platform admins.</p>
    </template>

    <div v-if="modalOpen" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <h2>Add Platform Admin</h2>
        <form @submit.prevent="submitForm">
          <div class="field">
            <label>Username <span class="required">*</span></label>
            <input v-model="form.username" type="text" placeholder="Username" />
          </div>
          <div class="field">
            <label>Password <span class="required">*</span></label>
            <input v-model="form.password" type="password" placeholder="Min 6 characters" />
          </div>
          <div class="field">
            <label>Email</label>
            <input v-model="form.email" type="text" placeholder="Email" />
          </div>
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="submitLoading">
              {{ submitLoading ? 'Creating...' : 'Create' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="editModal.open" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal">
        <h2>Edit Platform Admin</h2>
        <form @submit.prevent="submitEdit">
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
        <p>Delete platform admin &quot;{{ deleteConfirm.username }}&quot;?</p>
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
  </div>
</template>

<style scoped>
.platform-admins {
  max-width: 800px;
  margin: 0 auto;
  padding: 1.5rem;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.page-header h1 { margin: 0; font-size: 1.5rem; }
.error { padding: 0.75rem; background: #fee; color: #c33; border-radius: 8px; margin-bottom: 1rem; }
.loading, .empty { color: #666; text-align: center; padding: 2rem; }
.table {
  width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.table th, .table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #eee; }
.table th { background: #f5f5f5; font-weight: 600; font-size: 0.875rem; }
.table td.actions { white-space: nowrap; }
.table td.actions .btn + .btn { margin-left: 0.35rem; }
.btn { padding: 0.5rem 1rem; border: none; border-radius: 6px; font-size: 0.875rem; font-weight: 500; cursor: pointer; }
.btn.small { padding: 0.35rem 0.65rem; font-size: 0.8rem; }
.btn.primary { background: #646cff; color: #fff; }
.btn.primary:hover:not(:disabled) { background: #535bf2; }
.btn.secondary { background: #e0e0e0; color: #333; }
.btn.secondary:hover:not(:disabled) { background: #d0d0d0; }
.btn.danger { background: #dc3545; color: #fff; }
.btn.danger:hover:not(:disabled) { background: #c82333; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.modal-subtitle { margin: -0.5rem 0 1rem; font-size: 0.875rem; color: #666; }
.required { color: #c33; }
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center;
  z-index: 100; padding: 1rem;
}
.modal {
  background: #fff; border-radius: 12px; padding: 1.5rem; width: 100%; max-width: 440px; max-height: 90vh; overflow-y: auto;
}
.modal h2 { margin: 0 0 1rem; font-size: 1.25rem; }
.modal .field { margin-bottom: 1rem; }
.modal .field label { display: block; margin-bottom: 0.35rem; font-size: 0.875rem; font-weight: 500; }
.modal .field input { width: 100%; padding: 0.5rem 0.75rem; border: 1px solid #ddd; border-radius: 6px; font-size: 1rem; box-sizing: border-box; }
.form-error { margin: 0 0 1rem; padding: 0.5rem; background: #fee; color: #c33; border-radius: 6px; font-size: 0.875rem; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.25rem; }
</style>
