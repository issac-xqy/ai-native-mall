import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { post, get } from '../utils/request'

export interface UserInfo {
  id: number
  username: string
  phone: string
  nickname: string
  avatar?: string
  email?: string
  createTime?: string
}

const ACCESS_TOKEN_EXPIRE = 15 * 60 * 1000

export const useUserStore = defineStore('user', () => {
  const rawAccessToken = localStorage.getItem('accessToken') || ''
  const rawRefreshToken = localStorage.getItem('refreshToken') || ''

  const accessToken = ref<string>(rawAccessToken === 'undefined' || rawAccessToken === 'null' || rawAccessToken.length < 10 ? '' : rawAccessToken)
  const refreshToken = ref<string>(rawRefreshToken === 'undefined' || rawRefreshToken === 'null' || rawRefreshToken.length < 10 ? '' : rawRefreshToken)

  const safeJsonParse = (key: string) => {
    try {
      const item = localStorage.getItem(key)
      if (!item || item === 'undefined' || item === 'null') return null
      return JSON.parse(item)
    } catch { return null }
  }

  const userInfo = ref<UserInfo | null>(safeJsonParse('userInfo'))
  const loginTime = ref<number>(Number(localStorage.getItem('loginTime')) || 0)

  const isLoggedIn = computed(() => {
    if (!accessToken.value) return false
    return (Date.now() - loginTime.value) <= ACCESS_TOKEN_EXPIRE
  })

  async function login(username: string, password: string) {
    try {
      const data = await post<{ success: boolean; message?: string; data: { accessToken: string; refreshToken: string; userInfo: UserInfo } }>(
        '/api/user/login',
        { username, password }
      )
      if (data.success && data.data?.accessToken) {
        accessToken.value = data.data.accessToken
        refreshToken.value = data.data.refreshToken
        userInfo.value = data.data.userInfo
        loginTime.value = Date.now()
        localStorage.setItem('accessToken', data.data.accessToken)
        localStorage.setItem('refreshToken', data.data.refreshToken)
        localStorage.setItem('userInfo', JSON.stringify(data.data.userInfo))
        localStorage.setItem('loginTime', String(Date.now()))
        return { success: true as const }
      }
      return { success: false as const, message: data.message }
    } catch (e: any) {
      return { success: false as const, message: e?.message || '登录失败，请稍后重试' }
    }
  }

  async function refreshAuth(): Promise<boolean> {
    if (!refreshToken.value) return false
    try {
      const data = await post<{ success: boolean; data: { accessToken: string; refreshToken: string } }>(
        '/api/user/refresh',
        { refreshToken: refreshToken.value }
      )
      if (data.success && data.data?.accessToken) {
        accessToken.value = data.data.accessToken
        refreshToken.value = data.data.refreshToken
        loginTime.value = Date.now()
        localStorage.setItem('accessToken', data.data.accessToken)
        localStorage.setItem('refreshToken', data.data.refreshToken)
        localStorage.setItem('loginTime', String(Date.now()))
        return true
      }
    } catch {
      // refresh 失败，清除登录状态
    }
    logout()
    return false
  }

  async function register(username: string, phone: string, password: string, email?: string) {
    try {
      const data = await post<{ success: boolean; message?: string }>(
        '/api/user/register',
        { username, phone, email, password }
      )
      return { success: data.success, message: data.message }
    } catch (e: any) {
      return { success: false as const, message: e?.message || '注册失败，请稍后重试' }
    }
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    userInfo.value = null
    loginTime.value = 0
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('loginTime')
    localStorage.removeItem('token')
  }

  async function checkExpiry(): Promise<void> {
    if (!accessToken.value) return
    if (Date.now() - loginTime.value > ACCESS_TOKEN_EXPIRE) {
      await refreshAuth()
    }
  }

  async function fetchUserInfo() {
    try {
      const data = await get<{ success: boolean; data: UserInfo }>('/api/user/info')
      if (data.success && data.data) {
        userInfo.value = data.data
        localStorage.setItem('userInfo', JSON.stringify(data.data))
        return true
      }
      return false
    } catch { return false }
  }

  return { accessToken, refreshToken, userInfo, loginTime, isLoggedIn, login, refreshAuth, register, logout, checkExpiry, fetchUserInfo }
})
