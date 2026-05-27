/**
 * HTTP 请求拦截器
 * 统一处理错误、重试机制、Token 过期
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

/**
 * 封装 fetch 请求
 */
export async function request<T = any>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const { retry = 1, timeout = 120000, ...fetchOptions } = options
  
  // 默认请求头
  const defaultHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  
  // 如果 body 是 URLSearchParams 或 FormData，浏览器会自动设置 Content-Type
  if (fetchOptions.body instanceof URLSearchParams || fetchOptions.body instanceof FormData) {
    delete defaultHeaders['Content-Type']
  }
  
  // 添加 Token
  const token = localStorage.getItem('token')
  if (token) {
    defaultHeaders['Authorization'] = token
  }
  
  const headers = {
    ...defaultHeaders,
    ...fetchOptions.headers,
  }
  
  // 请求重试机制
  let lastError: Error
  
  for (let attempt = 0; attempt <= retry; attempt++) {
    try {
      // 超时控制
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), timeout)
      
      const response = await fetch(url, {
        ...fetchOptions,
        headers,
        signal: controller.signal,
      })
      
      clearTimeout(timeoutId)
      
      // 处理响应
      const data = await response.json()
      
      // Token 过期，跳转登录
      if (response.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('loginTime')
        
        // 避免在登录页重复跳转
        if (window.location.pathname !== '/login') {
          window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
        }
        
        throw new HttpError('登录已过期，请重新登录', 401, 'UNAUTHORIZED')
      }
      
      // 业务错误
      if (!data.success) {
        throw new HttpError(data.message || '请求失败', response.status, data.code)
      }
      
      return data as T
    } catch (error: any) {
      lastError = error
      
      // 超时错误
      if (error.name === 'AbortError') {
        console.warn(`请求超时: ${url} (尝试 ${attempt + 1}/${retry + 1})`)
        continue
      }
      
      // 网络错误可重试
      if (error.name === 'TypeError' || error.message.includes('NetworkError')) {
        console.warn(`网络错误: ${url} (尝试 ${attempt + 1}/${retry + 1})`)
        continue
      }
      
      // 其他错误直接抛出
      throw error
    }
  }
  
  throw lastError!
}

/**
 * GET 请求
 */
export function get<T = any>(url: string, options?: RequestOptions): Promise<T> {
  return request<T>(url, { ...options, method: 'GET' })
}

/**
 * POST 请求
 */
export function post<T = any>(url: string, body?: any, options?: RequestOptions): Promise<T> {
  // 如果 body 是 URLSearchParams 或 FormData，不需要 JSON.stringify
  let requestBody = body
  if (body && !(body instanceof URLSearchParams) && !(body instanceof FormData)) {
    requestBody = JSON.stringify(body)
  }
  
  return request<T>(url, {
    ...options,
    method: 'POST',
    body: requestBody,
  })
}

/**
 * PUT 请求
 */
export function put<T = any>(url: string, body?: any, options?: RequestOptions): Promise<T> {
  // 如果 body 是 URLSearchParams 或 FormData，不需要 JSON.stringify
  let requestBody = body
  if (body && !(body instanceof URLSearchParams) && !(body instanceof FormData)) {
    requestBody = JSON.stringify(body)
  }
  
  return request<T>(url, {
    ...options,
    method: 'PUT',
    body: requestBody,
  })
}

/**
 * DELETE 请求
 */
export function del<T = any>(url: string, options?: RequestOptions): Promise<T> {
  return request<T>(url, { ...options, method: 'DELETE' })
}

// 别名，兼容 remove 命名
export const remove = del
