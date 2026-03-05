<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="customer-layout">
    <main class="customer-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.customer-layout {
  min-height: 100vh;
  background: #f5f5f5;
}
.customer-main {
  padding: 0;
  max-width: 640px;
  margin: 0 auto;
  min-height: 100vh;
}
</style>
