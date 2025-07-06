attribute vec3 a_Position;
uniform mat4 u_MVP;

// Límites de la malla para normalización
const float MIN_Y = -1.0;
const float MAX_Y =  1.0;
const float MIN_X = -1.0;
const float MAX_X =  1.0;

varying float v_PosY;
varying float v_PosX;

void main() {
    // Transformación MVP
    gl_Position = u_MVP * vec4(a_Position, 1.0);

    // Normaliza coordenadas Y y X a [0,1]
    v_PosY = (a_Position.y - MIN_Y) / (MAX_Y - MIN_Y);
    v_PosX = (a_Position.x - MIN_X) / (MAX_X - MIN_X);
}
