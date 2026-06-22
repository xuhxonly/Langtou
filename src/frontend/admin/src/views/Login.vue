<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 登录表单数据
const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)

// 提交登录
const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (err: any) {
    ElMessage.error(err?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="brand-logo">L</div>
        <h1>榔头管理后台</h1>
        <p>请登录以继续</p>
      </div>
      <el-form
        :model="form"
        @keyup.enter="handleLogin"
        class="login-form"
      >
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-tip">提示：默认账号 admin / admin123</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--lt-bg-primary), var(--lt-brand-bg));
}

.login-card {
  background: var(--lt-card-bg);
  border: 1px solid var(--lt-border-color);
  border-radius: 12px;
  padding: 40px;
  width: 380px;
  max-width: 90vw;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.brand-logo {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--lt-brand-color), var(--lt-brand-secondary));
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 12px;
}

h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--lt-text-primary);
  margin: 0 0 4px;
}

.login-header p {
  font-size: 13px;
  color: var(--lt-text-muted);
  margin: 0;
}

.login-form {
  :deep(.el-input__wrapper) {
    background-color: var(--lt-input-bg);
    border-color: var(--lt-border-color);
  }
}

.login-btn {
  width: 100%;
}

.login-tip {
  text-align: center;
  font-size: 12px;
  color: var(--lt-text-muted);
  margin: 16px 0 0;
}
</style>
