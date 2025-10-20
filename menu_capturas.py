#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Black Hole Glow - Menu Principal de Capturas
Interfaz interactiva para elegir tipo de captura
"""

import sys
import os
from pathlib import Path
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.prompt import Prompt

# Fix encoding for Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except:
        pass

console = Console(legacy_windows=False)

def clear_screen():
    """Limpia la pantalla"""
    os.system('cls' if os.name == 'nt' else 'clear')

def print_menu():
    """Imprime el menu principal"""
    clear_screen()

    # Banner
    banner = """
    ╔══════════════════════════════════════════════════╗
    ║   BLACK HOLE GLOW - Menu de Capturas           ║
    ╚══════════════════════════════════════════════════╝
    """
    console.print(banner, style="bold cyan")

    # Tabla de opciones
    table = Table(show_header=False, box=None, padding=(0, 2))
    table.add_column("Opcion", style="bold green", width=8)
    table.add_column("Nombre", style="bold white", width=30)
    table.add_column("Descripcion", style="dim white")

    table.add_row(
        "1",
        "[1] Captura Rapida",
        "Solo 1 screenshot de lo que ves ahora (3 seg)"
    )
    table.add_row(
        "2",
        "[2] Captura + Instalacion",
        "Instala APK + multiples capturas (15-30 seg)"
    )
    table.add_row(
        "3",
        "[3] Grabar Video",
        "Graba animaciones y movimiento (10-35 seg)"
    )
    table.add_row(
        "4",
        "[4] Abrir carpeta",
        r"Abre D:\img\ en el explorador"
    )
    table.add_row(
        "5",
        "[5] Documentacion",
        "Ver guia de uso (SCRIPTS_CAPTURA.md)"
    )
    table.add_row(
        "6",
        "[6] Info del sistema",
        "Verificar ADB y dispositivo conectado"
    )
    table.add_row(
        "7",
        "[7] Salir",
        "Cerrar el menu"
    )

    console.print(table)
    console.print()

def opcion_1_captura_rapida():
    """Ejecuta captura rapida"""
    clear_screen()
    console.print("[bold cyan]Ejecutando: Captura Rapida[/bold cyan]\n")
    os.system("python captura_rapida.py")

def opcion_2_captura_completa():
    """Ejecuta captura completa"""
    clear_screen()
    console.print("[bold cyan]Ejecutando: Captura Completa[/bold cyan]\n")
    os.system("python capturar_app.py")

def opcion_3_grabar_video():
    """Ejecuta grabacion de video"""
    clear_screen()
    console.print("[bold cyan]Ejecutando: Grabacion de Video[/bold cyan]\n")
    os.system("python grabar_video.py")

def opcion_4_abrir_carpeta():
    """Abre la carpeta de imagenes"""
    clear_screen()
    console.print(r"[cyan][FOLDER] Abriendo D:\img\...[/cyan]" + "\n")

    img_dir = Path("D:/img")

    if img_dir.exists():
        os.startfile(str(img_dir))
        console.print("[green][OK] Carpeta abierta[/green]")
    else:
        console.print(r"[yellow][WARN] La carpeta D:\img\ no existe todavia[/yellow]")
        console.print("[dim]Se creara automaticamente al tomar la primera captura[/dim]")

    console.print("\n[yellow]Presiona Enter para volver al menu...[/yellow]")
    input()

def opcion_5_documentacion():
    """Abre la documentacion"""
    clear_screen()
    console.print("[cyan][DOCS] Abriendo documentacion...[/cyan]\n")

    doc_path = Path("SCRIPTS_CAPTURA.md")

    if doc_path.exists():
        os.startfile(str(doc_path))
        console.print("[green][OK] Documentacion abierta[/green]")
    else:
        console.print("[yellow][WARN] Archivo SCRIPTS_CAPTURA.md no encontrado[/yellow]")

    console.print("\n[yellow]Presiona Enter para volver al menu...[/yellow]")
    input()

def opcion_6_info_sistema():
    """Muestra informacion del sistema"""
    clear_screen()
    console.print("[bold cyan][INFO] Informacion del Sistema[/bold cyan]\n")

    from capture_utils import ADBHelper

    adb = ADBHelper()

    # Info de ADB
    console.print("[cyan]Ruta de ADB:[/cyan]")
    console.print(f"   {adb.adb_path}\n")

    # Info de carpeta de destino
    console.print("[cyan]Carpeta de capturas:[/cyan]")
    console.print(f"   {adb.img_dir}")

    if adb.img_dir.exists():
        # Contar archivos
        png_files = list(adb.img_dir.glob("blackhole_*.png"))
        mp4_files = list(adb.img_dir.glob("blackhole_*.mp4"))

        console.print(f"   [green][OK] Existe ({len(png_files)} PNG, {len(mp4_files)} MP4)[/green]\n")
    else:
        console.print("   [yellow][WARN] No existe (se creara al capturar)[/yellow]\n")

    # Verificar dispositivo
    console.print("[cyan]Dispositivo Android:[/cyan]")
    if adb.check_device():
        console.print()
    else:
        console.print()

    # Info de version de librerias
    console.print("[cyan]Librerias instaladas:[/cyan]")
    try:
        import rich
        import tqdm
        import colorama
        console.print(f"   [green][OK] rich {rich.__version__}[/green]")
        console.print(f"   [green][OK] tqdm {tqdm.__version__}[/green]")
        console.print(f"   [green][OK] colorama {colorama.__version__}[/green]")
    except ImportError as e:
        console.print(f"   [red][ERROR] Error: {e}[/red]")

    console.print("\n[yellow]Presiona Enter para volver al menu...[/yellow]")
    input()

def main():
    """Loop principal del menu"""
    while True:
        print_menu()

        opcion = Prompt.ask(
            "[bold cyan]Selecciona una opcion[/bold cyan]",
            choices=["1", "2", "3", "4", "5", "6", "7"],
            default="1"
        )

        if opcion == "1":
            opcion_1_captura_rapida()
        elif opcion == "2":
            opcion_2_captura_completa()
        elif opcion == "3":
            opcion_3_grabar_video()
        elif opcion == "4":
            opcion_4_abrir_carpeta()
        elif opcion == "5":
            opcion_5_documentacion()
        elif opcion == "6":
            opcion_6_info_sistema()
        elif opcion == "7":
            clear_screen()
            console.print("[bold green]Hasta luego![/bold green]\n")
            sys.exit(0)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        clear_screen()
        console.print("\n[yellow]Interrumpido por el usuario[/yellow]")
        console.print("[bold green]Hasta luego![/bold green]\n")
        sys.exit(0)
