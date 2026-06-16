import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('../views/user/UserList.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('../views/product/ProductList.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'product/add',
        name: 'ProductAdd',
        component: () => import('../views/product/ProductForm.vue'),
        meta: { title: '新增商品' }
      },
      {
        path: 'product/:id',
        name: 'ProductEdit',
        component: () => import('../views/product/ProductForm.vue'),
        meta: { title: '编辑商品' }
      },
      {
        path: 'product/category',
        name: 'Category',
        component: () => import('../views/product/Category.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('../views/order/OrderList.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('../views/order/OrderDetail.vue'),
        meta: { title: '订单详情' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
