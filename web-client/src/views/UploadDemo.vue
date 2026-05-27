<template>
  <div class="upload-demo">
    <h2>📸 图片上传示例</h2>

    <el-row :gutter="20">
      <!-- 基础用法 -->
      <el-col :span="12">
        <el-card header="基础用法 - 点击或拖拽上传">
          <ImageUpload v-model="imageUrl1" @success="handleSuccess" />
          <div class="result-info" v-if="imageUrl1">
            <p><strong>上传结果：</strong></p>
            <el-input v-model="imageUrl1" readonly />
            <el-image :src="imageUrl1" fit="cover" style="width: 100%; margin-top: 10px; border-radius: 4px;" />
          </div>
        </el-card>
      </el-col>

      <!-- 自定义配置 -->
      <el-col :span="12">
        <el-card header="自定义配置 - 压缩 + 自定义大小">
          <ImageUpload
            v-model="imageUrl2"
            placeholder="点击上传头像"
            tip="建议尺寸 200x200，不超过 5MB"
            :compress="true"
            :max-size="5"
            endpoint="/api/upload/avatar"
          />
          <div class="result-info" v-if="imageUrl2">
            <p><strong>上传结果：</strong></p>
            <el-input v-model="imageUrl2" readonly />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card header="使用说明" style="margin-top: 20px;">
      <h3>🎯 功能特性</h3>
      <ul>
        <li>✅ 支持点击选择和拖拽上传</li>
        <li>✅ 自动图片压缩（可选）</li>
        <li>✅ 实时上传进度显示</li>
        <li>✅ 文件大小和类型验证</li>
        <li>✅ 上传成功/失败提示</li>
        <li>✅ 预览、替换、删除功能</li>
      </ul>

      <h3>📦 使用方法</h3>
      <pre class="code-block"><code>&lt;!-- 1. 导入组件 --&gt;
import ImageUpload from '../components/ImageUpload.vue'

&lt;!-- 2. 基础用法 --&gt;
&lt;ImageUpload v-model="imageUrl" /&gt;

&lt;!-- 3. 自定义配置 --&gt;
&lt;ImageUpload
  v-model="imageUrl"
  placeholder="点击上传"
  tip="支持 JPG、PNG，不超过 5MB"
  :compress="true"
  :max-size="5"
  endpoint="/api/upload/avatar"
/&gt;

&lt;!-- 4. 监听上传成功 --&gt;
&lt;ImageUpload @success="handleUploadSuccess" /&gt;</code></pre>

      <h3>⚙️ 配置参数</h3>
      <el-table :data="configParams" border>
        <el-table-column prop="name" label="参数名" width="150" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="default" label="默认值" width="150" />
        <el-table-column prop="desc" label="说明" />
      </el-table>

      <h3>🔥 后端接口</h3>
      <p>上传接口地址：<code>POST /api/upload/product</code></p>
      <p>图片访问地址：<code>http://localhost:8081/uploads/products/{filename}</code></p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ImageUpload from '../components/ImageUpload.vue'

const imageUrl1 = ref('')
const imageUrl2 = ref('')

const handleSuccess = (data: any) => {
  console.log('上传成功', data)
}

const configParams = [
  { name: 'v-model', type: 'string', default: "''", desc: '图片 URL（支持 v-model 双向绑定）' },
  { name: 'placeholder', type: 'string', default: '点击或拖拽上传图片', desc: '上传框提示文字' },
  { name: 'tip', type: 'string', default: '支持 JPG、PNG...', desc: '上传框提示说明' },
  { name: 'compress', type: 'boolean', default: 'true', desc: '是否自动压缩图片' },
  { name: 'max-size', type: 'number', default: '10', desc: '最大文件大小（MB）' },
  { name: 'endpoint', type: 'string', default: '/api/upload/product', desc: '上传接口地址' },
  { name: '@success', type: 'function', default: '-', desc: '上传成功回调，返回上传数据' }
]
</script>

<style scoped>
.upload-demo {
  max-width: 1200px;
  margin: 0 auto;
}

.result-info {
  margin-top: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.result-info p {
  margin: 0 0 10px 0;
  font-size: 14px;
}

.code-block {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 20px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
}

h3 {
  margin: 20px 0 10px 0;
  color: #303133;
}

ul {
  padding-left: 20px;
  color: #606266;
}

ul li {
  margin: 5px 0;
}
</style>
