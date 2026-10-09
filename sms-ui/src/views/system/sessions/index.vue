<template>
  <ManagementTable resource="sessions" :filters="{ all }">
    <template #toolbar>
      <ElSwitch
        v-if="hasAuth('system:session:read')"
        v-model="all"
        active-text="当前租户全部设备"
        inactive-text="我的设备"
      />
    </template>
  </ManagementTable>
</template>
<script setup lang="ts">
  import { ref } from 'vue'
  import { useAuth } from '@/hooks/core/useAuth'
  import ManagementTable from '@/components/business/management/ManagementTable.vue'
  defineOptions({ name: 'DeviceSessions' })
  const all = ref(false)
  const { hasAuth } = useAuth()
</script>
