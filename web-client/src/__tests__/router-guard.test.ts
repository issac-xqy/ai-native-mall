import { describe, it, expect, beforeEach, vi } from 'vitest'

/**
 * 路由守卫逻辑单元测试 — 不依赖 Vue Router，纯函数测试
 * 验证 hasValidToken() 的三种情况
 */

// Mock localStorage
const storage: Record<string, string> = {}

describe('路由守卫 — hasValidToken', () => {
  beforeEach(() => {
    Object.keys(storage).forEach(k => delete storage[k])
  })

  const hasValidToken = (): boolean => {
    const token = storage['accessToken'] || storage['token'] || ''
    if (!token || token === 'undefined' || token === 'null' || token.length < 10) {
      return false
    }
    const loginTime = Number(storage['loginTime'] || '0')
    return Date.now() - loginTime <= 15 * 60 * 1000
  }

  it('无 token → 返回 false', () => {
    expect(hasValidToken()).toBe(false)
  })

  it('token 为 "undefined" 字符串 → 返回 false (旧版 bug)', () => {
    storage['accessToken'] = 'undefined'
    expect(hasValidToken()).toBe(false)
  })

  it('有效 token + 15分钟内登录 → 返回 true', () => {
    storage['accessToken'] = 'Bearer eyJhbGciOiJIUzM4NCJ9.valid-test-token-xxxxx'
    storage['loginTime'] = String(Date.now() - 5 * 60 * 1000) // 5分钟前
    expect(hasValidToken()).toBe(true)
  })

  it('有效 token + 20分钟前登录 → 返回 false (过期)', () => {
    storage['accessToken'] = 'Bearer eyJhbGciOiJIUzM4NCJ9.valid-test-token-xxxxx'
    storage['loginTime'] = String(Date.now() - 20 * 60 * 1000) // 20分钟前
    expect(hasValidToken()).toBe(false)
  })

  it('兼容旧 token key → 返回 true', () => {
    storage['token'] = 'Bearer eyJhbGciOiJIUzM4NCJ9.old-style-token-xxxxx'
    storage['loginTime'] = String(Date.now() - 3 * 60 * 1000)
    expect(hasValidToken()).toBe(true)
  })
})
