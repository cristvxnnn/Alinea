# Estatuas Inteligentes — Arquitectura Técnica

**Plataforma objetivo:** Android nativo (Kotlin).
**Documento hermano:** `PROYECTO.md` (qué se construye y por qué).

> Las versiones de librerías se escriben como `<ultima-version>`: verifica la actual en la documentación oficial al crear el proyecto. Los nombres exactos de clases de MediaPipe pueden variar entre versiones; contrástalos con el ejemplo oficial de Pose Landmarker para Android.

---

## 1. Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI | Jetpack Compose (plantilla *Empty Activity*) |
| Cámara | CameraX (`ImageAnalysis` + vista previa) |
| Detección de pose | MediaPipe Tasks Vision — `PoseLandmarker`, modo `LIVE_STREAM` |
| Modelo | `pose_landmarker_lite.task` en `assets/` |
| Sonido | `SoundPool` o `ToneGenerator` (nunca bloqueante) |
| Navegación | Navigation Compose (fase posterior) |
| Pruebas | JUnit (la lógica corre en el PC, sin teléfono) |
| IDE | Android Studio (no requiere plugins extra) |
| Dispositivo de prueba | Teléfono real con depuración por USB (el emulador es poco representativo para pose) |

`minSdk` 26 o superior.

### Dependencias (`app/build.gradle.kts`)

```kotlin
dependencies {
    implementation("com.google.mediapipe:tasks-vision:<ultima-version>")
    implementation("androidx.camera:camera-core:<v>")
    implementation("androidx.camera:camera-camera2:<v>")
    implementation("androidx.camera:camera-lifecycle:<v>")
    implementation("androidx.camera:camera-view:<v>")
    // Fase posterior:
    // implementation("androidx.navigation:navigation-compose:<v>")
}
```

Permiso en el manifiesto: `android.permission.CAMERA`.

## 2. Estructura del proyecto

```
app/src/main/
├── assets/
│   ├── pose_landmarker_lite.task
│   └── ejercicios.json
└── java/<paquete>/
    ├── core/                 # Kotlin puro: SIN imports de android.*
    │   ├── Angulo.kt         # calcularAngulo, Punto
    │   ├── EvaluadorReto.kt  # máquina de estados + Resultado
    │   └── Reto.kt           # modelo del JSON + mapa de landmarks
    ├── vision/
    │   ├── DetectorPose.kt   # envuelve PoseLandmarker
    │   └── CamaraAnalisis.kt # CameraX → Bitmap rotado → detector
    ├── audio/
    │   └── Sonidos.kt        # tick / éxito / error
    └── ui/
        ├── PantallaCamara.kt # preview + overlay + semáforo + barra
        └── tema/             # colores y estilo glassmorfismo
```

**Regla de oro:** `core/` no importa nada de Android ni de MediaPipe. Así se prueba con JUnit y se puede mover a un módulo compartido (o portar a otra plataforma) sin cambios.

## 3. Contrato de datos del reto (`ejercicios.json`)

```json
{
  "estiramiento_brazo_izquierdo": {
    "nombre": "Estiramiento Lateral Izquierdo",
    "puntos": ["LEFT_SHOULDER", "LEFT_ELBOW", "LEFT_WRIST"],
    "rango_esperado": [160, 180],
    "hold_time_seconds": 3
  }
}
```

| Campo | Tipo | Significado |
|---|---|---|
| `nombre` | texto | Título mostrado en la UI |
| `puntos` | 3 nombres | Landmarks; el **segundo es el vértice** del ángulo |
| `rango_esperado` | [min, max] en grados | Rango que cuenta como `APROBADO` |
| `hold_time_seconds` | número | Segundos a sostener |

### Landmarks de MediaPipe usados

| Nombre | Índice | Nombre | Índice |
|---|---|---|---|
| NOSE | 0 | LEFT_HIP | 23 |
| LEFT_SHOULDER | 11 | RIGHT_HIP | 24 |
| RIGHT_SHOULDER | 12 | LEFT_KNEE | 25 |
| LEFT_ELBOW | 13 | RIGHT_KNEE | 26 |
| RIGHT_ELBOW | 14 | LEFT_ANKLE | 27 |
| LEFT_WRIST | 15 | RIGHT_ANKLE | 28 |
| RIGHT_WRIST | 16 | LEFT/RIGHT_FOOT_INDEX | 31 / 32 |

## 4. Flujo de datos por frame

```
CameraX ImageAnalysis
   │  ImageProxy
   ▼
Conversión a Bitmap + rotación (rotationDegrees)      ← NO espejar aquí
   │
   ▼
DetectorPose.detectAsync(bitmap, timestampMs)         ← timestamps crecientes
   │  (callback en el hilo de MediaPipe)
   ▼
Landmarks normalizados (0..1) → escalar por ancho y alto de la imagen
   │
   ▼
EvaluadorReto.evaluar(p1, p2, p3, ahora) → Resultado
   │
   ├─► Estado de UI (StateFlow) → semáforo, mensaje, barra de progreso
   ├─► Sonidos (según Resultado.sonido)
   └─► Overlay: esqueleto dibujado con el color del semáforo
```

Los resultados llegan en otro hilo: publícalos en un `StateFlow` y deja que Compose los observe en el hilo principal.

## 5. Núcleo: `EvaluadorReto`

### Tipos

```kotlin
data class Punto(val x: Float, val y: Float)

enum class Postura { APROBADO, CORRIGIENDO, INCORRECTO }
enum class EstadoReto { ANALIZANDO, SOSTENIENDO, EXITO }
enum class Sonido { TICK, EXITO, ERROR }

data class Resultado(
    val angulo: Double,
    val postura: Postura,
    val estado: EstadoReto,
    val progreso: Float,     // 0.0 a 1.0
    val sonido: Sonido?,
)
```

### API

```kotlin
class EvaluadorReto(tiempoObjetivo: Double, rangoMin: Double, rangoMax: Double, tolerancia: Double = 20.0) {
    fun evaluar(a: Punto, b: Punto, c: Punto, ahora: Double): Resultado
    fun perderCuerpo(): Sonido?   // cuando no hay pose detectada
    fun reiniciar()               // para repetir el reto
}
```

### Reglas

- `calcularAngulo(a, b, c)` devuelve grados en **[0, 180]**, con el vértice en `b`.
- Clasificación: `APROBADO` si el ángulo está en `[min, max]`; `CORRIGIENDO` si está a ≤ `tolerancia` por debajo del mínimo **o** por encima del máximo; si no, `INCORRECTO`.
- Máquina de estados:

| Estado actual | Postura | Resultado |
|---|---|---|
| `ANALIZANDO` | `APROBADO` | → `SOSTENIENDO`, guarda el instante de inicio |
| `SOSTENIENDO` | `APROBADO` | calcula progreso; tick cada segundo; → `EXITO` al llegar al objetivo |
| `SOSTENIENDO` | no `APROBADO` | → `ANALIZANDO`, sonido `ERROR` |
| `SOSTENIENDO` | sin cuerpo (`perderCuerpo`) | → `ANALIZANDO`, sonido `ERROR` |
| `EXITO` | cualquiera | permanece; **sigue devolviendo el ángulo real** y progreso 1.0 |

- El **tiempo se inyecta** (`ahora`, en segundos) para poder probar sin esperar. En producción: `System.nanoTime() / 1e9`.
- En código compartido evita `Math.toDegrees`; usa `rad * 180.0 / PI` (de `kotlin.math`).

## 6. Trampas conocidas (checklist)

- [ ] **Escalar coordenadas:** MediaPipe devuelve valores 0..1; multiplica por ancho y alto **antes** de calcular el ángulo, o se distorsiona.
- [ ] **Izquierda/derecha:** manda el bitmap **sin voltear** al modelo. Si usas cámara frontal, espeja solo el dibujo (`x → 1 − x`). Si no, `LEFT_*` seguirá a la persona al revés.
- [ ] **Rotación:** aplica `imageInfo.rotationDegrees` al frame antes del detector.
- [ ] **Timestamps:** en `detectAsync` deben ser estrictamente crecientes.
- [ ] **Delegado:** empieza con GPU; si falla en algún equipo, cae a CPU.
- [ ] **Sonido:** nunca bloqueante en el hilo del análisis.
- [ ] **Hilos:** el callback de MediaPipe no corre en el hilo principal; no toques la UI desde ahí.
- [ ] **Ciclo de vida:** cierra el `PoseLandmarker` y libera la cámara al salir de la pantalla.
- [ ] **Cuerpo perdido:** llama a `perderCuerpo()` cuando no haya landmarks.

## 7. Estilo glassmorfismo en Compose

`Modifier.blur` difumina el propio contenido, **no** lo que hay detrás, así que no da el efecto cristal por sí solo. Para lograrlo:

- Fondo con degradado y "luces" de color (círculos con degradado radial).
- Tarjeta con degradado blanco translúcido (de ~22 % a ~5 % de opacidad), borde fino claro y sombra suave.
- Opcional: una librería de desenfoque de fondo (por ejemplo *Haze*; comprueba que siga mantenida).

## 8. Pruebas

**Unitarias (JUnit, sin teléfono):**
- Éxito tras sostener el tiempo objetivo (usando `ahora` inyectado).
- Perder la postura reinicia el conteo y emite `ERROR`.
- `perderCuerpo()` reinicia solo si estaba `SOSTENIENDO`.
- En `EXITO` el ángulo sigue siendo el real.
- `CORRIGIENDO` aparece por debajo del mínimo **y** por encima del máximo.
- `calcularAngulo` con casos conocidos (180°, 90°, 0°).

**Manuales (teléfono real):** los criterios de aceptación del MVP en `PROYECTO.md`, sección 11.

## 9. Plan por fases

1. **Base:** instalar Android Studio, crear proyecto *Empty Activity*, probar "Hello World" en el teléfono.
2. **Núcleo:** `core/` + tests JUnit en verde.
3. **Cámara:** permiso, CameraX, vista previa.
4. **Detección:** `DetectorPose` + dibujo del esqueleto.
5. **Integración:** evaluador conectado, semáforo, barra, sonidos.
6. **MVP validado** contra los criterios de aceptación.
7. **V1:** navegación, lista de retos, calibración, Saltar, aviso legal, calentamiento, gamificación, glassmorfismo.

## 10. Prototipo Python (referencia)

Existe una versión funcional en PC: `main.py` (consola), una UI en Flet con glassmorfismo, `vision/angles.py` y `ejercicios/ejercicios.json`. Su papel ahora:

- Laboratorio para probar umbrales y ejercicios nuevos rápidamente.
- Referencia de la lógica que se portó a Kotlin.
- No se mantiene en paralelo; si se retoma, conviene aplicar las correcciones identificadas (reinicio al perder el cuerpo, sonidos no bloqueantes, y revisar el efecto de voltear la imagen antes de procesar).

## 11. Registro de decisiones

| Decisión | Motivo | Descartado |
|---|---|---|
| Android nativo (Kotlin) | MediaPipe funciona de forma oficial y con GPU; cámara estable | Flet → APK: `mediapipe` no figura entre los paquetes móviles de Flet y la cámara requiere `flet-camera` |
| Sin Kotlin Multiplatform por ahora | PC es un deseo, no un requisito; menos complejidad de Gradle | KMP: la cámara y MediaPipe seguirían siendo específicos por plataforma |
| Sin PWA web | Se prioriza rendimiento nativo y el stack elegido | PWA con MediaPipe JS: habría dado móvil y PC, pero con otro lenguaje |
| Evaluador sin dependencias | Probable con JUnit y portable | Lógica mezclada con cámara y UI |
| *Empty Activity* (Compose) | La pantalla principal es cámara + overlay | *Navigation UI Activity*: genera Fragments/XML que no se usarían |
| Navegación después | Una sola pantalla en el MVP | Navegación desde el inicio |

## 12. Riesgos y pendientes por verificar

- Versiones actuales de MediaPipe Tasks, CameraX y Compose (usar las últimas estables).
- Rendimiento y fluidez en el teléfono real elegido.
- Compatibilidad del delegado GPU en distintos equipos.
- Si algún día se quiere PC: confirmar cómo detectar pose en escritorio (hasta donde se sabe, MediaPipe Tasks no tiene soporte oficial para JVM de escritorio).
