"""
Black Hole Glow - Utilidades de Captura
Módulo con funciones comunes para captura de screenshots y videos
Autor: Claude Code
Fecha: 2025-10-19
"""

import subprocess
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional, Tuple
from rich.console import Console
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
from rich.panel import Panel
from rich.table import Table
from rich import print as rprint

# Fix encoding for Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except:
        pass

# Configuración
console = Console(legacy_windows=False)

class ADBHelper:
    """Helper para operaciones ADB"""

    def __init__(self):
        self.adb_path = self._find_adb()
        self.img_dir = Path("D:/img")
        self.img_dir.mkdir(parents=True, exist_ok=True)

    def _find_adb(self) -> str:
        """Encuentra la ruta de ADB"""
        # Rutas comunes de ADB en Windows
        possible_paths = [
            r"D:\adb\platform-tools\adb.exe",  # Instalacion automatica
            r"C:\Users\eduar\AppData\Local\Android\Sdk\platform-tools\adb.exe",
            r"C:\Android\sdk\platform-tools\adb.exe",
            r"C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe",
        ]

        # Intentar encontrar en PATH
        try:
            result = subprocess.run(["where", "adb"], capture_output=True, text=True)
            if result.returncode == 0:
                return result.stdout.strip().split('\n')[0]
        except:
            pass

        # Buscar en rutas comunes
        for path in possible_paths:
            if os.path.exists(path):
                return path

        # Si no encuentra, asumir que está en PATH
        return "adb"

    def run_command(self, args: list, timeout: int = 30) -> Tuple[bool, str, str]:
        """
        Ejecuta un comando ADB

        Returns:
            Tuple[bool, str, str]: (success, stdout, stderr)
        """
        try:
            cmd = [self.adb_path] + args
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                encoding='utf-8',
                errors='replace'
            )

            success = result.returncode == 0
            return success, result.stdout, result.stderr

        except subprocess.TimeoutExpired:
            return False, "", "Timeout: El comando tardó demasiado"
        except FileNotFoundError:
            return False, "", f"ADB no encontrado en: {self.adb_path}"
        except Exception as e:
            return False, "", f"Error: {str(e)}"

    def check_device(self) -> bool:
        """Verifica que haya un dispositivo conectado"""
        success, stdout, stderr = self.run_command(["devices"])

        if not success:
            console.print("[red]❌ Error al ejecutar ADB[/red]")
            console.print(f"[red]{stderr}[/red]")
            return False

        # Verificar si hay dispositivos (más de 1 línea = hay dispositivo)
        lines = [line for line in stdout.strip().split('\n') if line.strip() and not line.startswith('List')]

        if not lines:
            console.print("[red][ERROR] No hay dispositivo conectado[/red]")
            console.print("[yellow][INFO] Conecta tu celular via USB con USB Debugging habilitado[/yellow]")
            return False

        # Mostrar dispositivo conectado
        device_info = lines[0].split('\t')[0] if '\t' in lines[0] else lines[0]
        console.print(f"[green][OK] Dispositivo conectado: {device_info}[/green]")
        return True

    def screenshot(self, output_path: Path) -> bool:
        """Toma un screenshot del dispositivo"""
        temp_path = "/sdcard/screenshot_temp.png"

        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            TaskProgressColumn(),
            console=console
        ) as progress:

            task = progress.add_task("[cyan]Capturando pantalla...", total=3)

            # 1. Capturar en el dispositivo
            success, _, stderr = self.run_command(["shell", "screencap", "-p", temp_path])
            if not success:
                console.print(f"[red][ERROR] Error al capturar: {stderr}[/red]")
                return False
            progress.update(task, advance=1)

            # 2. Descargar a PC
            success, _, stderr = self.run_command(["pull", temp_path, str(output_path)])
            if not success:
                console.print(f"[red][ERROR] Error al descargar: {stderr}[/red]")
                return False
            progress.update(task, advance=1)

            # 3. Limpiar del dispositivo
            self.run_command(["shell", "rm", temp_path])
            progress.update(task, advance=1)

        return True

    def record_video(self, duration: int, output_path: Path) -> bool:
        """Graba video de la pantalla del dispositivo"""
        temp_path = "/sdcard/recording_temp.mp4"

        console.print(f"[yellow][REC] Grabando {duration} segundos...[/yellow]")
        console.print("[dim]Asegurate de que la app este abierta en el celular[/dim]")

        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            console=console
        ) as progress:

            task = progress.add_task("[cyan]Grabando video...", total=duration + 2)

            # 1. Iniciar grabación (bloqueante, toma 'duration' segundos)
            success, _, stderr = self.run_command(
                ["shell", "screenrecord", "--time-limit", str(duration), temp_path],
                timeout=duration + 10
            )

            if not success:
                console.print(f"[red][ERROR] Error al grabar: {stderr}[/red]")
                return False

            progress.update(task, advance=duration)

            # 2. Descargar video
            progress.update(task, description="[cyan]Descargando video...")
            success, _, stderr = self.run_command(["pull", temp_path, str(output_path)])

            if not success:
                console.print(f"[red][ERROR] Error al descargar: {stderr}[/red]")
                return False

            progress.update(task, advance=1)

            # 3. Limpiar
            self.run_command(["shell", "rm", temp_path])
            progress.update(task, advance=1)

        return True

    def install_apk(self, apk_path: Path) -> bool:
        """Instala un APK en el dispositivo"""
        if not apk_path.exists():
            console.print(f"[red][ERROR] APK no encontrado: {apk_path}[/red]")
            return False

        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            console=console
        ) as progress:

            task = progress.add_task("[cyan]Instalando APK...", total=1)

            success, stdout, stderr = self.run_command(
                ["install", "-r", str(apk_path)],
                timeout=60
            )

            progress.update(task, advance=1)

            if not success:
                console.print(f"[red][ERROR] Error al instalar APK[/red]")
                console.print(f"[red]{stderr}[/red]")
                return False

            if "Success" in stdout:
                console.print("[green][OK] APK instalado correctamente[/green]")
                return True
            else:
                console.print(f"[yellow][WARN] Resultado inesperado: {stdout}[/yellow]")
                return False

    def launch_app(self, package: str, activity: str) -> bool:
        """Lanza una aplicación"""
        success, _, stderr = self.run_command(
            ["shell", "am", "start", "-n", f"{package}/{activity}"]
        )

        if success:
            console.print("[green][OK] App lanzada correctamente[/green]")
            return True
        else:
            console.print(f"[red][ERROR] Error al lanzar app: {stderr}[/red]")
            return False

    def get_timestamp(self) -> str:
        """Genera timestamp para nombres de archivo"""
        return datetime.now().strftime("%Y%m%d_%H%M%S")

    def open_file(self, path: Path):
        """Abre un archivo con la aplicación predeterminada"""
        try:
            os.startfile(str(path))
        except Exception as e:
            console.print(f"[yellow][WARN] No se pudo abrir automaticamente: {e}[/yellow]")

    def open_folder(self, path: Path):
        """Abre una carpeta en el explorador"""
        try:
            os.startfile(str(path))
        except Exception as e:
            console.print(f"[yellow][WARN] No se pudo abrir carpeta: {e}[/yellow]")


def print_header(title: str, subtitle: str = ""):
    """Imprime un header bonito"""
    console.print()
    panel = Panel(
        f"[bold cyan]{title}[/bold cyan]\n[dim]{subtitle}[/dim]" if subtitle else f"[bold cyan]{title}[/bold cyan]",
        border_style="cyan",
        padding=(1, 2)
    )
    console.print(panel)
    console.print()


def print_success(message: str):
    """Imprime mensaje de éxito"""
    console.print(f"[green][OK] {message}[/green]")


def print_error(message: str):
    """Imprime mensaje de error"""
    console.print(f"[red][ERROR] {message}[/red]")


def print_warning(message: str):
    """Imprime mensaje de advertencia"""
    console.print(f"[yellow][WARN] {message}[/yellow]")


def print_info(message: str):
    """Imprime mensaje informativo"""
    console.print(f"[cyan][INFO] {message}[/cyan]")


def get_int_input(prompt: str, min_val: int, max_val: int, default: int) -> int:
    """Obtiene input entero validado del usuario"""
    while True:
        console.print(f"[cyan]{prompt}[/cyan]", end="")
        user_input = input()

        if not user_input.strip():
            return default

        try:
            value = int(user_input)
            if min_val <= value <= max_val:
                return value
            else:
                print_error(f"Debe estar entre {min_val} y {max_val}")
        except ValueError:
            print_error("Debe ser un número entero")
