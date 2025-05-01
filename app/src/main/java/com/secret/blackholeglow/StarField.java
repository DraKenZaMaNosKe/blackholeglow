package com.secret.blackholeglow;

import java.util.ArrayList;
import java.util.List;

public class StarField implements SceneObject {
    private final List<Star> stars = new ArrayList<>();
    private final PathController pathController = new PathController();

    private float tunnelAngle = 0f;
    private float tunnelSpeed = 0.5f;

    private final TextureLoader textureLoader;
    private final int textureId;

    public StarField(TextureLoader textureLoader, int count) {
        this.textureLoader = textureLoader;
        this.textureId = textureLoader.getStarTexture();

        for (int i = 0; i < count; i++) {
            stars.add(new Star(textureId));
        }
    }

    @Override
    public void update(float deltaTime) {
        tunnelAngle += tunnelSpeed * deltaTime;
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
