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

private fun colorPostura(p: Postura) = when (p) {
    Postura.APROBADO -> VERDE
    Postura.CORRIGIENDO -> AMARILLO
    Postura.INCORRECTO -> ROJO
}

@Composable
fun PantallaCamara() {
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
        VistaCamara()
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
            // Si Android ya no vuelve a preguntar, el permiso se activa desde los ajustes
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
private fun VistaCamara() {
    val contexto = LocalContext.current
    val cargado = remember { runCatching { CatalogoRetos.cargar(contexto).first() } }
    // El detector solo se crea si el reto cargó bien
    val detectorCargado = remember {
        if (cargado.isSuccess) runCatching { DetectorPose(contexto) } else null
    }
    val reto = cargado.getOrNull()
    val detector = detectorCargado?.getOrNull()

    when {
        reto == null ->
            MensajeError("No se pudo cargar ejercicios.json: ${cargado.exceptionOrNull()?.message}")
        detector == null ->
            MensajeError("No se pudo iniciar el detector de pose: ${detectorCargado?.exceptionOrNull()?.message}")
        else -> SesionReto(reto, detector)
    }
}

@Composable
private fun SesionReto(reto: Reto, detector: DetectorPose) {
    val contexto = LocalContext.current
    val actividad = contexto as ComponentActivity
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val evaluador = remember(reto) { EvaluadorReto(reto) }
    val sonidos = remember { Sonidos() }
    val pose by detector.pose.collectAsState()
    var resultado by remember { mutableStateOf<Resultado?>(null) }
    var errorCamara by remember { mutableStateOf<String?>(null) }

    // Medición temporal de FPS (RNF-02)
    val medida = remember { LongArray(2) }   // [0] inicio de ventana (ms), [1] cuadros
    var fps by remember { mutableStateOf(0) }

    // Cada vez que llega una pose nueva: evaluar, sonar y actualizar la UI
    LaunchedEffect(pose) {
        val t = SystemClock.elapsedRealtime()
        if (medida[0] == 0L) medida[0] = t
        medida[1]++
        if (t - medida[0] >= 1000) {
            fps = medida[1].toInt()
            medida[0] = t
            medida[1] = 0
        }

        val p = pose
        if (p == null) {
            evaluador.perderCuerpo()?.let { sonidos.reproducir(it) }
            resultado = null
        } else {
            // Coordenadas normalizadas -> píxeles, para que el ángulo no se distorsione
            val puntos = p.puntos.map { Punto(it.x * p.ancho, it.y * p.alto) }
            val r = evaluador.evaluar(puntos, SystemClock.elapsedRealtime() / 1000.0)
            r.sonido?.let { sonidos.reproducir(it) }
            resultado = r
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ejecutor.shutdown()
            detector.cerrar()
            sonidos.liberar()
        }
    }

    val colorEsqueleto = resultado?.let { colorPostura(it.postura) } ?: Color.White

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
                                // Un fotograma fallido no debe tumbar la app
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

        PanelEstado(
            reto = reto,
            resultado = resultado,
            onRepetir = { evaluador.reiniciar() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp),
        )

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

        Text(
            text = "FPS: $fps",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(12.dp),
        )

        Text(
            text = "Procesado en tu dispositivo · no se guarda ni se envía video",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp)
                .background(Color(0x99000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PanelEstado(
    reto: Reto,
    resultado: Resultado?,
    onRepetir: () -> Unit,
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
    val progreso = (resultado?.progreso ?: 0f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x99000000), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(reto.nombre, color = Color.White, fontSize = 14.sp)
        Text(mensaje, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        // Barra de progreso del tiempo sostenido
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(5.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progreso)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(5.dp))
            )
        }

        resultado?.criterios?.forEachIndexed { i, c ->
            Text(
                "Criterio ${i + 1}: ${c.angulo.toInt()}°",
                color = colorPostura(c.postura),
                fontSize = 14.sp,
            )
        }

        if (resultado?.estado == EstadoReto.EXITO) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRepetir) { Text("Repetir") }
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
            val x = if (espejar) 1f - pt.x else pt.x   // espejo solo en el dibujo
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