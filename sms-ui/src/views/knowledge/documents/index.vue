<template>
  <div>
    <div class="mb-4 flex gap-3">
      <ElSelect
        v-model="baseId"
        filterable
        clearable
        placeholder="按知识库筛选"
        style="width: 300px"
      >
        <ElOption v-for="base in bases" :key="base.id" :value="base.id" :label="base.name" />
      </ElSelect>
      <ElButton @click="loadBases">刷新知识库</ElButton>
    </div>
    <ManagementTable
      ref="documentsTable"
      domain="knowledge"
      resource="documents"
      :filters="{ knowledgeBaseId: baseId }"
    >
      <template #toolbar
        ><ElButton v-if="hasAuth('knowledge:document:create')" type="primary" @click="openUpload()"
          >上传文档</ElButton
        ></template
      >
      <template #actions="{ row }">
        <ElButton
          v-if="hasAuth('knowledge:document:create')"
          link
          type="primary"
          @click="openUpload(row)"
          >上传新版本</ElButton
        >
        <ElButton
          v-if="hasAuth('knowledge:document:read')"
          link
          type="primary"
          @click="selectedDocument = row"
          >版本与分块</ElButton
        >
      </template>
    </ManagementTable>
    <ElDialog v-model="uploadVisible" title="上传并解析文档" width="560px">
      <ElForm label-width="100px">
        <ElFormItem label="知识库">
          <ElSelect v-model="uploadBaseId" :disabled="!!uploadDocumentId" filterable>
            <ElOption v-for="base in bases" :key="base.id" :value="base.id" :label="base.name" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="文件"
          ><input
            type="file"
            accept=".txt,.md,.csv,.json,.html,.htm,.docx,.pdf"
            @change="selectFile"
        /></ElFormItem>
      </ElForm>
      <p
        >支持 TXT、Markdown、CSV、JSON、HTML、DOCX 和文字型 PDF，最大
        4MB。按知识库当前策略切分并保存版本，随后可提交异步入库。</p
      >
      <template #footer
        ><ElButton @click="uploadVisible = false">取消</ElButton
        ><ElButton
          type="primary"
          :loading="uploading"
          :disabled="!file || !uploadBaseId"
          @click="upload"
          >上传</ElButton
        ></template
      >
    </ElDialog>
    <ElDrawer
      :model-value="!!selectedDocument"
      :title="selectedDocument?.documentName + ' · 版本管理'"
      size="90%"
      @close="selectedDocument = undefined"
    >
      <ManagementTable
        v-if="selectedDocument"
        domain="knowledge"
        resource="versions"
        :filters="{ documentId: selectedDocument.id }"
      >
        <template #actions="{ row, reload }">
          <ElButton
            v-if="hasAuth('knowledge:ingestion:create')"
            link
            type="primary"
            :disabled="busyVersion === row.id"
            @click="enqueue(row, reload)"
            >提交入库</ElButton
          >
          <ElButton link type="primary" @click="selectedVersion = row">查看分块与策略</ElButton>
        </template>
      </ManagementTable>
    </ElDrawer>
    <ElDrawer
      :model-value="!!selectedVersion"
      title="分块预览"
      size="80%"
      @close="selectedVersion = undefined"
    >
      <template v-if="selectedVersion">
        <p
          >版本 {{ selectedVersion.version }} · {{ selectedVersion.splitStrategy }} · 大小
          {{ selectedVersion.chunkSize }} · 重叠 {{ selectedVersion.chunkOverlap }}</p
        >
        <ManagementTable
          domain="knowledge"
          resource="chunks"
          :filters="{ documentVersionId: selectedVersion.id }"
        >
          <template #actions="{ row }"
            ><ElButton link type="primary" @click="openChunk(row)">全文</ElButton></template
          >
        </ManagementTable>
      </template>
    </ElDrawer>
    <ElDialog v-model="chunkVisible" title="分块内容" width="70%">
      <pre class="whitespace-pre-wrap">{{ chunkContent }}</pre>
    </ElDialog>
  </div>
</template>
<script setup lang="ts">
  import { ElMessage } from 'element-plus'
  import ManagementTable from '@/components/business/management/ManagementTable.vue'
  import { allManagement, type ManagementRow } from '@/api/management'
  import { uploadDocument, enqueueVersion } from '@/api/knowledge-management'
  import { useAuth } from '@/hooks/core/useAuth'
  defineOptions({ name: 'KnowledgeDocuments' })
  const { hasAuth } = useAuth()
  const bases = ref<ManagementRow[]>([]),
    baseId = ref<string>()
  const documentsTable = ref<InstanceType<typeof ManagementTable>>()
  const selectedDocument = ref<ManagementRow>(),
    selectedVersion = ref<ManagementRow>()
  const chunkVisible = ref(false),
    chunkContent = ref('')
  const uploadVisible = ref(false),
    uploading = ref(false),
    busyVersion = ref<string>()
  const uploadBaseId = ref<string>(),
    uploadDocumentId = ref<string>(),
    file = ref<File>()
  function openChunk(row: ManagementRow) {
    chunkContent.value = row.content
    chunkVisible.value = true
  }
  async function loadBases() {
    bases.value = await allManagement('knowledge', 'bases')
  }
  function openUpload(row?: ManagementRow) {
    uploadBaseId.value = row?.knowledgeBaseId ?? baseId.value
    uploadDocumentId.value = row?.id
    file.value = undefined
    uploadVisible.value = true
  }
  function selectFile(event: Event) {
    file.value = (event.target as HTMLInputElement).files?.[0]
    if (file.value && file.value.size > 4 * 1024 * 1024) {
      ElMessage.error('文件不能超过 4MB')
      file.value = undefined
    }
  }
  async function upload() {
    if (!file.value || !uploadBaseId.value) return
    uploading.value = true
    try {
      await uploadDocument(file.value, uploadBaseId.value, uploadDocumentId.value)
      uploadVisible.value = false
      ElMessage.success('解析完成，文档版本已保存')
      await documentsTable.value?.reload()
    } finally {
      uploading.value = false
    }
  }
  async function enqueue(row: ManagementRow, reload: () => Promise<void>) {
    busyVersion.value = row.id
    try {
      const result = await enqueueVersion(row.id)
      ElMessage.success(`已排队：${result.jobIds.join(', ')}`)
      await reload()
    } finally {
      busyVersion.value = undefined
    }
  }
  onMounted(loadBases)
</script>
