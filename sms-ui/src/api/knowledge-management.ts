import request from '@/utils/http'
import type { ManagementRow } from './management'

export function uploadDocument(file: File, knowledgeBaseId: string, documentId?: string) {
  const data = new FormData()
  data.append('file', file)
  data.append('knowledgeBaseId', knowledgeBaseId)
  if (documentId) data.append('documentId', documentId)
  return request.post<ManagementRow>({
    url: '/api/knowledge/manage/documents/upload',
    data,
    timeout: 60000,
    headers: { 'Content-Type': undefined }
  })
}
export function enqueueVersion(documentVersionId: string) {
  return request.post<{ jobIds: string[]; status: string }>({
    url: '/api/ai/knowledge/ingestions',
    data: { documentVersionId }
  })
}
export function jobAction(id: string, action: 'retry' | 'cancel') {
  return request.post<void>({ url: `/api/knowledge/manage/jobs/${id}/${action}` })
}
export function activateIndex(id: string, indexRevision: number) {
  return request.post<void>({
    url: `/api/ai/knowledge/versions/${id}/activate`,
    data: { indexRevision }
  })
}
