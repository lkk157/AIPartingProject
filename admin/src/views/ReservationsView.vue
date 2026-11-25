<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="预约状态" clearable style="width: 140px" @change="reload">
        <el-option label="待使用" :value="0" />
        <el-option label="已完成" :value="1" />
        <el-option label="已取消" :value="2" />
        <el-option label="已过期" :value="3" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <div class="spacer" />
      <span class="count">共 {{ total }} 条预约</span>
    </div>

    <el-table v-loading="loading" :data="rows" class="table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户" width="130" />
      <el-table-column prop="lotName" label="停车场" min-width="160" />
      <el-table-column prop="spaceNo" label="车位" width="110" />
      <el-table-column prop="startTime" label="开始时间" width="180" />
      <el-table-column prop="endTime" label="结束时间" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="reservationStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      :page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      class="pager"
      @current-change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listReservations } from '@/api/admin'
import type { ReservationItem } from '@/api/types'
import { reservationStatusType } from '@/utils/status'

const loading = ref(false)
const rows = ref<ReservationItem[]>([])
const total = ref(0)
const query = reactive<{ page: number; size: number; status?: number }>({
  page: 1,
  size: 10,
  status: undefined,
})

async function load() {
  loading.value = true
  try {
    const result = await listReservations(query)
    rows.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.page = 1
  void load()
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
</style>
