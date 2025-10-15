@echo off
echo ========================================
echo   Starting Spring Boot in DEBUG mode
echo   Debug Port: 5005
echo ========================================
echo.
echo After application starts, attach debugger to localhost:5005
echo.
call gradlew.bat bootRun --debug-jvm
