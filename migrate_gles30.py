#!/usr/bin/env python3
"""
Script para migrar de GLES20 a GLES30 en todos los archivos Java
"""
import os
import re

def migrate_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # Reemplazar import
    content = content.replace('import android.opengl.GLES20;', 'import android.opengl.GLES30;')

    # Reemplazar todas las llamadas GLES20. -> GLES30.
    content = content.replace('GLES20.', 'GLES30.')

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    base_path = r'D:\Orbix\blackholeglow\app\src\main\java'
    count = 0

    for root, dirs, files in os.walk(base_path):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if migrate_file(filepath):
                    print(f'Migrado: {file}')
                    count += 1

    print(f'\n✅ Total archivos migrados: {count}')

if __name__ == '__main__':
    main()
