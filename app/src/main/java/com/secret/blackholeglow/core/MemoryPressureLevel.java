package com.secret.blackholeglow.core;

/**
 * Niveles de presion de memoria para el sistema adaptativo de escenas.
 * Compartido por SceneHealthMonitor y las escenas que reaccionan a memoria.
 */
public enum MemoryPressureLevel {
    NORMAL,     // Memoria suficiente - calidad completa
    WARNING,    // Memoria baja - reducir elementos no esenciales
    CRITICAL    // Memoria critica - minimo absoluto para evitar OOM
}
