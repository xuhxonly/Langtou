import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http } from '@/utils/request'

// 定义用户信息类型
interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  email: string
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('langtou_token') || '')
  const userInfo = ref<UserInfo | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')

  // 登录
  const login = async (loginName: string, password: string) => {
    // 优先尝试真实接口，失败则走本地 mock（便于离线开发）
    try {
      const data = await http.post<{ token: string; user: UserInfo }>('/admin/login', {
        username: loginName,
        password
      })
      token.value = data.token
      userInfo.value = data.user
      localStorage.setItem('langtou_token', data.token)
      return data
    } catch (e) {
      // Mock：默认 admin / admin123
      if (loginName === 'admin' && password === 'admin123') {
        const mockToken = 'mock-token-' + Date.now()
        token.value = mockToken
        userInfo.value = {
          id: 1,
          username: 'admin',
          nickname: '超级管理员',
          avatar: '',
          role: 'admin',
          email: 'admin@langtou.com'
        }
        localStorage.setItem('langtou_token', mockToken)
        return { token: mockToken, user: userInfo.value }
      }
      throw new Error('用户名或密码错误')
    }
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    if (!token.value) return
    try {
      const data = await http.get<UserInfo>('/admin/profile')
      userInfo.value = data
      return data
    } catch {
      // token 失效
      logout()
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('langtou_token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    login,
    fetchUserInfo,
    logout
  }
})
