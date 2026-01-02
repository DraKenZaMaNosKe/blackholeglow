package com.secret.blackholeglow.rubik;

import android.util.Log;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  CUBE STATE - Rastreo del estado de los 54 stickers del cubo Rubik       ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║  Cada cara tiene 9 posiciones (0-8):                                      ║
 * ║     [0][1][2]                                                             ║
 * ║     [3][4][5]    donde [4] es el centro (nunca cambia)                   ║
 * ║     [6][7][8]                                                             ║
 * ║                                                                           ║
 * ║  Caras: U=Up(White), D=Down(Yellow), F=Front(Red),                       ║
 * ║         B=Back(Orange), L=Left(Green), R=Right(Blue)                     ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class CubeState {
    private static final String TAG = "CubeState";

    // Índices de caras
    public static final int U = 0;  // Up - White
    public static final int D = 1;  // Down - Yellow
    public static final int F = 2;  // Front - Red
    public static final int B = 3;  // Back - Orange
    public static final int L = 4;  // Left - Green
    public static final int R = 5;  // Right - Blue

    // Colores (matching Cubie.java)
    public static final int WHITE = 0;
    public static final int YELLOW = 1;
    public static final int RED = 2;
    public static final int ORANGE = 3;
    public static final int GREEN = 4;
    public static final int BLUE = 5;

    // Estado: 6 caras x 9 stickers cada una
    private int[][] state = new int[6][9];

    public CubeState() {
        reset();
    }

    /**
     * Resetea el cubo al estado resuelto
     */
    public void reset() {
        // Cada cara tiene su color correspondiente
        for (int i = 0; i < 9; i++) {
            state[U][i] = WHITE;
            state[D][i] = YELLOW;
            state[F][i] = RED;
            state[B][i] = ORANGE;
            state[L][i] = GREEN;
            state[R][i] = BLUE;
        }
        Log.d(TAG, "Cubo reseteado a estado resuelto");
    }

    /**
     * Copia el estado actual
     */
    public CubeState copy() {
        CubeState c = new CubeState();
        for (int face = 0; face < 6; face++) {
            System.arraycopy(this.state[face], 0, c.state[face], 0, 9);
        }
        return c;
    }

    /**
     * Obtiene el color de un sticker
     */
    public int getSticker(int face, int pos) {
        return state[face][pos];
    }

    /**
     * Establece el color de un sticker
     */
    public void setSticker(int face, int pos, int color) {
        state[face][pos] = color;
    }

    /**
     * Verifica si el cubo está resuelto
     */
    public boolean isSolved() {
        for (int face = 0; face < 6; face++) {
            int centerColor = state[face][4];
            for (int i = 0; i < 9; i++) {
                if (state[face][i] != centerColor) {
                    return false;
                }
            }
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROTACIONES DE CARAS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rota una cara 90° en sentido horario (viendo la cara de frente)
     */
    private void rotateFaceClockwise(int face) {
        int[] temp = state[face].clone();
        state[face][0] = temp[6];
        state[face][1] = temp[3];
        state[face][2] = temp[0];
        state[face][3] = temp[7];
        state[face][4] = temp[4]; // centro no cambia
        state[face][5] = temp[1];
        state[face][6] = temp[8];
        state[face][7] = temp[5];
        state[face][8] = temp[2];
    }

    /**
     * Rota una cara 90° en sentido antihorario
     */
    private void rotateFaceCounterClockwise(int face) {
        int[] temp = state[face].clone();
        state[face][0] = temp[2];
        state[face][1] = temp[5];
        state[face][2] = temp[8];
        state[face][3] = temp[1];
        state[face][4] = temp[4];
        state[face][5] = temp[7];
        state[face][6] = temp[0];
        state[face][7] = temp[3];
        state[face][8] = temp[6];
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MOVIMIENTOS ESTÁNDAR (Notación Singmaster)
    // ═══════════════════════════════════════════════════════════════════════

    public void moveU(boolean clockwise) {
        if (clockwise) {
            rotateFaceClockwise(U);
            int[] temp = {state[F][0], state[F][1], state[F][2]};
            state[F][0] = state[R][0]; state[F][1] = state[R][1]; state[F][2] = state[R][2];
            state[R][0] = state[B][0]; state[R][1] = state[B][1]; state[R][2] = state[B][2];
            state[B][0] = state[L][0]; state[B][1] = state[L][1]; state[B][2] = state[L][2];
            state[L][0] = temp[0]; state[L][1] = temp[1]; state[L][2] = temp[2];
        } else {
            rotateFaceCounterClockwise(U);
            int[] temp = {state[F][0], state[F][1], state[F][2]};
            state[F][0] = state[L][0]; state[F][1] = state[L][1]; state[F][2] = state[L][2];
            state[L][0] = state[B][0]; state[L][1] = state[B][1]; state[L][2] = state[B][2];
            state[B][0] = state[R][0]; state[B][1] = state[R][1]; state[B][2] = state[R][2];
            state[R][0] = temp[0]; state[R][1] = temp[1]; state[R][2] = temp[2];
        }
    }

    public void moveD(boolean clockwise) {
        if (clockwise) {
            rotateFaceClockwise(D);
            int[] temp = {state[F][6], state[F][7], state[F][8]};
            state[F][6] = state[L][6]; state[F][7] = state[L][7]; state[F][8] = state[L][8];
            state[L][6] = state[B][6]; state[L][7] = state[B][7]; state[L][8] = state[B][8];
            state[B][6] = state[R][6]; state[B][7] = state[R][7]; state[B][8] = state[R][8];
            state[R][6] = temp[0]; state[R][7] = temp[1]; state[R][8] = temp[2];
        } else {
            rotateFaceCounterClockwise(D);
            int[] temp = {state[F][6], state[F][7], state[F][8]};
            state[F][6] = state[R][6]; state[F][7] = state[R][7]; state[F][8] = state[R][8];
            state[R][6] = state[B][6]; state[R][7] = state[B][7]; state[R][8] = state[B][8];
            state[B][6] = state[L][6]; state[B][7] = state[L][7]; state[B][8] = state[L][8];
            state[L][6] = temp[0]; state[L][7] = temp[1]; state[L][8] = temp[2];
        }
    }

    public void moveF(boolean clockwise) {
        if (clockwise) {
            rotateFaceClockwise(F);
            int[] temp = {state[U][6], state[U][7], state[U][8]};
            state[U][6] = state[L][8]; state[U][7] = state[L][5]; state[U][8] = state[L][2];
            state[L][2] = state[D][0]; state[L][5] = state[D][1]; state[L][8] = state[D][2];
            state[D][0] = state[R][6]; state[D][1] = state[R][3]; state[D][2] = state[R][0];
            state[R][0] = temp[0]; state[R][3] = temp[1]; state[R][6] = temp[2];
        } else {
            rotateFaceCounterClockwise(F);
            int[] temp = {state[U][6], state[U][7], state[U][8]};
            state[U][6] = state[R][0]; state[U][7] = state[R][3]; state[U][8] = state[R][6];
            state[R][0] = state[D][2]; state[R][3] = state[D][1]; state[R][6] = state[D][0];
            state[D][0] = state[L][2]; state[D][1] = state[L][5]; state[D][2] = state[L][8];
            state[L][2] = temp[2]; state[L][5] = temp[1]; state[L][8] = temp[0];
        }
    }

    public void moveB(boolean clockwise) {
        if (clockwise) {
            rotateFaceClockwise(B);
            int[] temp = {state[U][0], state[U][1], state[U][2]};
            state[U][0] = state[R][2]; state[U][1] = state[R][5]; state[U][2] = state[R][8];
            state[R][2] = state[D][8]; state[R][5] = state[D][7]; state[R][8] = state[D][6];
            state[D][6] = state[L][0]; state[D][7] = state[L][3]; state[D][8] = state[L][6];
            state[L][0] = temp[2]; state[L][3] = temp[1]; state[L][6] = temp[0];
        } else {
            rotateFaceCounterClockwise(B);
            int[] temp = {state[U][0], state[U][1], state[U][2]};
            state[U][0] = state[L][6]; state[U][1] = state[L][3]; state[U][2] = state[L][0];
            state[L][0] = state[D][6]; state[L][3] = state[D][7]; state[L][6] = state[D][8];
            state[D][6] = state[R][8]; state[D][7] = state[R][5]; state[D][8] = state[R][2];
            state[R][2] = temp[0]; state[R][5] = temp[1]; state[R][8] = temp[2];
        }
    }

    public void moveL(boolean clockwise) {
        if (clockwise) {
            rotateFaceClockwise(L);
            int[] temp = {state[U][0], state[U][3], state[U][6]};
            state[U][0] = state[B][8]; state[U][3] = state[B][5]; state[U][6] = state[B][2];
            state[B][2] = state[D][6]; state[B][5] = state[D][3]; state[B][8] = state[D][0];
            state[D][0] = state[F][0]; state[D][3] = state[F][3]; state[D][6] = state[F][6];
            state[F][0] = temp[0]; state[F][3] = temp[1]; state[F][6] = temp[2];
        } else {
            rotateFaceCounterClockwise(L);
            int[] temp = {state[U][0], state[U][3], state[U][6]};
            state[U][0] = state[F][0]; state[U][3] = state[F][3]; state[U][6] = state[F][6];
            state[F][0] = state[D][0]; state[F][3] = state[D][3]; state[F][6] = state[D][6];
            state[D][0] = state[B][8]; state[D][3] = state[B][5]; state[D][6] = state[B][2];
            state[B][2] = temp[2]; state[B][5] = temp[1]; state[B][8] = temp[0];
        }
    }

    public void moveR(boolean clockwise) {
        if (clockwise) {
            rotateFaceClockwise(R);
            int[] temp = {state[U][2], state[U][5], state[U][8]};
            state[U][2] = state[F][2]; state[U][5] = state[F][5]; state[U][8] = state[F][8];
            state[F][2] = state[D][2]; state[F][5] = state[D][5]; state[F][8] = state[D][8];
            state[D][2] = state[B][6]; state[D][5] = state[B][3]; state[D][8] = state[B][0];
            state[B][0] = temp[2]; state[B][3] = temp[1]; state[B][6] = temp[0];
        } else {
            rotateFaceCounterClockwise(R);
            int[] temp = {state[U][2], state[U][5], state[U][8]};
            state[U][2] = state[B][6]; state[U][5] = state[B][3]; state[U][8] = state[B][0];
            state[B][0] = state[D][8]; state[B][3] = state[D][5]; state[B][6] = state[D][2];
            state[D][2] = state[F][2]; state[D][5] = state[F][5]; state[D][8] = state[F][8];
            state[F][2] = temp[0]; state[F][5] = temp[1]; state[F][8] = temp[2];
        }
    }

    /**
     * Ejecuta un movimiento dado como String (U, U', D, D', F, F', B, B', L, L', R, R')
     */
    public void applyMove(String move) {
        boolean clockwise = !move.contains("'");
        char face = move.charAt(0);

        switch (face) {
            case 'U': moveU(clockwise); break;
            case 'D': moveD(clockwise); break;
            case 'F': moveF(clockwise); break;
            case 'B': moveB(clockwise); break;
            case 'L': moveL(clockwise); break;
            case 'R': moveR(clockwise); break;
        }
    }

    /**
     * Aplica una secuencia de movimientos
     */
    public void applyMoves(String[] moves) {
        for (String move : moves) {
            if (move != null && !move.isEmpty()) {
                applyMove(move);
            }
        }
    }

    /**
     * Debug: imprime el estado del cubo
     */
    public void printState() {
        String[] colors = {"W", "Y", "R", "O", "G", "B"};
        String[] faceNames = {"U", "D", "F", "B", "L", "R"};

        StringBuilder sb = new StringBuilder("\n╔══ CUBE STATE ══╗\n");
        for (int face = 0; face < 6; face++) {
            sb.append(faceNames[face]).append(": ");
            for (int i = 0; i < 9; i++) {
                sb.append(colors[state[face][i]]);
                if (i == 2 || i == 5) sb.append("|");
            }
            sb.append("\n");
        }
        sb.append("╚════════════════╝");
        Log.d(TAG, sb.toString());
    }

    /**
     * Obtiene el estado completo como array 2D
     */
    public int[][] getState() {
        return state;
    }
}
