// Check the SQL bootstrap graph against the actual frontend route modules.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const uiRoot = path.resolve(__dirname, '..')
const repoRoot = path.resolve(uiRoot, '..')
const cache = new Map()

function load(file) {
  if (cache.has(file)) return cache.get(file)
  const exports = {}
  const js = ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText
  vm.runInNewContext(
    js,
    {
      exports,
      __APP_VERSION__: '0.0.0',
      require(specifier) {
        if (specifier === '@/utils/constants') {
          return load(path.join(uiRoot, 'src/utils/constants/links.ts'))
        }
        if (specifier.startsWith('.')) {
          return load(path.resolve(path.dirname(file), `${specifier}.ts`))
        }
        throw new Error(`Unsupported route import: ${specifier}`)
      }
    },
    { filename: file }
  )
  cache.set(file, exports)
  return exports
}

function readRows(sql, table) {
  const result = []
  const pattern = new RegExp(
    `INSERT INTO ${table}\\s*\\(([\\s\\S]*?)\\)\\s*VALUES\\s*([\\s\\S]*?)ON DUPLICATE KEY UPDATE`,
    'g'
  )
  for (const match of sql.matchAll(pattern)) {
    const columns = match[1].split(',').map((column) => column.trim())
    const tuples = match[2].match(/\((?:[^'()]|'(?:''|[^'])*')+\)/g) || []
    for (const tuple of tuples) {
      const tokens = tuple.slice(1, -1).match(/'(?:''|[^'])*'|[^,]+/g)
      const values = tokens.map((token) => {
        token = token.trim()
        if (token.startsWith("'")) return token.slice(1, -1).replace(/''/g, "'")
        if (token === 'NULL') return null
        return /^\d+$/.test(token) ? Number(token) : token
      })
      assert.equal(values.length, columns.length, `${table}: malformed value tuple`)
      result.push(Object.fromEntries(columns.map((column, i) => [column, values[i]])))
    }
  }
  return result
}

const sql = fs.readFileSync(path.join(repoRoot, 'doc/sql/data_init.sql'), 'utf8')
const menus = readRows(sql, 'sys_menu')
const buttons = readRows(sql, 'sys_button')
const byName = new Map(menus.map((menu) => [menu.route_name, menu]))
assert.equal(byName.size, menus.length, 'Duplicate menu route name')
assert.equal(new Set(menus.map((menu) => menu.id)).size, menus.length, 'Duplicate menu ID')
const buttonKeys = buttons.map((button) => `${button.menu_id}:${button.auth_remark}`)
assert.equal(new Set(buttonKeys).size, buttons.length, 'Duplicate button within a menu')
let routeCount = 0
let buttonCount = 1 // Backend user:read authority is also seeded.

function visit(routes, parent = null) {
  for (const [i, route] of routes.entries()) {
    routeCount++
    const menu = byName.get(route.name)
    assert.ok(menu, `Missing menu: ${route.name}`)
    const meta = route.meta || {}
    const expected = {
      parent_id: parent ? byName.get(parent).id : null,
      path: route.path,
      component: route.component,
      redirect: route.redirect ?? null,
      title: meta.title,
      icon: meta.icon ?? null,
      sort_no: (i + 1) * 10,
      keep_alive: Number(!!meta.keepAlive),
      visible: Number(!meta.isHide),
      hide_tab: Number(!!meta.isHideTab),
      full_page: Number(!!meta.isFullPage),
      external_link: meta.link ?? null,
      iframe_flag: Number(!!meta.isIframe),
      active_path: meta.activePath ?? null,
      status: 'ENABLED'
    }
    for (const [key, value] of Object.entries(expected)) {
      assert.equal(menu[key], value, `${route.name}.${key}`)
    }
    for (const button of meta.authList || []) {
      buttonCount++
      assert.ok(
        buttons.some(
          (item) =>
            item.menu_id === menu.id &&
            item.auth_remark === button.authMark &&
            item.button_name === button.title
        ),
        `Missing button: ${route.name}.${button.authMark}`
      )
    }
    if (route.children) visit(route.children, route.name)
  }
}

visit(load(path.join(uiRoot, 'src/router/modules/index.ts')).routeModules)
assert.equal(routeCount, menus.length, 'Seed contains extra menus')
assert.equal(buttonCount, buttons.length, 'Seed contains extra buttons')
assert.match(
  sql,
  /FROM sys_menu m CROSS JOIN sys_role r\s+WHERE r\.role_code = 'R_SUPER' AND m\.status = 'ENABLED'/
)
assert.match(
  sql,
  /FROM sys_button b CROSS JOIN sys_role r\s+WHERE r\.role_code = 'R_SUPER' AND b\.status = 'ENABLED'/
)
console.log(
  `Menu seed verified: ${routeCount} menus, ${buttonCount} button permissions, all granted to R_SUPER.`
)
