import request from '@/utils/http'

export type ManagementDomain = 'system' | 'knowledge' | 'ai'
export type ManagementRow = Record<string, any> & { id: string }
export interface ManagementPage {
  records: ManagementRow[]
  current: number
  size: number
  total: number
}

export const managementUrl = (domain: ManagementDomain, resource: string) =>
  `/api/${domain}/manage/${resource}`

export function listManagement(
  domain: ManagementDomain,
  resource: string,
  params: Record<string, unknown>
) {
  return request.get<ManagementPage>({ url: managementUrl(domain, resource), params })
}
export function saveManagement(
  domain: ManagementDomain,
  resource: string,
  id: string | undefined,
  data: Record<string, unknown>
) {
  const url = managementUrl(domain, resource) + (id ? `/${id}` : '')
  return id ? request.put<ManagementRow>({ url, data }) : request.post<ManagementRow>({ url, data })
}
export function deleteManagement(domain: ManagementDomain, resource: string, id: string) {
  return request.del<void>({ url: `${managementUrl(domain, resource)}/${id}` })
}
export async function allManagement(
  domain: ManagementDomain,
  resource: string,
  params: Record<string, unknown> = {}
) {
  const records: ManagementRow[] = []
  for (let current = 1; ; current++) {
    const page = await listManagement(domain, resource, { ...params, current, size: 200 })
    records.push(...page.records)
    if (records.length >= page.total || page.records.length === 0) return records
  }
}
export function getAssignments(resource: string, id: string) {
  return request.get<{ roleIds?: string[]; menuIds?: string[]; buttonIds?: string[] }>({
    url: `/api/system/manage/${resource}/${id}/assignments`
  })
}
export function assignRoles(id: string, roleIds: string[]) {
  return request.put<void>({ url: `/api/system/manage/users/${id}/roles`, data: { roleIds } })
}
export function grantPermissions(id: string, menuIds: string[], buttonIds: string[]) {
  return request.put<void>({
    url: `/api/system/manage/roles/${id}/permissions`,
    data: { menuIds, buttonIds }
  })
}
export function revokeSession(id: string) {
  return request.post<void>({ url: `/api/system/manage/sessions/${id}/revoke` })
}
