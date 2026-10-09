import { AppRouteRecord } from '@/types/router'
const auth = (resource: string, actions: string[]) =>
  actions.map((action) => ({
    title: (
      {
        read: '查询',
        create: '新增',
        update: '编辑',
        delete: '删除',
        retry: '重试',
        cancel: '取消',
        activate: '启用索引'
      } as Record<string, string>
    )[action],
    authMark: `knowledge:${resource}:${action}`
  }))
export const knowledgeRoutes: AppRouteRecord = {
  path: '/knowledge',
  name: 'Knowledge',
  component: '/index/index',
  meta: { title: '知识管理', icon: 'ri:book-open-line' },
  children: [
    {
      path: 'bases',
      name: 'KnowledgeBases',
      component: '/knowledge/bases',
      meta: {
        title: '知识库',
        icon: 'ri:database-2-line',
        authList: auth('base', ['read', 'create', 'update', 'delete'])
      }
    },
    {
      path: 'documents',
      name: 'KnowledgeDocuments',
      component: '/knowledge/documents',
      meta: {
        title: '文档与版本',
        icon: 'ri:file-text-line',
        authList: auth('document', ['read', 'create', 'update', 'delete'])
      }
    },
    {
      path: 'jobs',
      name: 'KnowledgeJobs',
      component: '/knowledge/jobs',
      meta: {
        title: '入库任务',
        icon: 'ri:loader-line',
        authList: [
          ...auth('ingestion', ['read', 'create', 'retry', 'cancel']),
          ...auth('index', ['activate'])
        ]
      }
    }
  ]
}
