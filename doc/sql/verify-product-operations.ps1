param(
    [string]$MySqlExecutable = 'D:\MySQL\bin\mysql.exe',
    [switch]$SkipTests
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$values = @{}
Get-Content -LiteralPath (Join-Path $repo '.env') -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $values[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'") }
}
if ($values.AI_DATASOURCE_URL -notmatch '^jdbc:mysql://([^/:]+)(?::([0-9]+))?/') { throw 'Unsupported verification MySQL URL' }
$dbHost = $Matches[1]
$dbPort = if ($Matches[2]) { $Matches[2] } else { '3306' }
$dbUser = $values.AI_DATASOURCE_USERNAME
$previousPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $values.AI_DATASOURCE_PASSWORD
$suffix = [guid]::NewGuid().ToString('N').Substring(0, 12)
$fresh = "sms_codex_verify_new_$suffix"
$legacy = "sms_codex_verify_old_$suffix"
$schemas = @($fresh, $legacy)
function Invoke-VerifySql([string]$sql, [string]$schema = '') {
    $arguments = @('--protocol=TCP', "--host=$dbHost", "--port=$dbPort", "--user=$dbUser", '--default-character-set=utf8mb4', '--batch', '--skip-column-names')
    if ($schema) {
        if ($schema -notmatch '^sms_codex_verify_[a-z0-9_]+$') { throw 'Refusing a non-verification schema' }
        $arguments += "--database=$schema"
    }
    $sql | & $MySqlExecutable @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Verification SQL failed' }
}
function Assert-Count([string]$schema, [string]$sql, [int]$expected) {
    $actual = Invoke-VerifySql $sql $schema
    if ([int]$actual -ne $expected) { throw "Database assertion failed: expected $expected, got $actual" }
}
try {
    foreach ($schema in $schemas) {
        if ($schema -notmatch '^sms_codex_verify_[a-z0-9_]+$') { throw 'Invalid disposable schema' }
        Invoke-VerifySql "CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    }
    $tableSql = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'table_init.sql') -Raw -Encoding UTF8
    $dataSql = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'data_init.sql') -Raw -Encoding UTF8
    $upgradeSql = Get-Content -LiteralPath (Join-Path $PSScriptRoot '2026-10-09_product_operations.sql') -Raw -Encoding UTF8
    for ($i=0; $i -lt 2; $i++) {
        Invoke-VerifySql $tableSql $fresh
        Invoke-VerifySql $dataSql $fresh
        Invoke-VerifySql $upgradeSql $fresh
    }
    Assert-Count $fresh 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();' 23
    Assert-Count $fresh 'SELECT COUNT(*) FROM sys_menu;' 77
    Assert-Count $fresh 'SELECT COUNT(*) FROM sys_button;' 50
    Assert-Count $fresh 'SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1101;' 77
    Assert-Count $fresh 'SELECT COUNT(*) FROM sys_role_button WHERE role_id=1101;' 50
    # Reconstruct the checked-in baseline, then emulate the pre-tenant/audit schema.
    $baseline = (& git -C $repo show HEAD:doc/sql/table_init.sql) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read baseline schema' }
    $baseline = [regex]::Replace($baseline, '(?s)CREATE TABLE IF NOT EXISTS ai_chat_(?:conversation|message) \(.*?;\s*', '')
    $baseline = [regex]::Replace($baseline, '(?m)^    tenant_id .*\r?\n', '')
    $baseline = [regex]::Replace($baseline, '(?m)^    (?:UNIQUE )?KEY [^\r\n]*tenant[^\r\n]*\r?\n', '')
    $baseline = [regex]::Replace($baseline, ',\s*\)', "`n)")
    $baseline = $baseline.Replace('create_by', 'sys_creator').Replace('modify_by', 'sys_modifier').Replace('create_time', 'sys_create_time').Replace('update_time', 'sys_update_time')
    Invoke-VerifySql $baseline $legacy
    Invoke-VerifySql "INSERT INTO student(id,student_no,name,sys_creator) VALUES(88888,'KEEP-ME','legacy sentinel','legacy-actor'); INSERT INTO ai_knowledge_base(id,name,sys_creator) VALUES(88887,'legacy-base','legacy-actor'); ALTER TABLE sys_button DROP INDEX uk_sys_button_menu_auth, ADD UNIQUE INDEX old_global_auth(auth_remark);" $legacy
    for ($i=0; $i -lt 2; $i++) {
        Invoke-VerifySql $upgradeSql $legacy
        Invoke-VerifySql $dataSql $legacy
    }
    Assert-Count $legacy "SELECT COUNT(*) FROM student WHERE id=88888 AND student_no='KEEP-ME' AND name='legacy sentinel' AND create_by='legacy-actor';" 1
    Assert-Count $legacy "SELECT COUNT(*) FROM ai_knowledge_base WHERE id=88887 AND name='legacy-base' AND tenant_id='default' AND create_by='legacy-actor';" 1
    Assert-Count $legacy "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='sys_button' AND index_name='old_global_auth';" 0
    Assert-Count $legacy 'SELECT COUNT(*) FROM sys_button;' 50
    Write-Output 'New schema and legacy migration: repeated execution, data preservation and R_SUPER grants passed.'
    if (-not $SkipTests) {
        $env:SMS_TEST_MYSQL_URL = "jdbc:mysql://${dbHost}:${dbPort}/${fresh}?useSSL=false&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&serverTimezone=Asia/Shanghai"
        $env:SMS_TEST_MYSQL_USERNAME = $dbUser
        $env:SMS_TEST_MYSQL_PASSWORD = $values.AI_DATASOURCE_PASSWORD
        Push-Location $repo
        try {
            & ./mvnw.cmd -pl sms-web,sms-ai/sms-ai-provider,sms-knowledge/sms-knowledge-provider,sms-system/sms-system-provider -am '-Dtest=AiControllerSecurityTest,AiStreamRecoveryTest,ApiExceptionHandlerTest,RpcDtoSerializationTest,AiTaskRunServiceTest,AiChatCancellationTest,AiSafetyPolicyTest,KnowledgeServiceSecurityTest,DocumentPipelineTest,SystemManagementMySqlTest,KnowledgeQueueMySqlTest,AiChatHistoryMySqlTest,KnowledgeManagementMySqlTest' '-Dsurefire.failIfNoSpecifiedTests=false' test -q
            if ($LASTEXITCODE -ne 0) { throw 'Targeted tests failed' }
        } finally { Pop-Location }
    }
} finally {
    foreach ($schema in $schemas) {
        if ($schema -match '^sms_codex_verify_[a-z0-9_]+$') { Invoke-VerifySql "DROP DATABASE IF EXISTS $schema;" }
    }
    $env:MYSQL_PWD = $previousPassword
    Remove-Item Env:SMS_TEST_MYSQL_URL -ErrorAction SilentlyContinue
    Remove-Item Env:SMS_TEST_MYSQL_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:SMS_TEST_MYSQL_PASSWORD -ErrorAction SilentlyContinue
    Write-Output 'Disposable schemas cleaned; application databases were not changed.'
}
