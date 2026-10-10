# Estatuas Inteligentes — Documento de Producto

> App móvil de bienestar con reconocimiento de poses en tiempo real (Edge AI).
> Proyecto del **Semillero de IA**, alineado con el **ODS 3: Salud y Bienestar**.

**Estado:** prototipo funcional en Python (PC) → migración a **Android nativo (Kotlin)**.
**Documento hermano:** `ARQUITECTURA_TECNICA.md` (cómo se construye).

---

## 1. Resumen

Estatuas Inteligentes es un "entrenador virtual" y juego de espejo. El usuario elige un reto de movimiento, se ubica frente a la cámara y debe replicar una pose y **sostenerla** unos segundos. La app calcula en tiempo real los ángulos de sus articulaciones, le indica con un semáforo si va bien y le avisa con sonidos.

Todo el análisis ocurre **en el dispositivo**: ningún video ni imagen sale del teléfono.

## 2. Propósito y ODS

- Combatir el sedentarismo (entornos universitarios y laborales) con pausas activas gamificadas.
- Prevenir lesiones articulares mediante corrección de postura.
- Promover un uso ético y práctico de la IA: transparente, local y respetuoso de la privacidad.

## 3. Usuario objetivo

Estudiantes, programadores, oficinistas y cualquier persona que pase muchas horas frente a una pantalla. La mecánica de "juego de las estatuas" también funciona para público joven e infantil.

## 4. Mecánica principal

1. **Selección del reto:** el usuario elige una categoría (Pausas Activas, Yoga Básico, Estiramiento Post-Entrenamiento).
2. **Posicionamiento:** coloca el teléfono a ~2 m, con buena luz, de modo que la cámara capte el cuerpo completo.
3. **Ejecución y análisis:** la pantalla muestra la pose objetivo; la IA rastrea 33 puntos del cuerpo y calcula el ángulo de las articulaciones del reto.
4. **Validación (semáforo):** rojo/amarillo si la postura es incorrecta, verde si está en rango.
5. **Completar:** el usuario mantiene la pose en verde durante el tiempo objetivo (p. ej. 3 s) y el reto se marca como superado.

### Estados de la postura

| Estado | Condición | Color |
|---|---|---|
| `APROBADO` | ángulo dentro de `rango_esperado` | Verde |
| `CORRIGIENDO` | ángulo fuera del rango, a menos de 20° de distancia | Amarillo |
| `INCORRECTO` | ángulo a más de 20° del rango | Rojo |

### Estados del reto

| Estado | Significado |
|---|---|
| `ANALIZANDO` | esperando que el usuario alcance la pose |
| `SOSTENIENDO` | pose correcta, el cronómetro corre |
| `EXITO` | sostuvo la pose el tiempo requerido |

Reglas: perder la pose (o salir de cámara) mientras se sostiene **reinicia** el conteo. En `EXITO` se sigue mostrando el ángulo real.

## 5. Reto de ejemplo (actual)

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

El ángulo se mide en el **codo** (vértice, segundo punto) entre hombro y muñeca.

## 6. Alcance

### MVP (primera versión funcional en Android)
- Una pantalla de cámara con vista previa y esqueleto superpuesto.
- Un reto cargado desde JSON.
- Semáforo (colores del esqueleto + mensaje) y barra de progreso del sostenido.
- Sonidos: tick por segundo, éxito y error.
- Aviso visible de privacidad ("procesado en tu dispositivo").
- Permiso de cámara y manejo de cámara no disponible.

### Versión 1
- Lista de retos por categoría y navegación entre pantallas.
- Pantalla de calibración / onboarding.
- Botón **Saltar** (disponible a los 15 s, sin penalización severa).
- Aviso legal al iniciar y mini-reto de calentamiento.
- Gamificación: puntuación por precisión, rachas de días.
- Estilo visual glassmorfismo.

### Fuera de alcance (por ahora)
- Versión de escritorio (PC) y iOS. La ejecución en PC es un deseo personal, no un requisito del semillero.
- Subida de datos, cuentas de usuario o cualquier servicio en la nube.

## 7. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-01 | Capturar video de la cámara y detectar la pose en tiempo real. |
| RF-02 | Calcular el ángulo entre tres landmarks definidos por el reto. |
| RF-03 | Clasificar la postura en `APROBADO` / `CORRIGIENDO` / `INCORRECTO`. |
| RF-04 | Contar el tiempo sostenido y declarar éxito al alcanzar `hold_time_seconds`. |
| RF-05 | Reiniciar el conteo si la postura se pierde o el cuerpo sale del encuadre. |
| RF-06 | Emitir sonidos de tick, éxito y error sin congelar el video. |
| RF-07 | Cargar los retos desde un archivo JSON, sin tocar código para agregar uno. |
| RF-08 | Mostrar el esqueleto con el color del semáforo, en alto contraste. |
| RF-09 | (V1) Validar silueta completa (landmark 0 hasta 31/32) antes de empezar. |
| RF-10 | (V1) Permitir saltar un reto tras 15 s. |

## 8. Requisitos no funcionales

- **Privacidad:** análisis 100 % local; no se guarda ni se envía video ni imagen.
- **Rendimiento:** fluidez de video sin retraso perceptible en un teléfono de gama media.
- **Accesibilidad:** colores brillantes visibles a 2–3 m, y retroalimentación sonora para quien no mira la pantalla.
- **Robustez:** el esqueleto no debe "temblar" (confianzas de detección y seguimiento ajustadas).
- **Mantenibilidad:** la lógica de evaluación no depende de cámara ni de UI y se prueba con tests.

## 9. Experiencia de usuario

- **Manos libres:** sonido de "pose identificada", tick mientras sostiene, sonido distinto para éxito y fallo.
- **Alto contraste:** esqueleto verde neón / amarillo / rojo brillante.
- **Mensajes claros** en la UI: "Ubícate frente a la cámara", "Ajusta tu postura", "Casi… ajusta el brazo", "¡Sostén la pose!", "¡Reto superado!".
- **Estilo visual:** glassmorfismo (tarjeta translúcida con degradado, borde fino y sombra sobre un fondo con luces de color).

## 10. Seguridad física y ética

- **Disclaimer:** los ejercicios son sugerencias; detenerse si hay dolor.
- **Inclusión:** botón Saltar a los 15 s, sin castigo severo en la puntuación.
- **Calentamiento:** mini-reto articular la primera vez del día.
- **Transparencia de IA:** informar en la interfaz que el análisis es local.

## 11. Criterios de aceptación del MVP

- [ ] La app pide el permiso de cámara y muestra un mensaje claro si se niega.
- [ ] Se ve la vista previa y el esqueleto sobre el usuario, sin distorsión.
- [ ] Al levantar el **brazo izquierdo**, reacciona el brazo izquierdo (no el derecho).
- [ ] El ángulo mostrado coincide con el real (verificado con un transportador o referencia).
- [ ] Sostener la pose 3 s en verde marca `EXITO`; romperla antes reinicia el conteo.
- [ ] Los sonidos no congelan el video.
- [ ] Salir del encuadre durante el sostenido reinicia el conteo.
- [ ] Los tests unitarios del evaluador pasan.

## 12. Decisiones de plataforma

- **Android nativo con Kotlin** es la plataforma objetivo.
- **PC no es requisito.** El prototipo en Python sigue siendo útil como laboratorio para probar umbrales y ejercicios.
- Kotlin Multiplatform queda descartado por ahora; la lógica se separa en un módulo propio para poder reutilizarla si algún día se necesita.

## 13. Glosario

- **Edge AI:** inferencia de IA ejecutada en el dispositivo del usuario.
- **Landmark:** punto articular detectado por MediaPipe (hay 33 en el cuerpo).
- **Vértice:** el landmark central de los tres usados para medir un ángulo.
- **Sostenido (hold):** tiempo continuo que se debe mantener la pose en verde.
- **Semáforo:** indicador visual verde/amarillo/rojo del estado de la postura.
