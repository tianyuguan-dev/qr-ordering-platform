<script setup>
import { computed } from 'vue'

const user = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('user') || 'null')
  } catch (_) {
    return null
  }
})
</script>

<template>
  <div class="home">
    <div class="home-card">
      <h1>Dashboard</h1>
      <p v-if="user" class="welcome">Welcome, {{ user.username }}</p>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}
.home-card {
  width: 100%;
  max-width: 420px;
  padding: 2rem;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}
.home-card h1 {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
  color: #1a1a2e;
}
.welcome {
  margin: 0;
  color: #666;
  font-size: 0.95rem;
}
</style>
