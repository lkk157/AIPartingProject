<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="订单号 / 用户名" clearable style="width: 220px" @keyup.enter="reload" />
      <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 140px" @change="reload">
        <el-option label="待支付" :value="0" />
        <el-option label="已支付" :value="1" />
        <el-option label="已取消" :value="2" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <div class="spacer" />
      <span class="count">共 {{ total }} 笔订单</span>
    </div>

    <el-table v-loading="loading" :data="rows" class="table">
      <el-table-column prop="orderNo" label="订单号" min-width="200" />
      <el-table-column prop="username" label="用户" width="130" />
      <el-table-column prop="lotName" label="停车场" min-width="150" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">
          <span class="amount">{{ formatMoney(row.amountCents) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="计费时长" width="110">
        <template #default="{ row }">{{ formatDuration(row.billedMinutes) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="orderStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="paidAt" label="支付时间" width="180" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
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
import { listOrders } from '@/api/admin'
import type { OrderItem } from '@/api/types'
import { formatDuration, formatMoney } from '@/utils/format'
import { orderStatusType } from '@/utils/status'

const loading = ref(false)
const rows = ref<OrderItem[]>([])
const total = ref(0)
const query = reactive<{ page: number; size: number; keyword: string; status?: number }>({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined,
})

async function load() {
  loading.value = true
  try {
    const result = await listOrders(query)
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
.amount {
  color: #ff7a00;
  font-weight: 600;
}
</style>
