import { createRouter, createWebHistory } from 'vue-router'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import RestaurantListView from '../views/RestaurantListView.vue'
import PlatformAdminsView from '../views/PlatformAdminsView.vue'
import ProfileView from '../views/ProfileView.vue'
import ManageAdminsView from '../views/ManageAdminsView.vue'
import MyRestaurantView from '../views/MyRestaurantView.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: LoginView, meta: { title: 'Login' } },
  {
    path: '/',
    component: DashboardLayout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'Home', component: HomeView, meta: { title: 'Dashboard' } },
      { path: 'restaurants', name: 'Restaurants', component: RestaurantListView, meta: { title: 'Restaurants' } },
      { path: 'platform-admins', name: 'PlatformAdmins', component: PlatformAdminsView, meta: { title: 'Platform Admins' } },
      { path: 'my-restaurant', name: 'MyRestaurant', component: MyRestaurantView, meta: { title: 'My Restaurant' } },
      { path: 'staff', name: 'Staff', component: ManageAdminsView, meta: { title: 'Staff' } },
      { path: 'profile', name: 'Profile', component: ProfileView, meta: { title: 'Profile' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  const title = to.meta?.title ? `${to.meta.title} - QR Ordering` : (to.name ? `${String(to.name)} - QR Ordering` : 'QR Ordering')
  document.title = title
})

// Restrict routes by role
router.beforeEach((to) => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    const role = user?.roleName
    if (to.path === '/restaurants' || to.path === '/platform-admins') {
      if (role !== 'PLATFORM_ADMIN') return { path: '/home' }
    } else if (to.path === '/my-restaurant') {
      if (role !== 'RESTAURANT_ADMIN') return { path: '/home' }
    } else if (to.path === '/staff') {
      if (role !== 'PLATFORM_ADMIN' && role !== 'RESTAURANT_ADMIN') return { path: '/home' }
    }
  } catch (_) {
    if (to.path === '/my-restaurant' || to.path === '/staff') return { path: '/home' }
  }
})

export default router
