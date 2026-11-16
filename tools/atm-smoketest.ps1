<#
.SYNOPSIS
    Headless modpack smoke test: unpacks an ATM10/ATM11 server pack, drops the current Astral Vigil jar in,
    starts the server, waits for "Done" (or a crash/timeout) and writes a report to docs/compat/.

.EXAMPLE
    .\tools\atm-smoketest.ps1 -ServerPack C:\Downloads\ServerFiles-ATM10.zip -Target 1.21.1-neoforge
    .\tools\atm-smoketest.ps1 -ServerPack C:\Downloads\ServerFiles-ATM11.zip -Target 26.1-neoforge -TimeoutMinutes 20
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string] $ServerPack,
    [Parameter(Mandatory)] [ValidateSet('1.21.1-neoforge', '26.1-neoforge')] [string] $Target,
    [int] $TimeoutMinutes = 15,
    [switch] $SkipBuild,
    [switch] $Clean
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$packName = if ($Target -like '1.21.1*') { 'atm10' } else { 'atm11' }
$runDir = Join-Path $repo (Join-Path 'run' "atm-$Target")
$reportDir = Join-Path $repo (Join-Path 'docs' 'compat')
$javaHome = if ($Target -like '1.21.1*') { 21 } else { 25 }

if (-not (Test-Path -LiteralPath $ServerPack)) { throw "Server pack not found: $ServerPack" }

# 1. Build the mod jar
if (-not $SkipBuild) {
    Push-Location $repo
    try { & (Join-Path $repo 'gradlew.bat') ":${Target}:build" -x test; if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed' } }
    finally { Pop-Location }
}
$libs = Join-Path $repo (Join-Path 'versions' (Join-Path $Target (Join-Path 'build' 'libs')))
$jar = Get-ChildItem -LiteralPath $libs -Filter 'astralvigil-*.jar' |
    Where-Object { $_.Name -notmatch 'sources' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "No mod jar in $libs" }

# 2. Unpack the server pack (once, or again with -Clean)
if ($Clean -and (Test-Path -LiteralPath $runDir)) { Remove-Item -LiteralPath $runDir -Recurse -Force -Confirm:$false }
if (-not (Test-Path -LiteralPath $runDir)) {
    New-Item -ItemType Directory -Force -Path $runDir | Out-Null
    Expand-Archive -LiteralPath $ServerPack -DestinationPath $runDir -Force
    # Many packs nest everything in one top-level folder -> flatten
    $children = Get-ChildItem -LiteralPath $runDir
    if ($children.Count -eq 1 -and $children[0].PSIsContainer) {
        Get-ChildItem -LiteralPath $children[0].FullName -Force | Move-Item -Destination $runDir
        Remove-Item -LiteralPath $children[0].FullName -Recurse -Force
    }
    Set-Content -LiteralPath (Join-Path $runDir 'eula.txt') -Value 'eula=true' -Encoding ascii
}

# 3. Drop the current jar in (replace older Astral Vigil jars)
$mods = Join-Path $runDir 'mods'
New-Item -ItemType Directory -Force -Path $mods | Out-Null
Get-ChildItem -LiteralPath $mods -Filter 'astralvigil-*.jar' | Remove-Item -Force -Confirm:$false
Copy-Item -LiteralPath $jar.FullName -Destination $mods

# 4. Find the start mechanism. Packs ship startserver.bat or a NeoForge installer; run installer if needed.
$java = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter "jdk-$javaHome*" |
    Select-Object -First 1 | ForEach-Object { Join-Path $_.FullName (Join-Path 'bin' 'java.exe') }
if (-not $java) { $java = 'java' }

$argsFile = Get-ChildItem -LiteralPath (Join-Path $runDir 'libraries') -Recurse -Filter 'win_args.txt' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $argsFile) {
    $installer = Get-ChildItem -LiteralPath $runDir -Filter 'neoforge-*-installer.jar' | Select-Object -First 1
    if (-not $installer) { throw 'Neither libraries/**/win_args.txt nor a neoforge installer found in the pack.' }
    Push-Location $runDir
    try { & $java -jar $installer.FullName --install-server | Out-Null } finally { Pop-Location }
    $argsFile = Get-ChildItem -LiteralPath (Join-Path $runDir 'libraries') -Recurse -Filter 'win_args.txt' | Select-Object -First 1
}
$rel = $argsFile.FullName.Substring((Resolve-Path -LiteralPath $runDir).Path.TrimEnd('\').Length + 1)  # PS 5.1 has no GetRelativePath
if (-not (Test-Path -LiteralPath (Join-Path $runDir 'user_jvm_args.txt'))) {
    Set-Content -LiteralPath (Join-Path $runDir 'user_jvm_args.txt') -Value '-Xmx8G' -Encoding ascii
}

# 5. Start headless and watch the log
$log = Join-Path $runDir 'smoketest-console.log'
$err = Join-Path $runDir 'smoketest-console.err.log'
$proc = Start-Process -FilePath $java -WorkingDirectory $runDir -PassThru -NoNewWindow `
    -ArgumentList '@user_jvm_args.txt', "@$rel", 'nogui' -RedirectStandardOutput $log -RedirectStandardError $err `
    -RedirectStandardInput (New-Item -ItemType File -Force -Path (Join-Path $runDir 'smoketest-stdin.txt')).FullName
$deadline = (Get-Date).AddMinutes($TimeoutMinutes)
$status = 'TIMEOUT'
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    if ($proc.HasExited) { $status = 'EXITED'; break }
    if ((Test-Path -LiteralPath $log) -and (Select-String -LiteralPath $log -Pattern 'Done \(\d+[\.,]\d+s\)! For help' -Quiet)) {
        $status = 'STARTED'; break
    }
}
if (-not $proc.HasExited) {
    Add-Content -LiteralPath (Join-Path $runDir 'smoketest-stdin.txt') -Value 'stop'
    Start-Sleep -Seconds 20
    if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -Confirm:$false }
}

# 6. Analyse
$latest = Join-Path $runDir (Join-Path 'logs' 'latest.log')
$source = if (Test-Path -LiteralPath $latest) { $latest } else { $log }
$patterns = [ordered]@{
    'Crash'             = 'Crash report saved|---- Minecraft Crash Report ----|Exception in server tick loop'
    'Mixin'             = 'Mixin apply .*failed|MixinApplyError|InvalidMixinException|InjectionError'
    'Registry conflict' = 'Registry .* (duplicate|already registered)|Duplicate (key|registration)|IllegalStateException: Registry'
    'Missing texture'   = 'Missing texture|Unable to load model|Missing model'
    'Astral Vigil'      = 'astralvigil.*(ERROR|WARN|Exception)|(ERROR|WARN).*astralvigil'
}
$findings = foreach ($k in $patterns.Keys) {
    $hits = Select-String -LiteralPath $source -Pattern $patterns[$k] -ErrorAction SilentlyContinue
    [pscustomobject]@{ Category = $k; Count = @($hits).Count; Samples = @($hits | Select-Object -First 5 | ForEach-Object { $_.Line.Trim() }) }
}
$crashReports = Get-ChildItem -LiteralPath (Join-Path $runDir 'crash-reports') -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
$stamp = Get-Date -Format 'yyyy-MM-dd_HHmm'
$report = Join-Path $reportDir "$packName-$stamp.md"
$lines = @(
    "# $($packName.ToUpper()) smoke test – $stamp", '',
    "- Target: ``$Target``", "- Server pack: ``$(Split-Path -Leaf $ServerPack)``", "- Mod jar: ``$($jar.Name)``",
    "- Result: **$status**", "- Crash reports: $(@($crashReports).Count)", '',
    '| Category | Hits |', '|---|---|'
)
$lines += $findings | ForEach-Object { "| $($_.Category) | $($_.Count) |" }
foreach ($f in $findings | Where-Object Count -gt 0) {
    $lines += '', "## $($f.Category)", '', '```'
    $lines += $f.Samples
    $lines += '```'
}
Set-Content -LiteralPath $report -Value $lines -Encoding utf8
Write-Host "Report: $report ($status)"
if ($status -ne 'STARTED' -or ($findings | Where-Object { $_.Category -in 'Crash', 'Mixin', 'Registry conflict' -and $_.Count -gt 0 })) { exit 1 }
