import { createRouter, createWebHistory } from 'vue-router'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import RestaurantListView from '../views/RestaurantListView.vue'
import PlatformAdminsView from '../views/PlatformAdminsView.vue'
import ProfileView from '../views/ProfileView.vue'
import ManageAdminsView from '../views/ManageAdminsView.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: LoginView },
  {
    path: '/',
    component: DashboardLayout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'Home', component: HomeView },
      { path: 'restaurants', name: 'Restaurants', component: RestaurantListView },
      { path: 'platform-admins', name: 'PlatformAdmins', component: PlatformAdminsView },
      { path: 'staff', name: 'Staff', component: ManageAdminsView },
      { path: 'profile', name: 'Profile', component: ProfileView },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Restrict routes by role
router.beforeEach((to) => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    const role = user?.roleName
    if (to.path === '/restaurants' || to.path === '/platform-admins') {
      if (role !== 'PLATFORM_ADMIN') return { path: '/home' }
    } else if (to.path === '/staff') {
      if (role !== 'PLATFORM_ADMIN' && role !== 'RESTAURANT_ADMIN') return { path: '/home' }
    }
  } catch (_) {
    if (to.path === '/staff') return { path: '/home' }
  }
})

export default router
