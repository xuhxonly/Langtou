import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 定义后端统一响应结构
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// 请求拦截器：注入 JWT Token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('langtou_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    // 直接返回业务数据
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200 || res.code === 0) {
        return res.data as any
      }
      // 401 未登录或 Token 失效
      if (res.code === 401) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('langtou_token')
        router.push('/login')
        return Promise.reject(new Error(res.message || '未授权'))
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res as any
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('langtou_token')
      router.push('/login')
    } else if (status === 403) {
      ElMessage.error('无权限访问')
    } else if (status === 500) {
      ElMessage.error('服务器内部错误')
    } else {
      ElMessage.error(error?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

// 封装通用请求方法
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service(config) as Promise<T>
}

export const http = {
  get: <T = unknown>(url: string, params?: any, config?: AxiosRequestConfig) =>
    request<T>({ ...config, url, method: 'get', params }),
  post: <T = unknown>(url: string, data?: any, config?: AxiosRequestConfig) =>
    request<T>({ ...config, url, method: 'post', data }),
  put: <T = unknown>(url: string, data?: any, config?: AxiosRequestConfig) =>
    request<T>({ ...config, url, method: 'put', data }),
  delete: <T = unknown>(url: string, params?: any, config?: AxiosRequestConfig) =>
    request<T>({ ...config, url, method: 'delete', params })
}

export default service
