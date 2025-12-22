import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/departments',
    },
    {
      path: '/departments',
      name: 'departments',
      component: () => import('../views/DepartmentView.vue'),
    },
    {
      path: '/locations',
      name: 'locations',
      component: () => import('../views/LocationView.vue'),
    },
    {
      path: '/assets',
      name: 'assets',
      component: () => import('../views/AssetView.vue'),
    },
    {
      path: '/assignees',
      name: 'assignees',
      component: () => import('../views/AssigneeView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

export default router
