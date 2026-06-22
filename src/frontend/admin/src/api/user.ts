import { http } from '@/utils/request'

// 用户相关 API
export interface UserListQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface UserItem {
  id: number
  nickname: string
  phone: string
  status: string
  noteCount: number
  followerCount: number
  createdAt: string
}

// 获取用户列表
export const getUserList = (params: UserListQuery) =>
  http.get<{ list: UserItem[]; total: number }>('/admin/users', params)

// 获取用户详情
export const getUserDetail = (id: number) =>
  http.get<UserItem>(`/admin/users/${id}`)

// 封禁用户
export const banUser = (id: number) =>
  http.post<void>(`/admin/users/${id}/ban`)

// 解封用户
export const unbanUser = (id: number) =>
  http.post<void>(`/admin/users/${id}/unban`)

// 删除用户
export const deleteUser = (id: number) =>
  http.delete<void>(`/admin/users/${id}`)
