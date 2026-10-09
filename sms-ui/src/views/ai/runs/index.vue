<template>
  <div>
    <ManagementTable domain="ai" resource="runs">
      <template #actions="{ row }">
        <ElButton v-if="hasAuth('ai:run:read')" link type="primary" @click="selected = row"
          >步骤详情</ElButton
        >
        <ElButton
          v-if="hasAuth('ai:log:read')"
          link
          type="primary"
          @click="requestId = row.requestId"
          >工具日志</ElButton
        >
      </template>
    </ManagementTable>
    <ElDrawer
      :model-value="!!selected"
      :title="'任务步骤 · ' + selected?.runId"
      size="90%"
      @close="selected = undefined"
    >
      <ManagementTable
        v-if="selected"
        domain="ai"
        resource="steps"
        :filters="{ runId: selected.runId }"
      >
        <template #actions="{ row }"
          ><ElButton link type="primary" @click="openStep(row)">输入/输出</ElButton></template
        >
      </ManagementTable>
    </ElDrawer>
    <ElDrawer
      :model-value="!!requestId"
      title="工具执行日志"
      size="90%"
      @close="requestId = undefined"
    >
      <ManagementTable v-if="requestId" domain="ai" resource="logs" :filters="{ requestId }" />
    </ElDrawer>
    <ElDialog v-model="detailVisible" title="步骤输入/输出" width="80%">
      <pre class="whitespace-pre-wrap">{{ JSON.stringify(detail, null, 2) }}</pre>
    </ElDialog>
  </div>
</template>
<script setup lang="ts">
  import ManagementTable from '@/components/business/management/ManagementTable.vue'
  import type { ManagementRow } from '@/api/management'
  import { useAuth } from '@/hooks/core/useAuth'
  defineOptions({ name: 'AiRuns' })
  const { hasAuth } = useAuth()
  const selected = ref<ManagementRow>(),
    detail = ref<ManagementRow>(),
    requestId = ref<string>()
  const detailVisible = ref(false)
  function openStep(row: ManagementRow) {
    detail.value = row
    detailVisible.value = true
  }
</script>
