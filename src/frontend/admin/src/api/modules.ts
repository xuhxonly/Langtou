import { http } from '@/utils/request'

// 标签 API
export interface TagItem {
  id: number
  name: string
  color: string
  count: number
  createdAt: string
}

export const getTagList = () => http.get<TagItem[]>('/admin/tags')
export const createTag = (data: Omit<TagItem, 'id' | 'count' | 'createdAt'>) =>
  http.post<TagItem>('/admin/tags', data)
export const updateTag = (id: number, data: Partial<TagItem>) =>
  http.put<TagItem>(`/admin/tags/${id}`, data)
export const deleteTag = (id: number) => http.delete<void>(`/admin/tags/${id}`)

// 敏感词 API
export interface SensitiveWordItem {
  id: number
  word: string
  level: 'high' | 'medium' | 'low'
  hitCount: number
}

export const getSensitiveWords = () => http.get<SensitiveWordItem[]>('/admin/sensitive-words')
export const addSensitiveWord = (data: Omit<SensitiveWordItem, 'id' | 'hitCount'>) =>
  http.post<SensitiveWordItem>('/admin/sensitive-words', data)
export const deleteSensitiveWord = (id: number) =>
  http.delete<void>(`/admin/sensitive-words/${id}`)

// 评论 API
export interface CommentItem {
  id: number
  content: string
  author: string
  targetType: string
  targetId: number
  status: string
  createdAt: string
}

export const getCommentList = (params?: any) =>
  http.get<{ list: CommentItem[]; total: number }>('/admin/comments', params)
export const deleteComment = (id: number) =>
  http.delete<void>(`/admin/comments/${id}`)
export const auditComment = (id: number, status: string) =>
  http.post<void>(`/admin/comments/${id}/audit`, { status })

// 笔记 API
export interface NoteItem {
  id: number
  title: string
  author: string
  category: string
  status: string
  viewCount: number
  likeCount: number
  createdAt: string
}

export const getNoteList = (params?: any) =>
  http.get<{ list: NoteItem[]; total: number }>('/admin/notes', params)
export const auditNote = (id: number, action: string) =>
  http.post<void>(`/admin/notes/${id}/audit`, { action })
