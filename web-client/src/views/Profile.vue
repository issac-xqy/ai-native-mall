<template>
  <div class="profile">
    <el-row :gutter="20">
      <!-- 左侧菜单 -->
      <el-col :span="6">
        <el-card class="menu-card">
          <div class="user-header">
            <el-avatar :size="80" class="user-avatar">
              {{ userStore.userInfo?.nickname?.[0] || 'U' }}
            </el-avatar>
            <h3 class="nickname">{{ userStore.userInfo?.nickname || '用户' }}</h3>
            <p class="username">@{{ userStore.userInfo?.username }}</p>
          </div>
          
          <el-menu :default-active="activeTab" @select="handleTabChange">
            <el-menu-item index="info">
              <el-icon><User /></el-icon>
              <span>个人资料</span>
            </el-menu-item>
            <el-menu-item index="address">
              <el-icon><Location /></el-icon>
              <span>收货地址</span>
            </el-menu-item>
            <el-menu-item index="orders">
              <el-icon><ShoppingCart /></el-icon>
              <span>我的订单</span>
            </el-menu-item>
            <el-menu-item index="favorites">
              <el-icon><Star /></el-icon>
              <span>我的收藏</span>
            </el-menu-item>
            <el-menu-item index="security">
              <el-icon><Lock /></el-icon>
              <span>安全设置</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- 右侧内容区 -->
      <el-col :span="18">
        <!-- 个人资料 -->
        <el-card v-if="activeTab === 'info'" class="content-card">
          <template #header>
            <div class="card-header">
              <span>📝 个人资料</span>
            </div>
          </template>

          <el-form :model="profileForm" label-width="100px" class="profile-form">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            
            <el-form-item label="昵称">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
            </el-form-item>
            
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            
            <el-form-item label="注册时间">
              <el-input v-model="profileForm.createTime" disabled />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="handleUpdateProfile" :loading="updating">
                保存修改
              </el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 收货地址 -->
        <el-card v-if="activeTab === 'address'" class="content-card">
          <template #header>
            <div class="card-header">
              <span>📍 收货地址</span>
              <el-button type="primary" size="small" @click="handleAddAddress">+ 新增地址</el-button>
            </div>
          </template>

          <el-empty v-if="!addresses.length" description="暂无收货地址">
            <el-button type="primary" @click="handleAddAddress">新增地址</el-button>
          </el-empty>
          
          <div v-else class="address-list">
            <el-card
              v-for="addr in addresses"
              :key="addr.id"
              class="address-card"
              :class="{ 'address-card-default': addr.isDefault === 1 }"
            >
              <div class="address-header">
                <span class="receiver-name">{{ addr.receiverName }}</span>
                <span class="receiver-phone">{{ addr.receiverPhone }}</span>
                <el-tag v-if="addr.isDefault === 1" type="success" size="small">默认</el-tag>
              </div>
              <div class="address-detail">
                {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
              </div>
              <div class="address-actions">
                <el-button size="small" @click="handleEditAddress(addr)">编辑</el-button>
                <el-button size="small" type="primary" plain @click="handleSetDefault(addr)" v-if="addr.isDefault !== 1">
                  设为默认
                </el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteAddress(addr)">删除</el-button>
              </div>
            </el-card>
          </div>

          <!-- 地址编辑对话框 -->
          <el-dialog v-model="addressDialogVisible" :title="isEditAddress ? '编辑地址' : '新增地址'" width="600px">
            <el-form :model="currentAddress" label-width="100px">
              <el-form-item label="收货人">
                <el-input v-model="currentAddress.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="currentAddress.receiverPhone" placeholder="请输入手机号" maxlength="11" />
              </el-form-item>
              <el-form-item label="所在地区">
                <el-row :gutter="10">
                  <el-col :span="8">
                    <el-input v-model="currentAddress.province" placeholder="省份" />
                  </el-col>
                  <el-col :span="8">
                    <el-input v-model="currentAddress.city" placeholder="城市" />
                  </el-col>
                  <el-col :span="8">
                    <el-input v-model="currentAddress.district" placeholder="区县" />
                  </el-col>
                </el-row>
              </el-form-item>
              <el-form-item label="详细地址">
                <el-input v-model="currentAddress.detailAddress" type="textarea" :rows="3" placeholder="街道、楼牌号等" />
              </el-form-item>
              <el-form-item label="设为默认">
                <el-switch v-model="currentAddress.isDefault" :active-value="1" :inactive-value="0" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="addressDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="handleSaveAddress">保存</el-button>
            </template>
          </el-dialog>
        </el-card>

        <!-- 我的订单 -->
        <el-card v-if="activeTab === 'orders'" class="content-card">
          <template #header>
            <div class="card-header">
              <span>📦 我的订单</span>
              <el-button type="primary" size="small" @click="router.push('/orders')">查看全部</el-button>
            </div>
          </template>

          <el-empty v-if="!orders.length" description="暂无订单" />
          
          <div v-else class="order-list">
            <div v-for="order in orders" :key="order.id" class="order-item" @click="router.push(`/orders`)">
              <div class="order-header">
                <span class="order-no">订单号: {{ order.orderNo }}</span>
                <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
              </div>
              <div class="order-body">
                <div class="order-items">
                  <span v-for="item in order.items?.slice(0, 2)" :key="item.id" class="order-item-name">
                    {{ item.productName }} x{{ item.quantity }}
                  </span>
                  <span v-if="order.items?.length > 2" class="more-items">等{{ order.items.length }}件商品</span>
                </div>
                <div class="order-footer">
                  <span class="order-time">{{ formatDate(order.createTime) }}</span>
                  <span class="order-amount">¥{{ order.totalAmount }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 我的收藏 -->
        <el-card v-if="activeTab === 'favorites'" class="content-card">
          <template #header>
            <div class="card-header">
              <span>⭐ 我的收藏</span>
            </div>
          </template>

          <el-empty description="暂无收藏商品">
            <el-button type="primary" @click="router.push('/products')">去逛逛</el-button>
          </el-empty>
        </el-card>

        <!-- 安全设置 -->
        <el-card v-if="activeTab === 'security'" class="content-card">
          <template #header>
            <div class="card-header">
              <span>🔒 安全设置</span>
            </div>
          </template>

          <div class="security-list">
            <div class="security-item">
              <div class="security-info">
                <el-icon :size="24" color="#409EFF"><Lock /></el-icon>
                <div>
                  <h4>登录密码</h4>
                  <p class="desc">定期修改密码可以保护账号安全</p>
                </div>
              </div>
              <el-button type="primary" plain @click="showPasswordDialog = true">修改密码</el-button>
            </div>

            <div class="security-item">
              <div class="security-info">
                <el-icon :size="24" color="#67c23a"><Cellphone /></el-icon>
                <div>
                  <h4>绑定手机</h4>
                  <p class="desc">{{ maskPhone(profileForm.phone) }}</p>
                </div>
              </div>
              <el-button type="primary" plain>更换手机</el-button>
            </div>

            <div class="security-item">
              <div class="security-info">
                <el-icon :size="24" color="#e6a23c"><Message /></el-icon>
                <div>
                  <h4>绑定邮箱</h4>
                  <p class="desc">{{ maskEmail(profileForm.email) }}</p>
                </div>
              </div>
              <el-button type="primary" plain>更换邮箱</el-button>
            </div>

            <div class="security-item">
              <div class="security-info">
                <el-icon :size="24" color="#f56c6c"><Delete /></el-icon>
                <div>
                  <h4>账号注销</h4>
                  <p class="desc">注销后将无法恢复，请谨慎操作</p>
                </div>
              </div>
              <el-button type="danger" plain>申请注销</el-button>
            </div>
          </div>

          <!-- 修改密码对话框 -->
          <el-dialog v-model="showPasswordDialog" title="修改密码" width="500px">
            <el-form :model="passwordForm" label-width="100px">
              <el-form-item label="当前密码">
                <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" />
              </el-form-item>
              <el-form-item label="新密码">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
              </el-form-item>
              <el-form-item label="确认密码">
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="showPasswordDialog = false">取消</el-button>
              <el-button type="primary" @click="handleChangePassword">确定</el-button>
            </template>
          </el-dialog>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { User, ShoppingCart, Star, Lock, Cellphone, Message, Delete, Location } from '@element-plus/icons-vue'
import { put, get, post, remove } from '../utils/request'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')
const updating = ref(false)
const showPasswordDialog = ref(false)
const addressDialogVisible = ref(false)
const isEditAddress = ref(false)

// 个人资料表单
const profileForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  createTime: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 订单列表
const orders = ref<any[]>([])

// 地址列表
const addresses = ref<any[]>([])
const currentAddress = ref<any>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

// 加载用户信息
const loadUserInfo = async () => {
  if (userStore.userInfo) {
    profileForm.username = userStore.userInfo.username || ''
    profileForm.nickname = userStore.userInfo.nickname || ''
    profileForm.phone = userStore.userInfo.phone || ''
    profileForm.email = userStore.userInfo.email || ''
    profileForm.createTime = userStore.userInfo.createTime || '-'
  }
}

// 加载收货地址
const loadAddresses = async () => {
  try {
    const data = await get<any>('/api/address/list')
    if (data.success) {
      addresses.value = data.data || []
    }
  } catch (error) {
    console.error('加载地址失败', error)
  }
}

// 新增地址
const handleAddAddress = () => {
  isEditAddress.value = false
  currentAddress.value = {
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: 0
  }
  addressDialogVisible.value = true
}

// 编辑地址
const handleEditAddress = (address: any) => {
  isEditAddress.value = true
  currentAddress.value = { ...address }
  addressDialogVisible.value = true
}

// 保存地址
const handleSaveAddress = async () => {
  if (!currentAddress.value.receiverName || !currentAddress.value.receiverPhone || !currentAddress.value.detailAddress) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    const payload = { ...currentAddress.value }
    
    if (isEditAddress.value) {
      await put(`/api/address/${currentAddress.value.id}`, payload)
      ElMessage.success('地址更新成功')
    } else {
      await post('/api/address', payload)
      ElMessage.success('地址添加成功')
    }
    
    addressDialogVisible.value = false
    loadAddresses()
  } catch (error: any) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

// 设置默认地址
const handleSetDefault = async (address: any) => {
  try {
    await put(`/api/address/${address.id}/default`, {})
    ElMessage.success('设置成功')
    loadAddresses()
  } catch (error: any) {
    ElMessage.error('设置失败: ' + (error.message || '未知错误'))
  }
}

// 删除地址
const handleDeleteAddress = async (address: any) => {
  try {
    await remove(`/api/address/${address.id}`)
    ElMessage.success('删除成功')
    loadAddresses()
  } catch (error: any) {
    ElMessage.error('删除失败: ' + (error.message || '未知错误'))
  }
}

// 加载最近订单
const loadRecentOrders = async () => {
  try {
    const userId = userStore.userInfo?.id || 1
    const data = await get<any>(`/api/order/list?userId=${userId}`)
    if (data.success) {
      orders.value = (data.data || []).slice(0, 5)
    }
  } catch (error) {
    console.error('加载订单失败', error)
  }
}

// 切换标签
const handleTabChange = (tab: string) => {
  activeTab.value = tab
}

// 更新个人资料
const handleUpdateProfile = async () => {
  if (!profileForm.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }

  updating.value = true
  try {
    await put('/api/user/info', {
      nickname: profileForm.nickname,
      phone: profileForm.phone,
      email: profileForm.email
    })

    // 更新本地 Store
    userStore.userInfo = {
      ...userStore.userInfo,
      nickname: profileForm.nickname,
      phone: profileForm.phone,
      email: profileForm.email
    } as any
    localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))

    ElMessage.success('保存成功')
  } catch (error: any) {
    console.error('更新失败', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    updating.value = false
  }
}

// 修改密码
const handleChangePassword = () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }

  // TODO: 调用后端修改密码接口
  ElMessage.success('密码修改成功')
  showPasswordDialog.value = false
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

// 重置表单
const resetForm = () => {
  loadUserInfo()
  ElMessage.info('已重置')
}

// 获取订单状态类型
const getStatusType = (status: number) => {
  const types = ['warning', 'success', 'primary', 'info', 'danger']
  return types[status] || 'info'
}

// 获取订单状态文本
const getStatusText = (status: number) => {
  const texts = ['待支付', '已支付', '已发货', '已完成', '已取消']
  return texts[status] || '未知'
}

// 格式化时间
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 手机号脱敏
const maskPhone = (phone: string) => {
  if (!phone || phone.length < 11) return '未绑定'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// 邮箱脱敏
const maskEmail = (email: string) => {
  if (!email) return '未绑定'
  const [name, domain] = email.split('@')
  if (name.length <= 2) return email
  return name.substring(0, 2) + '***@' + domain
}

onMounted(() => {
  loadUserInfo()
  loadRecentOrders()
  loadAddresses()
})
</script>

<style scoped>
.profile {
  max-width: 1200px;
  margin: 0 auto;
}

.menu-card {
  position: sticky;
  top: 20px;
}

.user-header {
  text-align: center;
  padding: 20px 0 30px 0;
  border-bottom: 1px solid #ebeef5;
}

.user-avatar {
  background-color: #409EFF;
  color: white;
  font-size: 32px;
  margin-bottom: 15px;
}

.nickname {
  margin: 0;
  color: #303133;
  font-size: 18px;
}

.username {
  margin: 5px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.content-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-form {
  max-width: 500px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.order-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.order-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f5f7fa;
}

.order-no {
  font-size: 14px;
  color: #606266;
}

.order-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.order-item-name {
  font-size: 14px;
  color: #303133;
}

.more-items {
  color: #909399;
  font-size: 14px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.order-time {
  font-size: 12px;
  color: #909399;
}

.order-amount {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.security-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.security-info h4 {
  margin: 0 0 5px 0;
  color: #303133;
  font-size: 16px;
}

.security-info .desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
}

.address-card {
  position: relative;
  cursor: pointer;
  transition: all 0.3s;
}

.address-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.address-card-default {
  border: 2px solid #67c23a;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 10px;
}

.receiver-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.receiver-phone {
  font-size: 14px;
  color: #606266;
}

.address-detail {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
}

.address-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  border-top: 1px solid #f5f7fa;
  padding-top: 10px;
}
</style>
