import { http } from '@/utils/request'

// 仪表盘 API
export interface DashboardStats {
  totalUsers: number
  todayDAU: number
  newUsers: number
  noteCount: number
  interactionCount: number
}

export interface TrendItem {
  date: string
  dau: number
  newUsers: number
  notes: number
}

// 获取统计数据
export const getStats = () => http.get<DashboardStats>('/admin/dashboard/stats')

// 获取趋势数据
export const getTrend = (days = 7) =>
  http.get<TrendItem[]>(`/admin/dashboard/trend`, { days })
