package com.secret.blackholeglow;

public interface TextureLoader {
    int getTexture(int resourceId);

    // 👇 Este método es el que faltaba
    int getStarTexture();
}