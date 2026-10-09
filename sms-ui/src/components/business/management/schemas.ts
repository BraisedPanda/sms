export interface ManagementField {
  key: string
  label: string
  type?: 'number' | 'boolean' | 'password' | 'textarea' | 'select'
  required?: boolean
  default?: unknown
  options?: { label: string; value: string }[]
  related?: string
}
export interface ManagementSchema {
  title: string
  permission: string
  fields: ManagementField[]
  columns: { key: string; label: string }[]
  readonly?: boolean
}
const status: ManagementField = {
  key: 'status',
  label: '状态',
  type: 'select',
  default: 'ENABLED',
  options: [
    { label: '启用', value: 'ENABLED' },
    { label: '禁用', value: 'DISABLED' }
  ]
}
const columns = (...pairs: string[][]) => pairs.map(([key, label]) => ({ key, label }))
export const managementSchemas: Record<string, ManagementSchema> = {
  users: {
    title: '用户管理',
    permission: 'system:user',
    fields: [
      { key: 'username', label: '用户名', required: true },
      { key: 'nickname', label: '昵称' },
      { key: 'realName', label: '姓名' },
      { key: 'email', label: '邮箱' },
      { key: 'phone', label: '电话' },
      { key: 'avatar', label: '头像地址' },
      { key: 'password', label: '密码（编辑时留空表示不修改）', type: 'password' },
      status
    ],
    columns: columns(
      ['username', '用户名'],
      ['nickname', '昵称'],
      ['email', '邮箱'],
      ['phone', '电话'],
      ['status', '状态'],
      ['lastLoginTime', '最近登录']
    )
  },
  roles: {
    title: '角色管理',
    permission: 'system:role',
    fields: [
      { key: 'roleCode', label: '角色编码', required: true },
      { key: 'roleName', label: '角色名称', required: true },
      { key: 'description', label: '说明', type: 'textarea' },
      status
    ],
    columns: columns(
      ['roleCode', '角色编码'],
      ['roleName', '角色名称'],
      ['description', '说明'],
      ['status', '状态']
    )
  },
  menus: {
    title: '菜单管理',
    permission: 'system:menu',
    fields: [
      { key: 'parentId', label: '父菜单（留空为根菜单）', type: 'select', related: 'menus' },
      { key: 'title', label: '标题或语言键', required: true },
      { key: 'routeName', label: '路由名称' },
      { key: 'path', label: '路径' },
      { key: 'component', label: '页面组件' },
      { key: 'redirect', label: '重定向' },
      { key: 'icon', label: '图标' },
      { key: 'sortNo', label: '排序', type: 'number', default: 0 },
      { key: 'keepAlive', label: '缓存页面', type: 'boolean', default: false },
      { key: 'visible', label: '显示菜单', type: 'boolean', default: true },
      { key: 'hideTab', label: '隐藏标签', type: 'boolean', default: false },
      { key: 'fullPage', label: '全屏页面', type: 'boolean', default: false },
      { key: 'externalLink', label: '外链地址' },
      { key: 'iframeFlag', label: '内嵌页面', type: 'boolean', default: false },
      { key: 'activePath', label: '高亮菜单路径' },
      status
    ],
    columns: columns(
      ['title', '标题'],
      ['routeName', '路由名称'],
      ['path', '路径'],
      ['component', '组件'],
      ['sortNo', '排序'],
      ['status', '状态']
    )
  },
  buttons: {
    title: '按钮权限管理',
    permission: 'system:button',
    fields: [
      { key: 'menuId', label: '所属菜单', type: 'select', related: 'menus', required: true },
      { key: 'buttonName', label: '按钮名称', required: true },
      { key: 'authRemark', label: '授权标识', required: true },
      { key: 'description', label: '说明', type: 'textarea' },
      { key: 'sortNo', label: '排序', type: 'number', default: 0 },
      status
    ],
    columns: columns(
      ['buttonName', '名称'],
      ['authRemark', '授权标识'],
      ['menuId', '菜单 ID'],
      ['description', '说明'],
      ['status', '状态']
    )
  },
  sessions: {
    title: '设备会话',
    permission: 'system:session',
    fields: [],
    readonly: true,
    columns: columns(
      ['userId', '用户 ID'],
      ['deviceType', '设备类型'],
      ['userAgent', '浏览器/设备'],
      ['loginIp', '登录 IP'],
      ['loginTime', '登录时间'],
      ['expireTime', '访问令牌到期'],
      ['status', '状态']
    )
  },
  bases: {
    title: '知识库管理',
    permission: 'knowledge:base',
    fields: [
      { key: 'name', label: '名称', required: true },
      { key: 'description', label: '说明', type: 'textarea' },
      { key: 'embeddingModelAlias', label: 'Embedding 模型别名' },
      { key: 'chunkSize', label: '分块字符数', type: 'number', default: 800 },
      { key: 'chunkOverlap', label: '重叠字符数', type: 'number', default: 120 },
      {
        key: 'splitStrategy',
        label: '切分策略',
        type: 'select',
        default: 'PARAGRAPH',
        options: [
          { label: '按段落优先', value: 'PARAGRAPH' },
          { label: '按固定字符数', value: 'FIXED' }
        ]
      },
      { key: 'topk', label: '检索条数', type: 'number', default: 5 },
      { key: 'similarityThreshold', label: '相似度阈值', type: 'number', default: 0 },
      { key: 'enabled', label: '启用', type: 'boolean', default: true }
    ],
    columns: columns(
      ['name', '名称'],
      ['description', '说明'],
      ['chunkSize', '分块大小'],
      ['splitStrategy', '策略'],
      ['enabled', '启用']
    )
  },
  documents: {
    title: '文档管理',
    permission: 'knowledge:document',
    fields: [
      { key: 'documentName', label: '文档名称', required: true },
      { key: 'category', label: '分类' },
      { key: 'keywords', label: '关键词' }
    ],
    columns: columns(
      ['documentName', '文档名称'],
      ['documentNo', '文档编号'],
      ['sourceType', '来源类型'],
      ['version', '当前文档版本'],
      ['category', '分类']
    )
  },
  versions: {
    title: '文档版本',
    permission: 'knowledge:document',
    fields: [],
    readonly: true,
    columns: columns(
      ['version', '版本'],
      ['mimeType', '格式'],
      ['chunkCount', '分块数'],
      ['indexStatus', '索引状态'],
      ['activeIndexRevision', '生效索引版本'],
      ['errorMessage', '错误']
    )
  },
  jobs: {
    title: '入库任务',
    permission: 'knowledge:ingestion',
    fields: [],
    readonly: true,
    columns: columns(
      ['documentVersionId', '文档版本 ID'],
      ['indexRevision', '索引版本'],
      ['status', '状态'],
      ['progress', '进度 %'],
      ['retryCount', '重试次数'],
      ['errorMessage', '错误']
    )
  },
  chunks: {
    title: '文档分块',
    permission: 'knowledge:document',
    fields: [],
    readonly: true,
    columns: columns(['chunkNo', '序号'], ['content', '内容'], ['contentType', '类型'])
  },
  runs: {
    title: 'AI 任务',
    permission: 'ai:run',
    fields: [],
    readonly: true,
    columns: columns(
      ['runId', 'Run ID'],
      ['userId', '用户'],
      ['question', '问题'],
      ['status', '状态'],
      ['currentStepNo', '当前步骤'],
      ['startTime', '开始时间'],
      ['errorMessage', '错误']
    )
  },
  steps: {
    title: '任务步骤',
    permission: 'ai:run',
    fields: [],
    readonly: true,
    columns: columns(
      ['stepNo', '步骤'],
      ['stepType', '类型'],
      ['toolName', '工具'],
      ['status', '状态'],
      ['attempt', '尝试次数'],
      ['errorMessage', '错误']
    )
  },
  logs: {
    title: '工具执行日志',
    permission: 'ai:log',
    fields: [],
    readonly: true,
    columns: columns(
      ['requestId', '请求 ID'],
      ['domain', '领域'],
      ['toolName', '工具'],
      ['success', '成功'],
      ['durationTime', '耗时 ms'],
      ['errorMessage', '错误']
    )
  }
}
