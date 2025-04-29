package com.secret.blackholeglow;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

public class StarField implements SceneObject {
    private float tunnelAngle = 0f;
    private float tunnelSpeed = 0.5f;
    private final List<Star> stars = new ArrayList<>();
    private final PathController pathController = new PathController();

    public StarField(int count) {
        for (int i = 0; i < count; i++) {
            stars.add(new Star());
        }
    }

    @Override
    public void update(float deltaTime) {
        tunnelAngle += tunnelSpeed * deltaTime; // El túnel gira suavemente

        for (Star star : stars) {
            star.update(deltaTime, tunnelAngle);
        }
    }

    @Override
    public void draw() {
        for (Star star : stars) {
            star.draw();
        }
    }
}