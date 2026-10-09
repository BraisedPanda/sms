<template>
  <div class="page-content chat-page" :style="{ height: containerMinHeight }">
    <div class="chat-toolbar">
      <div
        ><strong>AI 助手</strong
        ><small v-if="activeTurn?.runId">Run: {{ activeTurn.runId }}</small></div
      >
      <ElSelect
        v-model="conversationId"
        placeholder="选择历史会话"
        clearable
        :disabled="isStreaming || historyLoading"
        @change="restoreHistory"
      >
        <ElOption
          v-for="item in conversations"
          :key="item.id"
          :value="item.id"
          :label="item.title || '未命名会话'"
        />
      </ElSelect>
      <ElButton :disabled="isStreaming" @click="loadConversations">刷新历史</ElButton>
    </div>
    <div
      ref="messageContainer"
      class="chat-messages"
      @scroll="handleMessageScroll"
      v-loading="historyLoading"
    >
      <div
        v-for="message in messages"
        :key="message.id"
        class="chat-message"
        :class="{ mine: message.isMe }"
      >
        <ElAvatar :size="32" :src="message.isMe ? meAvatar : aiAvatar" />
        <div class="message-body">
          <div class="message-meta">{{ message.isMe ? '我' : 'AI 助手' }} · {{ message.time }}</div>
          <div class="message-content">
            <div v-if="message.streaming" class="chat-status" aria-live="polite">{{
              getStatusLabel(message.status)
            }}</div>
            <div
              v-if="!message.isMe && message.content"
              class="markdown-content"
              v-html="renderMarkdown(message.content)"
            ></div>
            <span v-else>{{ message.content }}</span>
            <div v-if="message.error" class="chat-error" role="alert">{{ message.error }}</div>
            <ElButton
              v-if="message.turn && message.error && !isStreaming"
              link
              type="primary"
              @click="retryMessage(message)"
            >
              {{ message.turn.terminal ? '重新生成' : '恢复请求' }}
            </ElButton>
          </div>
        </div>
      </div>
      <ElEmpty v-if="!messages.length && !historyLoading" description="发送问题，或选择历史会话" />
    </div>
    <div class="chat-input">
      <ElInput
        v-model="messageText"
        type="textarea"
        :rows="3"
        resize="none"
        placeholder="输入问题，Ctrl + Enter 发送"
        @keydown.ctrl.enter.prevent="sendMessage"
      />
      <div class="chat-toolbar">
        <span class="chat-status">{{
          isStreaming ? '回答中，可停止生成' : '历史记录按当前用户保存'
        }}</span>
        <ElButton v-if="isStreaming" type="danger" :loading="cancelling" @click="stopRun"
          >停止生成</ElButton
        >
        <ElButton
          type="primary"
          :disabled="isStreaming || historyLoading || !messageText.trim()"
          @click="sendMessage"
          >发送</ElButton
        >
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ElMessage } from 'element-plus'
  import DOMPurify from 'dompurify'
  import { marked } from 'marked'
  import { startChat, cancelChat, chatSnapshot, conversationMessages } from '@/api/ai-chat'
  import { listManagement, type ManagementRow } from '@/api/management'
  import { fetchWithAuth, StreamHttpError } from '@/utils/http/stream'
  import { consumeSse } from '@/utils/http/sse'
  import { useUserStore } from '@/store/modules/user'
  import { useAutoLayoutHeight } from '@/hooks/core/useLayoutHeight'
  import meAvatar from '@/assets/images/avatar/avatar5.webp'
  import aiAvatar from '@/assets/images/avatar/avatar10.webp'

  defineOptions({ name: 'TemplateChat' })
  interface ChatTurn {
    question: string
    idempotencyKey: string
    conversationId?: string
    runId?: string
    cursor: string
    content: string
    terminal?: boolean
  }
  interface ChatMessage {
    id: number
    isMe: boolean
    content: string
    time: string
    streaming?: boolean
    status?: string
    error?: string
    turn?: ChatTurn
  }
  const { containerMinHeight } = useAutoLayoutHeight()
  const user = useUserStore()
  const messages = ref<ChatMessage[]>([])
  const conversations = ref<ManagementRow[]>([])
  const conversationId = ref<string>()
  const activeTurn = ref<ChatTurn>()
  const messageText = ref('')
  const isStreaming = ref(false),
    cancelling = ref(false),
    historyLoading = ref(false)
  const messageContainer = ref<HTMLElement>()
  let messageId = 0,
    controller: AbortController | undefined,
    scrollFrame: number | undefined
  let mounted = true,
    cancelRequested = false
  const nearBottom = ref(true)
  const draftKey = `sms:chat:pending:${user.info.userId}`
  const newKey = () => crypto.randomUUID()
  const time = (value?: string) =>
    new Date(value ?? Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  const terminal = (status: string) => ['SUCCEEDED', 'FAILED', 'CANCELLED'].includes(status)
  const getStatusLabel = (status?: string) =>
    ({
      start: '正在提交',
      planning: '分析问题中',
      executing: '查询相关数据',
      generating: '生成中',
      reconnecting: '连接中断，正在恢复'
    })[status ?? ''] ?? '处理中'
  const renderMarkdown = (content: string) =>
    DOMPurify.sanitize(marked.parse(content, { breaks: true, gfm: true }) as string)
  function persist(turn: ChatTurn) {
    try {
      if (turn.terminal) sessionStorage.removeItem(draftKey)
      else sessionStorage.setItem(draftKey, JSON.stringify(turn))
    } catch {
      /* Storage can be disabled or full. */
    }
  }
  function assistant(turn: ChatTurn) {
    const message = reactive<ChatMessage>({
      id: ++messageId,
      isMe: false,
      content: turn.content,
      time: time(),
      turn
    })
    messages.value.push(message)
    return message
  }
  async function loadConversations() {
    const page = await listManagement('ai', 'conversations', { current: 1, size: 200 })
    if (mounted) conversations.value = page.records
  }
  async function restoreHistory() {
    if (isStreaming.value) return
    messages.value = []
    if (!conversationId.value) return
    historyLoading.value = true
    try {
      const history: ManagementRow[] = []
      for (let page = 1; ; page++) {
        const result = await conversationMessages(conversationId.value, page)
        history.push(...result.records)
        if (history.length >= result.total || !result.records.length) break
      }
      if (!mounted) return
      messages.value = history.map((row) => ({
        id: ++messageId,
        isMe: row.role === 'user',
        content: row.content,
        time: time(row.createTime)
      }))
      for (const row of history.filter(
        (r) => r.role === 'user' && ['FAILED', 'CANCELLED'].includes(r.runStatus)
      )) {
        if (history.some((r) => r.runId === row.runId && r.role === 'assistant')) continue
        const turn: ChatTurn = {
          question: row.content,
          idempotencyKey: newKey(),
          conversationId: conversationId.value,
          runId: row.runId,
          cursor: '0-0',
          content: '',
          terminal: true
        }
        const failed = assistant(turn)
        failed.error =
          row.runStatus === 'CANCELLED' ? '已停止生成，可重新生成' : '处理失败，可重新生成'
      }
      const pending = [...history]
        .reverse()
        .find(
          (row) => row.role === 'user' && row.runId && row.runStatus && !terminal(row.runStatus)
        )
      if (pending) {
        const turn: ChatTurn = {
          question: pending.content,
          idempotencyKey: newKey(),
          conversationId: conversationId.value,
          runId: pending.runId,
          cursor: '0-0',
          content: ''
        }
        await runTurn(turn, assistant(turn))
      }
    } finally {
      historyLoading.value = false
      scheduleScrollToBottom()
    }
  }
  async function syncSnapshot(turn: ChatTurn, message: ChatMessage) {
    if (!turn.runId) return false
    const state = await chatSnapshot(turn.runId)
    if (state.conversationId) {
      turn.conversationId = state.conversationId
      conversationId.value = state.conversationId
    }
    if (!terminal(state.status)) return false
    turn.terminal = true
    if (state.answer != null) message.content = turn.content = state.answer
    message.status = state.status
    message.error =
      state.status === 'SUCCEEDED'
        ? undefined
        : state.status === 'CANCELLED'
          ? '已停止生成，可重新生成'
          : state.errorMessage || '处理失败，可重新生成'
    persist(turn)
    return true
  }
  async function recoverSnapshot(turn: ChatTurn, message: ChatMessage) {
    try {
      return await syncSnapshot(turn, message)
    } catch (error) {
      if ([401, 403, 404].includes((error as { code?: number })?.code ?? 0)) throw error
      return false
    }
  }
  async function runTurn(turn: ChatTurn, message: ChatMessage) {
    isStreaming.value = true
    activeTurn.value = turn
    message.streaming = true
    message.error = undefined
    message.status = 'start'
    cancelRequested = false
    const connection = new AbortController()
    controller = connection
    persist(turn)
    try {
      if (!turn.runId) {
        const receipt = await startChat(turn.question, turn.idempotencyKey, turn.conversationId)
        turn.runId = receipt.runId
        persist(turn)
      }
      if (cancelRequested) await cancelChat(turn.runId)
      if (await recoverSnapshot(turn, message)) return
      for (let attempt = 0; attempt < 4 && mounted; attempt++) {
        let reachedTerminal = false
        try {
          const response = await fetchWithAuth(
            `/api/ai/runs/${turn.runId}/events?after=${encodeURIComponent(turn.cursor)}`,
            { signal: connection.signal, headers: { Accept: 'text/event-stream' } }
          )
          await consumeSse(response, (event) => {
            if (event.event === 'token') message.content = turn.content += event.data
            else if (event.event === 'snapshot') message.content = turn.content = event.data
            else if (event.event === 'done' || event.event === 'error') {
              reachedTerminal = true
              message.status = event.event
              if (event.event === 'error') message.error = event.data || '处理失败'
            } else message.status = event.event
            if (event.id) turn.cursor = event.id
            persist(turn)
            scheduleScrollToBottom()
            return !reachedTerminal
          })
          if (reachedTerminal) {
            turn.terminal = true
            // The snapshot is authoritative and also restores the canonical conversation id.
            await syncSnapshot(turn, message).catch(() => {})
            persist(turn)
            break
          }
          if (await recoverSnapshot(turn, message)) break
        } catch (error) {
          if (
            connection.signal.aborted ||
            (error instanceof StreamHttpError && [401, 403, 404].includes(error.status))
          )
            throw error
          if (await recoverSnapshot(turn, message)) break
          if (attempt === 3) throw error
        }
        if (attempt === 3) throw new Error('连接中断，请点击恢复请求继续接收回答')
        message.status = 'reconnecting'
        await new Promise<void>((resolve) => {
          const timer = setTimeout(resolve, 500 * 2 ** attempt)
          connection.signal.addEventListener(
            'abort',
            () => {
              clearTimeout(timer)
              resolve()
            },
            { once: true }
          )
        })
        if (connection.signal.aborted) return
      }
    } catch (error) {
      if (mounted) message.error = error instanceof Error ? error.message : '请求失败，请恢复请求'
    } finally {
      message.streaming = false
      isStreaming.value = false
      cancelling.value = false
      controller = undefined
      persist(turn)
      scheduleScrollToBottom()
      if (mounted) loadConversations().catch(() => {})
    }
  }
  async function sendMessage() {
    const question = messageText.value.trim()
    if (!question || isStreaming.value || historyLoading.value) return
    const turn: ChatTurn = {
      question,
      idempotencyKey: newKey(),
      conversationId: conversationId.value,
      cursor: '0-0',
      content: ''
    }
    messages.value.push({ id: ++messageId, isMe: true, content: question, time: time() })
    messageText.value = ''
    nearBottom.value = true
    await runTurn(turn, assistant(turn))
  }
  async function retryMessage(message: ChatMessage) {
    if (!message.turn || isStreaming.value) return
    if (message.turn.terminal) {
      messageText.value = message.turn.question
      await sendMessage()
    } else await runTurn(message.turn, message)
  }
  async function stopRun() {
    cancelRequested = true
    cancelling.value = true
    try {
      if (activeTurn.value?.runId) await cancelChat(activeTurn.value.runId)
    } catch {
      cancelling.value = false
      ElMessage.error('取消失败，请重试')
    }
  }
  function handleMessageScroll() {
    const el = messageContainer.value
    if (el) nearBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight <= 80
  }
  function scheduleScrollToBottom() {
    if (!nearBottom.value || scrollFrame != null || !mounted) return
    scrollFrame = requestAnimationFrame(() => {
      scrollFrame = undefined
      const el = messageContainer.value
      if (el && nearBottom.value) el.scrollTop = el.scrollHeight
    })
  }
  onMounted(async () => {
    await loadConversations().catch(() => ElMessage.error('历史加载失败，可点击刷新历史重试'))
    try {
      const raw = sessionStorage.getItem(draftKey)
      if (!raw) return
      const draft = JSON.parse(raw) as ChatTurn
      if (!draft.question || !draft.idempotencyKey || !/^\d+-\d+$/.test(draft.cursor)) return
      // A restored partial answer and its cursor must always move together.
      conversationId.value = draft.conversationId
      messages.value.push({ id: ++messageId, isMe: true, content: draft.question, time: time() })
      await runTurn(draft, assistant(draft))
    } catch {
      ElMessage.error('历史加载失败，请刷新重试')
    }
  })
  onUnmounted(() => {
    mounted = false
    controller?.abort()
    if (scrollFrame != null) cancelAnimationFrame(scrollFrame)
  })
</script>

<style scoped>
  .chat-page {
    display: flex;
    flex-direction: column;
    padding: 0 !important;
  }
  .chat-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    flex-wrap: wrap;
  }
  .chat-toolbar small {
    display: block;
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
  .chat-toolbar .el-select {
    width: 260px;
    margin-left: auto;
  }
  .chat-messages {
    flex: 1;
    overflow-y: auto;
    border-block: 1px solid var(--el-border-color);
    padding: 24px 16px;
    min-height: 0;
  }
  .chat-message {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 24px;
  }
  .chat-message.mine {
    flex-direction: row-reverse;
  }
  .message-body {
    max-width: 80%;
    overflow-wrap: anywhere;
  }
  .message-meta,
  .chat-status {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
  .message-content {
    padding: 12px;
    margin-top: 6px;
    border-radius: 8px;
    background: var(--el-fill-color-light);
    white-space: pre-wrap;
  }
  .mine .message-content {
    background: var(--el-color-primary-light-9);
  }
  .chat-error {
    color: var(--el-color-danger);
    margin-top: 8px;
  }
  .chat-input {
    padding: 16px;
  }
  .chat-input .chat-toolbar {
    padding: 12px 0 0;
  }
  .chat-input .chat-status {
    margin-right: auto;
  }
  .markdown-content {
    white-space: normal;
  }
  .markdown-content :deep(pre) {
    overflow-x: auto;
    padding: 12px;
    background: rgb(0 0 0 / 8%);
    border-radius: 6px;
  }
  .markdown-content :deep(p) {
    margin: 0 0 8px;
  }
  .markdown-content :deep(ul),
  .markdown-content :deep(ol) {
    padding-left: 24px;
  }
</style>
