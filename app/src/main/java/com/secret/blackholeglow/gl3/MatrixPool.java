package com.secret.blackholeglow.gl3;

/**
 * ============================================================================
 *  MatrixPool - Pool de matrices reutilizables para evitar GC en draw loops
 * ============================================================================
 *
 *  Problema: Crear new float[16] en cada frame causa GC pauses.
 *  Solucion: Reutilizar matrices preallocadas.
 *
 *  Uso:
 *    // Al inicio del frame
 *    MatrixPool.reset();
 *
 *    // Durante draw
 *    float[] model = MatrixPool.obtain();
 *    float[] mvp = MatrixPool.obtain();
 *    Matrix.setIdentityM(model, 0);
 *    ...
 *
 *  IMPORTANTE: Las matrices se reutilizan cada frame, no guardar referencias!
 *
 * ============================================================================
 */
public final class MatrixPool {

    // Pool de matrices 4x4 (16 floats cada una)
    private static final int POOL_SIZE = 32;
    private static final float[][] matrices = new float[POOL_SIZE][16];
    private static int index = 0;

    // Pool de vectores (4 floats)
    private static final int VECTOR_POOL_SIZE = 16;
    private static final float[][] vectors = new float[VECTOR_POOL_SIZE][4];
    private static int vectorIndex = 0;

    // Pool de arrays temporales de vertices
    private static final int TEMP_ARRAY_POOL_SIZE = 8;
    private static final float[][] tempArrays = new float[TEMP_ARRAY_POOL_SIZE][];
    private static int tempArrayIndex = 0;

    private MatrixPool() {} // No instanciar

    /**
     * Resetear pools al inicio de cada frame.
     * DEBE llamarse en onDrawFrame antes de cualquier draw.
     */
    public static void reset() {
        index = 0;
        vectorIndex = 0;
        tempArrayIndex = 0;
    }

    /**
     * Obtiene una matriz 4x4 del pool.
     * NO guardar referencia entre frames!
     */
    public static float[] obtain() {
        if (index >= POOL_SIZE) {
            // Wrap around si se excede (no deberia pasar)
            index = 0;
        }
        return matrices[index++];
    }

    /**
     * Obtiene un vector (4 floats) del pool.
     */
    public static float[] obtainVector() {
        if (vectorIndex >= VECTOR_POOL_SIZE) {
            vectorIndex = 0;
        }
        return vectors[vectorIndex++];
    }

    /**
     * Obtiene un array temporal del pool con tamaño especificado.
     * Si no hay array del tamaño correcto, crea uno nuevo (y lo guarda para reusar).
     */
    public static float[] obtainArray(int size) {
        if (tempArrayIndex >= TEMP_ARRAY_POOL_SIZE) {
            tempArrayIndex = 0;
        }

        // Verificar si el array existente tiene el tamaño correcto
        if (tempArrays[tempArrayIndex] == null || tempArrays[tempArrayIndex].length < size) {
            tempArrays[tempArrayIndex] = new float[size];
        }

        return tempArrays[tempArrayIndex++];
    }

    /**
     * Obtiene dos matrices a la vez (comun para model y mvp)
     */
    public static float[][] obtainPair() {
        return new float[][] { obtain(), obtain() };
    }

    /**
     * Estadisticas de uso del pool
     */
    public static String getStats() {
        return "MatrixPool: " + index + "/" + POOL_SIZE +
               " | VectorPool: " + vectorIndex + "/" + VECTOR_POOL_SIZE +
               " | TempArrays: " + tempArrayIndex + "/" + TEMP_ARRAY_POOL_SIZE;
    }

    /**
     * Verifica si el pool esta cerca de llenarse (para debug)
     */
    public static boolean isNearFull() {
        return index > POOL_SIZE * 0.8f || vectorIndex > VECTOR_POOL_SIZE * 0.8f;
    }
}
