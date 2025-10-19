#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Demo de la interfaz hermosa de los scripts Python
Muestra cómo se ven los colores, paneles, progress bars, etc.
"""

import time
import sys
from rich.console import Console
from rich.panel import Panel
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
from rich.table import Table
from rich import print as rprint

# Fix encoding for Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding='utf-8')

console = Console(legacy_windows=False)

def demo_header():
    """Demo de headers"""
    console.print()
    panel = Panel(
        "[bold cyan]Captura Rapida[/bold cyan]\n[dim]Screenshot instantaneo del celular[/dim]",
        border_style="cyan",
        padding=(1, 2)
    )
    console.print(panel)
    console.print()

def demo_mensajes():
    """Demo de mensajes con colores"""
    console.print("[green][OK] Operacion exitosa[/green]")
    console.print("[red][ERROR] Error al ejecutar comando[/red]")
    console.print("[yellow][WARN] Advertencia: Archivo no encontrado[/yellow]")
    console.print("[cyan][INFO] Informacion importante[/cyan]")
    console.print()

def demo_tabla():
    """Demo de tabla bonita"""
    table = Table(show_header=True, box=None, padding=(0, 2))
    table.add_column("Archivo", style="bold green")
    table.add_column("Tamaño", style="dim white")
    table.add_column("Fecha", style="cyan")

    table.add_row("blackhole_20251019_150522_1.png", "2.3 MB", "2025-10-19 15:05:22")
    table.add_row("blackhole_20251019_150522_2.png", "2.1 MB", "2025-10-19 15:05:24")
    table.add_row("blackhole_20251019_150522_3.png", "2.4 MB", "2025-10-19 15:05:26")

    console.print(table)
    console.print()

def demo_progress():
    """Demo de progress bars animadas"""
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(),
        TaskProgressColumn(),
        console=console
    ) as progress:

        # Simular 3 tareas
        task1 = progress.add_task("[cyan]Capturando pantalla...", total=100)
        task2 = progress.add_task("[yellow]Descargando archivo...", total=100)
        task3 = progress.add_task("[green]Limpiando archivos temporales...", total=100)

        # Simular progreso
        for i in range(100):
            time.sleep(0.02)  # 2 segundos total
            progress.update(task1, advance=1)
            if i >= 30:
                progress.update(task2, advance=1)
            if i >= 60:
                progress.update(task3, advance=1)

    console.print()

def main():
    console.clear()

    # Banner principal
    console.print()
    console.print("[bold cyan]" + "="*60 + "[/bold cyan]")
    console.print("[bold cyan]  DEMO - Interfaz Python vs .BAT[/bold cyan]")
    console.print("[bold cyan]" + "="*60 + "[/bold cyan]")
    console.print()

    # 1. Header
    console.print("[bold white]1. Headers y Paneles:[/bold white]")
    demo_header()

    # 2. Mensajes
    console.print("[bold white]2. Mensajes con Colores:[/bold white]")
    console.print()
    demo_mensajes()

    # 3. Tabla
    console.print("[bold white]3. Tablas Bonitas:[/bold white]")
    console.print()
    demo_tabla()

    # 4. Progress bars
    console.print("[bold white]4. Progress Bars Animadas:[/bold white]")
    console.print()
    demo_progress()

    # Comparación final
    console.print("[bold cyan]" + "="*60 + "[/bold cyan]")
    console.print()

    # Tabla comparativa
    comparison_table = Table(title="Python vs .BAT", show_header=True)
    comparison_table.add_column("Característica", style="cyan", width=25)
    comparison_table.add_column(".BAT", style="red", justify="center", width=15)
    comparison_table.add_column("Python", style="green", justify="center", width=15)

    comparison_table.add_row("Colores", "X Limitado", "✓ Full RGB")
    comparison_table.add_row("Progress Bars", "X No", "✓ Animadas")
    comparison_table.add_row("Tablas", "X Texto plano", "✓ Formateadas")
    comparison_table.add_row("Manejo errores", "X Basico", "✓ Robusto")
    comparison_table.add_row("Multiplataforma", "X Solo Win", "✓ Win/Mac/Linux")

    console.print(comparison_table)
    console.print()

    console.print("[bold green]Como puedes ver, Python es MUCHO mas hermoso y profesional[/bold green]")
    console.print()

    console.print("[yellow]Presiona Enter para salir...[/yellow]")
    try:
        input()
    except KeyboardInterrupt:
        console.print("\n[dim]Saliendo...[/dim]")

if __name__ == "__main__":
    main()
