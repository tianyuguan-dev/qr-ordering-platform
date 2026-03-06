<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, changePassword } from '../api/auth'
import { setToken } from '../api/client'

const router = useRouter()
const profile = ref(null)
const loading = ref(false)
const error = ref('')
const pwForm = ref({ currentPassword: '', newPassword: '' })
const pwError = ref('')
const pwSuccess = ref('')
const pwLoading = ref(false)

async function loadProfile() {
  loading.value = true
  error.value = ''
  try {
    profile.value = await getProfile()
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      sessionStorage.removeItem('user')
      await router.push('/login')
      return
    }
    error.value = e.message || 'Failed to load profile'
  } finally {
    loading.value = false
  }
}

async function submitPassword() {
  pwError.value = ''
  pwSuccess.value = ''
  if (!pwForm.value.currentPassword || !pwForm.value.newPassword) {
    pwError.value = 'Current and new password are required'
    return
  }
  if (pwForm.value.newPassword.length < 6) {
    pwError.value = 'New password must be at least 6 characters'
    return
  }
  pwLoading.value = true
  try {
    await changePassword({
      currentPassword: pwForm.value.currentPassword,
      newPassword: pwForm.value.newPassword,
    })
    pwForm.value = { currentPassword: '', newPassword: '' }
    pwSuccess.value = 'Password changed successfully.'
  } catch (e) {
    pwError.value = e.message || 'Failed to change password'
  } finally {
    pwLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page">
    <header class="page-header">
      <h1>My Profile</h1>
    </header>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="loading">Loading...</div>
    <template v-else-if="profile">
      <div class="profile-card">
        <h2>Account</h2>
        <dl class="profile-dl">
          <dt>User ID</dt>
          <dd>{{ profile.userId }}</dd>
          <dt>Tenant ID</dt>
          <dd>{{ profile.tenantId }}</dd>
          <dt>Username</dt>
          <dd>{{ profile.username }}</dd>
          <dt>Role</dt>
          <dd>{{ profile.role?.name ?? '-' }}</dd>
        </dl>
      </div>

      <div class="profile-card">
        <h2>Change Password</h2>
        <form @submit.prevent="submitPassword" class="pw-form">
          <div class="field">
            <label>Current password <span class="required">*</span></label>
            <input v-model="pwForm.currentPassword" type="password" placeholder="Current password" />
          </div>
          <div class="field">
            <label>New password <span class="required">*</span></label>
            <input v-model="pwForm.newPassword" type="password" placeholder="Min 6 characters" />
          </div>
          <p v-if="pwError" class="form-error">{{ pwError }}</p>
          <p v-if="pwSuccess" class="form-success">{{ pwSuccess }}</p>
          <button type="submit" class="btn primary" :disabled="pwLoading">
            {{ pwLoading ? 'Saving...' : 'Change Password' }}
          </button>
        </form>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 520px;
  margin: 0 auto;
  padding: 1.5rem;
}
.page-header { margin-bottom: 1.5rem; }
.page-header h1 { margin: 0; font-size: 1.5rem; }
.error { padding: 0.75rem; background: #fee; color: #c33; border-radius: 8px; margin-bottom: 1rem; }
.loading { color: #666; padding: 2rem; }
.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.profile-card h2 { margin: 0 0 1rem; font-size: 1.1rem; color: #333; }
.profile-dl { margin: 0; display: grid; grid-template-columns: auto 1fr; gap: 0.5rem 1.5rem; }
.profile-dl dt { color: #666; font-weight: 500; }
.profile-dl dd { margin: 0; }
.pw-form .field { margin-bottom: 1rem; }
.pw-form .field label { display: block; margin-bottom: 0.35rem; font-size: 0.875rem; font-weight: 500; }
.pw-form .field input {
  width: 100%; padding: 0.5rem 0.75rem; border: 1px solid #ddd; border-radius: 6px; font-size: 1rem; box-sizing: border-box;
}
.required { color: #c33; }
.form-error { margin: 0 0 1rem; padding: 0.5rem; background: #fee; color: #c33; border-radius: 6px; font-size: 0.875rem; }
.form-success { margin: 0 0 1rem; padding: 0.5rem; background: #efe; color: #363; border-radius: 6px; font-size: 0.875rem; }
.btn { padding: 0.5rem 1rem; border: none; border-radius: 6px; font-size: 0.875rem; font-weight: 500; cursor: pointer; }
.btn.primary { background: #646cff; color: #fff; }
.btn.primary:hover:not(:disabled) { background: #535bf2; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
