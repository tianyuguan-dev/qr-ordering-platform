<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyRestaurant, updateMyRestaurant } from '../api/restaurants'
import { setToken } from '../api/client'
import { toast } from '../utils/toast'

const router = useRouter()
const loading = ref(true)
const error = ref('')
const form = ref({ name: '', description: '', logoUrl: '', address: '', phone: '' })
const formError = ref('')
const submitLoading = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const r = await getMyRestaurant()
    form.value = {
      name: r.name || '',
      description: r.description || '',
      logoUrl: r.logoUrl || '',
      address: r.address || '',
      phone: r.phone || '',
    }
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
      return
    }
    if (e.status === 403) await router.push('/home')
    else error.value = e.message || 'Failed to load'
  } finally {
    loading.value = false
  }
}

async function submit() {
  formError.value = ''
  if (!form.value.name?.trim()) {
    formError.value = 'Name is required'
    return
  }
  submitLoading.value = true
  try {
    await updateMyRestaurant({
      name: form.value.name.trim(),
      description: form.value.description?.trim() || undefined,
      logoUrl: form.value.logoUrl?.trim() || undefined,
      address: form.value.address?.trim() || undefined,
      phone: form.value.phone?.trim() || undefined,
    })
    toast('Saved')
    await load()
  } catch (e) {
    formError.value = e.message || 'Update failed'
  } finally {
    submitLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="my-restaurant">
    <h1>My Restaurant</h1>
    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="loading">Loading...</div>
    <form v-else @submit.prevent="submit" class="form">
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
      <p v-if="formError" class="form-error">{{ formError }}</p>
      <button type="submit" class="btn primary" :disabled="submitLoading">
        {{ submitLoading ? 'Saving...' : 'Save' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.my-restaurant {
  max-width: 480px;
}
.my-restaurant h1 {
  margin: 0 0 1.5rem;
  font-size: 1.5rem;
}
.error {
  padding: 0.75rem;
  background: #fee;
  color: #c33;
  border-radius: 8px;
  margin-bottom: 1rem;
}
.loading {
  color: #666;
  padding: 2rem;
}
.form .field {
  margin-bottom: 1rem;
}
.form .field label {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.875rem;
}
.form .field input,
.form .field textarea {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}
.form-error {
  color: #c33;
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
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
.btn.primary:hover:not(:disabled) {
  background: #535bf2;
}
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
