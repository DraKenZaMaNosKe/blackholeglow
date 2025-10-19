#!/usr/bin/env python3
"""
Black Hole Glow - Captura Rápida
Toma un screenshot rápido del dispositivo Android conectado
"""

import sys
from pathlib import Path
from capture_utils import ADBHelper, print_header, print_success, print_error, console

def main():
    print_header("📸 Captura Rápida", "Screenshot instantáneo del celular")

    # Inicializar helper
    adb = ADBHelper()

    # Verificar dispositivo
    if not adb.check_device():
        console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
        input()
        sys.exit(1)

    console.print()

    # Generar nombre de archivo
    timestamp = adb.get_timestamp()
    filename = f"blackhole_{timestamp}.png"
    output_path = adb.img_dir / filename

    # Capturar screenshot
    if adb.screenshot(output_path):
        console.print()
        print_success(f"Captura guardada en: {output_path}")
        console.print()

        # Mostrar instrucciones para Claude
        console.print("[bold cyan]📋 Para mostrar a Claude, copia esto:[/bold cyan]")
        console.print(f"[green]D:\\img\\{filename}[/green]")
        console.print()

        # Abrir imagen
        console.print("[dim]Abriendo imagen...[/dim]")
        adb.open_file(output_path)

    else:
        print_error("Falló la captura")
        console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
        input()
        sys.exit(1)

    # Esperar antes de salir
    console.print("\n[yellow]Presiona Enter para salir...[/yellow]")
    input()

if __name__ == "__main__":
    main()
