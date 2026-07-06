export interface ApiResponse<T = unknown> {
  code: number
  success: boolean
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface Product {
  id: number
  name: string
  price: number
  originalPrice?: number
  image?: string
  images?: string
  description?: string
  sales: number
  stock?: number
  sentimentScore: number | string
  publishStatus?: number
  categoryId?: number
  status?: number
}

export interface Category {
  id: number
  name: string
  parentId: number
  sortOrder?: number
  icon?: string
  status?: number
}

export interface OrderItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  totalAmount: number
}

export interface Order {
  id: number
  orderNo: string
  totalAmount: number
  status: number
  createTime: string
  items: OrderItem[]
}

export interface Review {
  id: number
  productId: number
  userId: number
  content: string
  rating: number
  sentiment?: string
  aiTags?: string
  summary?: string
  createTime: string
}

export interface WalletInfo {
  id: number
  userId: number
  balance: number
  totalRecharge: number
  totalSpent: number
}

export interface UserAddress {
  id: number
  userId: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: number
}

export interface RechargeRecord {
  id: number
  amount: number
  rechargeType: number
  status: number
  tradeNo: string
  remark?: string
  createTime: string
}
