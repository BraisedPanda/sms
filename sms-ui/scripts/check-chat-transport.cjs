const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
function moduleFrom(file, mocks, globals = {}) {
  const exports = {}
  const source = fs
    .readFileSync(path.join(__dirname, '../src/utils/http', file), 'utf8')
    .replaceAll('import.meta.env', 'globalThis.__env')
  const js = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(js, {
    exports,
    require(name) {
      if (!(name in mocks)) throw new Error('Unexpected import: ' + name)
      return mocks[name]
    },
    TextDecoder,
    Headers,
    DOMException,
    setTimeout,
    clearTimeout,
    __env: {},
    ...globals
  })
  return exports
}
const sse = moduleFrom('sse.ts', {})
test('SSE preserves UTF-8 and multiline data across byte/frame boundaries', async () => {
  const text =
    ':heartbeat\r\n\r\nid: 123-0\r\nevent: token\r\ndata: 你好😀\r\ndata: 第二行\r\n\r\nevent: done\ndata: 完成\n\n'
  const bytes = new TextEncoder().encode(text)
  const response = new Response(
    new ReadableStream({
      start(controller) {
        for (const byte of bytes) controller.enqueue(Uint8Array.of(byte))
        controller.close()
      }
    })
  )
  const events = []
  await sse.consumeSse(response, (event) => {
    events.push(event)
    return event.event !== 'done'
  })
  assert.equal(events.length, 2)
  assert.equal(events[0].data, '你好😀\n第二行')
  assert.equal(events[0].id, '123-0')
})
test('Terminal event cancels the reader before later tokens are applied', async () => {
  let cancelled = false
  const response = new Response(
    new ReadableStream({
      start(controller) {
        controller.enqueue(
          new TextEncoder().encode('event: done\ndata: ok\n\nevent: token\ndata: ignored\n\n')
        )
      },
      cancel() {
        cancelled = true
      }
    })
  )
  const events = []
  await sse.consumeSse(response, (event) => {
    events.push(event.event)
    return false
  })
  assert.deepEqual(events, ['done'])
  assert.equal(cancelled, true)
})
test('Concurrent REST and streaming requests share the rotating refresh token', async () => {
  let refreshes = 0,
    logout = 0
  const user = {
    accessToken: 'expired',
    refreshToken: 'refresh-old',
    setToken(token, refreshToken) {
      this.accessToken = token
      this.refreshToken = refreshToken
    },
    logOut() {
      logout++
    }
  }
  let responseInterceptor
  const client = {
    interceptors: {
      request: { use() {} },
      response: {
        use(success) {
          responseInterceptor = success
        }
      }
    },
    async post() {
      refreshes++
      await new Promise((resolve) => setTimeout(resolve, 10))
      return { data: { code: 0, data: { token: 'fresh', refreshToken: 'refresh-new' } } }
    },
    async request() {
      return { data: { code: 0, data: 'rest-ok' } }
    }
  }
  class HttpError extends Error {
    constructor(message, code) {
      super(message)
      this.code = code
    }
  }
  const store = { useUserStore: () => user }
  const http = moduleFrom('index.ts', {
    axios: { default: { create: () => client } },
    '@/store/modules/user': store,
    './status': { ApiStatus: { success: 200, unauthorized: 401 } },
    './error': { HttpError, showError() {}, showSuccess() {}, handleError() {} },
    '@/locales': { $t: (text) => text }
  })
  const calls = []
  const stream = moduleFrom(
    'stream.ts',
    { '@/store/modules/user': store, './index': http },
    {
      async fetch(url, init) {
        calls.push(init.headers.get('Authorization'))
        return new Response(
          init.headers.get('Authorization') === 'Bearer fresh' ? 'event: done\ndata: ok\n\n' : '{}',
          { status: init.headers.get('Authorization') === 'Bearer fresh' ? 200 : 401 }
        )
      }
    }
  )
  await Promise.all([
    stream.fetchWithAuth('/events'),
    stream.fetchWithAuth('/events'),
    responseInterceptor({ data: { code: 401 }, config: {} })
  ])
  assert.equal(refreshes, 1)
  assert.equal(logout, 0)
  assert.deepEqual(calls, ['Bearer expired', 'Bearer expired', 'Bearer fresh', 'Bearer fresh'])
  assert.equal(user.refreshToken, 'refresh-new')
})
test('Second 401 logs out once and does not loop or replay a new run', async () => {
  let refreshed = 0,
    calls = 0,
    logout = 0
  const stream = moduleFrom(
    'stream.ts',
    {
      '@/store/modules/user': {
        useUserStore: () => ({
          accessToken: 'bad',
          logOut() {
            logout++
          }
        })
      },
      './index': {
        async refreshAccessToken() {
          refreshed++
        }
      }
    },
    {
      async fetch() {
        calls++
        return Response.json({ msg: 'expired' }, { status: 401 })
      }
    }
  )
  await assert.rejects(stream.fetchWithAuth('/events'), (error) => error.status === 401)
  assert.equal(refreshed, 1)
  assert.equal(calls, 2)
  assert.equal(logout, 1)
})
