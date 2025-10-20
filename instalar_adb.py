#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Instalador automatico de ADB (Android Debug Bridge)
Descarga y configura ADB para Windows
"""

import os
import sys
import zipfile
import urllib.request
from pathlib import Path
from rich.console import Console
from rich.progress import Progress, DownloadColumn, BarColumn, TextColumn, TimeRemainingColumn

# Fix encoding for Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except:
        pass

console = Console(legacy_windows=False)

# Configuracion
ADB_URL = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
INSTALL_DIR = Path("D:/adb")
ZIP_FILE = INSTALL_DIR / "platform-tools.zip"

def crear_directorio():
    """Crea el directorio de instalacion"""
    console.print(f"[cyan][1/4] Creando directorio: {INSTALL_DIR}[/cyan]")
    INSTALL_DIR.mkdir(parents=True, exist_ok=True)
    console.print("[green][OK] Directorio creado[/green]\n")

def descargar_adb():
    """Descarga ADB desde Google"""
    console.print(f"[cyan][2/4] Descargando ADB desde Google...[/cyan]")
    console.print(f"[dim]URL: {ADB_URL}[/dim]\n")

    try:
        # Variables para tracking del progreso
        downloaded_size = 0
        total_download_size = 0

        with Progress(
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            DownloadColumn(),
            TimeRemainingColumn(),
            console=console
        ) as progress:

            task = progress.add_task("[cyan]Descargando...", total=None)

            def reporthook(count, block_size, total_size):
                nonlocal downloaded_size, total_download_size
                if total_size > 0 and total_download_size == 0:
                    total_download_size = total_size
                    progress.update(task, total=total_size)
                downloaded_size = count * block_size
                progress.update(task, completed=downloaded_size)

            urllib.request.urlretrieve(ADB_URL, ZIP_FILE, reporthook=reporthook)

        console.print("[green][OK] Descarga completada[/green]\n")
        return True

    except Exception as e:
        console.print(f"[red][ERROR] Error al descargar: {e}[/red]\n")
        return False

def extraer_adb():
    """Extrae el archivo ZIP"""
    console.print(f"[cyan][3/4] Extrayendo archivos...[/cyan]")

    try:
        with zipfile.ZipFile(ZIP_FILE, 'r') as zip_ref:
            zip_ref.extractall(INSTALL_DIR)

        console.print("[green][OK] Archivos extraidos[/green]\n")

        # Eliminar ZIP
        ZIP_FILE.unlink()
        console.print("[dim]Archivo ZIP eliminado[/dim]\n")
        return True

    except Exception as e:
        console.print(f"[red][ERROR] Error al extraer: {e}[/red]\n")
        return False

def configurar_path():
    """Muestra instrucciones para agregar ADB al PATH"""
    adb_path = INSTALL_DIR / "platform-tools"

    console.print(f"[cyan][4/4] Configuracion del PATH[/cyan]\n")

    console.print("[yellow]ADB se instalo correctamente en:[/yellow]")
    console.print(f"[green]{adb_path}[/green]\n")

    console.print("[bold cyan]Para usar ADB desde cualquier lugar, agrega esta ruta al PATH:[/bold cyan]\n")

    console.print("[yellow]Opcion A: Agregar al PATH manualmente (recomendado)[/yellow]")
    console.print("1. Presiona [Win + R]")
    console.print("2. Escribe: sysdm.cpl")
    console.print("3. Pestaña 'Opciones avanzadas' -> 'Variables de entorno'")
    console.print("4. En 'Variables del sistema', selecciona 'Path' -> 'Editar'")
    console.print("5. Click 'Nuevo' y agrega:")
    console.print(f"   [green]{adb_path}[/green]")
    console.print("6. Acepta todo y reinicia el terminal\n")

    console.print("[yellow]Opcion B: Usar ruta completa (temporal)[/yellow]")
    console.print(f"Los scripts Python usaran automaticamente: {adb_path}\\adb.exe\n")

def actualizar_scripts():
    """Actualiza los scripts para usar la nueva ruta de ADB"""
    console.print("[cyan]Actualizando scripts...[/cyan]")

    adb_exe_path = str(INSTALL_DIR / "platform-tools" / "adb.exe")

    # Leer capture_utils.py
    utils_file = Path("capture_utils.py")
    if utils_file.exists():
        content = utils_file.read_text(encoding='utf-8')

        # Buscar y reemplazar la ruta de ADB
        old_path = r'r"C:\Users\eduar\AppData\Local\Android\Sdk\platform-tools\adb.exe"'
        new_path = f'r"{adb_exe_path}"'
        new_content = content.replace(old_path, new_path)

        utils_file.write_text(new_content, encoding='utf-8')
        console.print(f"[green][OK] Scripts actualizados para usar: {adb_exe_path}[/green]\n")
    else:
        console.print("[yellow][WARN] capture_utils.py no encontrado[/yellow]\n")

def verificar_instalacion():
    """Verifica que ADB funcione"""
    console.print("[cyan]Verificando instalacion...[/cyan]")

    adb_exe = INSTALL_DIR / "platform-tools" / "adb.exe"

    if adb_exe.exists():
        console.print(f"[green][OK] ADB encontrado en: {adb_exe}[/green]")

        # Intentar ejecutar adb version
        try:
            import subprocess
            result = subprocess.run(
                [str(adb_exe), "version"],
                capture_output=True,
                text=True,
                timeout=5
            )

            if result.returncode == 0:
                version = result.stdout.split('\n')[0]
                console.print(f"[green][OK] {version}[/green]\n")
                return True
            else:
                console.print("[yellow][WARN] ADB no responde correctamente[/yellow]\n")
                return False

        except Exception as e:
            console.print(f"[yellow][WARN] Error al verificar: {e}[/yellow]\n")
            return False
    else:
        console.print(f"[red][ERROR] ADB no encontrado en: {adb_exe}[/red]\n")
        return False

def main():
    console.clear()

    console.print()
    console.print("[bold cyan]" + "="*60 + "[/bold cyan]")
    console.print("[bold cyan]  INSTALADOR DE ADB - Black Hole Glow[/bold cyan]")
    console.print("[bold cyan]" + "="*60 + "[/bold cyan]")
    console.print()

    console.print("[dim]ADB (Android Debug Bridge) es necesario para comunicarse con tu celular[/dim]")
    console.print("[dim]Este script lo descargara e instalara automaticamente[/dim]\n")

    # Verificar si ya existe
    existing_adb = INSTALL_DIR / "platform-tools" / "adb.exe"
    if existing_adb.exists():
        console.print(f"[yellow][!] ADB ya esta instalado en: {existing_adb}[/yellow]\n")
        console.print("[cyan]Deseas reinstalar? (s/n, default=n): [/cyan]", end="")
        response = input().strip().lower()
        if response != 's':
            console.print("\n[green]Usando instalacion existente[/green]")
            actualizar_scripts()
            verificar_instalacion()

            console.print("[yellow]Presiona Enter para salir...[/yellow]")
            input()
            return
        console.print()

    # Proceso de instalacion
    try:
        crear_directorio()

        if not descargar_adb():
            console.print("[red]Instalacion cancelada[/red]")
            return

        if not extraer_adb():
            console.print("[red]Instalacion cancelada[/red]")
            return

        configurar_path()
        actualizar_scripts()

        if verificar_instalacion():
            console.print("[bold green]" + "="*60 + "[/bold green]")
            console.print("[bold green]  INSTALACION COMPLETADA EXITOSAMENTE[/bold green]")
            console.print("[bold green]" + "="*60 + "[/bold green]")
            console.print()

            console.print("[cyan]Proximos pasos:[/cyan]")
            console.print("1. Conecta tu celular Android via USB")
            console.print("2. Habilita 'USB Debugging' en Opciones de Desarrollador")
            console.print("3. Ejecuta: python menu_capturas.py")
            console.print()

    except KeyboardInterrupt:
        console.print("\n\n[yellow]Instalacion cancelada por el usuario[/yellow]")
        sys.exit(1)
    except Exception as e:
        console.print(f"\n[red][ERROR] Error inesperado: {e}[/red]")
        sys.exit(1)

    console.print("[yellow]Presiona Enter para salir...[/yellow]")
    input()

if __name__ == "__main__":
    main()
