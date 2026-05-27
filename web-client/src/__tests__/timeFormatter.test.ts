import { describe, it, expect, vi } from 'vitest'
import {
  formatRelativeTime, formatFullTime, formatDate, formatTime,
  shouldShowFullTime, formatSmartTime, getTimeDiffDescription
} from '../utils/timeFormatter'

describe('timeFormatter', () => {
  describe('formatRelativeTime', () => {
    it('空字符串返回 -', () => {
      expect(formatRelativeTime('')).toBe('-')
    })

    it('1分钟内返回"刚刚"', () => {
      const now = new Date()
      const justNow = new Date(now.getTime() - 30 * 1000).toISOString()
      expect(formatRelativeTime(justNow)).toBe('刚刚')
    })

    it('5分钟前返回"5分钟前"', () => {
      const now = new Date()
      const fiveMinAgo = new Date(now.getTime() - 5 * 60 * 1000).toISOString()
      expect(formatRelativeTime(fiveMinAgo)).toBe('5分钟前')
    })

    it('3小时前返回"3小时前"', () => {
      const now = new Date()
      const threeHoursAgo = new Date(now.getTime() - 3 * 3600 * 1000).toISOString()
      expect(formatRelativeTime(threeHoursAgo)).toBe('3小时前')
    })

    it('2天前返回"2天前"', () => {
      const now = new Date()
      const twoDaysAgo = new Date(now.getTime() - 2 * 86400 * 1000).toISOString()
      expect(formatRelativeTime(twoDaysAgo)).toBe('2天前')
    })

    it('英文模式-刚刚返回 just now', () => {
      const now = new Date()
      const justNow = new Date(now.getTime() - 10 * 1000).toISOString()
      expect(formatRelativeTime(justNow, { useChinese: false })).toBe('just now')
    })
  })

  describe('formatDate', () => {
    it('返回 YYYY-MM-DD 格式', () => {
      expect(formatDate(new Date('2026-05-27'))).toBe('2026-05-27')
    })

    it('接受字符串参数', () => {
      expect(formatDate('2026-01-15')).toBe('2026-01-15')
    })
  })

  describe('formatTime', () => {
    it('返回 HH:mm:ss 格式', () => {
      const d = new Date('2026-05-27T14:30:45')
      expect(formatTime(d)).toBe('14:30:45')
    })
  })

  describe('formatFullTime', () => {
    it('返回完整时间格式', () => {
      expect(formatFullTime('2026-05-27T14:30:45')).toBe('2026-05-27 14:30:45')
    })

    it('空字符串返回 -', () => {
      expect(formatFullTime('')).toBe('-')
    })
  })

  describe('shouldShowFullTime', () => {
    it('超过30天返回 true', () => {
      const now = new Date()
      const fortyDaysAgo = new Date(now.getTime() - 40 * 86400 * 1000).toISOString()
      expect(shouldShowFullTime(fortyDaysAgo)).toBe(true)
    })

    it('5天内返回 false', () => {
      const now = new Date()
      const fiveDaysAgo = new Date(now.getTime() - 5 * 86400 * 1000).toISOString()
      expect(shouldShowFullTime(fiveDaysAgo)).toBe(false)
    })
  })

  describe('formatSmartTime', () => {
    it('近期时间返回相对时间', () => {
      const now = new Date()
      const recent = new Date(now.getTime() - 3600 * 1000).toISOString()
      expect(formatSmartTime(recent)).toBe('1小时前')
    })

    it('远期时间返回完整时间', () => {
      const now = new Date()
      const old = new Date(now.getTime() - 60 * 86400 * 1000).toISOString()
      const result = formatSmartTime(old, { fullTimeAfterDays: 30 })
      expect(result).toContain('-') // 包含日期格式
    })
  })

  describe('getTimeDiffDescription', () => {
    it('2小时30分 -> "2小时30分钟"', () => {
      const start = new Date('2026-05-27T10:00:00')
      const end = new Date('2026-05-27T12:30:00')
      expect(getTimeDiffDescription(start, end)).toBe('2小时30分钟')
    })

    it('0秒差值返回"0秒"', () => {
      const now = new Date()
      expect(getTimeDiffDescription(now, now)).toBe('0秒')
    })
  })
})
