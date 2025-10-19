@echo off
REM =========================================================================
REM Grabar Video Corto del Celular
REM Graba 10 segundos y lo guarda en D:\img\
REM =========================================================================

setlocal enabledelayedexpansion

set "ADB=C:\Users\eduar\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "IMG_DIR=D:\img"

REM Generar timestamp
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%

echo [96m🎥 Grabacion de Video - Black Hole Glow[0m
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

REM Preguntar duración
echo [96m¿Cuantos segundos quieres grabar?[0m
set /p DURACION="Duracion (5-30 segundos, default=10): "
if "%DURACION%"=="" set DURACION=10
if %DURACION% LSS 5 set DURACION=5
if %DURACION% GTR 30 set DURACION=30

echo.
echo [93m🔴 Grabando %DURACION% segundos...[0m
echo [93m(La app debe estar abierta en el celular)[0m
echo.

REM Iniciar grabación
"%ADB%" shell screenrecord --time-limit %DURACION% /sdcard/blackhole_recording.mp4

echo [93m⏹️  Grabacion completada, descargando...[0m

REM Descargar video
"%ADB%" pull /sdcard/blackhole_recording.mp4 "%IMG_DIR%\blackhole_%TIMESTAMP%.mp4"

REM Limpiar del celular
"%ADB%" shell rm /sdcard/blackhole_recording.mp4

echo [92m✓ Video guardado en: D:\img\blackhole_%TIMESTAMP%.mp4[0m
echo.

REM Abrir el video automáticamente
start "" "%IMG_DIR%\blackhole_%TIMESTAMP%.mp4"

echo [96mPuedes decirle a Claude:[0m
echo "El video esta en D:\img\blackhole_%TIMESTAMP%.mp4"
echo.
echo [93mNOTA: Puedo ver videos directamente ahora,[0m
echo [93m      no necesitas extraer frames con Python![0m
echo.
pause
