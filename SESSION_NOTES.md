# Black Hole Glow - Notas de Sesión

---

# SESIÓN: 30 de Noviembre, 2024 - Beta 1.0

## Branch: `beta1.0`

---

## 🎯 Resumen de Esta Sesión

Esta sesión se enfocó en:
1. **Configuración completa de AdMob** (Interstitial + Rewarded)
2. **Creación de nuevos Actores/Sistemas** para monetización
3. **Actualización del catálogo de wallpapers** (3 wallpapers)
4. **Optimización de UI** (AnimatedGlowButton, AnimatedGlowCard)
5. **Efecto épico al tocar botón** (onda expansiva + rebote + vibración)

---

## 💰 Configuración AdMob

### IDs de PRODUCCIÓN (Cuenta: Eduardo Javier Contreras Roman)
```
App ID:         ca-app-pub-6734758230109098~7716587922
Interstitial:   ca-app-pub-6734758230109098/1797212684
Rewarded:       ca-app-pub-6734758230109098/9484131013
AdSense:        pub-6734758230109098
Payment:        MXN 1,200.00 threshold
```

### IDs de TEST (Actualmente en uso - cuenta pendiente de aprobación)
```
App ID:         ca-app-pub-3940256099942544~3347511713
Interstitial:   ca-app-pub-3940256099942544/1033173712
Rewarded:       ca-app-pub-3940256099942544/5224354917
```

### Archivos Configurados
| Archivo | Configuración |
|---------|---------------|
| `AndroidManifest.xml` | App ID de test (temporal) |
| `AdsManager.java` | IDs de test/producción |
| `libs.versions.toml` | `play-services-ads = "23.5.0"` |
| `build.gradle.kts` | implementation de ads |
| `gma_ad_services_config.xml` | Config de servicios de ads |

### ⚠️ CAMBIAR A PRODUCCIÓN CUANDO:
1. AdMob apruebe la cuenta (24-48 horas)
2. En `AndroidManifest.xml`: Cambiar `APPLICATION_ID` al de producción
3. En `AdsManager.java`: Cambiar flags a usar IDs de producción

---

## 🏗️ Sistema de Actores (Architecture)

### NUEVOS Actores Creados Esta Sesión

| Actor | Archivo | Responsabilidad |
|-------|---------|-----------------|
| **AdsManager** | `systems/AdsManager.java` | Gestión AdMob (Interstitial + Rewarded) |
| **UsageTracker** | `systems/UsageTracker.java` | Rastreo de uso para trigger de ads |
| **RewardsManager** | `systems/RewardsManager.java` | Sistema de recompensas por ads |
| **RemoteConfigManager** | `systems/RemoteConfigManager.java` | Firebase Remote Config |
| **MissionsManager** | `systems/MissionsManager.java` | Misiones diarias/semanales |
| **SubscriptionManager** | `systems/SubscriptionManager.java` | Gestión de suscripciones premium |
| **WallpaperCatalog** | `systems/WallpaperCatalog.java` | Catálogo centralizado |
| **GLStateManager** | `systems/GLStateManager.java` | Estado OpenGL |
| **ScreenManager** | `systems/ScreenManager.java` | Control de pantallas |
| **WallpaperTier** | `models/WallpaperTier.java` | Enum de tiers |

### Actores Existentes
- `EventBus` - Sistema de eventos
- `MusicSystem` - Audio
- `ResourceManager` - Texturas y recursos
- `ScreenEffectsManager` - Efectos visuales
- `UIController` - Control de UI
- `FirebaseQueueManager` - Cola Firebase

---

## 🎨 Catálogo de Wallpapers

### Wallpapers Actuales (3 total)

| # | Nombre | Scene Name | Tier | Badge | Preview |
|---|--------|------------|------|-------|---------|
| 1 | Batalla Cósmica | `Universo` | FREE | 🔥 POPULAR | `preview_universo` |
| 2 | Fondo del Mar | `Fondo del Mar` | COMING_SOON | 🌊 PRÓXIMAMENTE | `preview_beach` (temp) |
| 3 | La Mansión | `La Mansión` | COMING_SOON | 👻 PRÓXIMAMENTE | `preview_storm` (temp) |

### Sistema de Tiers
```java
public enum WallpaperTier {
    FREE,           // Gratis para todos
    PREMIUM,        // Requiere suscripción
    VIP,            // Contenido exclusivo
    COMING_SOON,    // Próximamente (bloqueado)
    BETA            // En desarrollo
}
```

### UI para COMING_SOON
- Badge visible con texto del tier
- Overlay oscuro semitransparente (#80000000)
- Botón deshabilitado con texto "🔒 PRÓXIMAMENTE"

---

## ⚡ Optimizaciones de UI

### AnimatedGlowCard (OPTIMIZADO)
**Antes:**
- ValueAnimator constante (60 invalidate/seg)
- setShadowLayer (muy costoso GPU)

**Después:**
- Gradiente ESTÁTICO cacheado
- Solo recrea si cambia tamaño
- 0 animaciones constantes
- Sin setShadowLayer

### AnimatedGlowButton (OPTIMIZADO + ÉPICO)
**Optimizaciones:**
- Sin animaciones constantes
- Gradiente cacheado
- Sin setShadowLayer

**Efecto al TOCAR (on-demand):**
1. **Press**: Encoge a 92% (80ms) + vibración 20ms
2. **Release**:
   - Rebote con OvershootInterpolator(3f) (400ms)
   - Destello brillante RadialGradient (300ms)
   - Onda expansiva desde punto de toque (500ms)

---

## 📁 Estructura de Archivos Modificados

```
app/src/main/java/com/secret/blackholeglow/
├── systems/
│   ├── AdsManager.java          # NUEVO - AdMob
│   ├── UsageTracker.java        # NUEVO - Rastreo
│   ├── RewardsManager.java      # NUEVO - Recompensas
│   ├── RemoteConfigManager.java # NUEVO - Firebase Config
│   ├── MissionsManager.java     # NUEVO - Misiones
│   ├── SubscriptionManager.java # NUEVO - Suscripciones
│   ├── WallpaperCatalog.java    # NUEVO - Catálogo
│   ├── GLStateManager.java      # NUEVO - OpenGL State
│   ├── ScreenManager.java       # NUEVO - Pantallas
│   └── EventBus.java            # MODIFICADO
├── models/
│   ├── WallpaperItem.java       # MODIFICADO - Builder pattern
│   └── WallpaperTier.java       # NUEVO - Enum
├── ui/
│   ├── AnimatedGlowButton.java  # MODIFICADO - Efecto épico
│   └── AnimatedGlowCard.java    # MODIFICADO - Optimizado
├── adapters/
│   └── WallpaperAdapter.java    # MODIFICADO - Badges/Overlay
└── fragments/
    └── AnimatedWallpaperListFragment.java  # MODIFICADO

app/src/main/res/
├── drawable/
│   └── badge_background.xml     # NUEVO
├── layout/
│   └── item_wallpaper_fullscreen.xml  # MODIFICADO
└── xml/
    └── gma_ad_services_config.xml     # NUEVO
```

---

## 🔧 Comandos Útiles

```bash
# Build debug
./gradlew assembleDebug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Build release
./gradlew assembleRelease
./gradlew bundleRelease

# Logs de ads
adb logcat -s AdsManager:D

# Logs generales
adb logcat | grep -i "blackholeglow"
```

---

## 🔐 Keystore de Release

```
Archivo:     blackholeglow-release-key.jks
Ubicación:   Raíz del proyecto
Password:    blackholeglow2025
Key Alias:   blackholeglow
Key Pass:    blackholeglow2025
```

⚠️ **CRÍTICO**: Sin este keystore NO se puede actualizar en Play Store.

---

## 🚀 Próximos Pasos

### Inmediato
- [ ] Esperar aprobación AdMob → cambiar a IDs producción
- [ ] Crear imágenes preview reales para nuevos wallpapers

### Desarrollo
- [ ] Implementar escena "Fondo del Mar"
- [ ] Implementar escena "La Mansión"
- [ ] Integrar MissionsManager en UI
- [ ] Activar RewardsManager con rewarded ads

### Monetización
- [ ] Configurar suscripciones en Play Console
- [ ] Verificación de suscripción activa
- [ ] A/B testing con Remote Config

---

---

# SESIÓN ANTERIOR: Noviembre 24, 2024 - Version 4.0.0

## Branch: `version-4.0.0`

## Características Implementadas

### Sistema de Armas Láser del OVNI
- `UfoLaser.java` - Proyectiles láser verde/cyan
- Disparo automático cada 3-7 segundos
- Impactos en EarthShield

### Sistema de Vida del OVNI
- 3 HP, invencibilidad 1.5s post-daño
- Respawn después de 8 segundos
- Colisión con meteoritos

### Optimizaciones
- Shaders estáticos compartidos
- FloatBuffers reutilizables
- Cache de random cada 10 frames
- Distancias al cuadrado (sin sqrt)

### Documentación TV
- `exportTv.md` - Guía para Android TV

---

**Última actualización:** 30 de Noviembre, 2024
**Autor:** Claude Code + Eduardo
