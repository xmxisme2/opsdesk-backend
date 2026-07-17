<#
.SYNOPSIS
  安全初始化 OpsDesk 本地数据库。

.DESCRIPTION
  通过 mysql 客户端的 source 命令执行 SQL 文件，不把 SQL 内容写入 PowerShell 管道。
  这样可以避免中文种子数据在进入 mysql 客户端前被替换成问号。
#>
param(
    [string]$Mysql = "mysql",
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "root123456",
    [switch]$SchemaOnly,
    [switch]$SeedOnly
)

$ErrorActionPreference = "Stop"
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-OpsDeskSqlFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SqlFile
    )

    $resolvedPath = (Resolve-Path -LiteralPath $SqlFile).Path.Replace("\", "/")
    $arguments = @(
        "--default-character-set=utf8mb4",
        "--host=$HostName",
        "--port=$Port",
        "--user=$User",
        "--execute=source $resolvedPath"
    )

    if ($Password -ne "") {
        $arguments = @(
            "--default-character-set=utf8mb4",
            "--host=$HostName",
            "--port=$Port",
            "--user=$User",
            "--password=$Password",
            "--execute=source $resolvedPath"
        )
    }

    & $Mysql @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "SQL execution failed: $resolvedPath"
    }
}

if (-not $SeedOnly) {
    Invoke-OpsDeskSqlFile -SqlFile (Join-Path $scriptRoot "01_schema.sql")
    Invoke-OpsDeskSqlFile -SqlFile (Join-Path $scriptRoot "03_migrate_ticket_no_nullable.sql")
    Invoke-OpsDeskSqlFile -SqlFile (Join-Path $scriptRoot "04_migrate_attachment_temp_token.sql")
    Invoke-OpsDeskSqlFile -SqlFile (Join-Path $scriptRoot "05_migrate_ticket_category_active_unique.sql")
    Invoke-OpsDeskSqlFile -SqlFile (Join-Path $scriptRoot "06_migrate_knowledge_active_unique.sql")
}

if (-not $SchemaOnly) {
    Invoke-OpsDeskSqlFile -SqlFile (Join-Path $scriptRoot "02_seed.sql")
    Invoke-OpsDeskSqlFile -SqlFile (Join-Path $scriptRoot "verify-seed-utf8.sql")
}

Write-Host "OpsDesk local database initialization completed."
