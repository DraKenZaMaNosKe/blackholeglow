@echo off
REM =========================================================================
REM Menú Maestro de Capturas - Black Hole Glow
REM =========================================================================

:menu
cls
echo.
echo [96m╔════════════════════════════════════════════════╗[0m
echo [96m║  📱 BLACK HOLE GLOW - Menu de Capturas      ║[0m
echo [96m╚════════════════════════════════════════════════╝[0m
echo.
echo [92m  1.[0m 📸 Captura Rapida (3 seg)
echo      [90m→ Solo 1 screenshot de lo que ves ahora[0m
echo.
echo [92m  2.[0m 🔄 Captura + Instalacion (15-30 seg)
echo      [90m→ Instala APK nuevo + multiples capturas[0m
echo.
echo [92m  3.[0m 🎥 Grabar Video (10-35 seg)
echo      [90m→ Graba animaciones y movimiento[0m
echo.
echo [92m  4.[0m 📂 Abrir carpeta D:\img\
echo.
echo [92m  5.[0m 📖 Ver documentacion (SCRIPTS_CAPTURA.md)
echo.
echo [92m  6.[0m ❌ Salir
echo.
echo [96m════════════════════════════════════════════════[0m
echo.

set /p opcion="Selecciona una opcion (1-6): "

if "%opcion%"=="1" goto captura_rapida
if "%opcion%"=="2" goto captura_completa
if "%opcion%"=="3" goto grabar_video
if "%opcion%"=="4" goto abrir_carpeta
if "%opcion%"=="5" goto ver_docs
if "%opcion%"=="6" goto salir

echo [91mOpcion invalida. Intenta de nuevo.[0m
timeout /t 2 /nobreak >nul
goto menu

:captura_rapida
cls
echo [96m📸 Ejecutando Captura Rapida...[0m
echo.
call "%~dp0captura_rapida.bat"
goto fin

:captura_completa
cls
echo [96m🔄 Ejecutando Captura Completa...[0m
echo.
call "%~dp0capturar_app.bat"
goto fin

:grabar_video
cls
echo [96m🎥 Ejecutando Grabacion de Video...[0m
echo.
call "%~dp0grabar_video.bat"
goto fin

:abrir_carpeta
cls
echo [96m📂 Abriendo D:\img\...[0m
explorer "D:\img"
timeout /t 2 /nobreak >nul
goto menu

:ver_docs
cls
echo [96m📖 Abriendo documentacion...[0m
start "" "%~dp0SCRIPTS_CAPTURA.md"
timeout /t 2 /nobreak >nul
goto menu

:salir
cls
echo [92m¡Hasta luego![0m
exit /b 0

:fin
echo.
echo [96m════════════════════════════════════════════════[0m
echo [93m¿Quieres hacer otra captura?[0m
echo.
set /p continuar="Presiona [M] para Menu o [S] para Salir: "
if /i "%continuar%"=="M" goto menu
if /i "%continuar%"=="S" goto salir
goto menu
