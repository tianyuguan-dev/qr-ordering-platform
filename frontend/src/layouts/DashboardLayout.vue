<script setup>
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import { setToken } from '../api/client'

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (_) {
    return null
  }
})
const isPlatformAdmin = computed(() => user.value?.roleName === 'PLATFORM_ADMIN')
const isRestaurantAdmin = computed(() => user.value?.roleName === 'RESTAURANT_ADMIN')
const showManageAdmins = computed(() => isPlatformAdmin.value || isRestaurantAdmin.value)

function logout() {
  setToken(null)
  localStorage.removeItem('user')
  window.location.href = '/login'
}
</script>

<template>
  <div class="dashboard-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">QR Ordering</div>
      <nav class="sidebar-nav">
        <router-link to="/home" class="nav-item" active-class="active">
          <span class="nav-icon">◉</span>
          <span>Dashboard</span>
        </router-link>
        <router-link v-if="isPlatformAdmin" to="/restaurants" class="nav-item" active-class="active">
          <span class="nav-icon">◎</span>
          <span>Restaurants</span>
        </router-link>
        <router-link v-if="isPlatformAdmin" to="/platform-admins" class="nav-item" active-class="active">
          <span class="nav-icon">◇</span>
          <span>Platform Admins</span>
        </router-link>
        <router-link v-if="isRestaurantAdmin" to="/my-restaurant" class="nav-item" active-class="active">
          <span class="nav-icon">◎</span>
          <span>My Restaurant</span>
        </router-link>
        <router-link v-if="showManageAdmins" to="/staff" class="nav-item" active-class="active">
          <span class="nav-icon">▣</span>
          <span>Staff</span>
        </router-link>
        <router-link to="/profile" class="nav-item" active-class="active">
          <span class="nav-icon">○</span>
          <span>Profile</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <button type="button" class="logout-btn" @click="logout">Log out</button>
      </div>
    </aside>
    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.dashboard-layout {
  min-height: 100vh;
}
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  width: 220px;
  height: 100vh;
  background: #1a1d24;
  color: #e4e6eb;
  display: flex;
  flex-direction: column;
}
.sidebar-brand {
  padding: 1.25rem 1rem;
  font-size: 1.1rem;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
.sidebar-nav {
  flex: 1;
  padding: 0.75rem 0;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 1rem;
  margin: 0 0.5rem;
  color: #b0b3b8;
  text-decoration: none;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: background 0.15s, color 0.15s;
}
.nav-item:hover {
  color: #fff;
  background: rgba(255,255,255,0.08);
}
.nav-item.active {
  color: #fff;
  background: #646cff;
}
.nav-icon {
  font-size: 0.75rem;
  opacity: 0.9;
}
.sidebar-footer {
  padding: 1rem;
  border-top: 1px solid rgba(255,255,255,0.08);
}
.logout-btn {
  width: 100%;
  padding: 0.5rem 1rem;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 6px;
  background: transparent;
  color: #b0b3b8;
  font-size: 0.875rem;
  cursor: pointer;
}
.logout-btn:hover {
  background: rgba(255,255,255,0.08);
  color: #fff;
}
.main-content {
  margin-left: 220px;
  min-height: 100vh;
  min-width: 0;
  background: #f0f2f5;
  padding: 1.5rem;
}
</style>
