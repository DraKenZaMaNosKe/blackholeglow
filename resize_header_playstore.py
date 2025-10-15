"""
Script para adaptar imagen de encabezado para Google Play Console
Requisitos:
- 4096 x 2304 px
- JPEG o PNG de 24 bits (sin transparencia)
- Hasta 1 MB
"""

from PIL import Image
import os

def resize_header_for_playstore(input_path, output_path):
    """Redimensiona y adapta imagen para encabezado de Play Console"""

    print("=" * 70)
    print("ADAPTANDO IMAGEN DE ENCABEZADO PARA PLAY CONSOLE")
    print("=" * 70)
    print()

    # Abrir imagen original
    print(f"Abriendo: {input_path}")
    img = Image.open(input_path)

    # Mostrar info original
    print(f"Dimension original: {img.size[0]} x {img.size[1]} px")
    print(f"Modo original: {img.mode}")
    print()

    # Dimensiones objetivo
    target_width = 4096
    target_height = 2304
    target_ratio = target_width / target_height  # 16:9

    # Convertir a RGB si tiene canal alfa
    if img.mode in ('RGBA', 'LA', 'P'):
        print("Convirtiendo a RGB (removiendo transparencia)...")
        # Crear fondo blanco
        background = Image.new('RGB', img.size, (255, 255, 255))
        if img.mode == 'P':
            img = img.convert('RGBA')
        background.paste(img, mask=img.split()[-1] if img.mode == 'RGBA' else None)
        img = background
    elif img.mode != 'RGB':
        print("Convirtiendo a RGB...")
        img = img.convert('RGB')

    # Calcular ratio actual
    current_ratio = img.size[0] / img.size[1]

    print(f"Ratio actual: {current_ratio:.2f}")
    print(f"Ratio objetivo: {target_ratio:.2f}")
    print()

    # Estrategia: redimensionar manteniendo aspecto y luego recortar o rellenar
    if current_ratio > target_ratio:
        # Imagen mas ancha, ajustar por altura
        new_height = target_height
        new_width = int(new_height * current_ratio)
    else:
        # Imagen mas alta, ajustar por ancho
        new_width = target_width
        new_height = int(new_width / current_ratio)

    print(f"Redimensionando a {new_width} x {new_height} px...")
    img_resized = img.resize((new_width, new_height), Image.Resampling.LANCZOS)

    # Crear canvas de tamaño objetivo
    final_img = Image.new('RGB', (target_width, target_height), (0, 0, 0))

    # Centrar la imagen redimensionada
    paste_x = (target_width - new_width) // 2
    paste_y = (target_height - new_height) // 2

    # Si la imagen es mas grande, recortar del centro
    if new_width > target_width or new_height > target_height:
        left = (new_width - target_width) // 2
        top = (new_height - target_height) // 2
        right = left + target_width
        bottom = top + target_height
        img_resized = img_resized.crop((left, top, right, bottom))
        paste_x = 0
        paste_y = 0

    final_img.paste(img_resized, (paste_x, paste_y))

    print(f"Dimension final: {final_img.size[0]} x {final_img.size[1]} px")
    print(f"Modo final: {final_img.mode}")
    print()

    # Guardar como PNG optimizado
    print(f"Guardando: {output_path}")
    final_img.save(output_path, 'PNG', optimize=True)

    # Verificar tamaño del archivo
    file_size = os.path.getsize(output_path)
    file_size_mb = file_size / (1024 * 1024)

    print(f"Tamano del archivo: {file_size_mb:.2f} MB")

    if file_size_mb > 1.0:
        print()
        print("ADVERTENCIA: El archivo es mayor a 1 MB")
        print("Intentando guardar como JPEG con calidad optimizada...")

        # Intentar con JPEG de alta calidad
        output_jpg = output_path.replace('.png', '.jpg')
        quality = 95

        while quality >= 70:
            final_img.save(output_jpg, 'JPEG', quality=quality, optimize=True)
            file_size = os.path.getsize(output_jpg)
            file_size_mb = file_size / (1024 * 1024)

            if file_size_mb <= 1.0:
                print(f"JPEG guardado con calidad {quality}")
                print(f"Tamano: {file_size_mb:.2f} MB")
                print(f"Archivo guardado: {output_jpg}")
                break

            quality -= 5

        if file_size_mb > 1.0:
            print("No se pudo reducir a menos de 1 MB manteniendo calidad aceptable")

    print()
    print("=" * 70)
    print("IMAGEN ADAPTADA EXITOSAMENTE")
    print("=" * 70)
    print()
    print("Archivo generado:")
    if file_size_mb <= 1.0:
        print(f"  - {output_path}")
    else:
        print(f"  - {output_path.replace('.png', '.jpg')} (usar este)")
    print()
    print("Especificaciones:")
    print(f"  - Dimension: {target_width} x {target_height} px")
    print(f"  - Formato: PNG/JPEG de 24 bits")
    print(f"  - Sin transparencia")
    print(f"  - Tamano: {file_size_mb:.2f} MB")
    print()

if __name__ == "__main__":
    input_file = "C:\\piton\\img\\encabezado.png"
    output_file = "Z:\\orbix\\blackholeglow\\playstore_developer_header_4096x2304.png"

    if os.path.exists(input_file):
        resize_header_for_playstore(input_file, output_file)
    else:
        print(f"ERROR: No se encontro el archivo: {input_file}")
