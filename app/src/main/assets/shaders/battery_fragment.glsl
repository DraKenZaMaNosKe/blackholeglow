#ifdef GL_ES
precision mediump float;
#endif

uniform float u_Time;
uniform float u_ChargingGlow;
uniform float u_BatteryLevel;

varying vec4 v_Color;

void main() {
    vec3 finalColor = v_Color.rgb;

    // Efecto de carga - ondas de energía
    if (u_ChargingGlow > 0.0) {
        // Onda de energía que recorre la barra
        float wave = sin(u_Time * 5.0 - gl_FragCoord.x * 0.02) * 0.5 + 0.5;
        wave *= u_ChargingGlow;

        // Brillo pulsante
        float pulse = sin(u_Time * 8.0) * 0.3 + 0.7;

        // Color de carga (dorado/blanco brillante)
        vec3 chargeColor = vec3(1.0, 0.9, 0.5) * wave * pulse;
        finalColor += chargeColor * u_ChargingGlow;

        // Destellos ocasionales
        float sparkle = sin(u_Time * 30.0 + gl_FragCoord.x) * sin(u_Time * 20.0 + gl_FragCoord.y);
        if (sparkle > 0.8) {
            finalColor += vec3(1.0, 1.0, 0.8) * u_ChargingGlow;
        }
    }

    // Pulsación cuando está lleno
    if (u_BatteryLevel > 0.95) {
        float fullPulse = sin(u_Time * 4.0) * 0.2 + 0.8;
        finalColor *= fullPulse;
    }

    // Efecto de energía sutil
    float energy = sin(u_Time * 10.0 + gl_FragCoord.x * 0.1) * 0.05;
    finalColor += vec3(energy) * v_Color.a;

    gl_FragColor = vec4(finalColor, v_Color.a);
}
