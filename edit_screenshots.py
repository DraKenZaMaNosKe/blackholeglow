"""
Script para editar capturas de pantalla de Play Store
Agrega textos llamativos, badges y elementos visuales
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

# Colores del tema
COLOR_ACCENT = (102, 78, 162)  # Morado
COLOR_GLOW = (87, 117, 234)    # Azul brillante
COLOR_ORANGE = (255, 140, 50)  # Naranja
COLOR_WHITE = (255, 255, 255)
COLOR_BLACK = (0, 0, 0)
COLOR_SEMI_TRANSPARENT = (0, 0, 0, 180)

def draw_rounded_rectangle(draw, xy, radius, fill):
    """Dibuja un rectángulo con esquinas redondeadas"""
    x1, y1, x2, y2 = xy

    # Rectángulo principal
    draw.rectangle([x1 + radius, y1, x2 - radius, y2], fill=fill)
    draw.rectangle([x1, y1 + radius, x2, y2 - radius], fill=fill)

    # Esquinas redondeadas
    draw.pieslice([x1, y1, x1 + radius * 2, y1 + radius * 2], 180, 270, fill=fill)
    draw.pieslice([x2 - radius * 2, y1, x2, y1 + radius * 2], 270, 360, fill=fill)
    draw.pieslice([x1, y2 - radius * 2, x1 + radius * 2, y2], 90, 180, fill=fill)
    draw.pieslice([x2 - radius * 2, y2 - radius * 2, x2, y2], 0, 90, fill=fill)

def add_badge(img, text, position, bg_color, text_color):
    """Agrega un badge con texto"""
    draw = ImageDraw.Draw(img, 'RGBA')

    try:
        font = ImageFont.truetype("arialbd.ttf", 40)
    except:
        font = ImageFont.load_default()

    # Calcular tamaño del texto
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # Dimensiones del badge
    padding = 20
    badge_width = text_width + padding * 2
    badge_height = text_height + padding * 2

    x, y = position

    # Crear capa temporal para el badge con transparencia
    overlay = Image.new('RGBA', img.size, (0, 0, 0, 0))
    overlay_draw = ImageDraw.Draw(overlay)

    # Dibujar badge con sombra
    shadow_offset = 5
    draw_rounded_rectangle(
        overlay_draw,
        [x + shadow_offset, y + shadow_offset,
         x + badge_width + shadow_offset, y + badge_height + shadow_offset],
        15,
        (0, 0, 0, 100)
    )

    # Badge principal
    draw_rounded_rectangle(
        overlay_draw,
        [x, y, x + badge_width, y + badge_height],
        15,
        bg_color
    )

    # Texto
    overlay_draw.text(
        (x + padding, y + padding),
        text,
        fill=text_color,
        font=font
    )

    # Combinar
    return Image.alpha_composite(img.convert('RGBA'), overlay)

def add_text_with_background(img, text, position, font_size=60):
    """Agrega texto con fondo semi-transparente"""
    draw = ImageDraw.Draw(img, 'RGBA')

    try:
        font = ImageFont.truetype("arialbd.ttf", font_size)
    except:
        font = ImageFont.load_default()

    x, y = position

    # Crear overlay transparente
    overlay = Image.new('RGBA', img.size, (0, 0, 0, 0))
    overlay_draw = ImageDraw.Draw(overlay)

    # Calcular tamaño del texto
    bbox = overlay_draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]

    # Fondo semi-transparente
    padding = 30
    draw_rounded_rectangle(
        overlay_draw,
        [x - padding, y - padding,
         x + text_width + padding, y + text_height + padding],
        20,
        COLOR_SEMI_TRANSPARENT
    )

    # Texto con sombra
    shadow_offset = 3
    overlay_draw.text((x + shadow_offset, y + shadow_offset), text, fill=(0, 0, 0, 200), font=font)
    overlay_draw.text((x, y), text, fill=COLOR_WHITE, font=font)

    return Image.alpha_composite(img.convert('RGBA'), overlay)

def edit_screenshot_1(img_path, output_path):
    """Screenshot 1: Lista de wallpapers - Resalta la variedad"""
    print(f"Editando screenshot 1: {img_path}")
    img = Image.open(img_path).convert('RGBA')

    width, height = img.size

    # Título arriba
    img = add_text_with_background(img, "10 FONDOS UNICOS", (width//2 - 300, 100), 70)

    # Badge superior derecha
    img = add_badge(img, "3D REAL", (width - 300, 100), (*COLOR_GLOW, 220), COLOR_WHITE)

    # Badge inferior izquierda
    img = add_badge(img, "GRATIS", (50, height - 200), (*COLOR_ORANGE, 220), COLOR_WHITE)

    img.convert('RGB').save(output_path, 'PNG')
    print(f"[OK] Guardado: {output_path}")

def edit_screenshot_2(img_path, output_path):
    """Screenshot 2: Wallpaper aplicado - Resalta la belleza visual"""
    print(f"Editando screenshot 2: {img_path}")
    img = Image.open(img_path).convert('RGBA')

    width, height = img.size

    # Título arriba
    img = add_text_with_background(img, "AGUJERO NEGRO", (width//2 - 350, 100), 70)

    # Badge inferior
    img = add_badge(img, "RENDERIZADO 3D", (width//2 - 200, height - 200), (*COLOR_ACCENT, 220), COLOR_WHITE)

    img.convert('RGB').save(output_path, 'PNG')
    print(f"[OK] Guardado: {output_path}")

def edit_screenshot_3(img_path, output_path):
    """Screenshot 3: Menú perfil - Resalta características sociales"""
    print(f"Editando screenshot 3: {img_path}")
    img = Image.open(img_path).convert('RGBA')

    width, height = img.size

    # Título arriba
    img = add_text_with_background(img, "LOGIN CON GOOGLE", (width//2 - 350, 100), 70)

    # Badge derecha
    img = add_badge(img, "FACIL", (width - 250, height//2 - 50), (*COLOR_ORANGE, 220), COLOR_WHITE)

    # Badge inferior
    img = add_badge(img, "SEGURO", (50, height - 200), (*COLOR_GLOW, 220), COLOR_WHITE)

    img.convert('RGB').save(output_path, 'PNG')
    print(f"[OK] Guardado: {output_path}")

def edit_screenshot_4(img_path, output_path):
    """Screenshot 4: Otro wallpaper - Muestra variedad y música"""
    print(f"Editando screenshot 4: {img_path}")
    img = Image.open(img_path).convert('RGBA')

    width, height = img.size

    # Título arriba
    img = add_text_with_background(img, "ESPACIO INFINITO", (width//2 - 350, 100), 70)

    # Badge música
    img = add_badge(img, "REACCIONA A MUSICA", (50, height//2 - 50), (*COLOR_ORANGE, 220), COLOR_WHITE)

    # Badge optimizado
    img = add_badge(img, "SIN LAG", (width - 280, height - 200), (*COLOR_GLOW, 220), COLOR_WHITE)

    img.convert('RGB').save(output_path, 'PNG')
    print(f"[OK] Guardado: {output_path}")

def main():
    print("=" * 70)
    print("EDITANDO SCREENSHOTS PARA PLAY STORE")
    print("=" * 70)
    print()

    screenshots = [
        ("screenshot_1_fixed.png", "playstore_screenshot_1_edited.png", edit_screenshot_1),
        ("screenshot_2_fixed.png", "playstore_screenshot_2_edited.png", edit_screenshot_2),
        ("screenshot_3_fixed.png", "playstore_screenshot_3_edited.png", edit_screenshot_3),
        ("screenshot_4_fixed.png", "playstore_screenshot_4_edited.png", edit_screenshot_4),
    ]

    for input_file, output_file, edit_func in screenshots:
        if os.path.exists(input_file):
            edit_func(input_file, output_file)
        else:
            print(f"[ADVERTENCIA] No se encontro: {input_file}")

    print()
    print("=" * 70)
    print("CAPTURAS EDITADAS EXITOSAMENTE")
    print("=" * 70)
    print()
    print("Archivos generados:")
    for _, output_file, _ in screenshots:
        if os.path.exists(output_file):
            print(f"  - {output_file}")
    print()
    print("LISTO PARA SUBIR A PLAY CONSOLE!")
    print("=" * 70)

if __name__ == "__main__":
    main()
