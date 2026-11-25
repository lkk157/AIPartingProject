<template>
  <div class="login-page">
    <div class="login-card">
      <div class="title">智泊停车 · 管理后台</div>
      <div class="subtitle">智能停车场管理系统</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" native-type="submit">登录</el-button>
      </el-form>

      <div class="hint">开发环境默认账号：admin / admin123</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { extractErrorMessage } from '@/api/request'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login(form.username, form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } catch (error) {
    ElMessage.error(extractErrorMessage(error))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 20% 20%, #dbeafe 0, #f4f6fa 45%, #eef2ff 100%);
}
.login-card {
  width: 380px;
  background: #fff;
  border-radius: 18px;
  padding: 36px 32px 26px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}
.title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}
.subtitle {
  margin-top: 6px;
  margin-bottom: 22px;
  color: #64748b;
  font-size: 13px;
}
.submit {
  width: 100%;
  margin-top: 6px;
}
.hint {
  margin-top: 18px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
}
</style>
