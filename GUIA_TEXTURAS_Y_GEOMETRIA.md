# 📐 GUÍA: Texturas y Geometría en Black Hole Glow

## ✅ CUÁNDO USAR GENERACIÓN PROCEDURAL

### **Para Objetos Geométricos Simples:**

#### **1. ESFERAS (Planetas, Soles, Bolas Disco)**
```java
✅ Usar: ProceduralSphere
   - UVs perfectos garantizados
   - Sin problemas de seam
   - Ajustable en tiempo real
   - Más eficiente que .obj

Ejemplo:
ProceduralSphere.Mesh mesh = ProceduralSphere.generateMedium(1.0f);
```

**Casos de uso:**
- ✅ Planetas y lunas
- ✅ Bola disco (DiscoBall)
- ✅ Sol y estrellas
- ✅ Burbujas, esferas de energía
- ✅ Proyectiles esféricos

#### **2. OTROS PRIMITIVOS (Futuros)**
```java
🔮 ProceduralCylinder  → Para árboles, columnas, rayos
🔮 ProceduralCube      → Para edificios, cajas
🔮 ProceduralPlane     → Para suelos, agua, fondos
🔮 ProceduralTorus     → Para anillos, portales
```

---

## ⚠️ CUÁNDO USAR ARCHIVOS .OBJ (BLENDER)

### **Para Objetos Complejos e Irregulares:**

#### **1. MODELOS ORGÁNICOS**
```
✅ Usar: Blender + ObjLoader
   - Bichos, insectos, animales
   - Personajes humanoides
   - Plantas y vegetación
   - Rocas y terreno irregular
```

**Workflow:**
1. Modelar en Blender
2. UV Unwrap (Smart UV Project o manual)
3. Exportar como .obj (con UVs)
4. Cargar con ObjLoader.loadObj()

**IMPORTANTE:**
- El UV unwrap en Blender debe ser **correcto** desde el inicio
- Usar "Smart UV Project" para geometría compleja
- Verificar que no haya UVs estirados en Blender antes de exportar

#### **2. ARQUITECTURA Y ESCENARIOS**
```
✅ Usar: Blender + .obj
   - Edificios con muchos detalles
   - Vehículos (naves, autos)
   - Objetos decorativos complejos
   - Estructuras arquitectónicas
```

---

## 🎯 RESUMEN PRÁCTICO

### **DECISIÓN RÁPIDA:**

```
¿Es una ESFERA?
  └─ SÍ → ProceduralSphere ✅
  └─ NO → ¿Es simple (cubo, cilindro)?
           └─ SÍ → Procedural (cuando esté disponible) ✅
           └─ NO → Blender + .obj ⚠️
```

---

## 🔧 IMPLEMENTACIÓN ACTUAL

### **Objetos que YA usan ProceduralSphere:**
- ✅ **DiscoBall** (bola disco musical)

### **Objetos que DEBERÍAN migrar a ProceduralSphere:**
- ⏳ **Planeta** (todos los planetas esféricos)
- ⏳ **Sol** (estrella central en escena Universo)
- ⏳ **ForceField** (campo de fuerza esférico)
- ⏳ **AvatarSphere** (avatar del jugador)
- ⏳ **Meteorito** (asteroides esféricos)

### **Objetos que DEBEN quedarse con .obj:**
- ✅ **Asteroide** (formas irregulares no esféricas)
- ✅ Cualquier modelo futuro complejo (bichos, etc.)

---

## 📊 VENTAJAS DE LA MIGRACIÓN

### **Antes (usando planeta.obj):**
```
❌ UVs con seams y distorsión
❌ Archivo .obj de ~200 KB
❌ Parsing lento al cargar
❌ Normales con posibles errores
❌ No modificable en runtime
```

### **Después (usando ProceduralSphere):**
```
✅ UVs matemáticamente perfectos
✅ ~50 KB de código Java
✅ Generación instantánea
✅ Normales perfectas automáticamente
✅ Ajustable en tiempo real
✅ Menos memoria
```

---

## 🚀 PRÓXIMOS PASOS SUGERIDOS

### **Fase 1: Migrar Objetos Esféricos Existentes**
1. Modificar `Planeta.java` para usar ProceduralSphere
2. Modificar `ForceField.java` (ya genera esfera procedural, solo mejorar UVs)
3. Modificar `AvatarSphere.java`
4. Probar cada uno individualmente

### **Fase 2: Crear Más Generadores Procedurales**
1. `ProceduralCylinder.java` (para árboles, rayos)
2. `ProceduralCube.java` (para edificios)
3. `ProceduralPlane.java` (para suelos, agua)

### **Fase 3: Documentar Workflow de Blender**
1. Crear guía de UV unwrapping correcto
2. Documentar export settings óptimos
3. Crear checklist de calidad para .obj

---

## 💡 TIPS Y MEJORES PRÁCTICAS

### **Para Esferas Procedurales:**

```java
// Baja resolución (objetos lejanos, pequeños)
ProceduralSphere.generateLowPoly(radius);   // 8 lat x 16 lon

// Media resolución (balance perfecto) ← RECOMENDADO
ProceduralSphere.generateMedium(radius);    // 16 lat x 32 lon

// Alta resolución (objetos grandes, con zoom)
ProceduralSphere.generateHigh(radius);      // 32 lat x 64 lon

// Ultra detalle (solo casos especiales)
ProceduralSphere.generateUltra(radius);     // 64 lat x 128 lon
```

### **Optimización:**
- Usa **Medium** por defecto (mejor balance calidad/rendimiento)
- Solo usa **High** para el Sol u objetos principales
- Usa **LowPoly** para objetos de fondo o muy pequeños

---

## 🎨 TÉCNICAS AVANZADAS DE TEXTURIZADO

### **1. UV Scrolling (Textura en Movimiento)**
```java
// En el shader vertex:
v_TexCoord = a_TexCoord + vec2(u_Time * 0.1, 0.0);
```

### **2. Multi-textura (Combinar Texturas)**
```java
// Pasar 2 texturas al shader y mezclarlas
vec4 tex1 = texture2D(u_Texture1, uv);
vec4 tex2 = texture2D(u_Texture2, uv);
gl_FragColor = mix(tex1, tex2, 0.5);
```

### **3. Texturas Procedurales (Sin Imagen)**
```java
// Generar patrón directamente en shader
float pattern = sin(uv.x * 20.0) * cos(uv.y * 20.0);
```

---

## 📚 REFERENCIAS

- `ProceduralSphere.java` - Generador de esferas perfecto
- `ObjLoader.java` - Cargador de modelos Blender
- `DiscoBall.java` - Ejemplo de uso de ProceduralSphere
- `Planeta.java` - (Por migrar a ProceduralSphere)

---

**Última actualización:** 2025-10-23
**Estado:** Esfera procedural implementada y funcionando ✅
