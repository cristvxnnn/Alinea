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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.alinea.audio.Sonidos
import com.example.alinea.core.*
import com.example.alinea.data.CatalogoRetos
import com.example.alinea.vision.DetectorPose
import com.example.alinea.vision.PoseFrame
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.util.concurrent.Executors

// Colores de alto contraste (verde neón / amarillo / rojo brillante)
private val VERDE = Color(0xFF39FF14)
private val AMARILLO = Color(0xFFFFEB00)
private val ROJO = Color(0xFFFF1744)

private enum class Fase { CALIBRANDO, RETO }

private fun colorPostura(p: Postura) = when (p) {
    Postura.APROBADO -> VERDE
    Postura.CORRIGIENDO -> AMARILLO
    Postura.INCORRECTO -> ROJO
}

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
    val evaluador = remember(reto) { EvaluadorReto(reto) }
    val calibrador = remember { Calibrador() }
    val sonidos = remember { Sonidos() }
    val camara = remember { arrayOfNulls<ProcessCameraProvider>(1) }
    val pose by detector.pose.collectAsState()

    var fase by remember { mutableStateOf(Fase.CALIBRANDO) }
    var calibracion by remember { mutableStateOf<EstadoCalibracion?>(null) }
    var progresoCalibracion by remember { mutableFloatStateOf(0f) }
    var resultado by remember { mutableStateOf<Resultado?>(null) }
    var errorCamara by remember { mutableStateOf<String?>(null) }

    val medida = remember { LongArray(2) }
    var fps by remember { mutableIntStateOf(0) }

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

        val p = pose
        if (fase == Fase.CALIBRANDO) {
            if (p == null) {
                calibracion = null
                progresoCalibracion = calibrador.actualizar(false, ahora)
            } else {
                val estado = evaluarSilueta(p.puntos, p.visibilidad)
                calibracion = estado
                val yaEstabaListo = calibrador.listo
                progresoCalibracion = calibrador.actualizar(estado.completa, ahora)
                if (!yaEstabaListo && calibrador.listo) {
                    sonidos.reproducir(Sonido.LISTO)
                    fase = Fase.RETO
                }
            }
        } else if (p == null) {
            evaluador.perderCuerpo()?.let { sonidos.reproducir(it) }
            resultado = null
        } else {
            val puntos = p.puntos.map { Punto(it.x * p.ancho, it.y * p.alto) }
            val r = evaluador.evaluar(puntos, ahora)
            r.sonido?.let { sonidos.reproducir(it) }
            resultado = r
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            camara[0]?.unbindAll()
            ejecutor.shutdown()
            detector.cerrar()
            sonidos.liberar()
        }
    }

    val colorEsqueleto = if (fase == Fase.CALIBRANDO) {
        calibracion?.let { if (it.completa) VERDE else ROJO } ?: Color.White
    } else {
        resultado?.let { colorPostura(it.postura) } ?: Color.White
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val vista = PreviewView(ctx)
                vista.scaleType = PreviewView.ScaleType.FIT_CENTER
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

        EsqueletoOverlay(
            pose = pose,
            color = colorEsqueleto,
            espejar = true,
            modifier = Modifier.fillMaxSize(),
        )

        val modificadorPanel = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(16.dp)

        if (fase == Fase.CALIBRANDO) {
            PanelCalibracion(
                estado = calibracion,
                progreso = progresoCalibracion,
                modifier = modificadorPanel,
            )
        } else {
            PanelEstado(
                reto = reto,
                resultado = resultado,
                onRepetir = { evaluador.reiniciar() },
                onVolver = onVolver,
                modifier = modificadorPanel,
            )
        }

        errorCamara?.let { mensaje ->
            Text(
                text = mensaje,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                    .padding(16.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onVolver) { Text("← Ejercicios") }
                Text("FPS: $fps", color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Procesado en tu dispositivo · no se guarda ni se envía video",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color(0x99000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun BarraProgreso(progreso: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color(0x33FFFFFF), RoundedCornerShape(5.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progreso.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(5.dp))
        )
    }
}

@Composable
private fun PanelCalibracion(
    estado: EstadoCalibracion?,
    progreso: Float,
    modifier: Modifier = Modifier,
) {
    val color = when {
        estado == null -> Color.White
        estado.completa -> VERDE
        else -> ROJO
    }
    val mensaje = when {
        estado == null -> "No te veo. Ubícate frente a la cámara"
        estado.completa -> "¡Perfecto! Mantente quieto un momento"
        else -> "Aléjate hasta que se vea tu cuerpo completo. Falta: ${estado.faltantes.joinToString(", ")}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("Calibración", color = Color.White, fontSize = 14.sp)
        Text(mensaje, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        BarraProgreso(progreso, color)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ubica tu teléfono en el suelo o sobre una mesa, aléjate unos 2 metros y asegúrate de tener buena iluminación.",
            color = Color.White,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun PanelEstado(
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
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(reto.nombre, color = Color.White, fontSize = 14.sp)
        Text(mensaje, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        BarraProgreso(resultado?.progreso ?: 0f, color)

        resultado?.criterios?.forEachIndexed { i, c ->
            Text(
                "Criterio ${i + 1}: ${c.angulo.toInt()}°",
                color = colorPostura(c.postura),
                fontSize = 14.sp,
            )
        }

        if (resultado?.estado == EstadoReto.EXITO) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRepetir) { Text("Repetir") }
                Button(onClick = onVolver) { Text("Volver al Menú") }
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
        PoseLandmarker.POSE_LANDMARKS.forEach { c ->
            if (c.start() < puntos.size && c.end() < puntos.size) {
                drawLine(color, puntos[c.start()], puntos[c.end()], strokeWidth = 8f)
            }
        }
        puntos.forEach { drawCircle(color, radius = 10f, center = it) }
    }
}