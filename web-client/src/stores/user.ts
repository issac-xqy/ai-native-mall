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

const TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000

export const useUserStore = defineStore('user', () => {
  // 过滤旧版 bug 产生的无效 token
  const rawToken = localStorage.getItem('token') || ''
  const token = ref<string>(rawToken === 'undefined' || rawToken === 'null' || rawToken.length < 10 ? '' : rawToken)
  if (!token.value) localStorage.removeItem('token')

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
    if (!token.value) return false
    return (Date.now() - loginTime.value) <= TOKEN_EXPIRE_TIME
  })

  async function login(username: string, password: string) {
    try {
      const data = await post<{ success: boolean; message?: string; data: { token: string; userInfo: UserInfo } }>(
        '/api/user/login',
        { username, password }
      )
      if (data.success && data.data?.token) {
        token.value = data.data.token
        userInfo.value = data.data.userInfo
        loginTime.value = Date.now()
        localStorage.setItem('token', data.data.token)
        localStorage.setItem('userInfo', JSON.stringify(data.data.userInfo))
        localStorage.setItem('loginTime', String(Date.now()))
        return { success: true as const }
      }
      return { success: false as const, message: data.message }
    } catch (e: any) {
      return { success: false as const, message: e?.message || '登录失败，请稍后重试' }
    }
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
    token.value = ''
    userInfo.value = null
    loginTime.value = 0
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('loginTime')
  }

  async function checkExpiry(): Promise<void> {
    if (!token.value) return
    if (Date.now() - loginTime.value > TOKEN_EXPIRE_TIME) logout()
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

  return { token, userInfo, loginTime, isLoggedIn, login, register, logout, checkExpiry, fetchUserInfo }
})
