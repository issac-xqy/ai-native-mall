import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: '/api/v1/',
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Token 自动刷新
let isRefreshing = false
let refreshPromise: Promise<boolean> | null = null

async function doRefresh(): Promise<boolean> {
  const rt = localStorage.getItem('adminRefreshToken') || localStorage.getItem('refreshToken')
  if (!rt) return false
  try {
    const resp = await fetch('/api/user/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt })
    })
    const data = await resp.json()
    if (data.success && data.data?.accessToken) {
      localStorage.setItem('adminToken', data.data.accessToken)
      localStorage.setItem('adminRefreshToken', data.data.refreshToken)
      return true
    }
  } catch { /* refresh failed */ }
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminRefreshToken')
  localStorage.removeItem('adminUser')
  return false
}

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('adminToken') || localStorage.getItem('accessToken') || localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    if (res.success === false) {
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }

    return res
  },
  async (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      const status = error.response.status

      // 401: 尝试自动刷新 token
      if (status === 401 && error.config && !error.config._retry) {
        if (!isRefreshing) {
          isRefreshing = true
          refreshPromise = doRefresh().finally(() => {
            isRefreshing = false
            refreshPromise = null
          })
        }
        const refreshed = await refreshPromise
        if (refreshed) {
          error.config._retry = true
          const newToken = localStorage.getItem('adminToken')
          if (newToken) {
            error.config.headers.Authorization = newToken
          }
          return request(error.config)
        }

        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminRefreshToken')
        localStorage.removeItem('adminUser')
        window.location.href = '/login'
        return Promise.reject(error)
      }

      switch (status) {
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查后端服务是否启动')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export const get = <T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> => {
  return request.get(url, { params, ...config })
}

export const post = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return request.post(url, data, config)
}

export const put = <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  return request.put(url, data, config)
}

export const del = <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return request.delete(url, config)
}

export default request
