package com.secret.blackholeglow;

public class PathController {
    private float angle = 0f;

    public float dx = 0f;
    public float dy = 0f;

    private float time = 0f;

    public void update(float deltaTime) {
        time += deltaTime;

        // Oscilaciones suaves en la dirección con seno y coseno
        angle = (float) (Math.sin(time * 0.5f) * 0.5f); // Oscila entre -0.5 y 0.5 radianes (~30°)
        dx = (float) Math.cos(angle);
        dy = (float) Math.sin(angle);
    }
}