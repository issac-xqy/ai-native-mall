<template>
  <div class="cart">
    <h2>🛒 购物车</h2>
    
    <!-- 凑单提示 -->
    <el-alert
      v-if="cartStore.items.length > 0"
      :type="shippingTip.type"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    >
      <template #title>
        <span v-if="shippingTip.remaining > 0">
          再买 <strong style="color: #f56c6c;">¥{{ shippingTip.remaining.toFixed(2) }}</strong> 即可免运费！
          <el-link type="primary" @click="$router.push('/products')" style="margin-left: 10px;">去凑单 →</el-link>
        </span>
        <span v-else>
          🎉 已满足免运费条件，预计节省 <strong style="color: #67c23a;">¥10.00</strong>
        </span>
      </template>
    </el-alert>
    
    <el-empty v-if="cartStore.items.length === 0" description="购物车空空如也~">
      <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
    </el-empty>
    
    <div v-else>
      <!-- 全选按钮 -->
      <div class="cart-header">
        <el-checkbox v-model="cartStore.selectAll" @change="cartStore.toggleSelectAll">
          全选
        </el-checkbox>
        <el-button type="danger" size="small" @click="handleRemoveSelected">
          删除选中 ({{ selectedCount }})
        </el-button>
      </div>
      
      <el-card v-for="item in cartStore.items" :key="item.id" class="cart-item">
        <el-row :gutter="20" align="middle">
          <!-- 选择框 -->
          <el-col :span="2">
            <el-checkbox v-model="item.selected" @change="cartStore.updateSelectAllStatus" />
          </el-col>
          <!-- 商品图片 -->
          <el-col :span="3">
            <img :src="getFullImageUrl(item.image)" class="product-thumb" loading="lazy" />
          </el-col>
          <!-- 商品信息 -->
          <el-col :span="9">
            <h3>{{ item.productName }}</h3>
            <p class="price">¥{{ item.price }}</p>
            <!-- 库存提示 -->
            <p v-if="item.stock && item.stock <= 10" class="stock-warning">
              <el-icon><Warning /></el-icon>
              仅剩 {{ item.stock }} 件
            </p>
            <p v-else-if="item.stock" class="stock-normal">库存充足</p>
          </el-col>
          <!-- 数量 -->
          <el-col :span="6">
            <el-input-number
              v-model="item.quantity"
              :min="1"
              :max="item.stock || 999"
              @change="(val: number) => handleQuantityChange(item.id, val)"
            />
          </el-col>
          <!-- 小计和操作 -->
          <el-col :span="4">
            <p class="subtotal">小计: ¥{{ (item.price * item.quantity).toFixed(2) }}</p>
            <el-button type="danger" size="small" @click="removeItem(item.id)">
              删除
            </el-button>
          </el-col>
        </el-row>
      </el-card>

      <div class="cart-footer">
        <div class="total">
          <span>已选 {{ cartStore.selectedCount }} 件，合计:</span>
          <span class="amount">¥{{ cartStore.selectedTotalPrice.toFixed(2) }}</span>
        </div>
        <el-button type="primary" size="large" :disabled="cartStore.selectedCount === 0" @click="checkout">
          结算({{ cartStore.selectedCount }})
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useCartStore } from '../stores/cart'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const cartStore = useCartStore()
const router = useRouter()
const userStore = useUserStore()

// 运费规则：满 99 包邮，否则收 10 元运费
const FREE_SHIPPING_THRESHOLD = 99

const shippingTip = computed(() => {
  const totalPrice = cartStore.selectedTotalPrice
  if (totalPrice >= FREE_SHIPPING_THRESHOLD) {
    return { type: 'success' as const, remaining: 0 }
  }
  return { 
    type: 'warning' as const, 
    remaining: FREE_SHIPPING_THRESHOLD - totalPrice 
  }
})

// 选中商品数量
const selectedCount = computed(() => 
  cartStore.items.filter(item => item.selected).length
)

// 获取完整图片URL
const getFullImageUrl = (path?: string) => {
  if (!path) return 'https://via.placeholder.com/80x80?text=No+Image'
  if (path.startsWith('http')) return path
  return path
}

const handleQuantityChange = (itemId: number, quantity: number) => {
  const success = cartStore.updateQuantity(itemId, quantity)
  const item = cartStore.items.find(i => i.id === itemId)
  if (!success && item) {
    ElMessage.warning(`库存不足，最多只能购买 ${item.stock} 件`)
  }
}

const removeItem = (itemId: number) => {
  cartStore.removeFromCart(itemId)
  ElMessage.success('已删除')
}

const handleRemoveSelected = () => {
  const count = cartStore.items.filter(item => item.selected).length
  if (count === 0) {
    ElMessage.warning('请先选择要删除的商品')
    return
  }
  ElMessageBox.confirm(`确定要删除选中的 ${count} 件商品吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cartStore.removeSelected()
    ElMessage.success(`已删除 ${count} 件商品`)
  }).catch(() => {})
}

const checkout = () => {
  if (cartStore.selectedCount === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }
  
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login?redirect=/checkout?fromCart=true')
    return
  }
  
  router.push('/checkout?fromCart=true')
}
</script>

<style scoped>
.cart {
  max-width: 1200px;
  margin: 0 auto;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.cart-item {
  margin-bottom: 20px;
}

.product-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}

.stock-warning {
  color: #f56c6c;
  font-size: 12px;
  margin: 5px 0 0 0;
  display: flex;
  align-items: center;
  gap: 5px;
}

.stock-normal {
  color: #67c23a;
  font-size: 12px;
  margin: 5px 0 0 0;
}

.subtotal {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  margin-top: 20px;
}

.total {
  font-size: 18px;
}

.amount {
  color: #f56c6c;
  font-size: 24px;
  font-weight: bold;
  margin-left: 10px;
}
</style>
