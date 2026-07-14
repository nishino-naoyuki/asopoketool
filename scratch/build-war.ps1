$ErrorActionPreference = "Stop"

$ProjectDir = "C:\repo\asopoketool"
$LocalEnvDir = Join-Path $ProjectDir ".local-env"
$JdkDir = Join-Path $LocalEnvDir "jdk"
$MavenDir = Join-Path $LocalEnvDir "maven"

$subdirs = Get-ChildItem -Path $JdkDir -Directory
$JdkTarget = $subdirs[0].FullName
$MavenTarget = Join-Path $MavenDir "apache-maven-3.9.5"

$env:JAVA_HOME = $JdkTarget
$env:M2_HOME = $MavenTarget
$env:PATH = "$JdkTarget\bin;$MavenTarget\bin;" + $env:PATH

cd $ProjectDir

java -version
mvn -version

mvn clean package -DskipTests