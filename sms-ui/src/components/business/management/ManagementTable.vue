<template>
  <ElCard class="management-card">
    <template #header>
      <div class="toolbar">
        <strong>{{ schema.title }}</strong>
        <ElInput
          v-model="search"
          clearable
          placeholder="搜索"
          style="width: 240px"
          @keyup.enter="reload(true)"
        />
        <ElButton @click="reload(true)">查询</ElButton>
        <ElButton @click="reload()">刷新</ElButton>
        <ElButton
          v-if="!schema.readonly && resource !== 'documents' && can('create')"
          type="primary"
          @click="edit()"
          >新增</ElButton
        >
        <slot name="toolbar" :reload="reload" />
      </div>
    </template>
    <ElTable v-loading="loading" :data="rows" row-key="id" border>
      <ElTableColumn
        v-for="column in schema.columns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        min-width="130"
        show-overflow-tooltip
      />
      <ElTableColumn label="操作" min-width="240" fixed="right">
        <template #default="{ row }">
          <ElButton v-if="!schema.readonly && can('update')" link type="primary" @click="edit(row)"
            >编辑</ElButton
          >
          <ElButton v-if="!schema.readonly && can('delete')" link type="danger" @click="remove(row)"
            >删除</ElButton
          >
          <ElButton
            v-if="resource === 'users' && can('grant')"
            link
            type="primary"
            @click="openAssignments(row)"
            >分配角色</ElButton
          >
          <ElButton
            v-if="resource === 'roles' && can('grant') && row.roleCode !== 'R_SUPER'"
            link
            type="primary"
            @click="openAssignments(row)"
            >菜单/按钮授权</ElButton
          >
          <ElTag v-if="resource === 'sessions' && row.currentSession" size="small">当前设备</ElTag>
          <ElButton
            v-if="
              resource === 'sessions' &&
              row.status === 'ACTIVE' &&
              (row.currentSession || ownSessions || can('revoke'))
            "
            link
            type="danger"
            @click="revoke(row)"
            >退出该设备</ElButton
          >
          <slot name="actions" :row="row" :reload="reload" />
        </template>
      </ElTableColumn>
    </ElTable>
    <ElPagination
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @current-change="reload()"
      @size-change="reload(true)"
    />
    <ElDialog
      v-model="dialog"
      :title="`${editingId ? '编辑' : '新增'}${schema.title}`"
      width="640px"
      destroy-on-close
    >
      <ElForm ref="formRef" :model="form" label-width="170px">
        <ElFormItem
          v-for="field in schema.fields"
          :key="field.key"
          :label="field.label"
          :prop="field.key"
          :rules="
            field.required ? [{ required: true, message: '请填写此项', trigger: 'blur' }] : []
          "
        >
          <ElSwitch v-if="field.type === 'boolean'" v-model="form[field.key]" />
          <ElInputNumber v-else-if="field.type === 'number'" v-model="form[field.key]" :min="0" />
          <ElSelect
            v-else-if="field.type === 'select'"
            v-model="form[field.key]"
            clearable
            filterable
            style="width: 100%"
          >
            <ElOption
              v-for="option in options[field.key] ?? field.options ?? []"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </ElSelect>
          <ElInput
            v-else
            v-model="form[field.key]"
            :type="
              field.type === 'password'
                ? 'password'
                : field.type === 'textarea'
                  ? 'textarea'
                  : 'text'
            "
            :show-password="field.type === 'password'"
          />
        </ElFormItem>
      </ElForm>
      <template #footer
        ><ElButton @click="dialog = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="save">保存</ElButton></template
      >
    </ElDialog>
    <ElDialog
      v-model="assignmentDialog"
      :title="resource === 'users' ? '分配角色' : '菜单和按钮授权'"
      width="760px"
    >
      <template v-if="resource === 'users'">
        <ElCheckboxGroup v-model="roleIds"
          ><ElCheckbox v-for="role in availableRoles" :key="role.id" :value="role.id"
            >{{ role.roleName }} ({{ role.roleCode }})</ElCheckbox
          ></ElCheckboxGroup
        >
      </template>
      <template v-else>
        <p>选择按钮时会同时选择对应菜单及其父级。</p>
        <ElCheckboxGroup v-model="menuIds"
          ><ElCheckbox v-for="menu in availableMenus" :key="menu.id" :value="menu.id">{{
            menu.title
          }}</ElCheckbox></ElCheckboxGroup
        >
        <ElDivider>按钮权限</ElDivider>
        <ElCheckboxGroup v-model="buttonIds"
          ><ElCheckbox v-for="button in availableButtons" :key="button.id" :value="button.id"
            >{{ menuLabel(button.menuId) }} / {{ button.buttonName }} ({{
              button.authRemark
            }})</ElCheckbox
          ></ElCheckboxGroup
        >
      </template>
      <template #footer
        ><ElButton @click="assignmentDialog = false">取消</ElButton
        ><ElButton type="primary" :loading="saving" @click="saveAssignments"
          >保存授权</ElButton
        ></template
      >
    </ElDialog>
  </ElCard>
</template>

<script setup lang="ts">
  import { computed, ref, reactive, watch, onMounted } from 'vue'
  import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
  import { useAuth } from '@/hooks/core/useAuth'
  import { useUserStore } from '@/store/modules/user'
  import { fetchGetUserInfo } from '@/api/auth'
  import {
    listManagement,
    saveManagement,
    deleteManagement,
    allManagement,
    getAssignments,
    assignRoles,
    grantPermissions,
    revokeSession,
    type ManagementDomain,
    type ManagementRow
  } from '@/api/management'
  import { managementSchemas } from './schemas'

  const props = withDefaults(
    defineProps<{
      domain?: ManagementDomain
      resource: string
      filters?: Record<string, unknown>
    }>(),
    { domain: 'system', filters: () => ({}) }
  )
  const schema = computed(() => managementSchemas[props.resource])
  const { hasAuth } = useAuth()
  const user = useUserStore()
  const can = (action: string) => hasAuth(`${schema.value.permission}:${action}`)
  const ownSessions = computed(() => props.filters.all !== true)
  const rows = ref<ManagementRow[]>([])
  const current = ref(1),
    size = ref(20),
    total = ref(0),
    search = ref('')
  const loading = ref(false),
    saving = ref(false),
    dialog = ref(false)
  const form = reactive<Record<string, any>>({})
  const formRef = ref<FormInstance>()
  const editingId = ref<string>()
  const options = reactive<Record<string, { label: string; value: string }[]>>({})
  let generation = 0
  async function reload(reset = false) {
    if (reset) current.value = 1
    const active = ++generation
    loading.value = true
    try {
      const page = await listManagement(props.domain, props.resource, {
        ...props.filters,
        search: search.value,
        current: current.value,
        size: size.value
      })
      if (active !== generation) return
      rows.value = page.records
      total.value = page.total
    } finally {
      if (active === generation) loading.value = false
    }
  }
  async function edit(row?: ManagementRow) {
    editingId.value = row?.id
    Object.keys(form).forEach((key) => delete form[key])
    for (const field of schema.value.fields) {
      form[field.key] =
        field.type === 'password'
          ? ''
          : (row?.[field.key] ??
            field.default ??
            (field.type === 'number' ? 0 : field.type === 'boolean' ? false : ''))
      if (field.type === 'boolean') form[field.key] = Boolean(form[field.key])
      if (field.related) {
        const related = await allManagement(props.domain, field.related)
        options[field.key] = related
          .filter((item) => item.id !== row?.id || field.key !== 'parentId')
          .map((item) => ({ label: item.title ?? item.name, value: item.id }))
      }
    }
    dialog.value = true
  }
  async function save() {
    if (!(await formRef.value?.validate())) return
    saving.value = true
    try {
      const payload = Object.fromEntries(
        schema.value.fields.map((field) => [
          field.key,
          form[field.key] === '' && field.key.endsWith('Id') ? null : form[field.key]
        ])
      )
      await saveManagement(props.domain, props.resource, editingId.value, payload)
      dialog.value = false
      ElMessage.success('保存成功')
      await reload()
      if (props.domain === 'system') user.setUserInfo(await fetchGetUserInfo())
    } finally {
      saving.value = false
    }
  }
  async function remove(row: ManagementRow) {
    try {
      await ElMessageBox.confirm('确定删除这条记录吗？', '删除确认', { type: 'warning' })
    } catch {
      return
    }
    await deleteManagement(props.domain, props.resource, row.id)
    ElMessage.success('删除成功')
    await reload()
  }
  async function revoke(row: ManagementRow) {
    try {
      await ElMessageBox.confirm('退出该设备后，需要重新登录。是否继续？', '退出设备', {
        type: 'warning'
      })
    } catch {
      return
    }
    await revokeSession(row.id)
    if (row.currentSession) user.logOut()
    else {
      ElMessage.success('该设备已退出')
      await reload()
    }
  }
  const assignmentDialog = ref(false),
    assignmentId = ref('')
  const roleIds = ref<string[]>([]),
    menuIds = ref<string[]>([]),
    buttonIds = ref<string[]>([])
  const availableRoles = ref<ManagementRow[]>([]),
    availableMenus = ref<ManagementRow[]>([]),
    availableButtons = ref<ManagementRow[]>([])
  const menuLabel = (id: string) => availableMenus.value.find((menu) => menu.id === id)?.title ?? id
  async function openAssignments(row: ManagementRow) {
    const assignments = await getAssignments(props.resource, row.id)
    if (props.resource === 'users') availableRoles.value = await allManagement('system', 'roles')
    else
      [availableMenus.value, availableButtons.value] = await Promise.all([
        allManagement('system', 'menus'),
        allManagement('system', 'buttons')
      ])
    assignmentId.value = row.id
    roleIds.value = assignments.roleIds ?? []
    menuIds.value = assignments.menuIds ?? []
    buttonIds.value = assignments.buttonIds ?? []
    assignmentDialog.value = true
  }
  async function saveAssignments() {
    saving.value = true
    try {
      if (props.resource === 'users') await assignRoles(assignmentId.value, roleIds.value)
      else {
        const selected = new Set(menuIds.value)
        availableButtons.value
          .filter((button) => buttonIds.value.includes(button.id))
          .forEach((button) => selected.add(button.menuId))
        for (const id of selected) {
          let parent = availableMenus.value.find((menu) => menu.id === id)?.parentId
          const visited = new Set<string>()
          while (parent && !visited.has(parent)) {
            visited.add(parent)
            selected.add(parent)
            parent = availableMenus.value.find((menu) => menu.id === parent)?.parentId
          }
        }
        await grantPermissions(assignmentId.value, [...selected], buttonIds.value)
      }
      ElMessage.success('授权已保存，下次请求立即生效')
      assignmentDialog.value = false
      await reload()
      user.setUserInfo(await fetchGetUserInfo())
    } finally {
      saving.value = false
    }
  }
  watch(
    () => [props.resource, props.filters],
    () => reload(true),
    { deep: true }
  )
  onMounted(() => reload())
  defineExpose({ reload })
</script>

<style scoped>
  .management-card {
    width: 100%;
  }
  .toolbar {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
  }
  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
</style>
