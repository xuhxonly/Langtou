import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 路由配置：管理后台所有页面
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'contents',
        name: 'ContentList',
        component: () => import('@/views/ContentList.vue'),
        meta: { title: '内容管理', icon: 'Document' }
      },
      {
        path: 'notes',
        name: 'NoteList',
        component: () => import('@/views/NoteList.vue'),
        meta: { title: '笔记管理', icon: 'Notebook' }
      },
      {
        path: 'reports',
        name: 'ReportList',
        component: () => import('@/views/ReportList.vue'),
        meta: { title: '举报管理', icon: 'Warning' }
      },
      {
        path: 'comments',
        name: 'CommentList',
        component: () => import('@/views/CommentList.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' }
      },
      {
        path: 'tags',
        name: 'TagList',
        component: () => import('@/views/TagList.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' }
      },
      {
        path: 'sensitive-words',
        name: 'SensitiveWordList',
        component: () => import('@/views/SensitiveWordList.vue'),
        meta: { title: '敏感词库', icon: 'Lock' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '系统设置', icon: 'Setting' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：简单登录态校验
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('langtou_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
