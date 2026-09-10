<!-- 聊天页 -->
<template>
  <div class="page-content flex !p-0 max-md:flex-col" :style="{ height: containerMinHeight }">
    <div class="box-border flex-1 h-full max-md:h-[calc(70%-30px)]">
      <div class="flex-cb pt-4 px-4 pb-0 mb-5">
        <div>
          <span class="text-base font-medium">Art Bot</span>
          <div class="flex-c gap-1 mt-1.5">
            <div
              class="w-2 h-2 rounded-full"
              :class="isOnline ? 'bg-success/100' : 'bg-danger/100'"
            ></div>
            <span class="text-xs text-g-600">{{ isOnline ? '在线' : '离线' }}</span>
          </div>
        </div>
        <div class="flex-c gap-2">
          <ArtIconButton icon="ri:phone-line" circle class="size-11 text-g-600" />
          <ArtIconButton icon="ri:video-on-line" circle class="size-11 text-g-600" />
          <ArtIconButton icon="ri:more-2-fill" circle class="size-11 text-g-600" />
        </div>
      </div>
      <div class="flex flex-col h-[calc(100%-85px)]">
        <!-- 聊天消息区域 -->
        <div
          class="flex-1 py-7.5 px-4 overflow-y-auto border-t-d [&::-webkit-scrollbar]:!w-1"
          ref="messageContainer"
          @scroll="handleMessageScroll"
        >
          <template v-for="message in messages" :key="message.id">
            <div
              :class="[
                'flex gap-2 items-start w-full mb-7.5',
                message.isMe ? 'flex-row-reverse' : 'flex-row justify-start'
              ]"
            >
              <ElAvatar :size="32" :src="message.avatar" class="flex-shrink-0" />
              <div
                class="flex flex-col max-w-[70%]"
                :class="message.isMe ? 'items-end' : 'items-start'"
              >
                <div
                  class="flex gap-2 mb-1 text-xs"
                  :class="message.isMe ? 'flex-row-reverse' : 'flex-row'"
                >
                  <span class="font-medium">{{ message.sender }}</span>
                  <span class="text-g-600">{{ message.time }}</span>
                </div>
                <div
                  class="py-2.5 px-3.5 text-sm leading-[1.4] rounded-md"
                  :class="message.isMe ? '!bg-theme/15' : '!bg-active-color'"
                >
                  <div
                    v-if="message.streaming && message.status"
                    class="chat-status shimmer-text"
                    aria-live="polite"
                  >
                    {{ getStatusLabel(message.status) }}
                  </div>
                  <div
                    v-if="!message.isMe && message.content"
                    class="markdown-content"
                    v-html="renderMarkdown(message.content)"
                  ></div>
                  <span v-else-if="message.content">{{ message.content }}</span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 聊天输入区域 -->
        <div class="p-4">
          <ElInput
            v-model="messageText"
            type="textarea"
            :rows="3"
            placeholder="输入消息"
            resize="none"
            @keyup.enter.prevent="sendMessage"
          >
            <template #append>
              <div class="flex gap-2 py-2">
                <ElButton :icon="Paperclip" circle plain />
                <ElButton :icon="Picture" circle plain />
                <ElButton
                  type="primary"
                  @click="sendMessage"
                  :loading="isStreaming"
                  :disabled="isStreaming"
                  v-ripple
                  >发送</ElButton
                >
              </div>
            </template>
          </ElInput>
          <div class="flex-cb mt-3">
            <div class="flex-c">
              <ArtSvgIcon icon="ri:image-line" class="mr-5 c-p text-g-600 text-lg" />
              <ArtSvgIcon icon="ri:emotion-happy-line" class="mr-5 c-p text-g-600 text-lg" />
            </div>
            <ElButton
              type="primary"
              @click="sendMessage"
              :loading="isStreaming"
              :disabled="isStreaming"
              v-ripple
              class="min-w-20"
              >发送</ElButton
            >
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { Picture, Paperclip } from '@element-plus/icons-vue'
  import { ElMessage } from 'element-plus'
  import DOMPurify from 'dompurify'
  import { marked } from 'marked'
  import { useUserStore } from '@/store/modules/user'
  import meAvatar from '@/assets/images/avatar/avatar5.webp'
  import aiAvatar from '@/assets/images/avatar/avatar10.webp'
  import { useAutoLayoutHeight } from '@/hooks/core/useLayoutHeight'

  defineOptions({ name: 'TemplateChat' })

  const { containerMinHeight } = useAutoLayoutHeight()

  const isOnline = ref(true)
  const messageText = ref('')
  const messageId = ref(1)
  const userAvatar = ref(meAvatar)
  const messageContainer = ref<HTMLElement | null>(null)
  const isNearBottom = ref(true)
  const userStore = useUserStore()
  const sessionId = ref(createSessionId())
  const isStreaming = ref(false)
  let streamController: AbortController | null = null
  let scrollFrame: number | null = null
  const SCROLL_THRESHOLD = 80

  function createSessionId(): string {
    return typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  /**
   * 消息列表数据
   */
  interface ChatMessage {
    id: number
    sender: string
    content: string
    time: string
    isMe: boolean
    avatar: string
    streaming?: boolean
    status?: string
  }

  const messages = ref<ChatMessage[]>([])

  const statusLabels: Record<string, string> = {
    start: '正在思考',
    planning: '分析问题中',
    analyzing: '理解问题中',
    executing: '查询相关数据',
    generating: '生成中',
    done: '已完成',
    error: '处理失败'
  }

  const getStatusLabel = (status: string) => statusLabels[status] || '处理中'

  const renderMarkdown = (content: string) => {
    if (!content) return ''
    const html = marked.parse(content, { breaks: true, gfm: true }) as string
    return DOMPurify.sanitize(html)
  }

  interface ServerSentEvent {
    event: string
    data: string
  }

  const STREAM_CHARACTER_DELAY = 18

  /** Resolve both the Vite proxy URL and a production API base URL. */
  const getChatUrl = (): string => {
    const apiUrl = import.meta.env.VITE_API_URL
    if (!apiUrl || apiUrl === '/') return '/api/ai/chat'
    return `${apiUrl.replace(/\/+$/, '')}/api/ai/chat`
  }

  const parseServerSentEvent = (frame: string): ServerSentEvent | null => {
    let event = 'message'
    const data: string[] = []

    frame.split(/\r?\n/).forEach((line) => {
      if (!line || line.startsWith(':')) return
      const separator = line.indexOf(':')
      const field = separator === -1 ? line : line.slice(0, separator)
      const value = separator === -1 ? '' : line.slice(separator + 1).replace(/^ /, '')
      if (field === 'event') event = value
      if (field === 'data') data.push(value)
    })

    return data.length ? { event, data: data.join('\n') } : null
  }

  /** Consume the named SSE events emitted by AiChatController. */
  const readAiStream = async (response: Response, assistantMessage: ChatMessage) => {
    if (!response.body) throw new Error('服务器未返回流式响应')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let completed = false

    const appendToken = async (token: string) => {
      for (const character of Array.from(token)) {
        assistantMessage.content += character
        scheduleScrollToBottom()
        await new Promise<void>((resolve) => setTimeout(resolve, STREAM_CHARACTER_DELAY))
      }
    }

    const processFrame = async (frame: string) => {
      const serverEvent = parseServerSentEvent(frame)
      if (!serverEvent) return

      if (serverEvent.event === 'token') {
        await appendToken(serverEvent.data)
        assistantMessage.streaming = true
      } else if (
        serverEvent.event === 'start' ||
        serverEvent.event === 'planning' ||
        serverEvent.event === 'analyzing' ||
        serverEvent.event === 'executing' ||
        serverEvent.event === 'generating'
      ) {
        assistantMessage.status = serverEvent.event
        assistantMessage.streaming = true
      } else if (serverEvent.event === 'error') {
        assistantMessage.status = 'error'
        assistantMessage.streaming = false
        throw new Error(serverEvent.data || 'AI 服务处理失败')
      } else if (serverEvent.event === 'done') {
        completed = true
        assistantMessage.status = 'done'
        assistantMessage.streaming = false
      }
    }

    try {
      while (true) {
        const { value, done } = await reader.read()
        if (value) buffer += decoder.decode(value, { stream: true })
        if (done) {
          // Flush a possible partial UTF-8 sequence before parsing the final frame.
          buffer += decoder.decode()
        }
        const frames = buffer.split(/\r?\n\r?\n/)
        buffer = frames.pop() || ''
        for (const frame of frames) {
          await processFrame(frame)
        }
        if (done) break
      }

      if (buffer.trim()) await processFrame(buffer)
      if (!completed || !assistantMessage.content.trim()) {
        throw new Error('AI 服务未返回有效内容')
      }
    } finally {
      reader.releaseLock()
    }
  }

  /** Send a question and append each SSE token to the current AI message. */
  const sendMessage = async () => {
    const text = messageText.value.trim()
    if (!text || isStreaming.value) return

    messages.value.push({
      id: messageId.value++,
      sender: 'Ricky',
      content: text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isMe: true,
      avatar: userAvatar.value
    })

    const assistantMessage = reactive<ChatMessage>({
      id: messageId.value++,
      sender: 'Art Bot',
      content: '',
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isMe: false,
      avatar: aiAvatar,
      streaming: true,
      status: 'start'
    })
    messages.value.push(assistantMessage)

    messageText.value = ''
    isStreaming.value = true
    streamController = new AbortController()
    scheduleScrollToBottom()

    try {
      const response = await fetch(getChatUrl(), {
        method: 'POST',
        headers: {
          Accept: 'text/event-stream',
          'Content-Type': 'application/json',
          ...(userStore.accessToken ? { Authorization: userStore.accessToken } : {})
        },
        body: JSON.stringify({
          userId: String(userStore.info.userId ?? 'anonymous'),
          sessionId: sessionId.value,
          question: text,
          alias: 'balanced'
        }),
        signal: streamController.signal
      })

      if (!response.ok) {
        if (response.status === 401) userStore.logOut()
        throw new Error(
          response.status === 401 ? '登录已失效，请重新登录' : `请求失败（${response.status}）`
        )
      }

      await readAiStream(response, assistantMessage)
    } catch (error) {
      const isAborted = error instanceof DOMException && error.name === 'AbortError'
      assistantMessage.streaming = false
      if (!isAborted) {
        assistantMessage.status = 'error'
        const message = error instanceof Error ? error.message : 'AI 服务暂时不可用'
        assistantMessage.content = assistantMessage.content
          ? `${assistantMessage.content}\n\n抱歉，${message}`
          : `抱歉，${message}`
        ElMessage.error(message)
      }
    } finally {
      assistantMessage.streaming = false
      isStreaming.value = false
      streamController = null
      scheduleScrollToBottom()
    }
  }

  /**
   * 滚动到消息列表底部
   */
  const handleMessageScroll = () => {
    const container = messageContainer.value
    if (!container) return
    isNearBottom.value =
      container.scrollHeight - container.scrollTop - container.clientHeight <= SCROLL_THRESHOLD
  }

  const scheduleScrollToBottom = () => {
    if (!isNearBottom.value || scrollFrame !== null) return

    scrollFrame = requestAnimationFrame(() => {
      scrollFrame = null
      const container = messageContainer.value
      if (!container || !isNearBottom.value) return
      container.scrollTop = container.scrollHeight
    })
  }

  onMounted(() => {
    scheduleScrollToBottom()
  })

  onUnmounted(() => {
    streamController?.abort()
    if (scrollFrame !== null) cancelAnimationFrame(scrollFrame)
  })
</script>

<style scoped>
  .chat-status {
    margin-bottom: 0.35rem;
    color: var(--el-text-color-secondary);
    font-size: 0.75rem;
    line-height: 1.25rem;
  }

  .shimmer-text {
    background: linear-gradient(
      100deg,
      var(--el-text-color-secondary) 35%,
      var(--el-text-color-primary) 50%,
      var(--el-text-color-secondary) 65%
    );
    background-size: 250% 100%;
    background-clip: text;
    -webkit-background-clip: text;
    color: transparent;
    animation: shimmer 1.8s ease-in-out infinite;
  }

  .markdown-content {
    overflow-wrap: anywhere;
  }

  .markdown-content :deep(p) {
    margin: 0 0 0.65rem;
  }

  .markdown-content :deep(p:last-child) {
    margin-bottom: 0;
  }

  .markdown-content :deep(ul),
  .markdown-content :deep(ol) {
    margin: 0.5rem 0;
    padding-left: 1.25rem;
  }

  .markdown-content :deep(pre) {
    margin: 0.65rem 0;
    overflow-x: auto;
    padding: 0.65rem;
    border-radius: 0.35rem;
    background: rgb(0 0 0 / 8%);
  }

  .markdown-content :deep(code) {
    font-size: 0.9em;
  }

  @keyframes shimmer {
    0% {
      background-position: 100% 0;
    }

    100% {
      background-position: -100% 0;
    }
  }
</style>
