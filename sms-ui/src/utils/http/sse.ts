export interface StreamEvent {
  event: string
  data: string
  id?: string
}

export function parseSseFrame(frame: string): StreamEvent | null {
  let event = 'message',
    id: string | undefined
  const data: string[] = []
  for (const line of frame.split(/\r?\n/)) {
    if (!line || line.startsWith(':')) continue
    const colon = line.indexOf(':')
    const field = colon === -1 ? line : line.slice(0, colon)
    const value = colon === -1 ? '' : line.slice(colon + 1).replace(/^ /, '')
    if (field === 'event') event = value
    if (field === 'data') data.push(value)
    if (field === 'id' && !value.includes('\0')) id = value
  }
  return data.length ? { event, data: data.join('\n'), id } : null
}

/** Handles frame/UTF-8 boundaries and stops consuming immediately on a terminal event. */
export async function consumeSse(
  response: Response,
  onEvent: (event: StreamEvent) => boolean | Promise<boolean>
) {
  if (!response.body) throw new Error('服务器未返回事件流')
  const reader = response.body.getReader(),
    decoder = new TextDecoder()
  let buffer = ''
  const process = async (frame: string) => {
    const event = parseSseFrame(frame)
    return event ? await onEvent(event) : true
  }
  try {
    while (true) {
      const { value, done } = await reader.read()
      buffer += done ? decoder.decode() : decoder.decode(value, { stream: true })
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() ?? ''
      for (const frame of frames)
        if (!(await process(frame))) {
          await reader.cancel()
          return
        }
      if (done) {
        if (buffer.trim()) await process(buffer)
        return
      }
      if (buffer.length > 1024 * 1024) throw new Error('SSE 帧超过大小限制')
    }
  } finally {
    reader.releaseLock()
  }
}
