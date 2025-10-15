"""
Script para generar assets de Play Store para Black Hole Glow
Genera:
- Ícono de 512x512 px
- Banner de funciones de 1024x500 px
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

# Colores del tema espacial
COLOR_DARK_SPACE = (10, 10, 25)
COLOR_PURPLE_DARK = (76, 48, 96)
COLOR_PURPLE_LIGHT = (102, 78, 162)
COLOR_BLUE_GLOW = (87, 117, 234)
COLOR_WHITE = (255, 255, 255)
COLOR_ORANGE_GLOW = (255, 140, 50)

def create_gradient_background(width, height, color1, color2):
    """Crea un fondo con gradiente"""
    base = Image.new('RGB', (width, height), color1)
    draw = ImageDraw.Draw(base)

    for y in range(height):
        # Interpolar entre color1 y color2
        ratio = y / height
        r = int(color1[0] * (1 - ratio) + color2[0] * ratio)
        g = int(color1[1] * (1 - ratio) + color2[1] * ratio)
        b = int(color1[2] * (1 - ratio) + color2[2] * ratio)
        draw.line([(0, y), (width, y)], fill=(r, g, b))

    return base

def draw_black_hole(draw, center_x, center_y, radius):
    """Dibuja un agujero negro estilizado"""
    # Núcleo negro
    core_radius = radius // 3
    draw.ellipse(
        [center_x - core_radius, center_y - core_radius,
         center_x + core_radius, center_y + core_radius],
        fill=(0, 0, 0)
    )

    # Anillo de acreción (varios círculos concéntricos)
    for i in range(5):
        ring_radius = core_radius + (i * radius // 10)
        alpha_val = 255 - (i * 40)
        # Dibujamos el anillo como un círculo con borde
        draw.ellipse(
            [center_x - ring_radius, center_y - ring_radius,
             center_x + ring_radius, center_y + ring_radius],
            outline=COLOR_ORANGE_GLOW if i % 2 == 0 else COLOR_PURPLE_LIGHT,
            width=3
        )

def draw_stars(draw, width, height, count=100):
    """Dibuja estrellas aleatorias"""
    import random
    random.seed(42)  # Para consistencia
    for _ in range(count):
        x = random.randint(0, width)
        y = random.randint(0, height)
        size = random.randint(1, 3)
        brightness = random.randint(150, 255)
        draw.ellipse([x, y, x+size, y+size], fill=(brightness, brightness, brightness))

def generate_icon_512():
    """Genera el ícono de 512x512 px"""
    print("Generando ícono 512x512...")

    # Crear imagen base con gradiente
    img = create_gradient_background(512, 512, COLOR_DARK_SPACE, COLOR_PURPLE_DARK)
    draw = ImageDraw.Draw(img)

    # Agregar estrellas
    draw_stars(draw, 512, 512, count=150)

    # Dibujar agujero negro central
    draw_black_hole(draw, 256, 256, 180)

    # Agregar efecto de brillo (glow)
    glow = Image.new('RGBA', (512, 512), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    for i in range(10):
        radius = 100 + (i * 15)
        alpha = 25 - (i * 2)
        glow_draw.ellipse(
            [256 - radius, 256 - radius, 256 + radius, 256 + radius],
            fill=(*COLOR_BLUE_GLOW, alpha)
        )

    # Combinar con glow
    img = Image.alpha_composite(img.convert('RGBA'), glow)

    # Guardar
    output_path = 'playstore_icon_512x512.png'
    img.save(output_path, 'PNG')
    print(f"[OK] Icono guardado: {output_path}")
    return output_path

def generate_feature_graphic():
    """Genera el gráfico de funciones 1024x500 px"""
    print("Generando banner de funciones 1024x500...")

    # Crear imagen base con gradiente
    img = create_gradient_background(1024, 500, COLOR_DARK_SPACE, COLOR_PURPLE_DARK)
    draw = ImageDraw.Draw(img)

    # Agregar estrellas
    draw_stars(draw, 1024, 500, count=200)

    # Dibujar agujero negro a la izquierda
    draw_black_hole(draw, 200, 250, 150)

    # Agregar texto
    try:
        # Intentar usar una fuente grande
        font_title = ImageFont.truetype("arial.ttf", 80)
        font_subtitle = ImageFont.truetype("arial.ttf", 36)
    except:
        # Si falla, usar fuente por defecto
        font_title = ImageFont.load_default()
        font_subtitle = ImageFont.load_default()

    # Título: "Black Hole Glow"
    title_text = "Black Hole Glow"
    # Calcular posición centrada (lado derecho)
    draw.text((550, 150), title_text, fill=COLOR_WHITE, font=font_title)

    # Subtítulo: "Live Wallpapers 3D Espaciales"
    subtitle_text = "Live Wallpapers 3D"
    draw.text((550, 250), subtitle_text, fill=COLOR_PURPLE_LIGHT, font=font_subtitle)

    # Agregar efectos de brillo
    glow = Image.new('RGBA', (1024, 500), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)

    # Glow alrededor del agujero negro
    for i in range(15):
        radius = 80 + (i * 20)
        alpha = 20 - i
        if alpha < 0:
            alpha = 0
        glow_draw.ellipse(
            [200 - radius, 250 - radius, 200 + radius, 250 + radius],
            fill=(*COLOR_BLUE_GLOW, alpha)
        )

    # Combinar
    img = Image.alpha_composite(img.convert('RGBA'), glow)

    # Guardar
    output_path = 'playstore_feature_graphic_1024x500.png'
    img.save(output_path, 'PNG')
    print(f"[OK] Banner guardado: {output_path}")
    return output_path

def main():
    print("=" * 70)
    print("GENERADOR DE ASSETS PARA PLAY STORE - BLACK HOLE GLOW")
    print("=" * 70)
    print()

    # Generar assets
    icon_path = generate_icon_512()
    feature_path = generate_feature_graphic()

    print()
    print("=" * 70)
    print("ASSETS GENERADOS EXITOSAMENTE")
    print("=" * 70)
    print()
    print("Archivos creados:")
    print(f"   1. {icon_path}")
    print(f"   2. {feature_path}")
    print()
    print("Ubicacion:")
    print(f"   {os.getcwd()}")
    print()
    print("SIGUIENTE PASO:")
    print("   1. Sube playstore_icon_512x512.png como 'Icono de la app'")
    print("   2. Sube playstore_feature_graphic_1024x500.png como 'Grafico de funciones'")
    print("   3. Toma las 4 capturas de pantalla manualmente (ver INSTRUCCIONES_SCREENSHOTS.txt)")
    print()
    print("=" * 70)

if __name__ == "__main__":
    main()
