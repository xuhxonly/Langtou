<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Odometer,
  User,
  Document,
  Notebook,
  Warning,
  ChatDotRound,
  PriceTag,
  Lock,
  Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapse = ref(false)

// 菜单分组
const menuGroups = [
  {
    title: '概览',
    items: [{ path: '/dashboard', title: '仪表盘', icon: Odometer }]
  },
  {
    title: '业务管理',
    items: [
      { path: '/users', title: '用户管理', icon: User },
      { path: '/contents', title: '内容管理', icon: Document },
      { path: '/notes', title: '笔记管理', icon: Notebook },
      { path: '/comments', title: '评论管理', icon: ChatDotRound }
    ]
  },
  {
    title: '审核与风控',
    items: [
      { path: '/reports', title: '举报管理', icon: Warning },
      { path: '/sensitive-words', title: '敏感词库', icon: Lock },
      { path: '/tags', title: '标签管理', icon: PriceTag }
    ]
  },
  {
    title: '系统',
    items: [{ path: '/settings', title: '系统设置', icon: Setting }]
  }
]

// 当前激活菜单路径
const activeMenu = computed(() => route.path)

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter((m) => m.meta && m.meta.title)
  return matched.map((m) => ({ title: m.meta?.title as string }))
})

// 菜单点击
const handleMenuClick = (path: string) => {
  router.push(path)
}

// 退出登录
const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

// 跳转个人信息
const handleUserClick = () => {
  router.push('/settings')
}
</script>

<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside
      :width="isCollapse ? '64px' : '220px'"
      class="layout-aside"
    >
      <div class="brand">
        <div class="brand-logo">L</div>
        <div v-if="!isCollapse" class="brand-text">
          <div class="brand-title">榔头 Langtou</div>
          <div class="brand-sub">Admin</div>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="var(--lt-aside-bg)"
        text-color="var(--lt-aside-text)"
        active-text-color="var(--lt-brand-color)"
        class="layout-menu"
      >
        <template v-for="group in menuGroups" :key="group.title">
          <div
            v-if="!isCollapse"
            class="menu-group-title"
          >
            {{ group.title }}
          </div>
          <el-menu-item
            v-for="item in group.items"
            :key="item.path"
            :index="item.path"
            @click="handleMenuClick(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主容器 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="(bc, idx) in breadcrumbs"
              :key="idx"
            >
              {{ bc.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="刷新">
            <el-icon class="header-icon"><Refresh /></el-icon>
          </el-tooltip>
          <el-tooltip content="通知">
            <el-badge :value="12" class="header-badge">
              <el-icon class="header-icon"><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
          <el-dropdown @command="(cmd: string) => cmd === 'logout' && handleLogout()">
            <div class="user-info" @click="handleUserClick">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.username?.charAt(0) || '管' }}
              </el-avatar>
              <span class="user-name">{{ userStore.username || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.layout-aside {
  background-color: var(--lt-aside-bg);
  transition: width 0.2s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--lt-border-color);
  gap: 10px;
  flex-shrink: 0;
}

.brand-logo {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--lt-brand-color), var(--lt-brand-secondary));
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.brand-text {
  overflow: hidden;
}

.brand-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--lt-text-primary);
  white-space: nowrap;
}

.brand-sub {
  font-size: 11px;
  color: var(--lt-text-muted);
}

.layout-menu {
  flex: 1;
  border-right: none !important;
  overflow-y: auto;

  :deep(.el-menu-item) {
    margin: 4px 8px;
    border-radius: 6px;
    height: 40px;
    line-height: 40px;
  }

  :deep(.el-menu-item:hover) {
    background-color: var(--lt-aside-hover) !important;
  }

  :deep(.el-menu-item.is-active) {
    background-color: rgba(59, 130, 246, 0.15) !important;
  }
}

.menu-group-title {
  padding: 12px 20px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--lt-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.layout-header {
  background-color: var(--lt-header-bg);
  border-bottom: 1px solid var(--lt-border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px !important;
  line-height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--lt-text-secondary);
  transition: color 0.2s;

  &:hover {
    color: var(--lt-brand-color);
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  font-size: 18px;
  cursor: pointer;
  color: var(--lt-text-secondary);

  &:hover {
    color: var(--lt-brand-color);
  }
}

.header-badge {
  :deep(.el-badge__content) {
    top: 6px;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 6px;
  transition: background 0.2s;

  &:hover {
    background: var(--lt-aside-hover);
  }
}

.user-avatar {
  background: linear-gradient(135deg, var(--lt-brand-color), var(--lt-brand-secondary));
  color: #fff;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: var(--lt-text-primary);
}

.layout-main {
  background-color: var(--lt-bg-primary);
  padding: 20px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
