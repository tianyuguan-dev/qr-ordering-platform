<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getRestaurants } from '../api/restaurants'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../api/menu'
import { getMenuItems, getAllMenuItems, createMenuItem, updateMenuItem, deleteMenuItem, updateMenuItemStatus } from '../api/menu'
import { uploadImage } from '../api/upload'
import { setToken } from '../api/client'
import { toast } from '../utils/toast'

const STATUS_OPTIONS = [
  { value: 1, label: 'Available' },
  { value: 2, label: 'Sold Out' },
  { value: 3, label: 'Inactive' },
]

const router = useRouter()
const route = useRoute()
const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (_) {
    return null
  }
})
const isPlatformAdmin = computed(() => user.value?.roleName === 'PLATFORM_ADMIN')
const isRestaurantAdmin = computed(() => user.value?.roleName === 'RESTAURANT_ADMIN')
const isKitchen = computed(() => user.value?.roleName === 'KITCHEN')
const canEditMenu = computed(() => isPlatformAdmin.value || isRestaurantAdmin.value)

const restaurantList = ref([])
const selectedRestaurantId = ref('')
const tab = ref('items') // 'categories' | 'items'
const categories = ref([])
const categoryLoading = ref(false)
const categoryError = ref('')
const categoryModal = ref(false)
const categoryEditId = ref(null)
const categoryForm = ref({ name: '', sortOrder: 0 })
const categoryFormError = ref('')
const categorySaving = ref(false)
const categoryDeleteConfirm = ref(null)

const items = ref([])
const itemsTotal = ref(0)
const itemsPage = ref(0)
const itemsPageSize = 20
const categoryFilter = ref(null)
const statusFilter = ref(null)
const itemsLoading = ref(false)
const itemsError = ref('')
const itemModal = ref(false)
const itemEditId = ref(null)
const itemForm = ref({
  categoryId: null,
  name: '',
  description: '',
  price: '',
  imageUrl: '',
  allergens: '',
  status: 1,
})
const itemFormError = ref('')
const itemSaving = ref(false)
const itemImageUploading = ref(false)
const itemImageInput = ref(null)
const itemDeleteConfirm = ref(null)

const effectiveRestaurantId = computed(() => {
  if (isRestaurantAdmin.value || isKitchen.value) return 'me'
  return selectedRestaurantId.value || route.query.restaurant || null
})

const categoryOptions = computed(() => [
  { value: null, label: 'All categories' },
  ...categories.value.map((c) => ({ value: c.id, label: c.name })),
])

async function loadRestaurants() {
  if (!isPlatformAdmin.value) return
  try {
    const res = await getRestaurants({ page: 0, size: 500 })
    restaurantList.value = res.content || []
    if (route.query.restaurant && restaurantList.value.some((r) => r.id === route.query.restaurant)) {
      selectedRestaurantId.value = route.query.restaurant
    } else if (restaurantList.value.length && !selectedRestaurantId.value) {
      selectedRestaurantId.value = restaurantList.value[0].id
    }
  } catch (_) {}
}

async function loadCategories() {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  categoryLoading.value = true
  categoryError.value = ''
  try {
    categories.value = await getCategories(rid)
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
      return
    }
    if (e.status === 403) await router.push('/home')
    else categoryError.value = e.message || 'Failed to load categories'
  } finally {
    categoryLoading.value = false
  }
}

async function loadItems() {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  itemsLoading.value = true
  itemsError.value = ''
  try {
    const res = await getMenuItems(rid, {
      page: itemsPage.value,
      size: itemsPageSize,
      categoryId: categoryFilter.value ?? undefined,
      status: statusFilter.value ?? undefined,
    })
    items.value = res.content || []
    itemsTotal.value = res.totalElements ?? 0
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
      return
    }
    if (e.status === 403) await router.push('/home')
    else itemsError.value = e.message || 'Failed to load menu items'
  } finally {
    itemsLoading.value = false
  }
}

function openCategoryCreate() {
  categoryEditId.value = null
  categoryForm.value = { name: '', sortOrder: 0 }
  categoryFormError.value = ''
  categoryModal.value = true
}

function openCategoryEdit(c) {
  categoryEditId.value = c.id
  categoryForm.value = { name: c.name, sortOrder: c.sortOrder ?? 0 }
  categoryFormError.value = ''
  categoryModal.value = true
}

function closeCategoryModal() {
  categoryModal.value = false
  categoryEditId.value = null
}

async function saveCategory() {
  categoryFormError.value = ''
  if (!categoryForm.value.name?.trim()) {
    categoryFormError.value = 'Name is required'
    return
  }
  categorySaving.value = true
  const rid = effectiveRestaurantId.value
  if (!rid) return
  try {
    if (categoryEditId.value) {
      await updateCategory(rid, categoryEditId.value, {
        name: categoryForm.value.name.trim(),
        sortOrder: categoryForm.value.sortOrder ?? 0,
      })
      toast('Category updated')
    } else {
      await createCategory(rid, {
        name: categoryForm.value.name.trim(),
        sortOrder: categoryForm.value.sortOrder ?? 0,
      })
      toast('Category created')
    }
    closeCategoryModal()
    await loadCategories()
    await loadItems()
  } catch (e) {
    categoryFormError.value = e.message || 'Save failed'
  } finally {
    categorySaving.value = false
  }
}

function confirmDeleteCategory(c) {
  categoryDeleteConfirm.value = c
}

function cancelDeleteCategory() {
  categoryDeleteConfirm.value = null
}

async function doDeleteCategory() {
  if (!categoryDeleteConfirm.value) return
  const rid = effectiveRestaurantId.value
  if (!rid) return
  try {
    await deleteCategory(rid, categoryDeleteConfirm.value.id)
    toast('Category deleted')
    categoryDeleteConfirm.value = null
    await loadCategories()
    await loadItems()
  } catch (e) {
    toast(e.message || 'Delete failed', 'error')
  }
}

function openItemCreate() {
  itemEditId.value = null
  const firstCatId = categories.value.length ? categories.value[0].id : null
  itemForm.value = {
    categoryId: firstCatId,
    name: '',
    description: '',
    price: '',
    imageUrl: '',
    allergens: '',
    status: 1,
  }
  itemFormError.value = ''
  itemModal.value = true
}

function openItemEdit(item) {
  itemEditId.value = item.id
  itemForm.value = {
    categoryId: item.categoryId ?? null,
    name: item.name ?? '',
    description: item.description ?? '',
    price: item.price != null ? String(item.price) : '',
    imageUrl: item.imageUrl ?? '',
    allergens: item.allergens ?? '',
    status: item.status ?? 1,
  }
  itemFormError.value = ''
  itemModal.value = true
}

function closeItemModal() {
  itemModal.value = false
  itemEditId.value = null
}

async function onItemImageChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast('Please choose an image', 'error')
    return
  }
  itemImageUploading.value = true
  try {
    const { url } = await uploadImage(file, { prefix: 'dishes' })
    itemForm.value.imageUrl = url
    toast('Image uploaded')
  } catch (err) {
    toast(err.message || 'Upload failed', 'error')
  } finally {
    itemImageUploading.value = false
    if (itemImageInput.value) itemImageInput.value.value = ''
  }
}

async function saveItem() {
  itemFormError.value = ''
  if (!itemForm.value.name?.trim()) {
    itemFormError.value = 'Name is required'
    return
  }
  const price = parseFloat(itemForm.value.price)
  if (isNaN(price) || price < 0) {
    itemFormError.value = 'Valid price is required'
    return
  }
  const rid = effectiveRestaurantId.value
  if (!rid) return
  itemSaving.value = true
  try {
    const body = {
      categoryId: itemForm.value.categoryId || undefined,
      name: itemForm.value.name.trim(),
      description: itemForm.value.description?.trim() || undefined,
      price,
      imageUrl: itemForm.value.imageUrl?.trim() || undefined,
      allergens: itemForm.value.allergens?.trim() || undefined,
    }
    if (itemEditId.value) {
      body.status = itemForm.value.status
      await updateMenuItem(rid, itemEditId.value, body)
      toast('Item updated')
    } else {
      await createMenuItem(rid, body)
      toast('Item created')
    }
    closeItemModal()
    await loadItems()
  } catch (e) {
    itemFormError.value = e.message || 'Save failed'
  } finally {
    itemSaving.value = false
  }
}

function confirmDeleteItem(item) {
  itemDeleteConfirm.value = item
}

function cancelDeleteItem() {
  itemDeleteConfirm.value = null
}

async function doDeleteItem() {
  if (!itemDeleteConfirm.value) return
  const rid = effectiveRestaurantId.value
  if (!rid) return
  try {
    await deleteMenuItem(rid, itemDeleteConfirm.value.id)
    toast('Item deleted')
    itemDeleteConfirm.value = null
    await loadItems()
  } catch (e) {
    toast(e.message || 'Delete failed', 'error')
  }
}

function categoryName(id) {
  if (!id) return '—'
  const c = categories.value.find((x) => x.id === id)
  return c ? c.name : id
}

function statusLabel(code) {
  return STATUS_OPTIONS.find((o) => o.value === code)?.label ?? code
}

const kitchenItems = ref([])
const kitchenItemsLoading = ref(false)
async function loadKitchenItems() {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  kitchenItemsLoading.value = true
  try {
    kitchenItems.value = await getAllMenuItems(rid)
  } catch (e) {
    if (e.status === 401) {
      setToken(null)
      localStorage.removeItem('user')
      await router.push('/login')
    } else toast(e.message || 'Failed to load items', 'error')
  } finally {
    kitchenItemsLoading.value = false
  }
}

const statusUpdating = ref(null)
async function onKitchenStatusChange(item, newStatus) {
  const rid = effectiveRestaurantId.value
  if (!rid) return
  statusUpdating.value = item.id
  try {
    await updateMenuItemStatus(rid, item.id, { status: newStatus })
    item.status = newStatus
    toast('Status updated')
  } catch (e) {
    toast(e.message || 'Update failed', 'error')
  } finally {
    statusUpdating.value = null
  }
}

onMounted(async () => {
  await loadRestaurants()
  if (effectiveRestaurantId.value) {
    await loadCategories()
    if (isKitchen.value) await loadKitchenItems()
    else await loadItems()
  }
})

watch(effectiveRestaurantId, (val) => {
  if (val) {
    loadCategories()
    if (isKitchen.value) loadKitchenItems()
    else loadItems()
  } else {
    categories.value = []
    items.value = []
    kitchenItems.value = []
  }
})
</script>

<template>
  <div class="menu-page">
    <h1>Menu</h1>
    <div v-if="isPlatformAdmin" class="restaurant-selector">
      <label>Restaurant:</label>
      <select v-model="selectedRestaurantId" class="restaurant-select">
        <option value="">Select restaurant</option>
        <option v-for="r in restaurantList" :key="r.id" :value="r.id">{{ r.name }}</option>
      </select>
    </div>
    <div v-if="isPlatformAdmin && !effectiveRestaurantId" class="hint">Select a restaurant to manage its menu.</div>
    <template v-if="effectiveRestaurantId">
    <!-- Kitchen: item availability only -->
    <div v-if="isKitchen" class="panel">
      <div class="panel-header">
        <span>Item availability</span>
      </div>
      <p class="kitchen-hint">Set items as Available, Sold Out, or Inactive. Customers cannot order Sold Out or Inactive items.</p>
      <div v-if="kitchenItemsLoading" class="loading">Loading...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Category</th>
              <th>Price</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in kitchenItems" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ categoryName(item.categoryId) }}</td>
              <td>{{ item.price != null ? Number(item.price).toFixed(2) : '—' }}</td>
              <td>
                <select
                  :value="item.status"
                  class="status-select"
                  :disabled="statusUpdating === item.id"
                  @change="onKitchenStatusChange(item, Number(($event.target).value))"
                >
                  <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!kitchenItems.length" class="empty">No menu items.</div>
      </div>
    </div>

    <!-- Admin: full menu management -->
    <template v-else>
    <div class="tabs">
      <button type="button" class="tab" :class="{ active: tab === 'categories' }" @click="tab = 'categories'">
        Categories
      </button>
      <button type="button" class="tab" :class="{ active: tab === 'items' }" @click="tab = 'items'">
        Menu Items
      </button>
    </div>

    <!-- Categories tab -->
    <div v-show="tab === 'categories'" class="panel">
      <div class="panel-header">
        <span>Categories</span>
        <button v-if="canEditMenu" type="button" class="btn primary" @click="openCategoryCreate">Add Category</button>
      </div>
      <p v-if="categoryError" class="error">{{ categoryError }}</p>
      <div v-if="categoryLoading" class="loading">Loading...</div>
      <ul v-else class="category-list">
        <li v-for="c in categories" :key="c.id" class="category-row">
          <span class="cat-name">{{ c.name }}</span>
          <span class="cat-sort">Order: {{ c.sortOrder }}</span>
          <div v-if="canEditMenu" class="row-actions">
            <button type="button" class="btn small secondary" @click="openCategoryEdit(c)">Edit</button>
            <button type="button" class="btn small danger" @click="confirmDeleteCategory(c)">Delete</button>
          </div>
        </li>
        <li v-if="!categories.length" class="empty">No categories yet. Add one to group menu items.</li>
      </ul>
    </div>

    <!-- Menu items tab -->
    <div v-show="tab === 'items'" class="panel">
      <div class="panel-header">
        <span>Menu Items</span>
        <button v-if="canEditMenu" type="button" class="btn primary" @click="openItemCreate">Add Item</button>
      </div>
      <div class="filters">
        <select v-model="categoryFilter" class="filter-select" @change="loadItems">
          <option v-for="opt in categoryOptions" :key="String(opt.value)" :value="opt.value">{{ opt.label }}</option>
        </select>
        <select v-model="statusFilter" class="filter-select" @change="loadItems">
          <option :value="null">All statuses</option>
          <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
      <p v-if="itemsError" class="error">{{ itemsError }}</p>
      <div v-if="itemsLoading" class="loading">Loading...</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Image</th>
              <th>Name</th>
              <th>Category</th>
              <th>Price</th>
              <th>Status</th>
              <th class="actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>
                <img
                  v-if="item.imageUrl"
                  :src="item.imageUrl"
                  alt=""
                  class="item-thumb"
                  @error="$event.target.style.display='none'"
                />
                <span v-else class="no-img">—</span>
              </td>
              <td>{{ item.name }}</td>
              <td>{{ categoryName(item.categoryId) }}</td>
              <td>{{ item.price != null ? Number(item.price).toFixed(2) : '—' }}</td>
              <td>{{ statusLabel(item.status) }}</td>
              <td class="actions">
                <template v-if="canEditMenu">
                  <button type="button" class="btn small secondary" @click="openItemEdit(item)">Edit</button>
                  <button type="button" class="btn small danger" @click="confirmDeleteItem(item)">Delete</button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!items.length" class="empty">No menu items yet.</div>
        <div v-if="itemsTotal > itemsPageSize" class="pagination">
          <button
            type="button"
            class="btn small secondary"
            :disabled="itemsPage === 0"
            @click="itemsPage--; loadItems()"
          >
            Previous
          </button>
          <span class="page-info">Page {{ itemsPage + 1 }} ({{ itemsTotal }} total)</span>
          <button
            type="button"
            class="btn small secondary"
            :disabled="itemsPage >= Math.ceil(itemsTotal / itemsPageSize) - 1"
            @click="itemsPage++; loadItems()"
          >
            Next
          </button>
        </div>
      </div>
    </div>

    <!-- Category modal -->
    <div v-if="categoryModal" class="modal-overlay" @click.self="closeCategoryModal">
      <div class="modal">
        <h2>{{ categoryEditId ? 'Edit Category' : 'New Category' }}</h2>
        <form @submit.prevent="saveCategory">
          <div class="field">
            <label>Name <span class="required">*</span></label>
            <input v-model="categoryForm.name" type="text" placeholder="e.g. Drinks" />
          </div>
          <div class="field">
            <label>Sort order</label>
            <input v-model.number="categoryForm.sortOrder" type="number" min="0" />
          </div>
          <p v-if="categoryFormError" class="form-error">{{ categoryFormError }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeCategoryModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="categorySaving">
              {{ categorySaving ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Item modal -->
    <div v-if="itemModal" class="modal-overlay" @click.self="closeItemModal">
      <div class="modal modal-wide">
        <h2>{{ itemEditId ? 'Edit Item' : 'New Menu Item' }}</h2>
        <form @submit.prevent="saveItem">
          <div class="field">
            <label>Category</label>
            <select v-model="itemForm.categoryId" class="field-select">
              <option :value="null">No category</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="field">
            <label>Name <span class="required">*</span></label>
            <input v-model="itemForm.name" type="text" placeholder="Item name" />
          </div>
          <div class="field">
            <label>Description</label>
            <textarea v-model="itemForm.description" rows="2" placeholder="Optional description"></textarea>
          </div>
          <div class="field">
            <label>Price <span class="required">*</span></label>
            <input v-model="itemForm.price" type="number" step="0.01" min="0" placeholder="0.00" />
          </div>
          <div class="field">
            <label>Image</label>
            <input
              ref="itemImageInput"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="hidden"
              @change="onItemImageChange"
            />
            <div class="logo-row">
              <input v-model="itemForm.imageUrl" type="text" placeholder="URL or upload" />
              <button type="button" class="btn secondary" :disabled="itemImageUploading" @click="itemImageInput?.click()">
                {{ itemImageUploading ? 'Uploading...' : 'Upload' }}
              </button>
            </div>
            <img
              v-if="itemForm.imageUrl"
              :src="itemForm.imageUrl"
              alt="Preview"
              class="logo-preview"
              @error="$event.target.style.display='none'"
            />
          </div>
          <div class="field">
            <label>Allergens</label>
            <input v-model="itemForm.allergens" type="text" placeholder="e.g. Nuts, Gluten" />
          </div>
          <div v-if="itemEditId" class="field">
            <label>Status</label>
            <select v-model.number="itemForm.status" class="field-select">
              <option v-for="opt in STATUS_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
            </select>
          </div>
          <p v-if="itemFormError" class="form-error">{{ itemFormError }}</p>
          <div class="modal-actions">
            <button type="button" class="btn secondary" @click="closeItemModal">Cancel</button>
            <button type="submit" class="btn primary" :disabled="itemSaving">
              {{ itemSaving ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Delete category confirm -->
    <div v-if="categoryDeleteConfirm" class="modal-overlay" @click.self="cancelDeleteCategory">
      <div class="modal modal-sm">
        <h2>Delete category?</h2>
        <p>Delete "{{ categoryDeleteConfirm.name }}"? Menu items in this category will keep the category reference as empty.</p>
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="cancelDeleteCategory">Cancel</button>
          <button type="button" class="btn danger" @click="doDeleteCategory">Delete</button>
        </div>
      </div>
    </div>

    <!-- Delete item confirm -->
    <div v-if="itemDeleteConfirm" class="modal-overlay" @click.self="cancelDeleteItem">
      <div class="modal modal-sm">
        <h2>Delete item?</h2>
        <p>Delete "{{ itemDeleteConfirm.name }}"?</p>
        <div class="modal-actions">
          <button type="button" class="btn secondary" @click="cancelDeleteItem">Cancel</button>
          <button type="button" class="btn danger" @click="doDeleteItem">Delete</button>
        </div>
      </div>
    </div>
    </template>
    </template>
  </div>
</template>

<style scoped>
.menu-page {
  max-width: 900px;
  padding: 1.5rem;
}
.menu-page h1 {
  margin: 0 0 1rem;
  font-size: 1.5rem;
}
.restaurant-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.restaurant-selector label {
  font-weight: 500;
  font-size: 0.9rem;
}
.restaurant-select {
  padding: 0.4rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.95rem;
  min-width: 220px;
}
.hint {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}
.tabs {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 1rem;
}
.tab {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 0.9rem;
}
.tab:hover {
  background: #f5f5f5;
}
.tab.active {
  background: #646cff;
  color: #fff;
  border-color: #646cff;
}
.panel {
  background: #fff;
  border-radius: 8px;
  padding: 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.panel-header span {
  font-weight: 600;
}
.error {
  color: #c33;
  padding: 0.5rem 0;
  font-size: 0.9rem;
}
.kitchen-hint {
  margin: 0 0 1rem;
  font-size: 0.9rem;
  color: #666;
}
.status-select {
  padding: 0.35rem 0.6rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.875rem;
}
.loading {
  color: #666;
  padding: 1.5rem;
}
.category-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.category-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.6rem 0;
  border-bottom: 1px solid #eee;
}
.cat-name {
  flex: 1;
  font-weight: 500;
}
.cat-sort {
  color: #666;
  font-size: 0.875rem;
}
.row-actions {
  display: flex;
  gap: 0.5rem;
}
.filters {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.filter-select {
  padding: 0.4rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9rem;
}
.table-wrap {
  overflow-x: auto;
}
.table {
  width: 100%;
  border-collapse: collapse;
}
.table th,
.table td {
  padding: 0.6rem 0.75rem;
  text-align: left;
  border-bottom: 1px solid #eee;
}
.table th {
  background: #f5f5f5;
  font-size: 0.875rem;
  font-weight: 600;
}
.table td.actions {
  white-space: nowrap;
}
.item-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
}
.no-img {
  color: #999;
  font-size: 0.875rem;
}
.empty {
  color: #666;
  padding: 1.5rem;
  text-align: center;
}
.pagination {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
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
.btn.primary {
  background: #646cff;
  color: #fff;
}
.btn.primary:hover:not(:disabled) {
  background: #535bf2;
}
.btn.secondary {
  background: #e0e0e0;
  color: #333;
}
.btn.secondary:hover:not(:disabled) {
  background: #d0d0d0;
}
.btn.danger {
  background: #dc3545;
  color: #fff;
}
.btn.danger:hover:not(:disabled) {
  background: #c82333;
}
.btn.small {
  padding: 0.35rem 0.65rem;
  font-size: 0.8rem;
}
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.required {
  color: #c33;
}
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
  max-width: 420px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}
.modal.modal-wide {
  max-width: 520px;
}
.modal.modal-sm {
  max-width: 360px;
}
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
.modal .field input,
.modal .field textarea,
.modal .field select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  box-sizing: border-box;
}
.modal .field .hidden {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
.modal .field .logo-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.modal .field .logo-row input {
  flex: 1;
}
.modal .field .logo-preview {
  max-width: 120px;
  max-height: 80px;
  object-fit: contain;
  border-radius: 6px;
  border: 1px solid #eee;
}
.form-error {
  color: #c33;
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}
</style>
