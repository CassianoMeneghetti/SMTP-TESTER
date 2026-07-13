$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$buildDir = Join-Path $projectRoot "build"
$classesDir = Join-Path $buildDir "classes"
$distDir = Join-Path $projectRoot "dist"
$manifestFile = Join-Path $buildDir "MANIFEST.MF"
$jarFile = Join-Path $distDir "smtp-tester-pro.jar"
$sources = Get-ChildItem -Path (Join-Path $projectRoot "src/main/java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if (-not $sources) {
    throw "Nenhum arquivo Java encontrado em src/main/java."
}

Remove-Item -Recurse -Force -Path $buildDir, $distDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classesDir, $distDir | Out-Null

javac -d $classesDir $sources

@"
Manifest-Version: 1.0
Main-Class: br.com.smtptesterpro.Main

"@ | Set-Content -Path $manifestFile -Encoding ascii

jar cfm $jarFile $manifestFile -C $classesDir .

@"
@echo off
setlocal
java -jar "%~dp0smtp-tester-pro.jar"
"@ | Set-Content -Path (Join-Path $distDir "SMTP Tester Pro.bat") -Encoding ascii

@"
`$ErrorActionPreference = "Stop"
java -jar "`$PSScriptRoot\smtp-tester-pro.jar"
"@ | Set-Content -Path (Join-Path $distDir "SMTP Tester Pro.ps1") -Encoding ascii

Write-Host "Distribuicao gerada em: $distDir"
Write-Host "Arquivo principal: $jarFile"
