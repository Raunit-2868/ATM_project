@echo off
setlocal EnableDelayedExpansion

set "PROJECT_ROOT=%~dp0"
set "LIB_DIR=%PROJECT_ROOT%lib"
set "BIN_DIR=%PROJECT_ROOT%bin"
set "SRC_DIR=%PROJECT_ROOT%src\main\java"
set "RES_DIR=%PROJECT_ROOT%src\main\resources"
set "SQLITE_JAR=%LIB_DIR%\sqlite-jdbc-3.45.3.0.jar"
set "SLF4J_JAR=%LIB_DIR%\slf4j-api-1.7.36.jar"

echo ================================================
echo   SecureATM Build ^& Run System (Windows)
echo ================================================
echo.

:: 1. Create directories
if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

:: 2. Download dependencies if missing
if not exist "%SQLITE_JAR%" (
    echo [1/5] Downloading SQLite JDBC driver...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar' -OutFile '%SQLITE_JAR%' -UseBasicParsing"
    if not exist "%SQLITE_JAR%" (
        echo ERROR: Failed to download SQLite JDBC. Check your internet connection.
        pause
        exit /b 1
    )
) else (
    echo [1/5] SQLite JDBC: OK
)

if not exist "%SLF4J_JAR%" (
    echo       Downloading SLF4J API...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar' -OutFile '%SLF4J_JAR%' -UseBasicParsing"
)

:: 3. Clean bin
echo [2/5] Cleaning previous build...
if exist "%BIN_DIR%" rmdir /s /q "%BIN_DIR%"
mkdir "%BIN_DIR%"

:: 4. Collect all .java sources using a temp PowerShell script
::    javac @argfile treats backslashes as escape chars — use forward slashes instead
echo [3/5] Collecting source files...
set "PS_SCRIPT=%PROJECT_ROOT%_collect.ps1"
set "SOURCES_FILE=%PROJECT_ROOT%sources.txt"

(
echo $files = Get-ChildItem -Path '%SRC_DIR%' -Filter '*.java' -Recurse
echo $lines = $files ^| ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
echo [System.IO.File]::WriteAllLines('%SOURCES_FILE%', $lines, [System.Text.Encoding]::ASCII^)
) > "%PS_SCRIPT%"

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%"
del /q "%PS_SCRIPT%" 2>nul

if not exist "%SOURCES_FILE%" (
    echo ERROR: Could not generate sources list.
    pause
    exit /b 1
)

for /f %%A in ('find /c /v "" ^< "%SOURCES_FILE%"') do set SRC_COUNT=%%A
if "%SRC_COUNT%"=="0" (
    echo ERROR: No .java files found under %SRC_DIR%
    pause
    exit /b 1
)
echo       Found %SRC_COUNT% source file(s).

:: 5. Compile
echo [4/5] Compiling...
javac -encoding UTF-8 -d "%BIN_DIR%" -cp "%SQLITE_JAR%;%SLF4J_JAR%" @"%SOURCES_FILE%"
if %errorlevel% neq 0 (
    echo.
    echo ================================================
    echo   COMPILATION FAILED -- see errors above
    echo ================================================
    del /q "%SOURCES_FILE%" 2>nul
    pause
    exit /b 1
)
del /q "%SOURCES_FILE%" 2>nul

:: 6. Copy resources (logos, .properties)
echo [5/5] Copying resources...
if exist "%RES_DIR%" (
    xcopy /e /y /q "%RES_DIR%\*" "%BIN_DIR%\" >nul 2>&1
)

:: 7. Launch application
echo.
echo ================================================
echo   Launching SecureATM...
echo ================================================
java -Dfile.encoding=UTF-8 -cp "%BIN_DIR%;%SQLITE_JAR%;%SLF4J_JAR%" com.atm.Main

endlocal
