<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicRestaurantInfo, getPublicMenu, createOrder } from '../../api/public'
import { toast } from '../../utils/toast'

const route = useRoute()
const restaurantId = computed(() => route.params.restaurantId)
const tableId = computed(() => Number(route.query.tableId))
const tableNumber = computed(() => route.query.tableNumber || '')

const restaurantName = ref('')
const categories = ref([])
const items = ref([])
const loading = ref(true)
const error = ref('')
const cart = ref([]) // { menuItemId, name, price, quantity }
const showCart = ref(false)
const customerNotes = ref('')
const submitting = ref(false)

const cartTotal = computed(() => {
  return cart.value.reduce((sum, c) => sum + Number(c.price) * c.quantity, 0).toFixed(2)
})
const cartCount = computed(() => cart.value.reduce((n, c) => n + c.quantity, 0))

function itemByCategory(categoryId) {
  return items.value.filter((i) => (i.categoryId || null) === (categoryId || null))
}

function getItemById(id) {
  return items.value.find((i) => i.id === id)
}

function addToCart(item) {
  const existing = cart.value.find((c) => c.menuItemId === item.id)
  if (existing) {
    existing.quantity += 1
  } else {
    cart.value.push({
      menuItemId: item.id,
      name: item.name,
      price: item.price,
      quantity: 1,
    })
  }
}

function removeFromCart(item) {
  const idx = cart.value.findIndex((c) => c.menuItemId === item.menuItemId)
  if (idx === -1) return
  if (cart.value[idx].quantity <= 1) {
    cart.value.splice(idx, 1)
  } else {
    cart.value[idx].quantity -= 1
  }
}

function clearCart() {
  cart.value = []
  showCart.value = false
}

async function submitOrder() {
  if (!tableId.value || cart.value.length === 0) return
  submitting.value = true
  try {
    const order = await createOrder(restaurantId.value, {
      tableId: tableId.value,
      items: cart.value.map((c) => ({ menuItemId: c.menuItemId, quantity: c.quantity })),
      customerNotes: customerNotes.value?.trim() || undefined,
    })
    toast('Order submitted! Order #' + (order.orderNumber || order.id))
    clearCart()
    customerNotes.value = ''
  } catch (e) {
    toast(e.message || 'Order failed', 'error')
  } finally {
    submitting.value = false
  }
}

async function load() {
  const rid = restaurantId.value
  if (!rid) return
  loading.value = true
  error.value = ''
  try {
    const [info, menu] = await Promise.all([
      getPublicRestaurantInfo(rid),
      getPublicMenu(rid),
    ])
    restaurantName.value = info.name || 'Restaurant'
    categories.value = menu.categories || []
    items.value = menu.items || []
  } catch (e) {
    error.value = e.message || 'Failed to load menu'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="customer-order-menu">
    <header class="page-header">
      <h1>{{ restaurantName }}</h1>
      <p v-if="tableNumber" class="table-badge">Table {{ tableNumber }}</p>
    </header>

    <p v-if="error" class="error">{{ error }}</p>
    <div v-if="loading" class="loading">Loading menu...</div>

    <template v-else>
      <div v-for="cat in categories" :key="cat.id" class="category-block">
        <h2 class="category-name">{{ cat.name }}</h2>
        <div class="item-grid">
          <div
            v-for="item in itemByCategory(cat.id)"
            :key="item.id"
            class="item-card"
            @click="addToCart(item)"
          >
            <img
              v-if="item.imageUrl"
              :src="item.imageUrl"
              :alt="item.name"
              class="item-img"
              @error="$event.target.style.display='none'"
            />
            <div class="item-info">
              <span class="item-name">{{ item.name }}</span>
              <span class="item-price">${{ Number(item.price).toFixed(2) }}</span>
            </div>
            <button type="button" class="add-btn">+ Add</button>
          </div>
        </div>
      </div>
      <div v-if="items.filter((i) => !i.categoryId).length" class="category-block">
        <h2 class="category-name">Other</h2>
        <div class="item-grid">
          <div
            v-for="item in items.filter((i) => !i.categoryId)"
            :key="item.id"
            class="item-card"
            @click="addToCart(item)"
          >
            <img
              v-if="item.imageUrl"
              :src="item.imageUrl"
              :alt="item.name"
              class="item-img"
              @error="$event.target.style.display='none'"
            />
            <div class="item-info">
              <span class="item-name">{{ item.name }}</span>
              <span class="item-price">${{ Number(item.price).toFixed(2) }}</span>
            </div>
            <button type="button" class="add-btn">+ Add</button>
          </div>
        </div>
      </div>
    </template>

    <!-- Cart FAB -->
    <button
      v-if="cartCount > 0"
      type="button"
      class="cart-fab"
      @click="showCart = true"
    >
      Cart ({{ cartCount }})
    </button>

    <!-- Cart drawer -->
    <div v-if="showCart" class="cart-overlay" @click.self="showCart = false">
      <div class="cart-drawer">
        <div class="cart-header">
          <h2>Your order</h2>
          <button type="button" class="close-btn" @click="showCart = false">×</button>
        </div>
        <ul class="cart-list">
          <li v-for="c in cart" :key="c.menuItemId" class="cart-item">
            <span class="cart-item-name">{{ c.name }} × {{ c.quantity }}</span>
            <span class="cart-item-price">${{ (Number(c.price) * c.quantity).toFixed(2) }}</span>
            <div class="cart-item-actions">
              <button type="button" class="qty-btn" @click="removeFromCart(c)">−</button>
              <span class="qty">{{ c.quantity }}</span>
              <button type="button" class="qty-btn" @click="getItemById(c.menuItemId) && addToCart(getItemById(c.menuItemId))">+</button>
            </div>
          </li>
        </ul>
        <div class="cart-notes">
          <label>Notes (optional)</label>
          <textarea v-model="customerNotes" rows="2" placeholder="Special requests..."></textarea>
        </div>
        <div class="cart-total">
          Total: ${{ cartTotal }}
        </div>
        <button
          type="button"
          class="submit-btn"
          :disabled="submitting"
          @click="submitOrder"
        >
          {{ submitting ? 'Submitting...' : 'Submit order' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.customer-order-menu {
  padding: 1rem;
  padding-bottom: 5rem;
}
.page-header {
  background: #fff;
  margin: 0 -1rem 1rem;
  padding: 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.page-header h1 {
  margin: 0 0 0.25rem;
  font-size: 1.25rem;
}
.table-badge {
  margin: 0;
  font-size: 0.875rem;
  color: #646cff;
  font-weight: 500;
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
.category-block {
  margin-bottom: 1.5rem;
}
.category-name {
  margin: 0 0 0.75rem;
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 0.75rem;
}
.item-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.item-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.item-img {
  width: 100%;
  height: 100px;
  object-fit: cover;
}
.item-info {
  padding: 0.5rem 0.75rem;
}
.item-name {
  display: block;
  font-size: 0.9rem;
  font-weight: 500;
}
.item-price {
  font-size: 0.875rem;
  color: #646cff;
  font-weight: 600;
}
.add-btn {
  width: 100%;
  padding: 0.4rem;
  border: none;
  background: #646cff;
  color: #fff;
  font-size: 0.8rem;
  cursor: pointer;
}
.cart-fab {
  position: fixed;
  bottom: 1.5rem;
  right: 1rem;
  padding: 0.75rem 1.25rem;
  background: #646cff;
  color: #fff;
  border: none;
  border-radius: 24px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(100,108,255,0.4);
  cursor: pointer;
}
.cart-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 100;
  display: flex;
  justify-content: flex-end;
}
.cart-drawer {
  width: 100%;
  max-width: 360px;
  background: #fff;
  padding: 1rem;
  overflow-y: auto;
}
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.cart-header h2 {
  margin: 0;
  font-size: 1.25rem;
}
.close-btn {
  border: none;
  background: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
}
.cart-list {
  list-style: none;
  margin: 0 0 1rem;
  padding: 0;
}
.cart-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
}
.cart-item-name {
  flex: 1;
  font-size: 0.9rem;
}
.cart-item-price {
  font-weight: 600;
  font-size: 0.9rem;
}
.cart-item-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.qty-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 1rem;
}
.qty {
  min-width: 1.5rem;
  text-align: center;
  font-size: 0.9rem;
}
.cart-notes {
  margin-bottom: 1rem;
}
.cart-notes label {
  display: block;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}
.cart-notes textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-sizing: border-box;
}
.cart-total {
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
}
.submit-btn {
  width: 100%;
  padding: 0.75rem;
  border: none;
  background: #646cff;
  color: #fff;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
