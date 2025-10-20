#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Black Hole Glow - Grabacion de Video
Graba la pantalla del dispositivo Android para mostrar animaciones
"""

import sys
from pathlib import Path
from capture_utils import (
    ADBHelper, print_header, print_success, print_error,
    print_info, get_int_input, console
)

# Fix encoding for Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except:
        pass

def main():
    print_header(
        "Grabacion de Video",
        "Graba animaciones y movimiento de la pantalla"
    )

    # Inicializar helper
    adb = ADBHelper()

    # Verificar dispositivo
    if not adb.check_device():
        console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
        input()
        sys.exit(1)

    console.print()

    # Preguntar duración
    duracion = get_int_input(
        "¿Cuántos segundos quieres grabar? (5-30, default=10): ",
        min_val=5,
        max_val=30,
        default=10
    )

    console.print()

    # Advertencia
    print_info("Asegúrate de que la app esté abierta en el celular")
    console.print("[yellow]La grabación comenzará en 3 segundos...[/yellow]")
    console.print()

    import time
    time.sleep(3)

    # Generar nombre de archivo
    timestamp = adb.get_timestamp()
    filename = f"blackhole_{timestamp}.mp4"
    output_path = adb.img_dir / filename

    # Grabar video
    if adb.record_video(duracion, output_path):
        console.print()
        print_success(f"Video guardado en: {output_path}")
        console.print()

        # Mostrar tamaño del archivo
        file_size_mb = output_path.stat().st_size / (1024 * 1024)
        print_info(f"Tamaño del video: {file_size_mb:.2f} MB")
        console.print()

        # Instrucciones para Claude
        console.print("[bold cyan]Para mostrar a Claude, copia esto:[/bold cyan]")
        console.print(f"[green]D:\\img\\{filename}[/green]")
        console.print()

        console.print("[bold cyan]Nota importante:[/bold cyan]")
        console.print("[dim]Claude puede ver videos MP4 directamente.[/dim]")
        console.print("[dim]Ya NO necesitas extraer frames con Python![/dim]")
        console.print()

        # Abrir video
        console.print("[dim]Abriendo video...[/dim]")
        adb.open_file(output_path)

    else:
        print_error("Falló la grabación")
        console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
        input()
        sys.exit(1)

    # Esperar antes de salir
    console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
    input()

if __name__ == "__main__":
    main()
