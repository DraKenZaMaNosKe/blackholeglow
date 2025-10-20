# 🔥 INSTRUCCIONES PARA CONFIGURAR FIREBASE - BOT SYSTEM

## ⚠️ PROBLEMA ACTUAL

La app está intentando crear bots en Firestore pero recibe este error:
```
FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
```

## ✅ SOLUCIÓN: Actualizar Reglas de Firestore

Las reglas actuales de Firestore NO permiten que la app cree documentos para los bots porque los IDs de los bots (`bot_champion`, `bot_master`, `bot_hunter`) no coinciden con el `userId` del usuario autenticado.

### 📋 PASOS PARA APLICAR LAS REGLAS:

1. **Abre Firebase Console**
   - Ve a: https://console.firebase.google.com
   - Inicia sesión con tu cuenta de Google

2. **Selecciona tu proyecto**
   - Busca y haz clic en **"blackholeglow"**

3. **Navega a Firestore Database**
   - En el menú lateral izquierdo, haz clic en **"Firestore Database"**
   - Verás las colecciones existentes (player_stats, leaderboard, etc.)

4. **Abre el editor de Reglas**
   - En la parte superior, haz clic en la pestaña **"Reglas"** (Rules)
   - Verás el editor de reglas actual

5. **Copia las nuevas reglas**
   - Abre el archivo `firestore.rules` en este proyecto
   - **Selecciona TODO el contenido** (Ctrl+A)
   - **Copia** (Ctrl+C)

6. **Pega las nuevas reglas**
   - En el editor de Firebase Console, **selecciona todo** (Ctrl+A)
   - **Pega** las nuevas reglas (Ctrl+V)
   - Verifica que se pegó correctamente

7. **Publica las reglas**
   - Haz clic en el botón **"Publicar"** (Publish) en la parte superior
   - Confirma la publicación
   - Espera unos segundos hasta que se apliquen

## 🤖 ¿QUÉ HACEN LAS NUEVAS REGLAS?

Las reglas actualizadas permiten:

### ✅ Para Bots (IDs que empiezan con `bot_`):
- Cualquier usuario autenticado puede **crear** bots
- Cualquier usuario autenticado puede **actualizar** bots
- Los bots pueden ser leídos por cualquiera
- Los bots deben tener `isBot: true` en sus datos

### ✅ Para Usuarios Normales:
- Solo pueden leer/escribir **SUS PROPIOS** datos
- Solo pueden **incrementar** su contador (anti-trampa)
- Máximo +10 soles por actualización
- No pueden modificar el `userId`

### ✅ Para el Leaderboard:
- **Lectura pública** (todos pueden ver el ranking)
- Los usuarios solo pueden actualizar **su propia** entrada
- Los bots pueden ser actualizados por cualquier usuario autenticado

## 🔍 VERIFICAR QUE FUNCIONÓ

Después de aplicar las reglas:

1. **Reinicia la app** en tu dispositivo:
   ```bash
   D:/adb/platform-tools/adb.exe shell am force-stop com.secret.blackholeglow
   D:/adb/platform-tools/adb.exe shell am start -n com.secret.blackholeglow/.LoginActivity
   ```

2. **Revisa los logs**:
   ```bash
   D:/adb/platform-tools/adb.exe logcat -s BotManager:D LeaderboardManager:D
   ```

3. **Busca estos mensajes**:
   - ✅ `🤖 INICIALIZANDO BOTS`
   - ✅ `✅ 🏆 Champion creado con 100 soles`
   - ✅ `✅ ⚡ Master creado con 65 soles`
   - ✅ `✅ 🎯 Hunter creado con 35 soles`
   - ✅ `🎮 Todos los bots están listos!`

4. **Verifica en Firebase Console**:
   - Ve a Firestore Database → Colecciones
   - Busca la colección `player_stats`
   - Deberías ver 3 documentos nuevos:
     - `bot_champion` (100 soles)
     - `bot_master` (65 soles)
     - `bot_hunter` (35 soles)
   - Busca la colección `leaderboard`
   - Deberías ver los mismos 3 bots ahí

## ❌ SI SIGUE SIN FUNCIONAR

Si después de aplicar las reglas sigues viendo el error `PERMISSION_DENIED`:

1. **Verifica que publicaste las reglas** en Firebase Console
2. **Espera 30 segundos** para que se propaguen
3. **Cierra completamente la app** y vuelve a abrirla
4. **Revisa que las reglas se aplicaron correctamente**:
   - En Firebase Console → Firestore Database → Reglas
   - Verifica que tengan las funciones `isBot()`
   - Verifica que digan `|| (request.auth != null && isBot() ...)`

5. **Revisa los logs de Firestore** en Firebase Console:
   - Ve a Firestore Database → Uso (Usage)
   - Revisa si hay errores específicos

## 📞 CONTACTO

Si necesitas ayuda adicional:
- Revisa la documentación de Firestore Security Rules: https://firebase.google.com/docs/firestore/security/get-started
- Prueba las reglas en el simulador de Firebase Console (pestaña "Rules" → botón "Test rules")

---

**Fecha de creación**: Octubre 19, 2024
**Versión de la app**: 4.0.0
**Estado**: Esperando aplicación de reglas de Firestore
