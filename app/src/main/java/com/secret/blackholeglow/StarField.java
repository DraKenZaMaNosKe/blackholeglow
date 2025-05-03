package com.secret.blackholeglow;

import android.util.Log;

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
        Log.d("StarField", "Constructor: ");
        this.textureLoader = textureLoader;
        if (textureLoader != null) {
            Gatito.mensajito("el textureloader es distinto a null = " + this.textureLoader);
            this.textureId = textureLoader.getStarTexture();
            Gatito.mensajito(" entonces el textureId es = " + this.textureId);
        } else {
            this.textureId = -1; // Modo sin textura
            Gatito.mensajito("textureId= 0 , porque no hay textura cargada");
        }

        Gatito.mensajito("vamos a crear las estrellas, ciclo for: "+ count + "" );
        for (int i = 0; i < count; i++) {
            if (textureId > 0) {
                stars.add(new Star(textureId));
                Gatito.mensajito("creando una estrella con textura  textureId =  " + textureId);
            } else {
                Gatito.mensajito("creando estrella sin textura dentro del mismo for  ");
                stars.add(new Star()); // Fallback sin textura
            }
        }

        Log.d("StarField", "✅ Constructor: textureId=" + textureId);
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
