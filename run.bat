@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
set "OUT_DIR=%PROJECT_ROOT%out"
set "SOURCES_FILE=%TEMP%\smtp-tester-pro-sources.txt"

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
dir /s /b "%PROJECT_ROOT%src\main\java\*.java" > "%SOURCES_FILE%"

javac -d "%OUT_DIR%" @"%SOURCES_FILE%"
if errorlevel 1 exit /b %errorlevel%

java -cp "%OUT_DIR%" br.com.smtptesterpro.Main
