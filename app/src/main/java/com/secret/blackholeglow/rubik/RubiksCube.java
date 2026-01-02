package com.secret.blackholeglow.rubik;

import android.opengl.GLES20;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  🧊 RUBIK'S CUBE - El cubo completo con 27 cubies                        ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  Maneja:                                                                  ║
 * ║  - 27 cubies en grilla 3x3x3                                             ║
 * ║  - Rotaciones de caras (F, B, L, R, U, D)                                ║
 * ║  - Animación de rotación                                                  ║
 * ║  - Estado del juego sincronizado con CubeState                           ║
 * ║  - Auto-solve con RubikSolver                                            ║
 * ║  - Cola de movimientos para animación secuencial                         ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class RubiksCube {
    private static final String TAG = "RubiksCube";

    // ═══════════════════════════════════════════════════════════════════════
    // ESTADO Y SOLVER
    // ═══════════════════════════════════════════════════════════════════════
    private CubeState cubeState;           // Estado lógico del cubo
    private RubikSolver solver;            // Algoritmo de solución
    private Queue<String> moveQueue;       // Cola de movimientos pendientes
    private List<String> moveHistory;      // Historial de movimientos
    private boolean isSolving = false;     // ¿Está en modo auto-solve?
    private boolean isShuffling = false;   // ¿Está mezclando?
    private float solveDelay = 0f;         // Delay entre movimientos del solver
    private static final float SOLVE_MOVE_DELAY = 0.3f;  // Segundos entre movimientos

    // Callback para notificar cuando el cubo está resuelto
    public interface SolveListener {
        void onSolved();
        void onMoveExecuted(String move, int remaining);
    }
    private SolveListener solveListener;

    // Los 27 cubies
    private Cubie[][][] cubies = new Cubie[3][3][3];  // [x][y][z] donde -1→0, 0→1, 1→2

    // Shader program
    private int shaderProgram = -1;

    // Animación de rotación
    private boolean isAnimating = false;
    private int animatingFace = -1;  // Cuál cara está rotando
    private boolean animatingClockwise = true;
    private float animationAngle = 0f;
    private static final float ANIMATION_SPEED = 5f;  // Grados por frame
    private static final float TARGET_ANGLE = 90f;

    // Caras del cubo
    public static final int FACE_F = 0;  // Front (Z = 1)
    public static final int FACE_B = 1;  // Back (Z = -1)
    public static final int FACE_L = 2;  // Left (X = -1)
    public static final int FACE_R = 3;  // Right (X = 1)
    public static final int FACE_U = 4;  // Up (Y = 1)
    public static final int FACE_D = 5;  // Down (Y = -1)

    // Rotación del cubo completo para visualización
    private float cubeRotationX = 25f;   // Inclinación
    private float cubeRotationY = -35f;  // Giro horizontal

    public RubiksCube() {
        // Inicializar estado y solver
        cubeState = new CubeState();
        solver = new RubikSolver();
        moveQueue = new LinkedList<>();
        moveHistory = new ArrayList<>();

        // Crear los 27 cubies
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    cubies[x + 1][y + 1][z + 1] = new Cubie(x, y, z);
                }
            }
        }
        Log.d(TAG, "🧊 Cubo Rubik creado con 27 cubies + solver");
    }

    public void setSolveListener(SolveListener listener) {
        this.solveListener = listener;
    }

    /**
     * Inicializa el shader program
     */
    public void initShader() {
        String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;" +
            "attribute vec4 a_Position;" +
            "attribute vec4 a_Color;" +
            "varying vec4 v_Color;" +
            "void main() {" +
            "  gl_Position = u_MVPMatrix * a_Position;" +
            "  v_Color = a_Color;" +
            "}";

        String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 v_Color;" +
            "void main() {" +
            "  gl_FragColor = v_Color;" +
            "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);

        Log.d(TAG, "🎨 Shader inicializado");
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    /**
     * Actualiza la animación y procesa la cola de movimientos
     */
    public void update(float deltaTime) {
        if (isAnimating) {
            animationAngle += ANIMATION_SPEED;

            if (animationAngle >= TARGET_ANGLE) {
                // Animación completada
                animationAngle = TARGET_ANGLE;
                commitRotation();
                isAnimating = false;
                animationAngle = 0f;

                // Notificar el movimiento ejecutado
                if (solveListener != null && (isSolving || isShuffling)) {
                    solveListener.onMoveExecuted(
                        getFaceName(animatingFace) + (animatingClockwise ? "" : "'"),
                        moveQueue.size()
                    );
                }
            } else {
                // Actualizar rotación visual de los cubies afectados
                updateAnimatingCubies();
            }
        }

        // Procesar cola de movimientos (para shuffle/solve)
        if (!isAnimating && !moveQueue.isEmpty()) {
            solveDelay -= deltaTime;
            if (solveDelay <= 0) {
                String nextMove = moveQueue.poll();
                if (nextMove != null) {
                    applyMoveInternal(nextMove);
                    solveDelay = isSolving ? SOLVE_MOVE_DELAY : 0.15f;
                }
            }
        }

        // Verificar si terminó de resolver
        if (!isAnimating && moveQueue.isEmpty() && isSolving) {
            isSolving = false;
            if (cubeState.isSolved() && solveListener != null) {
                Log.d(TAG, "🎉 ¡CUBO RESUELTO!");
                solveListener.onSolved();
            }
        }

        // Verificar si terminó de mezclar
        if (!isAnimating && moveQueue.isEmpty() && isShuffling) {
            isShuffling = false;
            Log.d(TAG, "🔀 Mezcla completada");
        }
    }

    /**
     * Dibuja el cubo
     */
    public void draw(float[] viewProjectionMatrix) {
        if (shaderProgram == -1) {
            initShader();
        }

        GLES20.glUseProgram(shaderProgram);

        // Aplicar rotación del cubo completo
        float[] rotatedVP = new float[16];
        float[] rotX = new float[16];
        float[] rotY = new float[16];
        float[] temp = new float[16];

        android.opengl.Matrix.setIdentityM(rotX, 0);
        android.opengl.Matrix.setIdentityM(rotY, 0);
        android.opengl.Matrix.rotateM(rotX, 0, cubeRotationX, 1, 0, 0);
        android.opengl.Matrix.rotateM(rotY, 0, cubeRotationY, 0, 1, 0);
        android.opengl.Matrix.multiplyMM(temp, 0, rotY, 0, rotX, 0);
        android.opengl.Matrix.multiplyMM(rotatedVP, 0, viewProjectionMatrix, 0, temp, 0);

        // Dibujar todos los cubies
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    cubies[x][y][z].draw(shaderProgram, rotatedVP);
                }
            }
        }
    }

    /**
     * Inicia una rotación de cara
     */
    public void rotateFace(int face, boolean clockwise) {
        if (isAnimating) return;  // No permitir rotaciones durante animación

        isAnimating = true;
        animatingFace = face;
        animatingClockwise = clockwise;
        animationAngle = 0f;

        Log.d(TAG, "🔄 Rotando cara " + getFaceName(face) + (clockwise ? " CW" : " CCW"));
    }

    private String getFaceName(int face) {
        switch (face) {
            case FACE_F: return "Front";
            case FACE_B: return "Back";
            case FACE_L: return "Left";
            case FACE_R: return "Right";
            case FACE_U: return "Up";
            case FACE_D: return "Down";
            default: return "Unknown";
        }
    }

    /**
     * Obtiene los cubies de una cara específica
     */
    private List<Cubie> getCubiesForFace(int face) {
        List<Cubie> faceCubies = new ArrayList<>();

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    Cubie c = cubies[x][y][z];
                    boolean include = false;

                    switch (face) {
                        case FACE_F: include = (c.gridZ == 1); break;
                        case FACE_B: include = (c.gridZ == -1); break;
                        case FACE_L: include = (c.gridX == -1); break;
                        case FACE_R: include = (c.gridX == 1); break;
                        case FACE_U: include = (c.gridY == 1); break;
                        case FACE_D: include = (c.gridY == -1); break;
                    }

                    if (include) {
                        faceCubies.add(c);
                    }
                }
            }
        }

        return faceCubies;
    }

    /**
     * Actualiza la rotación visual de los cubies durante animación
     */
    private void updateAnimatingCubies() {
        List<Cubie> faceCubies = getCubiesForFace(animatingFace);
        float angle = animatingClockwise ? -animationAngle : animationAngle;

        float axisX = 0, axisY = 0, axisZ = 0;
        switch (animatingFace) {
            case FACE_F: axisZ = 1; break;
            case FACE_B: axisZ = -1; break;
            case FACE_L: axisX = -1; break;
            case FACE_R: axisX = 1; break;
            case FACE_U: axisY = 1; break;
            case FACE_D: axisY = -1; break;
        }

        for (Cubie cubie : faceCubies) {
            cubie.applyAnimationRotation(angle, axisX, axisY, axisZ);
        }
    }

    /**
     * Confirma la rotación después de la animación
     */
    private void commitRotation() {
        List<Cubie> faceCubies = getCubiesForFace(animatingFace);

        // Rotar posiciones en la grilla
        int axis = 0;
        switch (animatingFace) {
            case FACE_F: case FACE_B: axis = 2; break;  // Z
            case FACE_L: case FACE_R: axis = 0; break;  // X
            case FACE_U: case FACE_D: axis = 1; break;  // Y
        }

        // Determinar dirección real
        boolean cw = animatingClockwise;
        if (animatingFace == FACE_B || animatingFace == FACE_L || animatingFace == FACE_D) {
            cw = !cw;  // Invertir para caras negativas
        }

        // Rotar las posiciones de los cubies en la grilla
        for (Cubie cubie : faceCubies) {
            int oldX = cubie.gridX;
            int oldY = cubie.gridY;
            int oldZ = cubie.gridZ;
            int newX = oldX, newY = oldY, newZ = oldZ;

            if (axis == 2) { // Rotación en Z (F/B)
                if (cw) {
                    newX = oldY;
                    newY = -oldX;
                } else {
                    newX = -oldY;
                    newY = oldX;
                }
            } else if (axis == 0) { // Rotación en X (L/R)
                if (cw) {
                    newY = -oldZ;
                    newZ = oldY;
                } else {
                    newY = oldZ;
                    newZ = -oldY;
                }
            } else { // Rotación en Y (U/D)
                if (cw) {
                    newX = -oldZ;
                    newZ = oldX;
                } else {
                    newX = oldZ;
                    newZ = -oldX;
                }
            }

            cubie.updateGridPosition(newX, newY, newZ);
            cubie.rotateFaceColors(axis, cw);
            cubie.commitRotation();
        }

        // Reconstruir array de cubies
        rebuildCubiesArray();

        Log.d(TAG, "✅ Rotación completada");
    }

    /**
     * Reconstruye el array 3D de cubies después de una rotación
     */
    private void rebuildCubiesArray() {
        Cubie[][][] newCubies = new Cubie[3][3][3];

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 3; z++) {
                    Cubie c = cubies[x][y][z];
                    int nx = c.gridX + 1;
                    int ny = c.gridY + 1;
                    int nz = c.gridZ + 1;
                    newCubies[nx][ny][nz] = c;
                }
            }
        }

        cubies = newCubies;
    }

    /**
     * Rota el cubo completo (para visualización, no afecta el puzzle)
     */
    public void rotateCubeView(float deltaX, float deltaY) {
        cubeRotationY += deltaX * 0.5f;
        cubeRotationX += deltaY * 0.5f;

        // Limitar rotación vertical
        cubeRotationX = Math.max(-60f, Math.min(60f, cubeRotationX));
    }

    /**
     * ¿Está animando?
     */
    public boolean isAnimating() {
        return isAnimating;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS DE CONTROL DEL CUBO
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Mezcla el cubo con movimientos aleatorios
     */
    public void shuffle(int numMoves) {
        if (isAnimating || isSolving || isShuffling) return;

        Log.d(TAG, "🔀 Iniciando shuffle con " + numMoves + " movimientos");
        isShuffling = true;
        moveQueue.clear();
        moveHistory.clear();

        // Generar movimientos aleatorios
        List<String> shuffleMoves = solver.generateShuffle(numMoves);
        moveQueue.addAll(shuffleMoves);

        // Aplicar al estado lógico
        for (String move : shuffleMoves) {
            cubeState.applyMove(move);
            moveHistory.add(move);
        }

        solveDelay = 0;  // Empezar inmediatamente
    }

    /**
     * Inicia el auto-solve del cubo
     * TRUE SOLVER: Analiza posición actual y genera solución inteligente
     */
    public void startSolve() {
        Log.d(TAG, "🤖 startSolve() llamado");

        if (isAnimating || isSolving || isShuffling) {
            Log.d(TAG, "⚠️ SOLVE ignorado: ocupado");
            return;
        }

        if (cubeState.isSolved()) {
            Log.d(TAG, "✅ Cubo ya resuelto!");
            if (solveListener != null) solveListener.onSolved();
            return;
        }

        Log.d(TAG, "🧠 TRUE SOLVER: Analizando posición actual...");
        isSolving = true;
        moveQueue.clear();

        // USAR EL TRUE SOLVER - genera solución desde cualquier posición
        List<String> solution = solver.solve(cubeState);

        if (solution.isEmpty()) {
            Log.d(TAG, "✅ No se necesitan movimientos");
            isSolving = false;
            if (solveListener != null) solveListener.onSolved();
            return;
        }

        Log.d(TAG, "📋 Solución generada: " + solution.size() + " movimientos inteligentes");
        moveQueue.addAll(solution);

        // Actualizar estado lógico con la solución
        cubeState.reset();  // El solver ya aplicó los movimientos internamente

        // Limpiar historial
        moveHistory.clear();

        solveDelay = 0;
    }

    /**
     * Resetea el cubo a estado resuelto (instantáneo)
     */
    public void reset() {
        // Cancelar operaciones en progreso
        isAnimating = false;
        isSolving = false;
        isShuffling = false;
        moveQueue.clear();
        moveHistory.clear();
        animationAngle = 0f;

        // Resetear estado lógico
        cubeState.reset();

        // Recrear cubies visuales
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    cubies[x + 1][y + 1][z + 1] = new Cubie(x, y, z);
                }
            }
        }
        Log.d(TAG, "🔄 Cubo reseteado a estado resuelto");
    }

    /**
     * Aplica un movimiento desde notación de string (U, U', R, R', etc.)
     */
    private void applyMoveInternal(String move) {
        boolean clockwise = !move.contains("'");
        char face = move.charAt(0);
        int faceId = -1;

        switch (face) {
            case 'U': faceId = FACE_U; break;
            case 'D': faceId = FACE_D; break;
            case 'F': faceId = FACE_F; break;
            case 'B': faceId = FACE_B; break;
            case 'L': faceId = FACE_L; break;
            case 'R': faceId = FACE_R; break;
        }

        if (faceId >= 0) {
            rotateFace(faceId, clockwise);
        }
    }

    /**
     * Aplica un movimiento manual del usuario
     */
    public void applyMove(String move) {
        if (isAnimating || isSolving || isShuffling) return;

        // Actualizar estado lógico
        cubeState.applyMove(move);
        moveHistory.add(move);

        // Aplicar visualmente
        applyMoveInternal(move);
    }

    /**
     * ¿Está el cubo resuelto?
     */
    public boolean isSolved() {
        return cubeState.isSolved();
    }

    /**
     * ¿Está ocupado (animando, resolviendo o mezclando)?
     */
    public boolean isBusy() {
        return isAnimating || isSolving || isShuffling;
    }

    /**
     * ¿Está resolviendo automáticamente?
     */
    public boolean isSolving() {
        return isSolving;
    }

    /**
     * ¿Está mezclando?
     */
    public boolean isShuffling() {
        return isShuffling;
    }

    /**
     * Obtiene el número de movimientos pendientes
     */
    public int getPendingMoves() {
        return moveQueue.size();
    }

    /**
     * Libera recursos
     */
    public void dispose() {
        if (shaderProgram != -1) {
            GLES20.glDeleteProgram(shaderProgram);
            shaderProgram = -1;
        }
    }
}
