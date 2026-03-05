<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPublicRestaurantInfo, getPublicTables } from '../../api/public'

const router = useRouter()
const route = useRoute()
const restaurantId = computed(() => route.params.restaurantId)

const restaurantName = ref('')
const restaurantLogo = ref('')
const tables = ref([])
const loading = ref(true)
const error = ref('')

async function load() {
  const rid = restaurantId.value
  if (!rid) return
  loading.value = true
  error.value = ''
  try {
    const [info, tableList] = await Promise.all([
      getPublicRestaurantInfo(rid),
      getPublicTables(rid),
    ])
    restaurantName.value = info.name || 'Restaurant'
    restaurantLogo.value = info.logoUrl || ''
    tables.value = tableList || []
  } catch (e) {
    error.value = e.message || 'Failed to load'
  } finally {
    loading.value = false
  }
}

function selectTable(table) {
  router.push({
    name: 'CustomerOrderMenu',
    params: { restaurantId: restaurantId.value },
    query: { tableId: table.id, tableNumber: table.tableNumber },
  })
}

onMounted(load)
</script>

<template>
  <div class="customer-table-select">
    <header class="page-header">
      <img v-if="restaurantLogo" :src="restaurantLogo" alt="" class="restaurant-logo" />
      <h1>{{ restaurantName }}</h1>
      <p class="subtitle">Select your table</p>
    </header>
    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="loading">Loading...</div>
    <ul v-else class="table-list">
      <li
        v-for="t in tables"
        :key="t.id"
        class="table-card"
        :class="{ disabled: t.status !== 1 }"
        @click="t.status === 1 ? selectTable(t) : null"
      >
        <span class="table-number">{{ t.tableNumber }}</span>
        <span class="table-seats">{{ t.seats }} seats</span>
      </li>
      <li v-if="!tables.length && !loading" class="empty">No tables available.</li>
    </ul>
  </div>
</template>

<style scoped>
.customer-table-select {
  padding: 1rem;
  padding-bottom: 2rem;
}
.page-header {
  text-align: center;
  padding: 1.5rem 0;
  background: #fff;
  margin: 0 -1rem 1rem;
  padding: 1.5rem 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.restaurant-logo {
  width: 64px;
  height: 64px;
  object-fit: contain;
  border-radius: 8px;
  margin-bottom: 0.5rem;
}
.page-header h1 {
  margin: 0 0 0.25rem;
  font-size: 1.5rem;
}
.subtitle {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}
.error {
  color: #c33;
  padding: 0.75rem;
  background: #fee;
  border-radius: 8px;
  margin-bottom: 1rem;
}
.loading {
  text-align: center;
  padding: 2rem;
  color: #666;
}
.table-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 1rem;
}
.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 1.25rem;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.table-card:hover:not(.disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}
.table-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.table-number {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  color: #333;
}
.table-seats {
  font-size: 0.875rem;
  color: #666;
}
.empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #666;
  padding: 2rem;
}
</style>
