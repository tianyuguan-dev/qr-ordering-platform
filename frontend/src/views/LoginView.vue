<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { setToken } from '../api/client'

const router = useRouter()
const tenantId = ref('PLATFORM')
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = 'Please enter username and password'
    return
  }
  loading.value = true
  try {
    const res = await login({
      tenantId: tenantId.value.trim(),
      username: username.value.trim(),
      password: password.value,
    })
    setToken(res.accessToken)
    sessionStorage.setItem('user', JSON.stringify({
      userId: res.userId,
      tenantId: res.tenantId,
      username: res.username,
      roleName: res.role?.name,
    }))
    await router.push('/home')
  } catch (e) {
    error.value = e.message || (e.status === 401 ? 'Invalid username or password' : 'Login failed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>QR Ordering Platform</h1>
      <p class="subtitle">Platform admin: use Tenant ID <strong>PLATFORM</strong></p>
      <form @submit.prevent="onSubmit" class="login-form">
        <div class="field">
          <label for="tenantId">Tenant ID</label>
          <input
            id="tenantId"
            v-model="tenantId"
            type="text"
            placeholder="PLATFORM or restaurant ID"
            autocomplete="off"
          />
        </div>
        <div class="field">
          <label for="username">Username</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="Username"
            autocomplete="username"
          />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="Password"
            autocomplete="current-password"
          />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" class="btn primary" :disabled="loading">
          {{ loading ? 'Signing in...' : 'Sign in' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}
.login-card {
  width: 100%;
  max-width: 380px;
  padding: 2rem;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}
.login-card h1 {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
  color: #1a1a2e;
}
.subtitle {
  margin: 0 0 1.5rem;
  font-size: 0.875rem;
  color: #666;
}
.login-form .field {
  margin-bottom: 1rem;
}
.login-form label {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #333;
}
.login-form input {
  width: 100%;
  padding: 0.6rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
}
.login-form input:focus {
  outline: none;
  border-color: #646cff;
  box-shadow: 0 0 0 2px rgba(100, 108, 255, 0.2);
}
.error {
  margin: 0 0 1rem;
  padding: 0.5rem;
  background: #fee;
  color: #c33;
  border-radius: 6px;
  font-size: 0.875rem;
}
.btn {
  width: 100%;
  padding: 0.75rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
}
.btn.primary {
  background: #646cff;
  color: #fff;
}
.btn.primary:hover:not(:disabled) {
  background: #535bf2;
}
.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
