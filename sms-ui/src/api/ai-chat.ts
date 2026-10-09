import request from '@/utils/http'
import type { ManagementPage } from './management'

export interface ChatReceipt {
  runId: string
  status: string
  conversationId?: string
}
export interface ChatSnapshot extends ChatReceipt {
  question: string
  answer?: string
  errorMessage?: string
}
export function startChat(question: string, idempotencyKey: string, conversationId?: string) {
  return request.post<ChatReceipt>({
    url: '/api/ai/chat/runs',
    data: { question, alias: 'balanced', idempotencyKey, conversationId }
  })
}
export function cancelChat(runId: string) {
  return request.post<void>({ url: `/api/ai/runs/${runId}/cancel` })
}
export function chatSnapshot(runId: string) {
  return request.get<ChatSnapshot>({ url: `/api/ai/runs/${runId}/snapshot` })
}
export function conversationMessages(id: string, current = 1) {
  return request.get<ManagementPage>({
    url: `/api/ai/conversations/${id}/messages`,
    params: { current, size: 200 }
  })
}
