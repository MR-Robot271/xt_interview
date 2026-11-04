import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/users',
      children: [
        {
          path: 'users',
          name: 'UserManagement',
          component: () => import('@/views/UserManagement.vue'),
          meta: { requiresAuth: true, title: '用户管理' }
        },
        {
          path: 'departments',
          name: 'DepartmentManagement',
          component: () => import('@/views/DepartmentManagement.vue'),
          meta: { requiresAuth: true, title: '部门管理' }
        }
      ]
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.title) {
    document.title = `${to.meta.title} - 欣铁 面试项目`
  }
  next()
})

export default router
