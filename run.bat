@echo off
title Bank Management System Server
cls
echo ======================================================================
echo          ApexBank - Bank Management System Launch Script
echo ======================================================================
echo.

if not exist "bin" mkdir bin

echo [1/2] Compiling Java source files...
javac -encoding UTF-8 -cp "lib/*" -d bin src/com/bank/model/*.java src/com/bank/config/*.java src/com/bank/dao/*.java src/com/bank/server/*.java src/com/bank/*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ [ERROR] Compilation failed. Please make sure JDK is installed and in PATH.
    pause
    exit /b %ERRORLEVEL%
)

echo ✅ [1/2] Compilation successful!
echo.
echo [2/2] Launching ApexBank Server on port 8080...
echo.

java -cp "bin;lib/*" com.bank.Main

pause
