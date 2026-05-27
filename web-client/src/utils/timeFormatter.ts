/**
 * 时间格式化工具类
 * 
 * 提供智能的相对时间和完整时间格式化功能
 * 支持多种时间显示策略
 * 
 * @author xqy
 * @since 2026-04-15
 */

export interface TimeFormatOptions {
  /** 是否显示秒数（默认false） */
  showSeconds?: boolean
  /** 超过多少天显示完整时间（默认30天） */
  fullTimeAfterDays?: number
  /** 是否使用中文（默认true） */
  useChinese?: boolean
}

/**
 * 格式化时间为相对时间（智能分级显示）
 * 
 * 分级规则：
 * - 1分钟内：刚刚
 * - 1小时内：X分钟前
 * - 24小时内：X小时前
 * - 7天内：X天前
 * - 30天内：X周前
 * - 1年内：X个月前
 * - 超过1年：YYYY-MM-DD
 * 
 * @param dateStr ISO 8601 格式的时间字符串
 * @param options 格式化选项
 * @returns 相对时间字符串
 * 
 * @example
 * formatRelativeTime('2026-04-15T10:30:00') // "5分钟前"
 * formatRelativeTime('2025-01-01T00:00:00') // "2025-01-01"
 */
export function formatRelativeTime(
  dateStr: string,
  options: TimeFormatOptions = {}
): string {
  const { useChinese = true } = options
  
  if (!dateStr) return '-'
  
  const now = new Date()
  const date = new Date(dateStr)
  const diffMs = now.getTime() - date.getTime()
  
  // 处理未来时间（异常情况）
  if (diffMs < 0) {
    return useChinese ? '刚刚' : 'just now'
  }
  
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)
  const diffWeeks = Math.floor(diffDays / 7)
  const diffMonths = Math.floor(diffDays / 30)
  const diffYears = Math.floor(diffDays / 365)
  
  if (useChinese) {
    return formatChineseRelativeTime(
      diffSeconds,
      diffMinutes,
      diffHours,
      diffDays,
      diffWeeks,
      diffMonths,
      diffYears,
      date
    )
  } else {
    return formatEnglishRelativeTime(
      diffSeconds,
      diffMinutes,
      diffHours,
      diffDays,
      diffWeeks,
      diffMonths,
      diffYears,
      date
    )
  }
}

/**
 * 中文相对时间格式化
 */
function formatChineseRelativeTime(
  diffSeconds: number,
  diffMinutes: number,
  diffHours: number,
  diffDays: number,
  diffWeeks: number,
  diffMonths: number,
  diffYears: number,
  date: Date
): string {
  if (diffSeconds < 60) {
    return '刚刚'
  } else if (diffMinutes < 60) {
    return `${diffMinutes}分钟前`
  } else if (diffHours < 24) {
    return `${diffHours}小时前`
  } else if (diffDays < 7) {
    return `${diffDays}天前`
  } else if (diffWeeks < 4) {
    return `${diffWeeks}周前`
  } else if (diffMonths < 12) {
    return `${diffMonths}个月前`
  } else {
    // 超过1年，显示具体日期
    return formatDate(date)
  }
}

/**
 * 英文相对时间格式化
 */
function formatEnglishRelativeTime(
  diffSeconds: number,
  diffMinutes: number,
  diffHours: number,
  diffDays: number,
  diffWeeks: number,
  diffMonths: number,
  diffYears: number,
  date: Date
): string {
  if (diffSeconds < 60) {
    return 'just now'
  } else if (diffMinutes < 60) {
    return `${diffMinutes}m ago`
  } else if (diffHours < 24) {
    return `${diffHours}h ago`
  } else if (diffDays < 7) {
    return `${diffDays}d ago`
  } else if (diffWeeks < 4) {
    return `${diffWeeks}w ago`
  } else if (diffMonths < 12) {
    return `${diffMonths}mo ago`
  } else {
    return formatDate(date)
  }
}

/**
 * 格式化完整时间
 * 格式：YYYY-MM-DD HH:mm:ss
 * 
 * @param dateStr ISO 8601 格式的时间字符串
 * @param options 格式化选项
 * @returns 完整时间字符串
 * 
 * @example
 * formatFullTime('2026-04-15T10:30:45') // "2026-04-15 10:30:45"
 */
export function formatFullTime(
  dateStr: string,
  options: TimeFormatOptions = {}
): string {
  const { showSeconds = true } = options
  
  if (!dateStr) return '-'
  
  const date = new Date(dateStr)
  const datePart = formatDate(date)
  const timePart = showSeconds ? formatTime(date) : formatTimeWithoutSeconds(date)
  
  return `${datePart} ${timePart}`
}

/**
 * 格式化日期部分
 * 格式：YYYY-MM-DD
 */
export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 格式化时间部分（含秒）
 * 格式：HH:mm:ss
 */
export function formatTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

/**
 * 格式化时间部分（不含秒）
 * 格式：HH:mm
 */
export function formatTimeWithoutSeconds(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

/**
 * 判断是否应该显示完整时间而非相对时间
 * 
 * @param dateStr ISO 8601 格式的时间字符串
 * @param fullTimeAfterDays 超过多少天显示完整时间（默认30天）
 * @returns true 表示应显示完整时间
 */
export function shouldShowFullTime(
  dateStr: string,
  fullTimeAfterDays: number = 30
): boolean {
  if (!dateStr) return false
  
  const now = new Date()
  const date = new Date(dateStr)
  const diffDays = Math.floor(
    (now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24)
  )
  
  return diffDays > fullTimeAfterDays
}

/**
 * 智能格式化时间（根据时间远近自动选择相对时间或完整时间）
 * 
 * @param dateStr ISO 8601 格式的时间字符串
 * @param options 格式化选项
 * @returns 智能选择的时间字符串
 * 
 * @example
 * formatSmartTime('2026-04-15T10:30:00') // "5分钟前"
 * formatSmartTime('2025-01-01T00:00:00') // "2025-01-01 00:00:00"
 */
export function formatSmartTime(
  dateStr: string,
  options: TimeFormatOptions = {}
): string {
  const { fullTimeAfterDays = 30 } = options
  
  if (shouldShowFullTime(dateStr, fullTimeAfterDays)) {
    return formatFullTime(dateStr, options)
  }
  
  return formatRelativeTime(dateStr, options)
}

/**
 * 计算两个时间之间的差值描述
 * 
 * @param startDate 开始时间
 * @param endDate 结束时间（默认为当前时间）
 * @returns 时间差描述
 * 
 * @example
 * getTimeDiffDescription('2026-04-15T10:00:00', '2026-04-15T12:30:00')
 * // "2小时30分钟"
 */
export function getTimeDiffDescription(
  startDate: string | Date,
  endDate: string | Date = new Date()
): string {
  const start = typeof startDate === 'string' ? new Date(startDate) : startDate
  const end = typeof endDate === 'string' ? new Date(endDate) : endDate
  const diffMs = Math.abs(end.getTime() - start.getTime())
  
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)
  
  const hours = diffHours % 24
  const minutes = diffMinutes % 60
  const seconds = diffSeconds % 60
  
  const parts: string[] = []
  
  if (diffDays > 0) {
    parts.push(`${diffDays}天`)
  }
  if (hours > 0) {
    parts.push(`${hours}小时`)
  }
  if (minutes > 0) {
    parts.push(`${minutes}分钟`)
  }
  if (seconds > 0 && parts.length === 0) {
    parts.push(`${seconds}秒`)
  }
  
  return parts.join('') || '0秒'
}
