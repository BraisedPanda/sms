import { useUserStore } from '@/store/modules/user'
import { refreshAccessToken } from './index'

export class StreamHttpError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message)
  }
}
export function apiUrl(path: string) {
  const base = import.meta.env.VITE_API_URL
  return !base || base === '/' ? path : `${base.replace(/\/+$/, '')}${path}`
}
export async function fetchWithAuth(path: string, init: RequestInit = {}): Promise<Response> {
  const user = useUserStore()
  const send = () => {
    const headers = new Headers(init.headers)
    headers.set('Authorization', `Bearer ${user.accessToken}`)
    return fetch(apiUrl(path), { ...init, headers })
  }
  let response = await send()
  if (response.status === 401) {
    await response.body?.cancel()
    await refreshAccessToken()
    if (init.signal?.aborted) throw new DOMException('Aborted', 'AbortError')
    response = await send()
  }
  if (!response.ok) {
    const status = response.status
    let message = `请求失败（${status}）`
    try {
      const body = await response.json()
      message = body.detail ?? body.msg ?? message
    } catch {
      /* No JSON response. */
    }
    if (status === 401) user.logOut()
    throw new StreamHttpError(status, message)
  }
  return response
}
