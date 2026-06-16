import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: () => import('../layout/ShopLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue') },
      { path: 'cart', name: 'Cart', component: () => import('../views/Cart.vue') },
      { path: 'checkout', name: 'Checkout', component: () => import('../views/Checkout.vue') },
      { path: 'orders', name: 'MyOrders', component: () => import('../views/MyOrders.vue') },
      { path: 'order/:id', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue') },
      { path: 'address', name: 'AddressList', component: () => import('../views/AddressList.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('shop-token')
  const publicPaths = ['/login', '/register', '/']
  if (!token && !publicPaths.includes(to.path) && !to.path.startsWith('/product/')) {
    next('/login')
  } else {
    next()
  }
})

export default router
