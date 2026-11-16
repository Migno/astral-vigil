<#
.SYNOPSIS
    Runs an optional BlockPilot script, takes a screenshot and copies it to docs/screenshots/<Phase>/<Name>.png.
.EXAMPLE
    .\tools\bp-shot.ps1 -Target 26.1-neoforge -Phase phase0 -Name title
    .\tools\bp-shot.ps1 -Target 1.21.1-neoforge -Phase phase1 -Name shard -Before "select astralvigil:astral_shard"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [ValidateSet('1.21.1-neoforge', '26.1-neoforge')] [string] $Target,
    [Parameter(Mandatory)] [string] $Phase,
    [Parameter(Mandatory)] [string] $Name,
    [string] $Before,
    [string] $Run = 'client',
    [int] $Width = 1280,
    [string] $Options = ''
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$bp = Join-Path $repo (Join-Path 'run' (Join-Path $Target (Join-Path $Run (Join-Path 'blockpilot' 'blockpilot.cmd'))))
if (-not (Test-Path -LiteralPath $bp)) { throw "BlockPilot launcher not found (start the client first): $bp" }

if ($Before) {
    & $bp $Before
    if ($LASTEXITCODE -ne 0) { throw "BlockPilot script failed: $Before" }
}
$reply = (& $bp "screenshot $Width $Options".Trim()) -join "`n"
$m = [regex]::Match($reply, '^ok (.+?\.png)', 'Multiline')
if (-not $m.Success) { throw "Screenshot failed: $reply" }

$dir = Join-Path $repo (Join-Path 'docs' (Join-Path 'screenshots' $Phase))
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$dest = Join-Path $dir "$Target-$Name.png"
Copy-Item -LiteralPath $m.Groups[1].Value -Destination $dest -Force
Write-Output $dest
