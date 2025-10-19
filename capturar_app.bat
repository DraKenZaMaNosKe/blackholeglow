@echo off
REM =========================================================================
REM Script de Captura Automatizada para Black Hole Glow
REM Autor: Claude Code
REM Fecha: 2025-10-19
REM
REM Este script:
REM 1. Verifica conexión ADB
REM 2. Instala y lanza la app en el celular
REM 3. Captura screenshots automáticamente
REM 4. Los guarda en D:\img\ con timestamps
REM 5. Limpia archivos temporales del celular
REM =========================================================================

setlocal enabledelayedexpansion

REM Configuración de rutas
set "ADB=C:\Users\eduar\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "APK_PATH=%~dp0app\build\outputs\apk\debug\app-debug.apk"
set "IMG_DIR=D:\img"
set "PACKAGE=com.secret.blackholeglow"
set "ACTIVITY=%PACKAGE%/.SplashActivity"

REM Colores para mensajes (usando caracteres ANSI)
echo [92m========================================[0m
echo [96m  Black Hole Glow - Captura Auto[0m
echo [92m========================================[0m
echo.

REM Crear carpeta de destino si no existe
if not exist "%IMG_DIR%" (
    echo [93m[INFO] Creando carpeta D:\img\...[0m
    mkdir "%IMG_DIR%"
)

REM Verificar conexión ADB
echo [93m[1/6] Verificando dispositivo...[0m
"%ADB%" devices | findstr /r "device$" >nul
if errorlevel 1 (
    echo [91m[ERROR] No hay dispositivo conectado via ADB[0m
    echo [91mConecta tu celular con USB debugging habilitado[0m
    pause
    exit /b 1
)
echo [92m[OK] Dispositivo conectado[0m
echo.

REM Verificar si el APK existe
if not exist "%APK_PATH%" (
    echo [93m[AVISO] APK no encontrado, omitiendo instalacion...[0m
    goto :skip_install
)

REM Instalar APK actualizado
echo [93m[2/6] Instalando APK actualizado...[0m
"%ADB%" install -r "%APK_PATH%"
if errorlevel 1 (
    echo [91m[ERROR] Fallo la instalacion[0m
    pause
    exit /b 1
)
echo [92m[OK] App instalada[0m
echo.

:skip_install

REM Lanzar la app
echo [93m[3/6] Lanzando Black Hole Glow...[0m
"%ADB%" shell am start -n %ACTIVITY%
timeout /t 3 /nobreak >nul
echo [92m[OK] App ejecutandose[0m
echo.

REM Preguntar cuántas capturas quiere
echo [96m¿Cuantas capturas quieres tomar?[0m
echo [96m(Puedes tomar varias con intervalos de 2 segundos)[0m
set /p NUM_CAPTURAS="Numero de capturas (1-10): "

REM Validar entrada
if "%NUM_CAPTURAS%"=="" set NUM_CAPTURAS=1
if %NUM_CAPTURAS% LSS 1 set NUM_CAPTURAS=1
if %NUM_CAPTURAS% GTR 10 set NUM_CAPTURAS=10

echo.
echo [93m[4/6] Capturando %NUM_CAPTURAS% screenshot(s)...[0m

REM Generar timestamp base
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set datetime=%%I
set TIMESTAMP=%datetime:~0,8%_%datetime:~8,6%

REM Tomar capturas en loop
for /l %%i in (1,1,%NUM_CAPTURAS%) do (
    echo [96m  Captura %%i de %NUM_CAPTURAS%...[0m

    REM Capturar en el celular
    "%ADB%" shell screencap -p /sdcard/screenshot_%%i.png

    REM Descargar a PC con timestamp
    "%ADB%" pull /sdcard/screenshot_%%i.png "%IMG_DIR%\blackhole_%TIMESTAMP%_%%i.png"

    REM Limpiar del celular
    "%ADB%" shell rm /sdcard/screenshot_%%i.png

    REM Esperar 2 segundos antes de la siguiente (si no es la última)
    if %%i LSS %NUM_CAPTURAS% (
        timeout /t 2 /nobreak >nul
    )
)

echo [92m[OK] Capturas completadas[0m
echo.

REM Mostrar archivos generados
echo [93m[5/6] Archivos generados:[0m
dir /b "%IMG_DIR%\blackhole_%TIMESTAMP%*.png"
echo.

REM Abrir carpeta en explorador
echo [93m[6/6] Abriendo carpeta D:\img\...[0m
explorer "%IMG_DIR%"

echo.
echo [92m========================================[0m
echo [92m  ¡Proceso completado exitosamente![0m
echo [92m========================================[0m
echo.
echo [96mLas imagenes estan en: D:\img\[0m
echo [96mFormato: blackhole_YYYYMMDD_HHMMSS_N.png[0m
echo.
echo [93mAhora puedes decirle a Claude:[0m
echo [96m"Las capturas estan en D:\img\blackhole_%TIMESTAMP%_*.png"[0m
echo.

pause
