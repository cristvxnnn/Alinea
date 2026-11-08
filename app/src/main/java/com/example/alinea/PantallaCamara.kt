package com.example.alinea

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.alinea.audio.Sonidos
import com.example.alinea.audio.Voz
import com.example.alinea.core.*
import com.example.alinea.data.AppDatabase
import com.example.alinea.data.ProgresoAlcance
import com.example.alinea.data.ProgresoRepositorio
import com.example.alinea.data.RegistroSesion
import com.example.alinea.vision.DetectorPose
import com.example.alinea.vision.PoseFrame
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.util.concurrent.Executors
import kotlin.math.ceil

// Semáforo de alto contraste (verde neón / amarillo / rojo brillante)
private val VERDE = Color(0xFF39FF14)
private val AMARILLO = Color(0xFFFFEB00)
private val ROJO = Color(0xFFFF1744)

// Identidad "Aurora": vidrio oscuro con luces azul, cian y violeta
private val CIAN = Color(0xFF22D3EE)
private val CRISTAL_BASE = Color(0xB30A1A33)
private val TEXTO_SUAVE = Color(0xCCFFFFFF)
private val FONDO_CAMARA = Color(0xFF0A1A33)

// Ajustes de legibilidad a distancia (la maqueta usa tamaños menores; se pueden bajar)
private val TAMANO_TEMPORIZADOR = 64.sp
private val TAMANO_MENSAJE = 24.sp
private val TAMANO_SUBMENSAJE = 16.sp

/** false: la cámara se ve entera (con franjas). true: llena la pantalla, pero recorta los costados. */
private val LLENAR_PANTALLA = false

private enum class Fase { CALIBRANDO, RETO }

private fun colorPostura(p: Postura) = when (p) {
    Postura.APROBADO -> VERDE
    Postura.CORRIGIENDO -> AMARILLO
    Postura.INCORRECTO -> ROJO
}

/** Efecto vidrio: base azul oscura translúcida, brillo diagonal y borde de luz. */
private fun Modifier.cristal(forma: Shape = RoundedCornerShape(28.dp)): Modifier = this
    .clip(forma)
    .background(CRISTAL_BASE)
    .background(Brush.linearGradient(listOf(Color(0x26FFFFFF), Color(0x08FFFFFF))))
    .border(1.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF))), forma)

@Composable
fun PantallaCamara(reto: Reto?, onVolver: () -> Unit, resumenHoy: ResumenProgreso, metas: MetasDiarias) {    val context = LocalContext.current
    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> tienePermiso = concedido }

    LaunchedEffect(Unit) {
        if (!tienePermiso) lanzador.launch(Manifest.permission.CAMERA)
    }

    if (tienePermiso) {
        VistaCamara(reto, onVolver, resumenHoy, metas)    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Se necesita la cámara para analizar tu postura. El video se procesa en tu dispositivo.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { lanzador.launch(Manifest.permission.CAMERA) }) {
                Text("Dar permiso")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }) {
                Text("Abrir ajustes de la app")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onVolver) { Text("Volver") }
        }
    }
}

@Composable
private fun MensajeError(texto: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(texto)
    }
}

@Composable
private fun VistaCamara(reto: Reto?, onVolver: () -> Unit, resumenHoy: ResumenProgreso, metas: MetasDiarias) {    val contexto = LocalContext.current
    val detectorCargado = remember(reto) {
        if (reto != null) runCatching { DetectorPose(contexto) } else null
    }
    val detector = detectorCargado?.getOrNull()

    when {
        reto == null -> MensajeError("No se encontró el ejercicio.")
        detector == null ->
            MensajeError("No se pudo iniciar el detector de pose: ${detectorCargado?.exceptionOrNull()?.message}")
        else -> SesionReto(reto, detector, onVolver, resumenHoy, metas)    }
}

@Composable
private fun SesionReto(
    reto: Reto,
    detector: DetectorPose,
    onVolver: () -> Unit,
    resumenHoy: ResumenProgreso,
    metas: MetasDiarias,
) {    val contexto = LocalContext.current
    val actividad = contexto as ComponentActivity
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val evaluador = remember(reto) { EvaluadorReto(reto, graciaSegundos = 0.6) }
    val calibrador = remember { Calibrador() }
    val sonidos = remember { Sonidos() }
    val voz = remember { Voz(contexto) }
    val registro = remember {
        RegistroSesion(
            ProgresoRepositorio(AppDatabase.obtener(contexto).progresoDao()),
            ProgresoAlcance.alcance,
        )
    }
    val intento = remember { SeguimientoIntento() }
    val camara = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    val pose by detector.pose.collectAsState()

    var fase by remember { mutableStateOf(Fase.CALIBRANDO) }
    var calibracion by remember { mutableStateOf<EstadoCalibracion?>(null) }
    var progresoCalibracion by remember { mutableStateOf(0f) }
    var resultado by remember { mutableStateOf<Resultado?>(null) }
    var estadoAnterior by remember { mutableStateOf<EstadoReto?>(null) }
    var errorCamara by remember { mutableStateOf<String?>(null) }
    var vozActiva by remember { mutableStateOf(true) }
    var modoTecnico by remember { mutableStateOf(false) }
    var guiaAbierta by remember { mutableStateOf(false) }
    var exitoLogrado by remember { mutableStateOf(false) }
    var resumenIntento by remember { mutableStateOf<ResumenIntento?>(null) }
    val detalleIntento = remember(reto) { AnalisisIntento(reto) }
    val repetir = {
        evaluador.reiniciar()
        intento.reiniciar()
        detalleIntento.reiniciar()
        exitoLogrado = false
        resumenIntento = null
    }

    // Medición temporal de FPS (RNF-02)
    val medida = remember { LongArray(2) }   // [0] inicio de ventana (ms), [1] cuadros
    var fps by remember { mutableStateOf(0) }

    LaunchedEffect(vozActiva) {
        voz.activada = vozActiva
        if (!vozActiva) voz.callar()
    }

    // La guía pausa el intento: el tiempo con la guía abierta no cuenta como tiempo sostenido
    LaunchedEffect(guiaAbierta) {
        val ahora = SystemClock.elapsedRealtime() / 1000.0
        if (guiaAbierta) {
            evaluador.pausar(ahora)
            voz.callar()
        } else {
            evaluador.reanudar(ahora)
        }
    }

    // Cada vez que llega una pose nueva: calibrar o evaluar, sonar, hablar y actualizar la UI
    LaunchedEffect(pose) {
        val t = SystemClock.elapsedRealtime()
        val ahora = t / 1000.0
        if (medida[0] == 0L) medida[0] = t
        medida[1]++
        if (t - medida[0] >= 1000) {
            fps = medida[1].toInt()
            medida[0] = t
            medida[1] = 0
        }

        // Con la guía abierta no se evalúa, no se guarda nada y no se habla
        if (guiaAbierta) return@LaunchedEffect

        val p = pose
        if (fase == Fase.CALIBRANDO) {
            if (p == null) {
                calibracion = null
                progresoCalibracion = calibrador.actualizar(false, ahora)
                voz.decir("No te veo. Ubícate frente a la cámara", enfriamientoMs = 6000)
            } else {
                val estado = evaluarSilueta(p.puntos, p.visibilidad, vista = reto.vista)
                calibracion = estado
                val yaEstabaListo = calibrador.listo
                progresoCalibracion = calibrador.actualizar(estado.completa, ahora)
                if (!estado.completa) {
                    voz.decir(
                        "Ajusta tu posición. No veo: ${estado.faltantes.joinToString(", ")}",
                        enfriamientoMs = 6000,
                    )
                }
                if (!yaEstabaListo && calibrador.listo) {
                    sonidos.reproducir(Sonido.LISTO)
                    voz.decir("Listo. Comenzamos", urgente = true)
                    fase = Fase.RETO
                }
            }
        } else if (p == null) {
            evaluador.perderCuerpo()?.let {
                sonidos.reproducir(it)
                detalleIntento.alPerderCuerpo()
            }
            if (!exitoLogrado) voz.decir("No te veo. Ubícate frente a la cámara", enfriamientoMs = 6000)
            if (evaluador.estado != EstadoReto.EXITO) estadoAnterior = null
            resultado = null
        } else {
            // Coordenadas normalizadas -> píxeles, para que el ángulo no se distorsione
            val puntos = p.puntos.map { Punto(it.x * p.ancho, it.y * p.alto) }
            val r = evaluador.evaluar(puntos, ahora)
            detalleIntento.alEvaluar(r)
            if (intento.alEvaluar(r.estado, r.progreso * reto.tiempoObjetivo)) {
                registro.registrar(reto.id, reto.tiempoObjetivo, reto.tiempoObjetivo, EstadoEjercicio.COMPLETADO)
                resumenIntento = detalleIntento.resumen(r)
                exitoLogrado = true
            }
            r.sonido?.let { sonidos.reproducir(it) }
            when {
                r.estado == EstadoReto.EXITO && estadoAnterior != EstadoReto.EXITO ->
                    voz.decir("¡Lo lograste!", urgente = true)
                r.estado == EstadoReto.SOSTENIENDO && estadoAnterior != EstadoReto.SOSTENIENDO ->
                    voz.decir("Así, sostén la pose", urgente = true)
                r.estado == EstadoReto.ANALIZANDO && r.postura == Postura.CORRIGIENDO ->
                    voz.decir("Casi, ajusta un poco")
                r.estado == EstadoReto.ANALIZANDO && r.postura == Postura.INCORRECTO ->
                    voz.decir("Ajusta tu postura")
            }
            estadoAnterior = r.estado
            resultado = r
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            intento.segundosInterrumpidos()?.let { seg ->
                registro.registrar(reto.id, reto.tiempoObjetivo, seg, EstadoEjercicio.INTERRUMPIDO)
            }
            registro.cerrar(completada = intento.hayExito)
            camara[0]?.unbindAll()   // apaga la cámara al salir de esta pantalla
            ejecutor.shutdown()
            detector.cerrar()
            sonidos.liberar()
            voz.liberar()
        }
    }

    val colorEsqueleto = if (fase == Fase.CALIBRANDO) {
        calibracion?.let { if (it.completa) VERDE else ROJO } ?: Color.White
    } else {
        resultado?.let { colorPostura(it.postura) } ?: Color.White
    }
    // Solo en el reto: cada articulación evaluada se pinta según su propio estado
    val estadoPuntos = if (fase == Fase.RETO) resultado?.let { estadoPorPunto(it.criterios) } else null
    val estadoHuesos = if (fase == Fase.RETO) resultado?.let { estadoPorHueso(it.criterios) } else null
    val mostrarExito = fase == Fase.RETO && !modoTecnico && exitoLogrado;
    val progresoBarra = if (fase == Fase.CALIBRANDO) progresoCalibracion else (resultado?.progreso ?: 0f)
    val sostenidos = ((resultado?.progreso ?: 0f) * reto.tiempoObjetivo).toInt()
    val etiquetaTiempo = when {
        resultado == null -> "SIN DETECCIÓN"
        resultado?.estado == EstadoReto.SOSTENIENDO -> "SOSTENIENDO"
        resultado?.postura == Postura.CORRIGIENDO -> "CASI"
        else -> "AJUSTA"
    }

    Box(modifier = Modifier.fillMaxSize().background(FONDO_CAMARA)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val vista = PreviewView(ctx)
                vista.scaleType =
                    if (LLENAR_PANTALLA) PreviewView.ScaleType.FILL_CENTER
                    else PreviewView.ScaleType.FIT_CENTER
                vista.setBackgroundColor(0xFF0A1A33.toInt())   // las franjas laterales no quedan negras
                val futuro = ProcessCameraProvider.getInstance(ctx)
                futuro.addListener({
                    try {
                        val proveedor = futuro.get()
                        camara[0] = proveedor

                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(vista.surfaceProvider)

                        val analisis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                        analisis.setAnalyzer(ejecutor) { imagen ->
                            try {
                                val rotacion = imagen.imageInfo.rotationDegrees
                                val bitmap = imagen.toBitmap()
                                val derecho = if (rotacion != 0) {
                                    val m = Matrix().apply { postRotate(rotacion.toFloat()) }
                                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
                                } else bitmap
                                // El bitmap va SIN espejar al modelo
                                detector.detectar(derecho, SystemClock.uptimeMillis())
                            } catch (e: Exception) {
                                Log.e("Analisis", "Error en un fotograma: ${e.message}")
                            } finally {
                                imagen.close()
                            }
                        }

                        proveedor.unbindAll()
                        proveedor.bindToLifecycle(
                            actividad,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analisis,
                        )
                    } catch (e: Exception) {
                        Log.e("Camara", "No se pudo abrir la cámara: ${e.message}")
                        errorCamara = "No se pudo abrir la cámara. Cierra otras apps que la usen e inténtalo de nuevo."
                    }
                }, ContextCompat.getMainExecutor(ctx))
                vista
            },
        )

        // Viñetas: dan profundidad y aseguran el contraste de los paneles sobre cualquier fondo
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(Color(0xCC061226), Color.Transparent)))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC061226))))
        )

        // Resplandor y marco del color del estado: se ve a distancia sin leer nada
        if (fase == Fase.RETO) {
            BordeEstado(colorEsqueleto, Modifier.fillMaxSize())
        }

        EsqueletoOverlay(
            pose = pose,
            color = colorEsqueleto,
            espejar = true,
            modifier = Modifier.fillMaxSize(),
            estadoPuntos = estadoPuntos,
            estadoHuesos = estadoHuesos,
            llenar = LLENAR_PANTALLA,
        )

        // Zona superior: progreso en el borde, botones redondos y tiempo con la pose objetivo
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding()) {
            BarraBorde(progresoBarra, colorEsqueleto)
            BarraSuperior(
                etiqueta = if (fase == Fase.CALIBRANDO) "CALIBRACIÓN" else "SESIÓN ACTIVA",
                titulo = reto.nombre,
                colorEtiqueta = if (fase == Fase.CALIBRANDO) CIAN else colorEsqueleto,
                onSalir = onVolver,
                vozActiva = vozActiva,
                onVoz = { vozActiva = !vozActiva },
            )
            when {
                fase == Fase.CALIBRANDO -> PanelCalibracion(
                    estado = calibracion,
                    progreso = progresoCalibracion,
                    vista = reto.vista,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                modoTecnico -> PanelTecnico(
                    reto = reto,
                    resultado = resultado,
                    onRepetir = repetir,
                    onVolver = onVolver,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                else -> Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    InsigniaTemporizador(
                        sostenidos = sostenidos,
                        objetivo = reto.tiempoObjetivo.toInt(),
                        etiqueta = etiquetaTiempo,
                        color = colorEsqueleto,
                        aprobado = resultado?.postura == Postura.APROBADO,
                    )
                    TarjetaObjetivo(reto = reto, onClick = { guiaAbierta = true })
                }
            }
        }

        // Zona inferior: mensaje de guía y aviso de privacidad
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (fase == Fase.RETO) PildoraEstado(reto, resultado)
            BarraInferior(modoTecnico = modoTecnico, onTecnico = { modoTecnico = !modoTecnico }, fps = fps)
        }

        if (mostrarExito) {
            PantallaResultado(
                reto = reto,
                resumen = resumenIntento,
                hoy = resumenHoy,
                metas = metas,
                onRepetir = repetir,
                onVolver = onVolver,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (guiaAbierta) {
            GuiaExpandida(reto = reto, onCerrar = { guiaAbierta = false })
        }

        errorCamara?.let { mensaje ->
            AvisoCamara(mensaje, Modifier.align(Alignment.Center).padding(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Componentes de la pantalla de cámara (identidad "Aurora")
// ---------------------------------------------------------------------------------------------

/** Marco y resplandor interior del color del estado; el color cambia con una transición suave. */
@Composable
private fun BordeEstado(color: Color, modifier: Modifier = Modifier) {
    val animado by animateColorAsState(color, tween(500), label = "borde")
    Canvas(modifier) {
        val g = 90.dp.toPx()
        val c = animado.copy(alpha = 0.30f)
        drawRect(Brush.verticalGradient(listOf(c, Color.Transparent), 0f, g), Offset(0f, 0f), Size(size.width, g))
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, c), size.height - g, size.height),
            Offset(0f, size.height - g), Size(size.width, g),
        )
        drawRect(Brush.horizontalGradient(listOf(c, Color.Transparent), 0f, g), Offset(0f, 0f), Size(g, size.height))
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, c), size.width - g, size.width),
            Offset(size.width - g, 0f), Size(g, size.height),
        )
        val grosor = 5.dp.toPx()
        drawRoundRect(
            animado.copy(alpha = 0.75f),
            topLeft = Offset(grosor / 2, grosor / 2),
            size = Size(size.width - grosor, size.height - grosor),
            cornerRadius = CornerRadius(36.dp.toPx()),
            style = Stroke(grosor),
        )
    }
}

/** Barra de progreso de ancho completo, pegada al borde superior. */
@Composable
private fun BarraBorde(progreso: Float, color: Color) {
    val animado by animateColorAsState(color, tween(400), label = "barra")
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        drawRect(Color(0x26FFFFFF))
        val ancho = size.width * progreso.coerceIn(0f, 1f)
        if (ancho > 0f) drawRect(animado, size = Size(ancho, size.height))
    }
}

@Composable
private fun BarraSuperior(
    etiqueta: String,
    titulo: String,
    colorEtiqueta: Color,
    onSalir: () -> Unit,
    vozActiva: Boolean,
    onVoz: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BotonRedondo("Salir del ejercicio", onSalir) {
            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PuntoPulso(colorEtiqueta)
                Text(
                    etiqueta,
                    color = colorEtiqueta,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
            }
            Text(
                titulo,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BotonRedondo(if (vozActiva) "Silenciar la voz" else "Activar la voz", onVoz) {
            IconoVoz(vozActiva, Modifier.size(22.dp))
        }
    }
}

@Composable
private fun BotonRedondo(descripcion: String, onClick: () -> Unit, contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(onClickLabel = descripcion, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

@Composable
private fun PuntoPulso(color: Color) {
    val transicion = rememberInfiniteTransition(label = "pulso")
    val alfa by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alfa",
    )
    Box(Modifier.size(8.dp).background(color.copy(alpha = alfa), CircleShape))
}

/** Altavoz dibujado a mano (el conjunto de íconos básico no lo trae). */
@Composable
private fun IconoVoz(activo: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width / 24f
        val trazo = Stroke(width = 2f * s, cap = StrokeCap.Round)
        val cuerpo = Path().apply {
            moveTo(3f * s, 9f * s)
            lineTo(7f * s, 9f * s)
            lineTo(12f * s, 5f * s)
            lineTo(12f * s, 19f * s)
            lineTo(7f * s, 15f * s)
            lineTo(3f * s, 15f * s)
            close()
        }
        drawPath(cuerpo, Color.White)
        if (activo) {
            drawArc(Color.White, -45f, 90f, false, Offset(8f * s, 8f * s), Size(8f * s, 8f * s), style = trazo)
            drawArc(Color.White, -45f, 90f, false, Offset(4f * s, 4f * s), Size(16f * s, 16f * s), style = trazo)
        } else {
            drawLine(Color.White, Offset(15f * s, 9f * s), Offset(21f * s, 15f * s), 2f * s, StrokeCap.Round)
            drawLine(Color.White, Offset(21f * s, 9f * s), Offset(15f * s, 15f * s), 2f * s, StrokeCap.Round)
        }
    }
}

/** Tiempo sostenido sobre el objetivo, con el estado actual debajo. */
@Composable
private fun InsigniaTemporizador(
    sostenidos: Int,
    objetivo: Int,
    etiqueta: String,
    color: Color,
    aprobado: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.cristal(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$sostenidos",
                    color = Color.White,
                    fontSize = TAMANO_TEMPORIZADOR,
                    lineHeight = TAMANO_TEMPORIZADOR,
                    fontWeight = FontWeight.Black,
                )
                Text("s", color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("/ ${objetivo}s", color = TEXTO_SUAVE, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Text(etiqueta, color = TEXTO_SUAVE, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Box(
            Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (aprobado) Icons.Default.Check else Icons.Default.Close,
                null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Pose objetivo en miniatura. Al tocarla se abre la guía del ejercicio (el reloj se pausa). */
@Composable
private fun TarjetaObjetivo(reto: Reto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(96.dp)
            .cristal(RoundedCornerShape(20.dp))
            .clickable(onClickLabel = "Abrir la guía del ejercicio", role = Role.Button, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("OBJETIVO", color = CIAN, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        ImagenPose(
            nombre = reto.imagen,
            descripcion = "Pose objetivo de ${reto.nombre}",
            modifier = Modifier.size(width = 80.dp, height = 96.dp),
        )
        Text("Ver guía", color = TEXTO_SUAVE, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Mensaje grande según el estado real del ejercicio. */
@Composable
private fun PildoraEstado(reto: Reto, resultado: Resultado?, modifier: Modifier = Modifier) {
    val color = resultado?.let { colorPostura(it.postura) } ?: Color.White
    val titulo: String
    val subtitulo: String
    val icono: ImageVector
    if (resultado == null) {
        titulo = "No te veo"
        subtitulo = "Ubícate frente a la cámara con el cuerpo completo"
        icono = Icons.Default.Warning
    } else if (resultado.estado == EstadoReto.SOSTENIENDO) {
        val falta = ceil(reto.tiempoObjetivo * (1.0 - resultado.progreso)).toInt().coerceAtLeast(1)
        titulo = "¡Excelente postura!"
        subtitulo = if (falta == 1) "Sostén 1 segundo más sin moverte" else "Sostén $falta segundos más sin moverte"
        icono = Icons.Default.Check
    } else if (resultado.postura == Postura.CORRIGIENDO) {
        titulo = "Casi lo tienes"
        subtitulo = "Ajusta los puntos en amarillo"
        icono = Icons.Default.Warning
    } else {
        titulo = "Ajusta tu postura"
        subtitulo = "Corrige los puntos en rojo"
        icono = Icons.Default.Close
    }

    val forma = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color(0xB30A1A33))
            .border(1.5.dp, color.copy(alpha = 0.55f), forma)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = FONDO_CAMARA, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontSize = TAMANO_MENSAJE, fontWeight = FontWeight.Bold)
            Text(subtitulo, color = color, fontSize = TAMANO_SUBMENSAJE, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BarraInferior(modoTecnico: Boolean, onTecnico: () -> Unit, fps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvisoPrivacidad()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (modoTecnico) {
                Text("$fps FPS", color = TEXTO_SUAVE, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            BotonCristal("Técnico", onTecnico, activo = modoTecnico)
        }
    }
}

@Composable
private fun AvisoPrivacidad() {
    val forma = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(forma)
            .background(Color(0x990A1A33))
            .border(1.dp, Color(0x1AFFFFFF), forma)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Default.Lock, null, tint = CIAN, modifier = Modifier.size(14.dp))
        Text("Tu video no sale del teléfono", color = TEXTO_SUAVE, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BotonCristal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    activo: Boolean? = null,      // null = botón normal; true/false = interruptor con punto
    plano: Boolean = false,       // plano: para usarlo dentro de otro panel de vidrio
) {
    val forma = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(forma)
            .then(if (plano) Modifier.background(Color(0x1FFFFFFF)) else Modifier.cristal(forma))
            .then(if (activo == true) Modifier.border(1.5.dp, CIAN, forma) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        if (activo != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (activo) CIAN else Color(0x66FFFFFF), CircleShape)
            )
        }
        Text(texto, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = FONDO_CAMARA, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(texto, color = FONDO_CAMARA, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Barra con relleno degradado y una marca por segundo (si hay entre 2 y 12 segundos). */
@Composable
private fun BarraCristal(
    progreso: Float,
    color: Color,
    modifier: Modifier = Modifier,
    segmentos: Int = 0,
) {
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val radio = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(Color(0x33FFFFFF), size = size, cornerRadius = radio)
        val ancho = size.width * progreso.coerceIn(0f, 1f)
        if (ancho > 0f) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)),
                size = Size(maxOf(ancho, size.height), size.height),
                cornerRadius = radio,
            )
        }
        if (segmentos in 2..12) {
            for (k in 1 until segmentos) {
                val x = size.width * k / segmentos
                drawLine(Color(0xB30A1A33), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

/** estado: true = correcto, false = falta, null = pendiente. */
@Composable
private fun ChipEstado(texto: String, estado: Boolean?, modifier: Modifier = Modifier) {
    val fondo = when (estado) {
        true -> Color(0x2439FF14)
        false -> Color(0x2EFF1744)
        null -> Color(0x1AFFFFFF)
    }
    val borde = when (estado) {
        true -> Color(0x8039FF14)
        false -> Color(0x99FF1744)
        null -> Color(0x33FFFFFF)
    }
    val contenido = when (estado) {
        true -> Color(0xFFB8FFA8)
        false -> Color(0xFFFF9DB0)
        null -> TEXTO_SUAVE
    }
    val forma = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(forma)
            .background(fondo)
            .border(1.dp, borde, forma)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (estado) {
            true -> Icon(Icons.Default.Check, null, tint = contenido, modifier = Modifier.size(12.dp))
            false -> Icon(Icons.Default.Close, null, tint = contenido, modifier = Modifier.size(12.dp))
            null -> {}
        }
        Text(texto, color = contenido, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AvisoCamara(mensaje: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.cristal().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Warning, null, tint = AMARILLO, modifier = Modifier.size(24.dp))
        Text(mensaje, color = Color.White, fontSize = 15.sp)
    }
}


@Composable
private fun PanelCalibracion(
    estado: EstadoCalibracion?,
    progreso: Float,
    vista: VistaCalibracion,
    modifier: Modifier = Modifier,
) {
    val color = when {
        estado == null -> Color.White
        estado.completa -> VERDE
        else -> ROJO
    }
    val palabra = when {
        estado == null -> "UBÍCATE"
        estado.completa -> "¡QUIETO!"
        vista == VistaCalibracion.COMPLETA -> "ALÉJATE"
        else -> "AJUSTA"
    }
    val detalle = when {
        estado == null -> "No te veo. Ponte frente a la cámara"
        estado.completa -> "Mantén la posición un momento"
        else -> "Falta: ${estado.faltantes.joinToString(", ")}"
    }
    val ayuda = when (vista) {
        VistaCalibracion.COMPLETA ->
            "Ubica tu teléfono en el suelo o sobre una mesa, aléjate unos 2 metros y asegúrate de tener buena iluminación."
        VistaCalibracion.LATERAL ->
            "Colócate de lado, con tu lado izquierdo hacia la cámara, y aléjate hasta que se vea todo tu cuerpo."
        VistaCalibracion.BUSTO ->
            "Colócate de frente: tu cabeza, tus orejas y tus hombros deben verse completos."
    }

    Column(
        modifier = modifier.fillMaxWidth().cristal().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(palabra, color = color, fontSize = 44.sp, fontWeight = FontWeight.Black)
        Text(detalle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)

        // Lista de verificación: qué partes del cuerpo ya se ven
        gruposRequeridos(vista).chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fila.forEach { nombre ->
                    ChipEstado(
                        texto = nombre.replaceFirstChar { it.uppercase() },
                        estado = if (estado == null) null else nombre !in estado.faltantes,
                    )
                }
            }
        }

        BarraCristal(progreso, color)
        Text(ayuda, color = TEXTO_SUAVE, fontSize = 13.sp)
    }
}

/** Modo técnico: valor de cada criterio, en el mismo orden que en ejercicios.json. */
@Composable
private fun PanelTecnico(
    reto: Reto,
    resultado: Resultado?,
    onRepetir: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = resultado?.let { colorPostura(it.postura) } ?: Color.White
    val mensaje = when {
        resultado == null -> "Ubícate frente a la cámara con el cuerpo completo"
        resultado.estado == EstadoReto.EXITO -> "¡Reto superado!"
        resultado.estado == EstadoReto.SOSTENIENDO -> "¡Sostén la pose!"
        resultado.postura == Postura.CORRIGIENDO -> "Casi... ajusta tu postura"
        else -> "Ajusta tu postura"
    }

    Column(
        modifier = modifier.fillMaxWidth().cristal().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(mensaje, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        BarraCristal(resultado?.progreso ?: 0f, color, segmentos = reto.tiempoObjetivo.toInt())
        resultado?.criterios?.forEachIndexed { i, c ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(colorPostura(c.postura), CircleShape))
                Text(
                    if (c.esDistancia) "Criterio ${i + 1}: ${"%.2f".format(c.angulo)} anchos de hombro"
                    else "Criterio ${i + 1}: ${c.angulo.toInt()}°",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }
        if (resultado?.estado == EstadoReto.EXITO) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonCristal("Repetir", onRepetir, icono = Icons.Default.Refresh, plano = true)
                BotonCristal("Volver", onVolver, plano = true)
            }
        }
    }
}

@Composable
private fun EsqueletoOverlay(
    pose: PoseFrame?,
    color: Color,
    espejar: Boolean,
    modifier: Modifier = Modifier,
    estadoPuntos: Map<Int, Postura>? = null,
    estadoHuesos: Map<Pair<Int, Int>, Postura>? = null,
    llenar: Boolean = false,
) {
    Canvas(modifier = modifier) {
        val p = pose ?: return@Canvas
        val escala =
            if (llenar) maxOf(size.width / p.ancho, size.height / p.alto)
            else minOf(size.width / p.ancho, size.height / p.alto)
        val offX = (size.width - p.ancho * escala) / 2f
        val offY = (size.height - p.alto * escala) / 2f
        val puntos = p.puntos.map { pt ->
            val x = if (espejar) 1f - pt.x else pt.x
            Offset(offX + x * p.ancho * escala, offY + pt.y * p.alto * escala)
        }
        val neutro = Color(0xCCFFFFFF)

        // Sin estados por articulación (calibración o cuerpo perdido): todo del mismo color
        val huesos = estadoHuesos
        val articulaciones = estadoPuntos
        val porArticulacion = huesos != null && articulaciones != null

        fun colorDeHueso(a: Int, b: Int): Color =
            if (huesos == null) color
            else huesos[minOf(a, b) to maxOf(a, b)]?.let { colorPostura(it) } ?: neutro

        // Primero los huesos sin evaluar y después los evaluados (con halo), para que no queden tapados
        for (pasada in 0..1) {
            PoseLandmarker.POSE_LANDMARKS.forEach { c ->
                val a = c.start()
                val b = c.end()
                if (a < puntos.size && b < puntos.size) {
                    val col = colorDeHueso(a, b)
                    val evaluado = porArticulacion && col != neutro
                    if ((pasada == 1) == evaluado) {
                        if (evaluado) drawLine(col.copy(alpha = 0.25f), puntos[a], puntos[b], strokeWidth = 24f)
                        drawLine(col, puntos[a], puntos[b], strokeWidth = if (evaluado) 10f else 6f)
                    }
                }
            }
        }
        puntos.forEachIndexed { i, punto ->
            val estado = articulaciones?.get(i)
            val col = when {
                !porArticulacion -> color
                estado != null -> colorPostura(estado)
                else -> neutro
            }
            if (estado != null) drawCircle(col.copy(alpha = 0.25f), radius = 26f, center = punto)
            drawCircle(col, radius = if (estado != null) 15f else 9f, center = punto)
        }
    }
}