<template>
  <ElTabs v-model="tab">
    <ElTabPane label="菜单" name="menus"
      ><ManagementTable v-if="tab === 'menus'" resource="menus"
    /></ElTabPane>
    <ElTabPane v-if="hasAuth('system:button:read')" label="按钮权限" name="buttons"
      ><ManagementTable v-if="tab === 'buttons'" resource="buttons"
    /></ElTabPane>
  </ElTabs>
</template>
<script setup lang="ts">
  import { ref } from 'vue'
  import { useAuth } from '@/hooks/core/useAuth'
  import ManagementTable from '@/components/business/management/ManagementTable.vue'
  defineOptions({ name: 'Menus' })
  const tab = ref('menus')
  const { hasAuth } = useAuth()
</script>
