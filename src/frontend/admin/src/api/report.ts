import { http } from '@/utils/request'

// 举报相关 API
export interface ReportListQuery {
  page?: number
  pageSize?: number
  type?: string
  status?: string
}

export interface ReportItem {
  id: number
  type: string
  targetId: number
  reporter: string
  reason: string
  status: string
  createdAt: string
}

// 获取举报列表
export const getReportList = (params: ReportListQuery) =>
  http.get<{ list: ReportItem[]; total: number }>('/admin/reports', params)

// 处理举报
export const handleReport = (id: number, action: 'resolve' | 'reject') =>
  http.post<void>(`/admin/reports/${id}/handle`, { action })
