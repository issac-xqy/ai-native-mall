import { describe, it, expect, beforeEach, vi } from 'vitest'
import { HttpError } from '../utils/request'

describe('HttpError', () => {
  it('正确设置错误属性', () => {
    const err = new HttpError('登录过期', 401, 'UNAUTHORIZED')

    expect(err).toBeInstanceOf(Error)
    expect(err).toBeInstanceOf(HttpError)
    expect(err.message).toBe('登录过期')
    expect(err.status).toBe(401)
    expect(err.code).toBe('UNAUTHORIZED')
    expect(err.name).toBe('HttpError')
  })

  it('默认 code 为 UNKNOWN', () => {
    const err = new HttpError('服务器错误', 500)
    expect(err.code).toBe('UNKNOWN')
  })

  it('不同类型错误可以区分', () => {
    const authErr = new HttpError('未授权', 401, 'UNAUTHORIZED')
    const bizErr = new HttpError('库存不足', 200, 'INSUFFICIENT_STOCK')
    const serverErr = new HttpError('服务器崩溃', 500)

    expect(authErr.status).toBe(401)
    expect(bizErr.code).toBe('INSUFFICIENT_STOCK')
    expect(serverErr.code).toBe('UNKNOWN')
  })
})
