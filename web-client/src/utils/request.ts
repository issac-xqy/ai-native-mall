/**
 * HTTP 请求拦截器 — 统一处理 Token 自动刷新、重试、超时
 */

// 请求配置
interface RequestOptions extends RequestInit {
  retry?: number
  timeout?: number
}

// 自定义错误类
export class HttpError extends Error {
  status: number
  code: string

  constructor(message: string, status: number, code: string = 'UNKNOWN') {
    super(message)
    this.name = 'HttpError'
    this.status = status
    this.code = code
  }
}

// Token 自动刷新状态机
let isRefreshing = false
let refreshPromise: Promise<boolean> | null = null

async function doRefresh(): Promise<boolean> {
  const rt = localStorage.getItem('refreshToken')
  if (!rt) return false
  try {
    const resp = await fetch('/api/user/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt })
    })
    const data = await resp.json()
    if (data.success && data.data?.accessToken) {
      localStorage.setItem('accessToken', data.data.accessToken)
      localStorage.setItem('refreshToken', data.data.refreshToken)
      localStorage.setItem('loginTime', String(Date.now()))
      return true
    }
  } catch { /* refresh failed */ }

  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('loginTime')
  localStorage.removeItem('token')
  return false
}

/**
 * 封装 fetch 请求 — 支持 Token 自动刷新、重试、超时
 */
export async function request<T = any>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const { retry = 1, timeout = 120000, ...fetchOptions } = options

  const defaultHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  if (fetchOptions.body instanceof URLSearchParams || fetchOptions.body instanceof FormData) {
    delete defaultHeaders['Content-Type']
  }

  // API 版本化：自动将旧 /api/xxx 映射为 /api/v1/xxx
  const resolveUrl = (u: string) => {
    if (u.startsWith('/api/v1/')) return u
    if (u.startsWith('/api/')) return '/api/v1' + u.substring(4)
    return '/api/v1' + u
  }

  // 使用新的 accessToken，兼容旧的 token
  const token = localStorage.getItem('accessToken') || localStorage.getItem('token')
  if (token && token !== 'undefined' && token !== 'null' && token.length > 10) {
    defaultHeaders['Authorization'] = token
  }

  const buildHeaders = () => ({
    ...defaultHeaders,
    ...fetchOptions.headers,
  })

  let lastError: Error

  for (let attempt = 0; attempt <= retry; attempt++) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), timeout)

      const response = await fetch(resolveUrl(url), {
        ...fetchOptions,
        headers: buildHeaders(),
        signal: controller.signal,
      })

      clearTimeout(timeoutId)

      if (response.status === 401 && url !== '/api/user/refresh') {
        // 尝试自动刷新 token
        if (!isRefreshing) {
          isRefreshing = true
          refreshPromise = doRefresh().finally(() => {
            isRefreshing = false
            refreshPromise = null
          })
        }
        const refreshed = await refreshPromise
        if (refreshed) {
          // 更新 Authorization header 并重试
          const newToken = localStorage.getItem('accessToken')
          if (newToken) {
            defaultHeaders['Authorization'] = newToken
          }
          continue
        }

        // 刷新失败，清理并跳转登录
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('loginTime')
        localStorage.removeItem('token')
        if (window.location.pathname !== '/login') {
          window.location.assign('/login?redirect=' + encodeURIComponent(window.location.pathname))
        }
        throw new HttpError('登录已过期，请重新登录', 401, 'UNAUTHORIZED')
      }

      const data = await response.json()

      if (response.status === 401) {
        // /api/user/refresh 401 特殊处理
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('loginTime')
        localStorage.removeItem('token')
        if (window.location.pathname !== '/login') {
          window.location.assign('/login?redirect=' + encodeURIComponent(window.location.pathname))
        }
        throw new HttpError('登录已过期，请重新登录', 401, 'UNAUTHORIZED')
      }

      if (!data.success) {
        throw new HttpError(data.message || '请求失败', response.status, data.code)
      }

      return data as T
    } catch (error: any) {
      lastError = error

      if (error.name === 'AbortError') {
        console.warn(`请求超时: ${url} (尝试 ${attempt + 1}/${retry + 1})`)
        continue
      }

      if (error.name === 'TypeError' || error.message?.includes('NetworkError')) {
        console.warn(`网络错误: ${url} (尝试 ${attempt + 1}/${retry + 1})`)
        continue
      }

      throw error
    }
  }

  throw lastError!
}

export function get<T = any>(url: string, options?: RequestOptions): Promise<T> {
  return request<T>(url, { ...options, method: 'GET' })
}

export function post<T = any>(url: string, body?: any, options?: RequestOptions): Promise<T> {
  let requestBody = body
  if (body && !(body instanceof URLSearchParams) && !(body instanceof FormData)) {
    requestBody = JSON.stringify(body)
  }
  return request<T>(url, { ...options, method: 'POST', body: requestBody })
}

export function put<T = any>(url: string, body?: any, options?: RequestOptions): Promise<T> {
  let requestBody = body
  if (body && !(body instanceof URLSearchParams) && !(body instanceof FormData)) {
    requestBody = JSON.stringify(body)
  }
  return request<T>(url, { ...options, method: 'PUT', body: requestBody })
}

export function del<T = any>(url: string, options?: RequestOptions): Promise<T> {
  return request<T>(url, { ...options, method: 'DELETE' })
}

export const remove = del
