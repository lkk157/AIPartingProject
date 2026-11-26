<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <span class="brand-mark">P</span>
        <span class="brand-text">智泊停车</span>
      </div>
      <el-menu :default-active="activePath" router class="menu">
        <el-menu-item index="/">运营看板</el-menu-item>
        <el-menu-item index="/lots">停车场管理</el-menu-item>
        <el-menu-item index="/reservations">预约管理</el-menu-item>
        <el-menu-item index="/orders">订单管理</el-menu-item>
        <el-menu-item index="/predict">AI 预测</el-menu-item>
        <el-menu-item index="/knowledge">AI 知识库</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ pageTitle }}</div>
        <el-dropdown @command="onCommand">
          <span class="user">
            {{ auth.displayName }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activePath = computed(() => (route.path.startsWith('/lots') ? '/lots' : route.path))
const pageTitle = computed(() => (route.meta.title as string | undefined) ?? '智泊停车')

function onCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    void router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.96) 0%, rgba(30, 41, 59, 0.92) 100%);
  backdrop-filter: blur(14px);
  color: #e2e8f0;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 18px;
  font-weight: 700;
}
.brand-mark {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(160deg, #3b7bf7, #1d4ed8);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.brand-text {
  font-size: 16px;
}
.menu {
  border-right: none;
  background: transparent;
  --el-menu-text-color: #cbd5e1;
  --el-menu-hover-bg-color: rgba(59, 130, 246, 0.16);
  --el-menu-active-color: #93c5fd;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.66);
  backdrop-filter: blur(16px) saturate(150%);
  -webkit-backdrop-filter: blur(16px) saturate(150%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.7);
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user {
  cursor: pointer;
  color: #334155;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  margin-left: 12px;
}

@media (max-width: 960px) {
  .aside {
    width: 168px !important;
  }
  .brand-text {
    font-size: 14px;
  }
}
.main {
  background: transparent;
}
</style>
