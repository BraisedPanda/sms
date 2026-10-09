import { AppRouteRecord } from '@/types/router'
export const aiRoutes: AppRouteRecord = {
  path: '/ai',
  name: 'AiOperations',
  component: '/index/index',
  meta: { title: 'AI 运营', icon: 'ri:robot-line' },
  children: [
    {
      path: 'runs',
      name: 'AiRuns',
      component: '/ai/runs',
      meta: {
        title: '任务与步骤',
        icon: 'ri:flow-chart',
        authList: [{ title: '查询任务与步骤', authMark: 'ai:run:read' }]
      }
    },
    {
      path: 'logs',
      name: 'AiToolLogs',
      component: '/ai/logs',
      meta: {
        title: '工具执行日志',
        icon: 'ri:file-list-line',
        authList: [{ title: '查询工具日志', authMark: 'ai:log:read' }]
      }
    }
  ]
}
