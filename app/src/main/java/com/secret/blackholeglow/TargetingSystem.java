package com.secret.blackholeglow;

import android.util.Log;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║   🎯 TARGETING SYSTEM - Sistema de Apuntado Asistido Inteligente          ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  CARACTERÍSTICAS:                                                         ║
 * ║  • Detecta cuando el HumanInterceptor mira hacia un enemigo              ║
 * ║  • Sistema de Lock-On automático                                          ║
 * ║  • Tracking del enemigo aunque se mueva                                   ║
 * ║  • Integración con disparo especial por toque del usuario                ║
 * ║                                                                           ║
 * ║  FLUJO:                                                                   ║
 * ║  1. Interceptor mira hacia enemigo → Mira AMARILLA (buscando)            ║
 * ║  2. Mantiene vista 0.5s → Mira ROJA (locked)                             ║
 * ║  3. Usuario toca mira → PLASMA EXPLOSION                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class TargetingSystem {
    private static final String TAG = "TargetingSystem";

    // ═══════════════════════════════════════════════════════════════════════
    // 🎯 CONFIGURACIÓN DEL SISTEMA
    // ═══════════════════════════════════════════════════════════════════════
    private static final float LOCK_ANGLE_THRESHOLD = 35f;   // Grados para considerar "mirando"
    private static final float LOCK_DISTANCE_MAX = 8f;       // Distancia máxima para lock
    private static final float LOCK_TIME_REQUIRED = 0.5f;    // Tiempo para lock completo
    private static final float LOCK_LOST_TIME = 0.3f;        // Tiempo antes de perder lock

    // ═══════════════════════════════════════════════════════════════════════
    // 🔗 REFERENCIAS
    // ═══════════════════════════════════════════════════════════════════════
    private HumanInterceptor interceptor;
    private UfoAttacker ufoAttacker;
    private UfoScout ufoScout;
    private CameraController camera;

    // ═══════════════════════════════════════════════════════════════════════
    // 📊 ESTADO DEL TARGETING
    // ═══════════════════════════════════════════════════════════════════════
    public enum TargetState {
        NO_TARGET,      // Sin objetivo en vista
        SEARCHING,      // Objetivo detectado, buscando lock
        LOCKED,         // Lock completo, listo para disparar
        FIRING          // Disparando (breve estado)
    }

    private TargetState currentState = TargetState.NO_TARGET;
    private Object lockedTarget = null;         // UfoAttacker o UfoScout
    private float lockProgress = 0f;            // 0.0 a 1.0
    private float lockLostTimer = 0f;           // Timer para perder lock
    private float firingTimer = 0f;             // Timer durante disparo

    // Posición del objetivo en pantalla (para la mira y touch detection)
    private float targetScreenX = 0f;
    private float targetScreenY = 0f;
    private float targetWorldX = 0f;
    private float targetWorldY = 0f;
    private float targetWorldZ = 0f;

    // Tamaño del área de touch para disparar
    private static final float TOUCH_RADIUS = 0.15f;

    // Cooldown del disparo especial
    private float specialWeaponCooldown = 0f;
    private static final float SPECIAL_WEAPON_COOLDOWN_MAX = 1.5f;  // 1.5 segundos de cooldown

    // Callback para cuando se dispara
    private OnSpecialFireListener fireListener;

    /**
     * Constructor
     */
    public TargetingSystem() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   🎯 TARGETING SYSTEM INICIADO         ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔧 CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    public void setInterceptor(HumanInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public void setTargets(UfoAttacker attacker, UfoScout scout) {
        this.ufoAttacker = attacker;
        this.ufoScout = scout;
    }

    public void setCamera(CameraController camera) {
        this.camera = camera;
    }

    public void setOnSpecialFireListener(OnSpecialFireListener listener) {
        this.fireListener = listener;
    }

    /**
     * 👆 Forzar lock-on manual cuando el usuario toca una nave enemiga
     * La mira se coloca inmediatamente sobre el objetivo seleccionado
     * @param enemy El enemigo tocado (UfoAttacker o UfoScout)
     */
    public void setManualTarget(Object enemy) {
        if (enemy == null) {
            Log.d(TAG, "❌ setManualTarget: enemigo null");
            return;
        }

        // Obtener posición del enemigo
        if (enemy instanceof UfoAttacker) {
            UfoAttacker ufo = (UfoAttacker) enemy;
            if (ufo.isDestroyed()) {
                Log.d(TAG, "❌ UfoAttacker ya destruido");
                return;
            }
            targetWorldX = ufo.x;
            targetWorldY = ufo.y;
            targetWorldZ = ufo.z;
            lockedTarget = ufo;
        } else if (enemy instanceof UfoScout) {
            UfoScout ufo = (UfoScout) enemy;
            if (ufo.isDestroyed()) {
                Log.d(TAG, "❌ UfoScout ya destruido");
                return;
            }
            targetWorldX = ufo.getX();
            targetWorldY = ufo.getY();
            targetWorldZ = ufo.getZ();
            lockedTarget = ufo;
        } else {
            Log.d(TAG, "❌ Tipo de enemigo desconocido: " + enemy.getClass().getSimpleName());
            return;
        }

        // LOCK INMEDIATO - la mira aparece directamente sobre el objetivo
        currentState = TargetState.LOCKED;
        lockProgress = 1.0f;
        lockLostTimer = 0f;

        // Calcular posición en pantalla usando proyección real
        float[] screenCoords = worldToScreen(targetWorldX, targetWorldY, targetWorldZ);
        targetScreenX = screenCoords[0];
        targetScreenY = screenCoords[1];

        Log.d(TAG, "👆🎯 LOCK MANUAL: " + enemy.getClass().getSimpleName() +
              " en pantalla (" + String.format("%.2f", targetScreenX) + ", " +
              String.format("%.2f", targetScreenY) + ")");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔄 UPDATE
    // ═══════════════════════════════════════════════════════════════════════

    public void update(float deltaTime) {
        if (interceptor == null || interceptor.isDestroyed()) {
            resetLock();
            return;
        }

        // Actualizar cooldown
        if (specialWeaponCooldown > 0) {
            specialWeaponCooldown -= deltaTime;
        }

        // Estado de disparo
        if (currentState == TargetState.FIRING) {
            firingTimer -= deltaTime;
            if (firingTimer <= 0) {
                currentState = TargetState.NO_TARGET;
                lockedTarget = null;
            }
            return;
        }

        // Buscar el mejor objetivo
        Object bestTarget = findBestTarget();

        if (bestTarget != null) {
            // Tenemos un objetivo en vista
            if (lockedTarget != bestTarget) {
                // Nuevo objetivo
                lockedTarget = bestTarget;
                lockProgress = 0f;
                currentState = TargetState.SEARCHING;
                Log.d(TAG, "🎯 Nuevo objetivo detectado: " + bestTarget.getClass().getSimpleName());
            }

            // Incrementar progreso de lock
            lockProgress += deltaTime / LOCK_TIME_REQUIRED;
            lockLostTimer = 0f;

            if (lockProgress >= 1.0f) {
                lockProgress = 1.0f;
                if (currentState != TargetState.LOCKED) {
                    currentState = TargetState.LOCKED;
                    Log.d(TAG, "🔒 ¡OBJETIVO BLOQUEADO! Listo para disparo especial");
                }
            } else {
                currentState = TargetState.SEARCHING;
            }

            // Actualizar posición del objetivo
            updateTargetPosition();

        } else {
            // Sin objetivo en vista
            if (lockedTarget != null) {
                lockLostTimer += deltaTime;
                if (lockLostTimer >= LOCK_LOST_TIME) {
                    resetLock();
                    Log.d(TAG, "❌ Lock perdido");
                }
            }
        }
    }

    /**
     * Busca el mejor objetivo basado en la dirección del interceptor
     */
    private Object findBestTarget() {
        if (interceptor == null) return null;

        float interceptorX = interceptor.getX();
        float interceptorY = interceptor.getY();
        float interceptorZ = interceptor.getZ();

        // Obtener dirección del interceptor (basada en su rotación)
        // El interceptor mira en la dirección de su rotationY
        // Necesitamos calcular si el ángulo hacia el enemigo está dentro del threshold

        Object bestTarget = null;
        float bestScore = Float.MAX_VALUE;

        // Verificar UfoAttacker
        if (ufoAttacker != null && !ufoAttacker.isDestroyed()) {
            float score = calculateTargetScore(
                interceptorX, interceptorY, interceptorZ,
                ufoAttacker.x, ufoAttacker.y, ufoAttacker.z
            );
            if (score < bestScore && score >= 0) {
                bestScore = score;
                bestTarget = ufoAttacker;
            }
        }

        // Verificar UfoScout
        if (ufoScout != null && !ufoScout.isDestroyed()) {
            float score = calculateTargetScore(
                interceptorX, interceptorY, interceptorZ,
                ufoScout.getX(), ufoScout.getY(), ufoScout.getZ()
            );
            if (score < bestScore && score >= 0) {
                bestScore = score;
                bestTarget = ufoScout;
            }
        }

        return bestTarget;
    }

    /**
     * Calcula un score para el objetivo (menor = mejor)
     * Retorna -1 si está fuera de rango o ángulo
     */
    private float calculateTargetScore(float fromX, float fromY, float fromZ,
                                       float toX, float toY, float toZ) {
        // Calcular distancia
        float dx = toX - fromX;
        float dy = toY - fromY;
        float dz = toZ - fromZ;
        float distance = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (distance > LOCK_DISTANCE_MAX || distance < 0.5f) {
            return -1;
        }

        // Calcular ángulo hacia el objetivo (en el plano XZ)
        float angleToTarget = (float) Math.toDegrees(Math.atan2(dx, dz));

        // Normalizar ángulo
        while (angleToTarget < 0) angleToTarget += 360;
        while (angleToTarget >= 360) angleToTarget -= 360;

        // El interceptor generalmente mira hacia donde se mueve
        // Aproximar su dirección de vista basándonos en su movimiento previo
        // Por simplicidad, asumimos que siempre puede apuntar a objetivos frente a él

        // Calcular diferencia de ángulo (simplificado)
        // El interceptor puede ver objetivos en un cono amplio frente a él
        float angleDiff = Math.abs(angleToTarget);
        if (angleDiff > 180) angleDiff = 360 - angleDiff;

        // Si el ángulo es muy grande, no es un buen objetivo
        if (angleDiff > LOCK_ANGLE_THRESHOLD * 3) {
            return -1;
        }

        // Score basado en distancia y ángulo
        return distance + angleDiff * 0.1f;
    }

    /**
     * Actualiza la posición del objetivo lockeado
     */
    private void updateTargetPosition() {
        if (lockedTarget == null) return;

        if (lockedTarget instanceof UfoAttacker) {
            UfoAttacker ufo = (UfoAttacker) lockedTarget;
            targetWorldX = ufo.x;
            targetWorldY = ufo.y;
            targetWorldZ = ufo.z;
        } else if (lockedTarget instanceof UfoScout) {
            UfoScout ufo = (UfoScout) lockedTarget;
            targetWorldX = ufo.getX();
            targetWorldY = ufo.getY();
            targetWorldZ = ufo.getZ();
        }

        // Convertir a coordenadas de pantalla si tenemos cámara
        if (camera != null) {
            float[] screenCoords = worldToScreen(targetWorldX, targetWorldY, targetWorldZ);
            targetScreenX = screenCoords[0];
            targetScreenY = screenCoords[1];
        }
    }

    /**
     * Convierte coordenadas del mundo a pantalla normalizada (-1 a 1)
     * Usa la matriz VP de la cámara para proyección correcta
     */
    private float[] worldToScreen(float worldX, float worldY, float worldZ) {
        if (camera == null) {
            // Fallback a aproximación si no hay cámara
            return new float[]{worldX * 0.3f, worldY * 0.3f};
        }

        // Obtener matriz View-Projection
        float[] vpMatrix = camera.getViewProjectionMatrix();

        // Proyectar punto 3D: [x, y, z, 1] * VP = [clipX, clipY, clipZ, clipW]
        float clipX = vpMatrix[0] * worldX + vpMatrix[4] * worldY + vpMatrix[8] * worldZ + vpMatrix[12];
        float clipY = vpMatrix[1] * worldX + vpMatrix[5] * worldY + vpMatrix[9] * worldZ + vpMatrix[13];
        float clipW = vpMatrix[3] * worldX + vpMatrix[7] * worldY + vpMatrix[11] * worldZ + vpMatrix[15];

        // Dividir por W para obtener coordenadas normalizadas (NDC)
        float ndcX = clipX / clipW;
        float ndcY = clipY / clipW;

        // Clamp a rango válido
        ndcX = Math.max(-0.95f, Math.min(0.95f, ndcX));
        ndcY = Math.max(-0.95f, Math.min(0.95f, ndcY));

        return new float[]{ndcX, ndcY};
    }

    /**
     * Resetea el estado del lock
     */
    private void resetLock() {
        currentState = TargetState.NO_TARGET;
        lockedTarget = null;
        lockProgress = 0f;
        lockLostTimer = 0f;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 👆 MANEJO DE TOUCH
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Verifica si el toque está sobre la mira y dispara si es válido
     * @param touchX coordenada X normalizada (-1 a 1)
     * @param touchY coordenada Y normalizada (-1 a 1)
     * @return true si se disparó el arma especial
     */
    public boolean handleTouch(float touchX, float touchY) {
        if (currentState != TargetState.LOCKED) {
            return false;
        }

        if (specialWeaponCooldown > 0) {
            Log.d(TAG, "⏳ Arma especial en cooldown: " + specialWeaponCooldown + "s");
            return false;
        }

        // Verificar si el toque está cerca de la mira
        float dx = touchX - targetScreenX;
        float dy = touchY - targetScreenY;
        float distance = (float) Math.sqrt(dx*dx + dy*dy);

        if (distance <= TOUCH_RADIUS + 0.1f) {  // Margen extra para facilidad de uso
            fireSpecialWeapon();
            return true;
        }

        return false;
    }

    /**
     * Dispara el arma especial cuando hay lock (sin verificar posición de toque)
     * Usado cuando el usuario toca cualquier parte de la pantalla mientras hay lock
     * @return true si se disparó, false si está en cooldown
     */
    public boolean fireFromTouch() {
        if (currentState != TargetState.LOCKED) {
            Log.d(TAG, "❌ No hay lock activo");
            return false;
        }

        if (specialWeaponCooldown > 0) {
            Log.d(TAG, "⏳ Arma especial en cooldown: " + String.format("%.1f", specialWeaponCooldown) + "s");
            return false;
        }

        fireSpecialWeapon();
        return true;
    }

    /**
     * Dispara el arma especial (Plasma Explosion)
     */
    private void fireSpecialWeapon() {
        Log.d(TAG, "╔════════════════════════════════════════╗");
        Log.d(TAG, "║   💥 ¡PLASMA EXPLOSION ACTIVADA!       ║");
        Log.d(TAG, "╚════════════════════════════════════════╝");

        currentState = TargetState.FIRING;
        firingTimer = 0.5f;
        specialWeaponCooldown = SPECIAL_WEAPON_COOLDOWN_MAX;

        // Notificar al listener
        if (fireListener != null && lockedTarget != null) {
            fireListener.onSpecialFire(
                targetWorldX, targetWorldY, targetWorldZ,
                lockedTarget
            );
        }

        // Aplicar daño al objetivo
        applyDamageToTarget();
    }

    /**
     * Aplica daño al objetivo lockeado
     * El Plasma Explosion hace 3x de daño (llama takeDamage 3 veces)
     */
    private void applyDamageToTarget() {
        int damageMultiplier = 3;  // Daño del plasma explosion (3x normal)

        if (lockedTarget instanceof UfoAttacker) {
            UfoAttacker ufo = (UfoAttacker) lockedTarget;
            for (int i = 0; i < damageMultiplier; i++) {
                ufo.takeDamage();
            }
            Log.d(TAG, "💥 UfoAttacker recibió " + damageMultiplier + "x daño!");
        } else if (lockedTarget instanceof UfoScout) {
            UfoScout ufo = (UfoScout) lockedTarget;
            for (int i = 0; i < damageMultiplier; i++) {
                ufo.takeDamage();
            }
            Log.d(TAG, "💥 UfoScout recibió " + damageMultiplier + "x daño!");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📊 GETTERS PARA LA MIRA VISUAL
    // ═══════════════════════════════════════════════════════════════════════

    public TargetState getState() {
        return currentState;
    }

    public float getLockProgress() {
        return lockProgress;
    }

    public float getTargetScreenX() {
        return targetScreenX;
    }

    public float getTargetScreenY() {
        return targetScreenY;
    }

    public float getTargetWorldX() {
        return targetWorldX;
    }

    public float getTargetWorldY() {
        return targetWorldY;
    }

    public float getTargetWorldZ() {
        return targetWorldZ;
    }

    public boolean hasTarget() {
        return currentState == TargetState.SEARCHING || currentState == TargetState.LOCKED;
    }

    public boolean isLocked() {
        return currentState == TargetState.LOCKED;
    }

    public boolean isFiring() {
        return currentState == TargetState.FIRING;
    }

    public float getCooldownProgress() {
        if (specialWeaponCooldown <= 0) return 1f;
        return 1f - (specialWeaponCooldown / SPECIAL_WEAPON_COOLDOWN_MAX);
    }

    public Object getLockedTarget() {
        return lockedTarget;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🔔 INTERFACE DE CALLBACK
    // ═══════════════════════════════════════════════════════════════════════

    public interface OnSpecialFireListener {
        void onSpecialFire(float targetX, float targetY, float targetZ, Object target);
    }
}
