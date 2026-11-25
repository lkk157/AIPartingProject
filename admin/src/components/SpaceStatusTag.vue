<template>
  <el-tag :type="spaceStatusType(space.status)" effect="light" size="small">
    {{ spaceStatusMeta(space.status).label }}
  </el-tag>
</template>

<script setup lang="ts">
import { spaceStatusMeta, spaceStatusType } from '@/utils/status'

defineProps<{ space: { status: number } }>()
</script>
