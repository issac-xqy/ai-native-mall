import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
  id: number
  username: string
  phone: string
  nickname: string
  avatar?: string
  email?: string
  createTime?: string
}

export const useUserStore = defineStore('user', () => {
  const TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000
  
  const token = ref<string>(localStorage.getItem('token') || '')
  
  // 安全解析 localStorage
  const safeParse = (key: string, defaultValue: any = null) => {
    try {
      const item = localStorage.getItem(key)
      if (!item || item === 'undefined' || item === 'null') return defaultValue
      return JSON.parse(item)
    } catch {
      return defaultValue
    }
  }
  
  const userInfo = ref<UserInfo | null>(safeParse('userInfo'))
  const loginTime = ref<number>(safeParse('loginTime', 0))

  const isLoggedIn = computed(() => {
    if (!token.value) return false
    const now = Date.now()
    if (now - loginTime.value > TOKEN_EXPIRE_TIME) {
      logout()
      return false
    }
    return true
  })

  async function login(username: string, password: string) {
    try {
      const response = await fetch('/api/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })

      const data = await response.json()
      
      if (data.success) {
        token.value = data.token
        userInfo.value = data.userInfo
        loginTime.value = Date.now()
        
        // 保存到localStorage
        localStorage.setItem('token', data.token)
        localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
        localStorage.setItem('loginTime', String(Date.now()))
        
        return { success: true }
      } else {
        return { success: false, message: data.message }
      }
    } catch (error) {
      return { success: false, message: '登录失败，请稍后重试' }
    }
  }

  async function register(username: string, phone: string, password: string, email?: string) {
    try {
      const response = await fetch('/api/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, phone, email, password })
      })

      const data = await response.json()
      return { success: data.success, message: data.message }
    } catch (error) {
      return { success: false, message: '注册失败，请稍后重试' }
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    loginTime.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('loginTime')
  }

  function checkAndRefreshAuth(): boolean {
    if (!token.value) return false
    const now = Date.now()
    if (now - loginTime.value > TOKEN_EXPIRE_TIME) {
      logout()
      return false
    }
    return true
  }

  async function fetchUserInfo() {
    try {
      const response = await fetch('/api/user/info', {
        headers: { 'Authorization': token.value }
      })
      
      const data = await response.json()
      if (data.success) {
        userInfo.value = data.data
        localStorage.setItem('userInfo', JSON.stringify(data.data))
        return true
      }
      return false
    } catch (error) {
      return false
    }
  }

  return {
    token,
    userInfo,
    loginTime,
    isLoggedIn,
    login,
    register,
    logout,
    checkAndRefreshAuth,
    fetchUserInfo
  }
})
