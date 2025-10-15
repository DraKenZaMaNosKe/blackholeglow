"""
Fix corrupted PNG screenshots from adb screencap -p
The issue is that Windows adds CRLF line endings which corrupt the PNG
"""

def fix_screenshot(input_file, output_file):
    """Fix PNG file corrupted by Windows line endings"""
    try:
        with open(input_file, 'rb') as f:
            data = f.read()

        # Replace CRLF with LF
        fixed_data = data.replace(b'\r\n', b'\n')

        with open(output_file, 'wb') as f:
            f.write(fixed_data)

        print(f"[OK] Fixed: {input_file} -> {output_file}")
        return True
    except Exception as e:
        print(f"[ERROR] {input_file}: {e}")
        return False

def main():
    screenshots = [
        ("screenshot_1_lista_wallpapers.png", "screenshot_1_fixed.png"),
        ("screenshot_2_wallpaper_aplicado.png", "screenshot_2_fixed.png"),
        ("screenshot_3_menu_perfil.png", "screenshot_3_fixed.png"),
        ("screenshot_4_wallpaper_variedad.png", "screenshot_4_fixed.png"),
    ]

    print("Arreglando screenshots corruptos...")
    print("=" * 60)

    for input_file, output_file in screenshots:
        fix_screenshot(input_file, output_file)

    print("=" * 60)
    print("LISTO! Usa los archivos *_fixed.png")

if __name__ == "__main__":
    main()
