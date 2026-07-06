<template>
  <div class="product-form-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑商品' : '新增商品' }}</span>
          <el-button @click="handleBack">返回列表</el-button>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <!-- 基本信息 -->
        <el-divider content-position="left">基本信息</el-divider>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-input-number v-model="form.categoryId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="售价" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 图片上传 -->
        <el-divider content-position="left">商品图片</el-divider>
        
        <el-form-item label="商品主图" prop="image">
          <ImageUpload 
            v-model="form.image"
            :limit="1"
            upload-text="上传主图"
            tip="建议尺寸 800x800px，支持 JPG、PNG 格式，最大 5MB"
          />
        </el-form-item>

        <el-form-item label="详情图片" prop="images">
          <ImageUpload
            v-model="form.images"
            :limit="9"
            :multiple="true"
            upload-text="添加图片"
            tip="最多上传 9 张，建议尺寸 800x800px"
          />
        </el-form-item>

        <!-- 商品描述 -->
        <el-divider content-position="left">商品描述</el-divider>
        
        <el-form-item label="商品描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入商品描述"
          />
        </el-form-item>

        <el-form-item label="规格参数" prop="specs">
          <el-input 
            v-model="form.specs" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入规格参数（JSON格式）"
          />
        </el-form-item>

        <!-- SEO 信息 -->
        <el-divider content-position="left">SEO 优化</el-divider>
        
        <el-form-item label="SEO标题">
          <el-input v-model="form.seoTitle" placeholder="AI 将自动生成 SEO 标题" />
        </el-form-item>

        <el-form-item label="营销文案">
          <el-input 
            v-model="form.aiDescription" 
            type="textarea" 
            :rows="4" 
            placeholder="AI 将自动生成营销文案"
          />
          <el-button type="primary" @click="handleGenerateAI" :loading="aiGenerating">
            AI 一键生成
          </el-button>
        </el-form-item>

        <!-- 提交按钮 -->
        <el-divider />
        
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建商品' }}
          </el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import ImageUpload from '../components/ImageUpload.vue'
import { get, post, put } from '../utils/request'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const aiGenerating = ref(false)

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  id: null as number | null,
  name: '',
  categoryId: 1,
  price: 0,
  originalPrice: 0,
  stock: 0,
  image: '',
  images: [] as string[],
  description: '',
  specs: '',
  seoTitle: '',
  aiDescription: '',
  publishStatus: 0
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入售价', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' }
  ],
  image: [
    { required: true, message: '请上传商品主图', trigger: 'change' }
  ]
}

// 加载商品详情
const loadProduct = async () => {
  if (!isEdit.value) return

  try {
    const data = await get<any>(`/api/admin/product/${route.params.id}`)
    if (data.success && data.data) {
      const product = data.data
      
      // 解析多图 JSON
      let imagesArray: string[] = []
      if (product.images) {
        try {
          imagesArray = JSON.parse(product.images)
        } catch (e) {
          console.warn('解析多图失败', e)
        }
      }

      Object.assign(form, {
        id: product.id,
        name: product.name,
        categoryId: product.categoryId,
        price: product.price,
        originalPrice: product.originalPrice,
        stock: product.stock,
        image: product.image,
        images: imagesArray,
        description: product.description,
        specs: product.specs,
        seoTitle: product.seoTitle,
        aiDescription: product.aiDescription,
        publishStatus: product.publishStatus
      })
    }
  } catch (error) {
    console.error('加载商品详情失败', error)
    ElMessage.error('加载失败')
  }
}

// AI 生成文案
const handleGenerateAI = async () => {
  if (!form.name) {
    ElMessage.warning('请先输入商品名称')
    return
  }

  aiGenerating.value = true
  try {
    // TODO: 调用 AI 生成接口
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    form.seoTitle = `${form.name} - 高品质精选好物`
    form.aiDescription = `✨ 精选好物推荐！${form.name}，品质保证，性价比超高。立即选购，享受优惠！`
    
    ElMessage.success('AI 生成成功')
  } catch (error) {
    ElMessage.error('AI 生成失败')
  } finally {
    aiGenerating.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const submitData = {
        ...form,
        images: JSON.stringify(form.images) // 多图转 JSON 字符串
      }

      let data
      if (isEdit.value) {
        data = await put<any>(`/api/admin/product/${form.id}`, submitData)
      } else {
        data = await post<any>('/api/admin/product', submitData)
      }

      if (data.success) {
        ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
        router.push('/products')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
    } catch (error) {
      console.error('提交失败', error)
      ElMessage.error('操作失败')
    } finally {
      submitting.value = false
    }
  })
}

// 返回列表
const handleBack = () => {
  router.push('/products')
}

onMounted(() => {
  loadProduct()
})
</script>

<style scoped>
.product-form-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-divider {
  margin: 20px 0;
}
</style>
