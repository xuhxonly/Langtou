<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

// 系统设置
const settings = reactive({
  siteName: '榔头 Langtou',
  siteDescription: '分享社区管理后台',
  enableRegister: true,
  enableAudit: true,
  maxUploadSize: 10,
  keywords: ['榔头', '笔记', '分享']
})

const handleSave = () => {
  ElMessage.success('设置已保存')
}
</script>

<template>
  <div class="settings">
    <el-card shadow="never" class="settings-card">
      <template #header>
        <span class="card-title">基础设置</span>
      </template>
      <el-form :model="settings" label-width="140px" style="max-width: 600px">
        <el-form-item label="站点名称">
          <el-input v-model="settings.siteName" />
        </el-form-item>
        <el-form-item label="站点描述">
          <el-input v-model="settings.siteDescription" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="允许注册">
          <el-switch v-model="settings.enableRegister" />
        </el-form-item>
        <el-form-item label="开启审核">
          <el-switch v-model="settings.enableAudit" />
        </el-form-item>
        <el-form-item label="最大上传 (MB)">
          <el-input-number v-model="settings.maxUploadSize" :min="1" :max="100" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存设置</el-button>
          <el-button>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.settings {
  .settings-card {
    background: var(--lt-card-bg);
    border: 1px solid var(--lt-border-color);
  }

  .card-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--lt-text-primary);
  }
}
</style>
