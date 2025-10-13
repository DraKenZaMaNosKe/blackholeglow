@echo off
echo ========================================
echo Verificacion de Compatibilidad Gradle
echo ========================================
echo.

echo Verificando Java...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java no encontrado
    echo Configura JAVA_HOME primero
    pause
    exit /b 1
)
echo.

echo Verificando Gradle...
call gradlew.bat --version
if %errorlevel% neq 0 (
    echo ERROR: Gradle Wrapper no funciona
    pause
    exit /b 1
)
echo.

echo Probando compilacion (dry-run)...
call gradlew.bat assembleDebug --dry-run
if %errorlevel% == 0 (
    echo.
    echo ================================================
    echo   COMPATIBILIDAD OK - Seguro hacer push
    echo ================================================
) else (
    echo.
    echo ================================================
    echo   ERROR - NO hacer push, revisar errores
    echo ================================================
)

pause
