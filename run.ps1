$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir = Join-Path $projectRoot "out"
$sources = Get-ChildItem -Path (Join-Path $projectRoot "src/main/java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if (-not $sources) {
    throw "Nenhum arquivo Java encontrado em src/main/java."
}

New-Item -ItemType Directory -Force -Path $outDir | Out-Null
javac -d $outDir $sources
java -cp $outDir br.com.smtptesterpro.Main
