<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="用户名 / 昵称 / 手机号" clearable style="width: 240px" @keyup.enter="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
      <div class="spacer" />
      <span class="count">共 {{ total }} 位用户</span>
    </div>

    <el-table v-loading="loading" :data="rows" class="table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column label="角色" min-width="150">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles" :key="role" size="small" class="role">{{ roleLabel(role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            :disabled="row.username === 'admin'"
            @change="(value: string | number | boolean) => toggle(row, Boolean(value))"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsers, updateUserStatus } from '@/api/admin'
import type { UserInfo } from '@/api/types'

const loading = ref(false)
const rows = ref<UserInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')

const roleLabels: Record<string, string> = {
  USER: '车主',
  PARKING_ADMIN: '停车场管理员',
  SYS_ADMIN: '系统管理员',
}

function roleLabel(role: string): string {
  return roleLabels[role] ?? role
}

async function load() {
  loading.value = true
  try {
    const result = await listUsers({ page: page.value, size, keyword: keyword.value })
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  void load()
}

async function toggle(row: UserInfo, enabled: boolean) {
  const nextStatus = enabled ? 1 : 0
  try {
    await updateUserStatus(row.id, nextStatus)
    row.status = nextStatus
    ElMessage.success(enabled ? '已启用' : '已禁用')
  } catch {
    // 失败时保持原状态（拦截器已提示）
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  padding: 14px 16px;
  border-radius: 12px;
}
.spacer {
  flex: 1;
}
.count {
  color: #64748b;
  font-size: 13px;
}
.table {
  background: #fff;
  border-radius: 12px;
}
.pager {
  justify-content: flex-end;
}
.role {
  margin-right: 6px;
}
</style>
