@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME (
  for /d %%J in ("%USERPROFILE%\.jdks\ms-17*" "%USERPROFILE%\.jdks\ms-21*") do if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
)
if not exist ".tools\apache-maven-3.9.9\bin\mvn.cmd" (
  powershell -NoProfile -Command "$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force '.tools' | Out-Null; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '.tools/maven.zip'; Expand-Archive '.tools/maven.zip' -DestinationPath '.tools' -Force"
  if errorlevel 1 exit /b 1
)
call ".tools\apache-maven-3.9.9\bin\mvn.cmd" "-Dmaven.repo.local=%~dp0.tools\repository" %*
exit /b %errorlevel%
