<template>
  <div class="product-ai">
    <el-row :gutter="20">
      <!-- SEO 标题生成 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>🎯 SEO 标题生成</span>
          </template>
          <el-form label-width="100px">
            <el-form-item label="商品名称">
              <el-input v-model="seoForm.productName" placeholder="例如：iPhone 15 Pro" />
            </el-form-item>
            <el-form-item label="商品分类">
              <el-input v-model="seoForm.category" placeholder="例如：手机通讯" />
            </el-form-item>
            <el-form-item label="商品特点">
              <el-input
                v-model="seoForm.features"
                type="textarea"
                :rows="3"
                placeholder="例如：A17 Pro芯片,钛金属边框,4800万像素"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="generateSeoTitle"
                :loading="seoLoading"
              >
                生成 SEO 标题
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="seoResult"
            title="生成结果"
            type="success"
            :closable="false"
            show-icon
          >
            {{ seoResult }}
          </el-alert>
        </el-card>
      </el-col>

      <!-- 商品描述生成 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>商品描述生成</span>
          </template>
          <el-form label-width="100px">
            <el-form-item label="商品名称">
              <el-input v-model="descForm.productName" placeholder="例如：iPhone 15 Pro" />
            </el-form-item>
            <el-form-item label="目标人群">
              <el-input v-model="descForm.targetAudience" placeholder="例如：商务人士、摄影爱好者" />
            </el-form-item>
            <el-form-item label="规格参数">
              <el-input
                v-model="descForm.specs"
                type="textarea"
                :rows="4"
                placeholder="每行一个参数，例如：&#10;屏幕: 6.1英寸 OLED&#10;处理器: A17 Pro&#10;摄像头: 4800万主摄"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                @click="generateDescription"
                :loading="descLoading"
              >
                生成商品描述
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="descResult"
            title="生成结果"
            type="success"
            :closable="false"
            show-icon
          >
            <div style="white-space: pre-wrap;">{{ descResult }}</div>
          </el-alert>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { post } from '../utils/request'

const seoLoading = ref(false)
const seoResult = ref('')
const seoForm = ref({
  productName: '',
  category: '',
  features: ''
})

const descLoading = ref(false)
const descResult = ref('')
const descForm = ref({
  productName: '',
  targetAudience: '',
  specs: ''
})

const generateSeoTitle = async () => {
  if (!seoForm.value.productName) {
    ElMessage.warning('请输入商品名称')
    return
  }

  seoLoading.value = true
  try {
    const data = await post<any>('/api/ai/product/seo-title', seoForm.value)
    if (data.success) {
      seoResult.value = data.seoTitle
      ElMessage.success('SEO 标题生成成功')
    }
  } catch (error) {
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    seoLoading.value = false
  }
}

const generateDescription = async () => {
  if (!descForm.value.productName) {
    ElMessage.warning('请输入商品名称')
    return
  }

  descLoading.value = true
  try {
    // 将规格参数转换为 JSON 对象
    const specsLines = descForm.value.specs.split('\n').filter(line => line.trim())
    const specs: Record<string, string> = {}
    specsLines.forEach(line => {
      const [key, value] = line.split(':').map(s => s.trim())
      if (key && value) specs[key] = value
    })

    const data = await post<any>('/api/ai/product/description', {
      productName: descForm.value.productName,
      targetAudience: descForm.value.targetAudience,
      specs
    })
    if (data.success) {
      descResult.value = data.description
      ElMessage.success('商品描述生成成功')
    }
  } catch (error) {
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    descLoading.value = false
  }
}
</script>

<style scoped>
.product-ai {
  max-width: 1400px;
  margin: 0 auto;
}
</style>
