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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
fun PantallaCamara(reto: Reto?, onVolver: () -> Unit) {
    val context = LocalContext.current
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
        VistaCamara(reto, onVolver)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Se necesita la cámara para analizar tu postura. El video se procesa en tu dispositivo y no se envía a ningún lado.")
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
private fun VistaCamara(reto: Reto?, onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val detectorCargado = remember(reto) {
        if (reto != null) runCatching { DetectorPose(contexto) } else null
    }
    val detector = detectorCargado?.getOrNull()

    when {
        reto == null -> MensajeError("No se encontró el ejercicio.")
        detector == null ->
            MensajeError("No se pudo iniciar el detector de pose: ${detectorCargado?.exceptionOrNull()?.message}")
        else -> SesionReto(reto, detector, onVolver)
    }
}

@Composable
private fun SesionReto(reto: Reto, detector: DetectorPose, onVolver: () -> Unit) {
    val contexto = LocalContext.current
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
            evaluador.perderCuerpo()?.let { sonidos.reproducir(it) }
            voz.decir("No te veo. Ubícate frente a la cámara", enfriamientoMs = 6000)
            if (evaluador.estado != EstadoReto.EXITO) estadoAnterior = null
            resultado = null
        } else {
            // Coordenadas normalizadas -> píxeles, para que el ángulo no se distorsione
            val puntos = p.puntos.map { Punto(it.x * p.ancho, it.y * p.alto) }
            val r = evaluador.evaluar(puntos, ahora)
            if (intento.alEvaluar(r.estado, r.progreso * reto.tiempoObjetivo)) {
                registro.registrar(reto.id, reto.tiempoObjetivo, reto.tiempoObjetivo, EstadoEjercicio.COMPLETADO)
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
    val mostrarExito = fase == Fase.RETO && !modoTecnico && resultado?.estado == EstadoReto.EXITO

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FONDO_CAMARA)
            // Marco de color redondeado: se ve de lejos sin leer nada
            .then(
                if (fase == Fase.RETO) Modifier.border(8.dp, colorEsqueleto, RoundedCornerShape(36.dp))
                else Modifier
            ),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val vista = PreviewView(ctx)
                vista.scaleType = PreviewView.ScaleType.FIT_CENTER
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
                .height(260.dp)
                .background(Brush.verticalGradient(listOf(Color(0xCC061226), Color.Transparent)))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC061226))))
        )

        EsqueletoOverlay(
            pose = pose,
            color = colorEsqueleto,
            espejar = true,
            modifier = Modifier.fillMaxSize(),
            estadoPuntos = estadoPuntos,
            estadoHuesos = estadoHuesos,
        )

        val zonaSuperior = Modifier.align(Alignment.TopCenter).statusBarsPadding()
        when {
            fase == Fase.CALIBRANDO -> PanelCalibracion(
                estado = calibracion,
                progreso = progresoCalibracion,
                vista = reto.vista,
                modifier = zonaSuperior.padding(16.dp),
            )
            modoTecnico -> PanelTecnico(
                reto = reto,
                resultado = resultado,
                onRepetir = { evaluador.reiniciar(); intento.reiniciar() },
                onVolver = onVolver,
                modifier = zonaSuperior.padding(start = 16.dp, end = 16.dp, top = 64.dp),
            )
            else -> HudDistancia(
                reto = reto,
                resultado = resultado,
                modifier = zonaSuperior.padding(start = 16.dp, end = 16.dp, top = 64.dp),
            )
        }

        if (fase == Fase.RETO) {
            CapsulaGuia(
                onClick = { guiaAbierta = true },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
            )
        }

        // Zona inferior: controles y aviso de privacidad
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DockControles(
                onVolver = onVolver,
                vozActiva = vozActiva,
                onVoz = { vozActiva = !vozActiva },
                modoTecnico = modoTecnico,
                onTecnico = { modoTecnico = !modoTecnico },
                fps = fps,
            )
            Spacer(Modifier.height(8.dp))
            AvisoPrivacidad()
        }

        if (mostrarExito) {
            TarjetaExito(
                nombre = reto.nombre,
                onRepetir = { evaluador.reiniciar(); intento.reiniciar() },
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
private fun DockControles(
    onVolver: () -> Unit,
    vozActiva: Boolean,
    onVoz: () -> Unit,
    modoTecnico: Boolean,
    onTecnico: () -> Unit,
    fps: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cristal(RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonCristal("Ejercicios", onVolver, icono = Icons.AutoMirrored.Filled.ArrowBack, plano = true)
        if (modoTecnico) {
            Text("$fps FPS", color = TEXTO_SUAVE, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BotonCristal("Voz", onVoz, activo = vozActiva, plano = true)
            BotonCristal("Técnico", onTecnico, activo = modoTecnico, plano = true)
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
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Default.Lock, null, tint = TEXTO_SUAVE, modifier = Modifier.size(12.dp))
        Text(
            "Procesado en tu dispositivo",
            color = TEXTO_SUAVE,
            fontSize = 11.sp,
        )
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
private fun TarjetaExito(
    nombre: String,
    onRepetir: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color(0x80061226)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp).cristal().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(88.dp).background(Color(0xFF2BD97C), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, null, tint = FONDO_CAMARA, modifier = Modifier.size(52.dp))
            }
            Text("¡LOGRADO!", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text(nombre, color = TEXTO_SUAVE, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            BotonPrimario("Repetir", onRepetir, icono = Icons.Default.Refresh)
            BotonCristal("Volver a la lista", onVolver, modifier = Modifier.fillMaxWidth().height(52.dp))
        }
    }
}

/** Indicador para leer a 2 m: número grande + una palabra corta. */
@Composable
private fun HudDistancia(reto: Reto, resultado: Resultado?, modifier: Modifier = Modifier) {
    val color = resultado?.let { colorPostura(it.postura) } ?: Color.White
    val sosteniendo = resultado?.estado == EstadoReto.SOSTENIENDO
    val exito = resultado?.estado == EstadoReto.EXITO

    val grande = when {
        exito -> "✓"
        sosteniendo -> {
            val restante = ceil(reto.tiempoObjetivo * (1.0 - (resultado?.progreso ?: 0f))).toInt()
            restante.coerceAtLeast(1).toString()
        }
        resultado == null -> "?"
        else -> "✕"
    }
    val palabra = when {
        exito -> "¡LOGRADO!"
        sosteniendo -> "¡SOSTÉN!"
        resultado == null -> "UBÍCATE"
        resultado.postura == Postura.CORRIGIENDO -> "CASI"
        else -> "AJUSTA"
    }
    val bien = resultado?.criterios?.count { it.postura == Postura.APROBADO }
    val total = resultado?.criterios?.size

    Column(
        modifier = modifier.fillMaxWidth().cristal().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(grande, color = color, fontSize = 80.sp, lineHeight = 88.sp, fontWeight = FontWeight.Black)
            Column {
                Text(palabra, color = color, fontSize = 38.sp, fontWeight = FontWeight.Black)
                Text(reto.nombre, color = TEXTO_SUAVE, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        BarraCristal(resultado?.progreso ?: 0f, color, segmentos = reto.tiempoObjetivo.toInt())
        if (bien != null && total != null) {
            ChipEstado("$bien/$total articulaciones correctas", if (bien == total) true else null)
        }
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
        Text("CALIBRACIÓN", color = CIAN, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
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

/** Modo técnico: ángulo de cada criterio, en el mismo orden que en ejercicios.json. */
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
        Text(reto.nombre, color = TEXTO_SUAVE, fontSize = 13.sp)
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
                )            }
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
) {
    Canvas(modifier = modifier) {
        val p = pose ?: return@Canvas
        val escala = minOf(size.width / p.ancho, size.height / p.alto)
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