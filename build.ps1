[CmdletBinding()]
param(
    [string]$StarsectorPath = 'C:\Starsector',
    [string]$JdkPath = $env:JAVA_HOME,
    [switch]$Test,
    [switch]$Package
)
$ErrorActionPreference = 'Stop'
if (-not $JdkPath) {
    $portable = Get-ChildItem (Join-Path (Split-Path $PSScriptRoot) 'tools') -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    $JdkPath = if ($portable) { $portable.FullName } else { Join-Path $StarsectorPath 'jre' }
}
$java = Join-Path $JdkPath 'bin\java.exe'
$javac = Join-Path $JdkPath 'bin\javac.exe'
$jar = Join-Path $JdkPath 'bin\jar.exe'
foreach ($tool in @($java, $javac, $jar)) {
    if (-not (Test-Path -LiteralPath $tool)) { throw "Missing JDK tool: $tool" }
}
$dependencies = @((Get-ChildItem (Join-Path $StarsectorPath 'starsector-core') -Filter '*.jar').FullName)
$modFolders = Get-ChildItem (Join-Path $StarsectorPath 'mods') -Directory
foreach ($id in @('lunalib', 'lw_lazylib')) {
    $folders = @($modFolders | Where-Object {
        $info = Join-Path $_.FullName 'mod_info.json'
        (Test-Path -LiteralPath $info) -and ([regex]::Match((Get-Content $info -Raw), '"id"\s*:\s*"([^"]+)"').Groups[1].Value -eq $id)
    })
    if ($folders.Count -ne 1) { throw "Expected one installed $id dependency, found $($folders.Count)." }
    $dependencies += (Get-ChildItem $folders[0].FullName -Recurse -Filter '*.jar').FullName
}
$cp = $dependencies -join ';'
$classes = Join-Path $PSScriptRoot ('build\classes-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources = @((Get-ChildItem (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java').FullName)
& $javac --release 17 -encoding UTF-8 -cp $cp -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
if ($Test) {
    $testClasses = Join-Path $classes 'test-classes'
    New-Item -ItemType Directory -Path $testClasses -Force | Out-Null
    $testSources = @((Get-ChildItem (Join-Path $PSScriptRoot 'tests') -Recurse -Filter '*.java').FullName)
    & $javac --release 17 -encoding UTF-8 -cp "$classes;$cp" -d $testClasses $testSources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & $java '-Djava.awt.headless=true' -cp "$testClasses;$classes;$cp" adjustablesmods.UpgradeRegressionTest
    if ($LASTEXITCODE -ne 0) { throw 'Regression tests failed.' }
}
$builtJar = Join-Path $classes 'AdjustableSMods.jar'
& $jar "-J-Djava.io.tmpdir=$classes" --create --file $builtJar -C $classes adjustablesmods
if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
Copy-Item -LiteralPath $builtJar -Destination (Join-Path $PSScriptRoot 'AdjustableSMods.jar') -Force
if ($Package) {
    $dist = Join-Path $PSScriptRoot 'dist'
    $stage = Join-Path $dist ('stage-' + [guid]::NewGuid().ToString('N'))
    $mod = Join-Path $stage 'AdjustableSMods'
    New-Item -ItemType Directory -Path $mod -Force | Out-Null
    foreach ($item in @('data', 'mod_info.json', 'AdjustableSMods.jar', 'AdjustableSMods.version', 'README.md', 'CHANGELOG.txt', 'CREDITS.txt', 'LICENSE.txt')) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $item) -Destination $mod -Recurse
    }
    # Stable asset name matches the public Version Checker URL.
    $zip = Join-Path $dist 'AdjustableSMods.zip'
    Compress-Archive -Path $mod -DestinationPath $zip -Force
    $hash = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  AdjustableSMods.zip" | Set-Content ($zip + '.sha256') -Encoding ascii
    Write-Host "Install package: $zip"
}
Write-Host 'Built Adjustable S-Mods for Java 17.'
