export const BASE64_PLACEHOLDER = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iNDAwIiBoZWlnaHQ9IjQwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTgiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4='

export function getFullImageUrl(path: string): string {
  if (!path) return BASE64_PLACEHOLDER
  if (path.startsWith('/') || path.startsWith('http')) return path
  return BASE64_PLACEHOLDER
}

export function formatSales(sales: number): string {
  if (!sales) return '0'
  if (sales >= 10000) return (sales / 10000).toFixed(1) + '万'
  if (sales >= 1000) return (sales / 1000).toFixed(1) + 'k'
  return String(sales)
}

export function formatNumber(n: number): string {
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

export function getRating(score: unknown): string {
  if (!score) return '4.5'
  const num = typeof score === 'string' ? parseFloat(score) : Number(score)
  if (isNaN(num)) return '4.5'
  return num > 5 ? (num / 5 * 5).toFixed(1) : num.toFixed(1)
}

export function maskPhone(phone: string): string {
  if (!phone || phone.length < 11) return phone || ''
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

export function maskEmail(email: string): string {
  if (!email || !email.includes('@')) return email || ''
  const [name, domain] = email.split('@')
  return name.slice(0, 2) + '***@' + domain
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

export function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + '天前'
  return formatDate(dateStr)
}

export function getStatusType(status: number): 'warning' | 'success' | 'primary' | 'info' | 'danger' {
  return (['warning', 'success', 'primary', 'info', 'danger'] as const)[status] || 'info'
}

export function getStatusText(status: number): string {
  return ['待支付', '已支付', '已发货', '已完成', '已取消'][status] || '未知'
}
