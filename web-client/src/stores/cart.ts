import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number
  quantity: number
  image?: string
  stock?: number  // 库存数量
  selected: boolean  // 是否选中
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const selectAll = ref(true)  // 全选状态

  const totalCount = computed(() => 
    items.value.reduce((sum, item) => sum + item.quantity, 0)
  )

  // 已选中商品总价
  const selectedTotalPrice = computed(() =>
    items.value
      .filter(item => item.selected)
      .reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  // 已选中商品总数量
  const selectedCount = computed(() =>
    items.value
      .filter(item => item.selected)
      .reduce((sum, item) => sum + item.quantity, 0)
  )

  function addToCart(product: any, quantity: number = 1) {
    const existingItem = items.value.find(item => item.productId === product.id)
    
    if (existingItem) {
      existingItem.quantity += quantity
    } else {
      items.value.push({
        id: Date.now(),
        productId: product.id,
        productName: product.name,
        price: product.price,
        quantity: quantity,
        image: product.image,
        stock: product.stock || 999,
        selected: true
      })
    }
  }

  function removeFromCart(itemId: number) {
    const index = items.value.findIndex(item => item.id === itemId)
    if (index !== -1) {
      items.value.splice(index, 1)
    }
  }

  function updateQuantity(itemId: number, quantity: number) {
    const item = items.value.find(item => item.id === itemId)
    if (item) {
      if (quantity <= 0) {
        removeFromCart(itemId)
      } else {
        // 检查库存
        if (item.stock && quantity > item.stock) {
          item.quantity = item.stock
          return false  // 返回 false 表示超出库存
        }
        item.quantity = quantity
      }
    }
    return true
  }

  // 切换单个商品选中状态
  function toggleSelect(itemId: number) {
    const item = items.value.find(item => item.id === itemId)
    if (item) {
      item.selected = !item.selected
      updateSelectAllStatus()
    }
  }

  // 切换全选
  function toggleSelectAll() {
    selectAll.value = !selectAll.value
    items.value.forEach(item => {
      item.selected = selectAll.value
    })
  }

  // 更新全选状态
  function updateSelectAllStatus() {
    selectAll.value = items.value.length > 0 && items.value.every(item => item.selected)
  }

  // 删除选中商品
  function removeSelected() {
    items.value = items.value.filter(item => !item.selected)
    selectAll.value = true
  }

  function clearCart() {
    items.value = []
  }

  return {
    items,
    selectAll,
    totalCount,
    selectedTotalPrice,
    selectedCount,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    toggleSelect,
    toggleSelectAll,
    removeSelected,
    updateSelectAllStatus
  }
})
