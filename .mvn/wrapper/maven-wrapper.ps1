[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArguments
)

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$wrapperJar = ".mvn\wrapper\maven-wrapper.jar"

if (-not (Test-Path -LiteralPath (Join-Path $projectRoot $wrapperJar))) {
    throw "Maven Wrapper JAR was not found: $wrapperJar"
}

$javaExecutable = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    (Get-Command java -ErrorAction Stop).Source
}

Push-Location $projectRoot
try {
    & $javaExecutable "-Dmaven.multiModuleProjectDirectory=$projectRoot" `
        "-classpath" $wrapperJar `
        "org.apache.maven.wrapper.MavenWrapperMain" @MavenArguments
} finally {
    Pop-Location
}

exit $LASTEXITCODE
