<#
.SYNOPSIS
    一条命令把本仓库推送到 GitHub（含 v2.2+1.21.11 标签，推送后会触发 Actions 自动构建 + Release）。

.EXAMPLE
    .\push-to-github.ps1 Roldo7_/O123456789-fabric-1.21.11

.EXAMPLE
    .\push-to-github.ps1 https://github.com/Roldo7_/O123456789-fabric-1.21.11.git
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string] $Repo,

    [string] $Branch = 'main',

    [string] $Tag = 'v2.2+1.21.11'
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not (Test-Path '.git')) {
    throw "当前目录不是 git 仓库：$PSScriptRoot"
}

# 允许只写 "用户名/仓库名"
if ($Repo -notmatch '^(https?://|git@)') {
    $Repo = "https://github.com/$($Repo.Trim('/')).git"
}
if ($Repo -notmatch '\.git$') {
    $Repo = "$Repo.git"
}

Write-Host "[1/4] remote origin -> $Repo" -ForegroundColor Cyan
$remotes = git remote
if ($remotes -contains 'origin') {
    git remote set-url origin $Repo
} else {
    git remote add origin $Repo
}

Write-Host "[2/4] 推送分支 $Branch ..." -ForegroundColor Cyan
git push -u origin $Branch
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "推送被拒。通常是新建仓库时勾选了 README / .gitignore / license，" -ForegroundColor Yellow
    Write-Host "导致远程已有一个提交。本仓库是完整移植版本，可以用它覆盖远程。" -ForegroundColor Yellow
    $answer = Read-Host "用本地内容覆盖远程并继续？(y/N)"
    if ($answer -ne 'y') {
        throw "已取消。"
    }
    git push -u origin $Branch --force
    if ($LASTEXITCODE -ne 0) { throw "推送失败。" }
}

Write-Host "[3/4] 推送标签 $Tag（触发构建与 Release）..." -ForegroundColor Cyan
git push origin $Tag
if ($LASTEXITCODE -ne 0) {
    Write-Host "标签推送失败（可能远程已存在同名标签，可忽略）。" -ForegroundColor Yellow
}

# 推导网页地址用于核对
$web = $Repo -replace '^git@github\.com:', 'https://github.com/' -replace '\.git$', ''
Write-Host ""
Write-Host "[4/4] 完成。核对这几处：" -ForegroundColor Green
Write-Host "  代码：   $web"
Write-Host "  Actions：$web/actions   （首次构建约 5-8 分钟，需下载 MC 1.21.11）"
Write-Host "  Release：$web/releases  （出现 v2.2+1.21.11，附件为 O123456789-2.2+1.21.11.jar）"
