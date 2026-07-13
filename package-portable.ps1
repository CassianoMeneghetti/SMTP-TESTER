$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkHome = $env:JAVA_HOME

if (-not $jdkHome -or -not (Test-Path (Join-Path $jdkHome "bin\jpackage.exe"))) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $javaSettings = & java -XshowSettings:properties -version 2>&1
    $ErrorActionPreference = $previousErrorActionPreference
    $javaHomeLine = $javaSettings | Select-String -Pattern "^\s+java.home = " | Select-Object -First 1
    if ($javaHomeLine) {
        $jdkHome = ($javaHomeLine.ToString() -replace "^\s+java.home = ", "").Trim()
    }
}

if (-not $jdkHome -or -not (Test-Path (Join-Path $jdkHome "bin\jpackage.exe"))) {
    $jdkHome = "C:\Program Files\Java\jdk-26.0.1"
}

$javac = Join-Path $jdkHome "bin\javac.exe"
$jar = Join-Path $jdkHome "bin\jar.exe"
$jpackage = Join-Path $jdkHome "bin\jpackage.exe"

if (-not (Test-Path $javac) -or -not (Test-Path $jar) -or -not (Test-Path $jpackage)) {
    throw "JDK completo nao encontrado. Configure JAVA_HOME apontando para um JDK com javac, jar e jpackage."
}

$buildDir = Join-Path $projectRoot "build"
$classesDir = Join-Path $buildDir "classes"
$inputDir = Join-Path $buildDir "input"
$manifestFile = Join-Path $buildDir "MANIFEST.MF"
$jarFile = Join-Path $inputDir "smtp-tester-pro.jar"
$portableDir = Join-Path $projectRoot "dist-portable"
$sources = Get-ChildItem -Path (Join-Path $projectRoot "src/main/java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if (-not $sources) {
    throw "Nenhum arquivo Java encontrado em src/main/java."
}

Remove-Item -Recurse -Force -Path $buildDir, $portableDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classesDir, $inputDir, $portableDir | Out-Null

& $javac -d $classesDir $sources

@"
Manifest-Version: 1.0
Main-Class: br.com.smtptesterpro.Main

"@ | Set-Content -Path $manifestFile -Encoding ascii

& $jar cfm $jarFile $manifestFile -C $classesDir .

& $jpackage `
    --type app-image `
    --name "SMTP Tester Pro" `
    --input $inputDir `
    --main-jar "smtp-tester-pro.jar" `
    --main-class "br.com.smtptesterpro.Main" `
    --dest $portableDir `
    --vendor "SMTP Tester Pro" `
    --app-version "1.0.0" `
    --description "Ferramenta desktop para diagnostico SMTP"

Write-Host "Aplicacao portatil gerada em: $portableDir\SMTP Tester Pro"
Write-Host "Execute: $portableDir\SMTP Tester Pro\SMTP Tester Pro.exe"
