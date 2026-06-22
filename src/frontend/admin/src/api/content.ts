import { http } from '@/utils/request'

// 内容相关 API
export interface ContentListQuery {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
  category?: string
}

export interface ContentItem {
  id: number
  title: string
  author: string
  category: string
  status: string
  viewCount: number
  likeCount: number
  createdAt: string
}

// 获取内容列表
export const getContentList = (params: ContentListQuery) =>
  http.get<{ list: ContentItem[]; total: number }>('/admin/contents', params)

// 获取内容详情
export const getContentDetail = (id: number) =>
  http.get<ContentItem>(`/admin/contents/${id}`)

// 审核通过
export const approveContent = (id: number) =>
  http.post<void>(`/admin/contents/${id}/approve`)

// 审核拒绝
export const rejectContent = (id: number, reason?: string) =>
  http.post<void>(`/admin/contents/${id}/reject`, { reason })

// 下架
export const offlineContent = (id: number) =>
  http.post<void>(`/admin/contents/${id}/offline`)

// 删除
export const deleteContent = (id: number) =>
  http.delete<void>(`/admin/contents/${id}`)
