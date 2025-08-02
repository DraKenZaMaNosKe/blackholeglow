// CameraAware.java
package com.secret.blackholeglow;

/**
 * Marca los SceneObject que necesitan recibir el CameraController.
 */
public interface CameraAware {
    void setCameraController(CameraController camera);
}
