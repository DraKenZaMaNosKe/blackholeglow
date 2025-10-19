#!/usr/bin/env python3
"""
Black Hole Glow - Menú Principal de Capturas
Interfaz interactiva para elegir tipo de captura
"""

import sys
import os
from pathlib import Path
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.prompt import Prompt

console = Console()

def clear_screen():
    """Limpia la pantalla"""
    os.system('cls' if os.name == 'nt' else 'clear')

def print_menu():
    """Imprime el menú principal"""
    clear_screen()

    # Banner
    banner = """
    ╔══════════════════════════════════════════════════╗
    ║   📱 BLACK HOLE GLOW - Menú de Capturas        ║
    ╚══════════════════════════════════════════════════╝
    """
    console.print(banner, style="bold cyan")

    # Tabla de opciones
    table = Table(show_header=False, box=None, padding=(0, 2))
    table.add_column("Opción", style="bold green", width=8)
    table.add_column("Nombre", style="bold white", width=30)
    table.add_column("Descripción", style="dim white")

    table.add_row(
        "1",
        "📸 Captura Rápida",
        "Solo 1 screenshot de lo que ves ahora (3 seg)"
    )
    table.add_row(
        "2",
        "🔄 Captura + Instalación",
        "Instala APK + múltiples capturas (15-30 seg)"
    )
    table.add_row(
        "3",
        "🎥 Grabar Video",
        "Graba animaciones y movimiento (10-35 seg)"
    )
    table.add_row(
        "4",
        "📂 Abrir carpeta",
        "Abre D:\\img\\ en el explorador"
    )
    table.add_row(
        "5",
        "📖 Documentación",
        "Ver guía de uso (SCRIPTS_CAPTURA.md)"
    )
    table.add_row(
        "6",
        "ℹ️  Info del sistema",
        "Verificar ADB y dispositivo conectado"
    )
    table.add_row(
        "7",
        "❌ Salir",
        "Cerrar el menú"
    )

    console.print(table)
    console.print()

def opcion_1_captura_rapida():
    """Ejecuta captura rápida"""
    clear_screen()
    console.print("[bold cyan]Ejecutando: Captura Rápida[/bold cyan]\n")
    os.system("python captura_rapida.py")

def opcion_2_captura_completa():
    """Ejecuta captura completa"""
    clear_screen()
    console.print("[bold cyan]Ejecutando: Captura Completa[/bold cyan]\n")
    os.system("python capturar_app.py")

def opcion_3_grabar_video():
    """Ejecuta grabación de video"""
    clear_screen()
    console.print("[bold cyan]Ejecutando: Grabación de Video[/bold cyan]\n")
    os.system("python grabar_video.py")

def opcion_4_abrir_carpeta():
    """Abre la carpeta D:\img\"""
    clear_screen()
    console.print("[cyan]📂 Abriendo D:\\img\\...[/cyan]\n")

    img_dir = Path("D:/img")

    if img_dir.exists():
        os.startfile(str(img_dir))
        console.print("[green]✓ Carpeta abierta[/green]")
    else:
        console.print("[yellow]⚠️  La carpeta D:\\img\\ no existe todavía[/yellow]")
        console.print("[dim]Se creará automáticamente al tomar la primera captura[/dim]")

    console.print("\n[yellow]Presiona Enter para volver al menú...[/yellow]")
    input()

def opcion_5_documentacion():
    """Abre la documentación"""
    clear_screen()
    console.print("[cyan]📖 Abriendo documentación...[/cyan]\n")

    doc_path = Path("SCRIPTS_CAPTURA.md")

    if doc_path.exists():
        os.startfile(str(doc_path))
        console.print("[green]✓ Documentación abierta[/green]")
    else:
        console.print("[yellow]⚠️  Archivo SCRIPTS_CAPTURA.md no encontrado[/yellow]")

    console.print("\n[yellow]Presiona Enter para volver al menú...[/yellow]")
    input()

def opcion_6_info_sistema():
    """Muestra información del sistema"""
    clear_screen()
    console.print("[bold cyan]ℹ️  Información del Sistema[/bold cyan]\n")

    from capture_utils import ADBHelper

    adb = ADBHelper()

    # Info de ADB
    console.print("[cyan]📱 Ruta de ADB:[/cyan]")
    console.print(f"   {adb.adb_path}\n")

    # Info de carpeta de destino
    console.print("[cyan]📁 Carpeta de capturas:[/cyan]")
    console.print(f"   {adb.img_dir}")

    if adb.img_dir.exists():
        # Contar archivos
        png_files = list(adb.img_dir.glob("blackhole_*.png"))
        mp4_files = list(adb.img_dir.glob("blackhole_*.mp4"))

        console.print(f"   [green]✓ Existe ({len(png_files)} PNG, {len(mp4_files)} MP4)[/green]\n")
    else:
        console.print("   [yellow]⚠️  No existe (se creará al capturar)[/yellow]\n")

    # Verificar dispositivo
    console.print("[cyan]🔌 Dispositivo Android:[/cyan]")
    if adb.check_device():
        console.print()
    else:
        console.print()

    # Info de versión de librerías
    console.print("[cyan]📦 Librerías instaladas:[/cyan]")
    try:
        import rich
        import tqdm
        import colorama
        console.print(f"   [green]✓ rich {rich.__version__}[/green]")
        console.print(f"   [green]✓ tqdm {tqdm.__version__}[/green]")
        console.print(f"   [green]✓ colorama {colorama.__version__}[/green]")
    except ImportError as e:
        console.print(f"   [red]❌ Error: {e}[/red]")

    console.print("\n[yellow]Presiona Enter para volver al menú...[/yellow]")
    input()

def main():
    """Loop principal del menú"""
    while True:
        print_menu()

        opcion = Prompt.ask(
            "[bold cyan]Selecciona una opción[/bold cyan]",
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
            console.print("[bold green]¡Hasta luego! 👋[/bold green]\n")
            sys.exit(0)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        clear_screen()
        console.print("\n[yellow]Interrumpido por el usuario[/yellow]")
        console.print("[bold green]¡Hasta luego! 👋[/bold green]\n")
        sys.exit(0)
