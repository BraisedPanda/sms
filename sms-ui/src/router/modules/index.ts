import { AppRouteRecord } from '@/types/router'
import { dashboardRoutes } from './dashboard'
import { templateRoutes } from './template'
import { widgetsRoutes } from './widgets'
import { examplesRoutes } from './examples'
import { systemRoutes } from './system'
import { articleRoutes } from './article'
import { resultRoutes } from './result'
import { exceptionRoutes } from './exception'
import { safeguardRoutes } from './safeguard'
import { helpRoutes } from './help'
import { knowledgeRoutes } from './knowledge'
import { aiRoutes } from './ai'

/**
 * 导出所有模块化路由
 */
export const routeModules: AppRouteRecord[] = [
  dashboardRoutes,
  templateRoutes,
  widgetsRoutes,
  examplesRoutes,
  systemRoutes,
  articleRoutes,
  resultRoutes,
  exceptionRoutes,
  safeguardRoutes,
  ...helpRoutes,
  knowledgeRoutes,
  aiRoutes
]
