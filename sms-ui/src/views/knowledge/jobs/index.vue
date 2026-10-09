<template>
  <ManagementTable ref="table" domain="knowledge" resource="jobs">
    <template #toolbar><ElSwitch v-model="autoRefresh" active-text="每 5 秒刷新" /></template>
    <template #actions="{ row, reload }">
      <ElTag v-if="row.cancelRequested && row.status === 'RUNNING'" type="warning">取消中</ElTag>
      <ElButton
        v-if="hasAuth('knowledge:ingestion:retry') && ['FAILED', 'CANCELLED'].includes(row.status)"
        link
        type="primary"
        :disabled="busy === row.id"
        @click="action(row, 'retry', reload)"
        >重试</ElButton
      >
      <ElButton
        v-if="hasAuth('knowledge:ingestion:cancel') && ['PENDING', 'RUNNING'].includes(row.status)"
        link
        type="danger"
        :disabled="busy === row.id || !!row.cancelRequested"
        @click="action(row, 'cancel', reload)"
        >取消</ElButton
      >
      <ElButton
        v-if="hasAuth('knowledge:index:activate') && row.status === 'SUCCEEDED'"
        link
        type="primary"
        :disabled="busy === row.id"
        @click="activate(row, reload)"
        >启用此索引</ElButton
      >
    </template>
  </ManagementTable>
</template>
<script setup lang="ts">
  import { ElMessage, ElMessageBox } from 'element-plus'
  import ManagementTable from '@/components/business/management/ManagementTable.vue'
  import { jobAction, activateIndex } from '@/api/knowledge-management'
  import type { ManagementRow } from '@/api/management'
  import { useAuth } from '@/hooks/core/useAuth'
  defineOptions({ name: 'KnowledgeJobs' })
  const { hasAuth } = useAuth()
  const table = ref<InstanceType<typeof ManagementTable>>(),
    busy = ref<string>()
  const autoRefresh = ref(true)
  let timer: ReturnType<typeof setInterval>
  async function action(
    row: ManagementRow,
    operation: 'retry' | 'cancel',
    reload: () => Promise<void>
  ) {
    busy.value = row.id
    try {
      await jobAction(row.id, operation)
      await reload()
    } finally {
      busy.value = undefined
    }
  }
  async function activate(row: ManagementRow, reload: () => Promise<void>) {
    try {
      await ElMessageBox.confirm(
        '将此文档版本及索引设为生效版本，替换当前检索版本。是否继续？',
        '切换索引'
      )
    } catch {
      return
    }
    busy.value = row.id
    try {
      await activateIndex(row.documentVersionId, Number(row.indexRevision))
      ElMessage.success('索引已切换')
      await reload()
    } finally {
      busy.value = undefined
    }
  }
  onMounted(() => {
    timer = setInterval(() => {
      if (autoRefresh.value && !busy.value)
        table.value?.reload().catch(() => {
          autoRefresh.value = false
        })
    }, 5000)
  })
  onUnmounted(() => clearInterval(timer))
</script>
