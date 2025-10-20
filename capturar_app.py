#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Black Hole Glow - Captura Completa
Instala APK, lanza la app y toma multiples screenshots
"""

import sys
import time
from pathlib import Path
from capture_utils import (
    ADBHelper, print_header, print_success, print_error,
    print_warning, print_info, get_int_input, console
)

# Fix encoding for Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except:
        pass

# Configuración
PACKAGE = "com.secret.blackholeglow"
ACTIVITY = f"{PACKAGE}/.LoginActivity"  # Cambiado de SplashActivity a LoginActivity
APK_RELATIVE_PATH = "app/build/outputs/apk/debug/app-debug.apk"

def main():
    print_header(
        "Captura Completa con Instalacion",
        "Instala APK -> Lanza app -> Captura screenshots"
    )

    # Inicializar helper
    adb = ADBHelper()

    # Verificar dispositivo
    if not adb.check_device():
        console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
        input()
        sys.exit(1)

    console.print()

    # Verificar si existe APK
    apk_path = Path(APK_RELATIVE_PATH)
    should_install = False

    if apk_path.exists():
        print_info(f"APK encontrado: {apk_path}")
        console.print("[cyan]¿Instalar APK antes de capturar? (s/n, default=s): [/cyan]", end="")
        response = input().strip().lower()
        should_install = response != 'n'
    else:
        print_warning(f"APK no encontrado en: {apk_path}")
        print_info("Omitiendo instalación, solo se capturará la app actual")

    console.print()

    # Instalar APK si es necesario
    if should_install and apk_path.exists():
        if not adb.install_apk(apk_path):
            print_error("Falló la instalación del APK")
            console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
            input()
            sys.exit(1)
        console.print()

    # Lanzar app
    print_info("Lanzando Black Hole Glow...")
    if adb.launch_app(PACKAGE, ACTIVITY):
        print_info("Esperando 3 segundos para que la app se inicie...")
        time.sleep(3)
    else:
        print_warning("No se pudo lanzar la app automáticamente")
        print_info("Abre la app manualmente en el celular")
        console.print("\n[yellow]Presiona Enter cuando la app esté abierta...[/yellow]")
        input()

    console.print()

    # Preguntar cuántas capturas
    num_capturas = get_int_input(
        "¿Cuántas capturas quieres tomar? (1-10, default=3): ",
        min_val=1,
        max_val=10,
        default=3
    )

    console.print()

    # Tomar capturas
    timestamp = adb.get_timestamp()
    captured_files = []

    for i in range(1, num_capturas + 1):
        console.print(f"[bold cyan]Captura {i} de {num_capturas}[/bold cyan]")

        filename = f"blackhole_{timestamp}_{i}.png"
        output_path = adb.img_dir / filename

        if adb.screenshot(output_path):
            captured_files.append(filename)
            print_success(f"Guardada: {filename}")
        else:
            print_error(f"Falló captura {i}")

        # Esperar entre capturas (excepto la última)
        if i < num_capturas:
            console.print("[dim]Esperando 2 segundos...[/dim]")
            time.sleep(2)

        console.print()

    # Resumen
    if captured_files:
        console.print("[bold green]" + "="*50 + "[/bold green]")
        console.print("[bold green]✓ Proceso completado exitosamente[/bold green]")
        console.print("[bold green]" + "="*50 + "[/bold green]")
        console.print()

        console.print(f"[cyan]{len(captured_files)} archivo(s) guardados en: D:\\img\\[/cyan]")
        console.print()

        # Listar archivos
        for f in captured_files:
            console.print(f"  [green]* {f}[/green]")

        console.print()

        # Instrucciones para Claude
        console.print("[bold cyan]Para mostrar a Claude, copia esto:[/bold cyan]")
        if len(captured_files) > 1:
            console.print(f"[green]D:\\img\\blackhole_{timestamp}_*.png[/green]")
        else:
            console.print(f"[green]D:\\img\\{captured_files[0]}[/green]")

        console.print()

        # Abrir carpeta
        console.print("[dim]Abriendo carpeta D:\\img\\...[/dim]")
        adb.open_folder(adb.img_dir)

    else:
        print_error("No se capturó ninguna imagen")

    # Esperar antes de salir
    console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
    input()

if __name__ == "__main__":
    main()
