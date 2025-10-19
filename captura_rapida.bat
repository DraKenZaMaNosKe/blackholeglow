@echo off
REM =========================================================================
REM Captura Rápida - Sin reinstalar app
REM Solo captura la pantalla actual del celular
REM =========================================================================

setlocal enabledelayedexpansion

set "ADB=C:\Users\eduar\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "IMG_DIR=D:\img"

REM Generar timestamp único
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%

echo [96m📸 Captura Rapida - Black Hole Glow[0m
echo.

REM Verificar dispositivo
"%ADB%" devices | findstr /r "device$" >nul
if errorlevel 1 (
    echo [91mERROR: Sin dispositivo conectado[0m
    pause
    exit /b 1
)

REM Crear carpeta si no existe
if not exist "%IMG_DIR%" mkdir "%IMG_DIR%"

REM Captura única
echo [93mCapturando...[0m
"%ADB%" shell screencap -p /sdcard/screenshot_temp.png
"%ADB%" pull /sdcard/screenshot_temp.png "%IMG_DIR%\blackhole_%TIMESTAMP%.png"
"%ADB%" shell rm /sdcard/screenshot_temp.png

echo [92m✓ Guardado en: D:\img\blackhole_%TIMESTAMP%.png[0m
echo.

REM Abrir la imagen automáticamente
start "" "%IMG_DIR%\blackhole_%TIMESTAMP%.png"

echo [96mPuedes decirle a Claude:[0m
echo "La captura esta en D:\img\blackhole_%TIMESTAMP%.png"
echo.
pause
