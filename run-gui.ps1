$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = Join-Path $repoRoot ".tools\jdk-25.0.2+10"
$mavenHome = Join-Path $repoRoot ".tools\apache-maven-3.9.14"
$mavenRepo = Join-Path $repoRoot ".m2\repository"
$env:Path = "$($env:JAVA_HOME)\bin;$mavenHome\bin;$env:Path"

New-Item -ItemType Directory -Force -Path $mavenRepo | Out-Null
mvn "-Dmaven.repo.local=$mavenRepo" javafx:run
