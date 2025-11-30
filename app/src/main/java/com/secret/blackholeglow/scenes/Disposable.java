package com.secret.blackholeglow.scenes;

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║   🗑️ Disposable - Interfaz para liberacion de recursos OpenGL    ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Cualquier clase que maneje recursos OpenGL (texturas, shaders, buffers)
 * debe implementar esta interfaz para asegurar limpieza correcta.
 *
 * IMPORTANTE: dispose() DEBE llamarse desde el GL thread.
 */
public interface Disposable {

    /**
     * Libera todos los recursos OpenGL asociados a este objeto.
     *
     * Esto incluye:
     * - Texturas (glDeleteTextures)
     * - Shader programs (glDeleteProgram)
     * - Buffers (glDeleteBuffers)
     * - VAOs (glDeleteVertexArrays)
     *
     * DEBE llamarse desde el GL thread para evitar crashes.
     */
    void dispose();

    /**
     * Verifica si los recursos ya fueron liberados.
     *
     * @return true si dispose() ya fue llamado, false si aun tiene recursos
     */
    boolean isDisposed();
}
