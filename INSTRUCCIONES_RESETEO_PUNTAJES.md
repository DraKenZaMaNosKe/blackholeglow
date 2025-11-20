# 🔄 INSTRUCCIONES PARA RESETEAR PUNTAJES Y ELIMINAR BOTS

## 📋 RESUMEN DE CAMBIOS REALIZADOS

### ✅ Cambios en el Código:

1. **SceneRenderer.java** (líneas 901-920)
   - ❌ Deshabilitada inicialización de `BotManager`
   - ❌ Deshabilitada creación de bots en Firebase
   - ✅ Leaderboard ahora se actualiza directamente sin esperar bots

2. **SceneRenderer.java** (líneas 2326-2329)
   - ❌ Deshabilitada actualización automática de bots cada hora

3. **LeaderboardManager.java** (línea 91)
   - ✅ Agregado filtro `.whereEqualTo("isBot", false)`
   - ✅ Ahora SOLO se muestran jugadores reales en el Top 3
   - ❌ Los bots NUNCA aparecerán aunque existan en Firebase

---

## 🗑️ PASO 1: ELIMINAR BOTS DE FIREBASE

### Opción A: Desde Firebase Console (Recomendado)

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto "Black Hole Glow"
3. Ve a **Firestore Database**
4. Navega a la colección `player_stats`
5. Busca y **ELIMINA** estos 3 documentos:
   - `bot_champion` (🏆 Champion - 100 soles)
   - `bot_master` (⚡ Master - 65 soles)
   - `bot_hunter` (🎯 Hunter - 35 soles)

6. Navega a la colección `leaderboard`
7. Busca y **ELIMINA** los mismos 3 documentos:
   - `bot_champion`
   - `bot_master`
   - `bot_hunter`

### Opción B: Script Automático (Avanzado)

Si prefieres un script, puedes usar este código en la consola de Firebase (Rules Playground):

```javascript
// En Firebase Console > Firestore > Rules Playground
const botIds = ['bot_champion', 'bot_master', 'bot_hunter'];

botIds.forEach(async (botId) => {
  // Eliminar de player_stats
  await db.collection('player_stats').doc(botId).delete();

  // Eliminar de leaderboard
  await db.collection('leaderboard').doc(botId).delete();

  console.log('✅ Bot eliminado:', botId);
});
```

---

## 🔄 PASO 2: RESETEAR PUNTAJES DE TODOS LOS USUARIOS

### ⚠️ ADVERTENCIA: ESTA ACCIÓN ES IRREVERSIBLE

Tienes 2 opciones:

### Opción A: Reseteo Manual Individual

1. Ve a Firebase Console > Firestore Database
2. Colección `player_stats`
3. Para cada usuario (excepto bots):
   - Click en el documento
   - Encuentra el campo `sunsDestroyed`
   - Cambia el valor a `0`
   - Click "Update"

4. Repite en la colección `leaderboard`

### Opción B: Reseteo Masivo con Script

Crea un archivo `reset_scores.js` en tu proyecto:

```javascript
// reset_scores.js
// Ejecutar con Node.js + Firebase Admin SDK

const admin = require('firebase-admin');
const serviceAccount = require('./path/to/serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function resetAllScores() {
  console.log('🔄 Iniciando reseteo de puntajes...');

  // Resetear player_stats
  const statsSnapshot = await db.collection('player_stats')
    .where('isBot', '==', false)  // Solo usuarios reales
    .get();

  const batch1 = db.batch();
  statsSnapshot.forEach((doc) => {
    batch1.update(doc.ref, {
      sunsDestroyed: 0,
      sunHealth: 100,
      forceFieldHealth: 100,
      lastUpdate: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  await batch1.commit();
  console.log('✅ player_stats reseteado');

  // Resetear leaderboard
  const leaderboardSnapshot = await db.collection('leaderboard')
    .where('isBot', '==', false)
    .get();

  const batch2 = db.batch();
  leaderboardSnapshot.forEach((doc) => {
    batch2.update(doc.ref, {
      sunsDestroyed: 0,
      lastUpdate: admin.firestore.FieldValue.serverTimestamp()
    });
  });

  await batch2.commit();
  console.log('✅ leaderboard reseteado');

  console.log('🎉 ¡Reseteo completado!');
  process.exit(0);
}

resetAllScores().catch(console.error);
```

**Para ejecutar:**
```bash
npm install firebase-admin
node reset_scores.js
```

---

## 🔍 PASO 3: CREAR ÍNDICE COMPUESTO EN FIREBASE

⚠️ **MUY IMPORTANTE:** El filtro `.whereEqualTo("isBot", false)` + `.orderBy()` requiere un índice compuesto.

### Crear índice automáticamente:

1. Compila e instala la app
2. Abre el wallpaper (esto intentará cargar el leaderboard)
3. Verás un error en **Logcat** como este:
   ```
   FAILED_PRECONDITION: The query requires an index.
   You can create it here: https://console.firebase.google.com/...
   ```

4. **Haz click en el link del error** en Logcat
5. Te llevará directo a Firebase Console
6. Click en **"Create Index"**
7. Espera 2-5 minutos mientras se crea el índice

### O crear manualmente:

1. Ve a Firebase Console > Firestore Database
2. Click en pestaña **"Indexes"**
3. Click **"Create Index"**
4. Configuración:
   - **Collection ID:** `leaderboard`
   - **Fields:**
     - Campo 1: `isBot` → **Ascending**
     - Campo 2: `sunsDestroyed` → **Descending**
   - **Query scope:** Collection
5. Click **"Create"**

---

## ✅ VERIFICACIÓN FINAL

### Después de todos los cambios:

1. **Compila la app:**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Instala en el dispositivo:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Verifica en Firebase Console:**
   - ✅ No deben existir `bot_champion`, `bot_master`, `bot_hunter`
   - ✅ Todos los usuarios deben tener `sunsDestroyed: 0`

4. **Verifica en la app:**
   - ✅ El leaderboard debe mostrar solo jugadores reales
   - ✅ Todos deben aparecer con 0 puntos
   - ✅ Los bots NO deben aparecer NUNCA

5. **Verifica en Logcat:**
   ```bash
   adb logcat | grep "LeaderboardManager\|BotManager"
   ```
   - ✅ Debe decir: "Consultando Top 3 desde Firebase (solo jugadores reales)"
   - ❌ NO debe decir: "Bots inicializados" ni "Actualizando bots"

---

## 🎯 ESTADO FINAL ESPERADO

### En Firebase:
```
player_stats/
├── <userId1>          sunsDestroyed: 0, isBot: false
├── <userId2>          sunsDestroyed: 0, isBot: false
└── ...

leaderboard/
├── <userId1>          sunsDestroyed: 0, isBot: false
├── <userId2>          sunsDestroyed: 0, isBot: false
└── ...
```

### En la App:
- Leaderboard muestra: `#1 ---`, `#2 ---`, `#3 ---` (vacío hasta que alguien juegue)
- O muestra jugadores reales con 0 puntos
- **Los 3 bots NUNCA aparecen**

---

## 🔄 PARA REACTIVAR BOTS EN EL FUTURO

Si en el futuro quieres reactivar los bots:

1. En `SceneRenderer.java`:
   - Descomentar líneas 901-920 (inicialización)
   - Descomentar líneas 2326-2329 (actualización)

2. En `LeaderboardManager.java`:
   - Eliminar o comentar la línea 91: `.whereEqualTo("isBot", false)`

3. Recompilar e instalar

---

## 📝 NOTAS ADICIONALES

- **BotManager.java** permanece en el código pero NO se usa
- Puedes eliminarlo si quieres limpieza total, pero no es necesario
- El código está listo para publicación en Play Store
- Todos los cambios están comentados para fácil referencia

---

**Fecha de cambios:** 19 de Noviembre 2025
**Versión:** 4.0.0
**Estado:** ✅ Listo para Play Store (después de resetear Firebase)
