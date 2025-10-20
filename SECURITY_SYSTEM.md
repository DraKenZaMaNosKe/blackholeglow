# 🔐 SISTEMA DE SEGURIDAD - BLACK HOLE GLOW

## 📋 RESUMEN EJECUTIVO

Este documento explica el **sistema de seguridad anti-trampas** implementado en Black Hole Glow para proteger las estadísticas de soles destruidos.

---

## 🎯 OBJETIVO

Prevenir que los usuarios manipulen sus estadísticas (soles destruidos) mediante:
- Modificación de archivos locales (SharedPreferences)
- Inyección de valores falsos
- Reversión del contador
- Cualquier otro método de trampa

---

## 🛡️ CAPAS DE SEGURIDAD

### **1. Almacenamiento Híbrido**
- **Local (SharedPreferences)**: Para acceso rápido y funcionamiento offline
- **Remoto (Firebase Firestore)**: Fuente de verdad y sincronización

### **2. Sincronización Inteligente**
```
Al iniciar la app:
1. Cargar estadísticas locales
2. Cargar estadísticas de Firebase
3. Tomar el MAYOR de ambos valores
4. Actualizar ambos almacenamientos
```

Esto garantiza que:
- ✅ Si jugaste offline, tu progreso se sube a Firebase
- ✅ Si manipulaste archivos locales, Firebase restaura el valor real
- ✅ Nunca pierdes progreso legítimo

### **3. Validación de Incrementos**
Firebase rechaza cualquier actualización donde:
- ❌ El nuevo valor sea MENOR que el actual
- ❌ El incremento sea mayor a 10 soles de una vez
- ❌ El valor supere 100,000 soles

### **4. Hash de Seguridad (SHA-256)**
Cada estadística se guarda con un hash:
```
hash = SHA256(userId + sunsDestroyed + secret_salt)
```

Al cargar datos:
- ✅ Se recalcula el hash
- ✅ Si no coincide → datos manipulados (se loguea la advertencia)

### **5. Timestamp del Servidor**
Firebase usa `FieldValue.serverTimestamp()`:
- ✅ Imposible de manipular desde el cliente
- ✅ Auditoría de cuándo se actualizó cada estadística

---

## 🔥 REGLAS DE FIRESTORE

Las reglas de Firebase están en `firestore.rules` y **DEBES** aplicarlas:

### Cómo Aplicar las Reglas:
1. Abre [Firebase Console](https://console.firebase.google.com)
2. Selecciona el proyecto "blackholeglow"
3. Ve a **Firestore Database** → **Reglas**
4. Copia el contenido de `firestore.rules`
5. Pega en el editor
6. Haz clic en **Publicar**

### Reglas Clave:
```javascript
// Solo permite INCREMENTAR, nunca disminuir
allow update: if request.resource.data.sunsDestroyed >= resource.data.sunsDestroyed

// Máximo +10 soles por actualización
&& (request.resource.data.sunsDestroyed - resource.data.sunsDestroyed) <= 10

// Solo el propietario puede modificar sus datos
&& request.auth.uid == userId
```

---

## 📊 ESTRUCTURA DE DATOS EN FIRESTORE

### Colección: `player_stats/{userId}`
```json
{
  "userId": "abc123...",
  "sunsDestroyed": 42,
  "securityHash": "a7f3c2d9...",
  "lastUpdate": Timestamp(2024-10-19 10:30:00)
}
```

### Colección: `leaderboard/{userId}`
```json
{
  "userId": "abc123...",
  "sunsDestroyed": 42,
  "lastUpdate": Timestamp(2024-10-19 10:30:00)
}
```

**Nota**: El leaderboard es de **lectura pública**, para que todos puedan ver el ranking.

---

## 🚨 ESCENARIOS DE ATAQUE Y DEFENSA

### **Escenario 1: Usuario modifica SharedPreferences**
```
Ataque: Usuario edita archivo local y pone 9999 soles
Defensa:
1. Al abrir la app, se sincroniza con Firebase
2. Firebase tiene 50 soles (valor real)
3. Sistema detecta que 50 > 9999 es falso
4. Se intenta subir 9999 a Firebase
5. Firebase RECHAZA (incremento > 10)
6. Local se actualiza a 50 (valor de Firebase)
```

### **Escenario 2: Usuario intenta reducir contador**
```
Ataque: Usuario tiene 100 soles, intenta poner 50
Defensa:
1. Se intenta actualizar Firebase a 50
2. Regla: sunsDestroyed >= resource.data.sunsDestroyed
3. 50 < 100 → RECHAZADO
4. No se actualiza nada
```

### **Escenario 3: Usuario juega offline y luego online**
```
Escenario: Usuario destruye 10 soles sin internet
Defensa:
1. Se guardan localmente (60 soles ahora)
2. Cuando hay internet, se sincroniza
3. Firebase tiene 50, local tiene 60
4. Sistema toma el mayor: 60
5. Firebase se actualiza a 60 (incremento válido: +10)
6. Progreso legítimo guardado ✅
```

### **Escenario 4: Usuario usa herramientas de debugging**
```
Ataque: Usuario intenta inyectar valores con ADB/root
Defensa:
1. Valores locales pueden cambiar temporalmente
2. Al sincronizar con Firebase, se valida
3. Incremento inválido → RECHAZADO
4. Firebase mantiene el valor real
```

---

## 🏆 SISTEMA DE LEADERBOARD

### Características:
- ✅ **Lectura pública**: Todos ven el ranking
- ✅ **Solo escritura propia**: Solo puedes actualizar TU entrada
- ✅ **Ordenado por soles**: Los mejores arriba
- ✅ **Actualización automática**: Cada vez que destruyes un sol

### Consulta del Leaderboard (futuro):
```java
db.collection("leaderboard")
  .orderBy("sunsDestroyed", Query.Direction.DESCENDING)
  .limit(100)
  .get()
  .addOnSuccessListener(querySnapshot -> {
      // Top 100 jugadores
  });
```

---

## 📱 CÓMO USAR EN EL CÓDIGO

### Registrar Sol Destruido:
```java
// PlayerStats.java
playerStats.onSunDestroyed();
// Esto automáticamente:
// 1. Incrementa contador local
// 2. Guarda en SharedPreferences
// 3. Sincroniza con Firebase (con validación)
```

### Obtener Estadísticas:
```java
int totalSuns = playerStats.getSunsDestroyed();
```

### Sincronización Manual (opcional):
```java
FirebaseStatsManager.getInstance().syncStats(
    localSuns,
    new FirebaseStatsManager.StatsCallback() {
        @Override
        public void onSuccess(int finalSuns) {
            // Estadísticas sincronizadas
        }

        @Override
        public void onError(String error) {
            // Error de red, continuar con datos locales
        }
    }
);
```

---

## ⚙️ CONFIGURACIÓN INICIAL

### 1. Firebase Project Setup
- Proyecto: `blackholeglow`
- Firestore Database: Habilitado
- Authentication: Google Sign-In habilitado

### 2. Aplicar Reglas de Seguridad
Sigue las instrucciones en la sección **Cómo Aplicar las Reglas** arriba.

### 3. Verificar Permisos
En Firebase Console → Authentication:
- ✅ Google Sign-In habilitado
- ✅ Usuarios pueden registrarse

### 4. Índices de Firestore (para leaderboard)
Firebase puede pedirte crear un índice. Haz clic en el enlace del log y créalo automáticamente.

---

## 🧪 TESTING

### Pruebas Recomendadas:

1. **Test de Sincronización**
   - Destruir 5 soles
   - Cerrar app
   - Reabrir
   - Verificar que sigue en 5

2. **Test Anti-Trampa**
   - Usar `adb shell` para modificar SharedPreferences
   - Reabrir app
   - Verificar que Firebase restaura el valor real

3. **Test de Incremento**
   - Destruir 1 sol a la vez
   - Verificar que cada uno se sube a Firebase

4. **Test Offline**
   - Desactivar internet
   - Destruir 3 soles
   - Activar internet
   - Verificar sincronización

---

## 📝 LOGS IMPORTANTES

Busca estos logs en LogCat:
```
[FirebaseStats] 🔐 FirebaseStatsManager inicializado
[FirebaseStats] ☀️ SOLES GUARDADOS EN FIREBASE - Total: X soles
[FirebaseStats] 🏆 Leaderboard actualizado!
[FirebaseStats] 🚨 INTENTO DE TRAMPA DETECTADO!
[PlayerStats] 📥 Estadísticas actualizadas desde Firebase: X soles
```

---

## 🚀 FUTURAS MEJORAS

1. **Detección de patrones anómalos**
   - Alertar si incrementos son demasiado rápidos
   - Machine learning para detectar bots

2. **Reportes de usuarios**
   - Sistema para reportar tramposos
   - Moderación manual

3. **Rankings por temporada**
   - Reset mensual de leaderboard
   - Premios para top 10

4. **Cloud Functions**
   - Validación adicional en el servidor
   - Recalculo automático de rankings

---

## ✅ CHECKLIST DE DESPLIEGUE

Antes de lanzar a producción:

- [ ] Reglas de Firestore aplicadas
- [ ] Firebase Authentication configurado
- [ ] Google Sign-In funcionando
- [ ] Tests de seguridad pasados
- [ ] Logs de sincronización verificados
- [ ] Leaderboard funcional
- [ ] Prueba con múltiples usuarios
- [ ] Documentación actualizada

---

## 🆘 TROUBLESHOOTING

### Problema: "Usuario no autenticado"
**Solución**: Asegúrate de que el usuario haya hecho login con Google.

### Problema: "Error guardando en Firebase"
**Solución**: Verifica las reglas de Firestore y los permisos de red.

### Problema: "Hash inválido"
**Solución**: Esto indica que los datos locales fueron manipulados. Firebase restaurará el valor real.

### Problema: Incremento rechazado
**Solución**: Verifica que el incremento sea <= 10 soles por actualización.

---

## 📞 CONTACTO

Para dudas sobre el sistema de seguridad:
- Revisar logs con tag `FirebaseStats`
- Consultar Firebase Console para errores
- Verificar reglas de Firestore

**Última actualización**: 19 de Octubre 2024
**Versión**: 4.0.0
