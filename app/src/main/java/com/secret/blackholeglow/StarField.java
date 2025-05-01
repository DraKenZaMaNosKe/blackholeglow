package com.secret.blackholeglow;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class StarField implements SceneObject {

    private int textureId = 0;
    private final List<Star> stars = new ArrayList<>();
    private float tunnelAngle = 0f;
    private float tunnelSpeed = 0.5f;
    private boolean initialized = false;

    // Constructor vacío para diferir la inicialización hasta onSurfaceCreated
    public StarField() {
    }

    // Método que se llama cuando OpenGL ya está listo (dentro de onSurfaceCreated)
    public void initialize(Context context) {
        if (initialized) return; // Evita inicializar más de una vez

        try {
            textureId = ShaderUtils.loadTexture(context.getApplicationContext(), R.drawable.star_glow);
        } catch (RuntimeException e) {
            textureId = 0;
            android.util.Log.e("StarField", "❌ No se pudo cargar la textura star_glow", e);
        }

        // Crear estrellas una vez que tengamos textura
        for (int i = 0; i < 50; i++) {
            stars.add(new Star(textureId));
        }

        initialized = true;
    }

    @Override
    public void update(float deltaTime) {
        if (!initialized) return;

        tunnelAngle += tunnelSpeed * deltaTime;

        for (Star star : stars) {
            star.update(deltaTime, tunnelAngle);
        }
    }

    @Override
    public void draw() {
        if (!initialized) return;

        for (Star star : stars) {
            star.draw();
        }
    }
}