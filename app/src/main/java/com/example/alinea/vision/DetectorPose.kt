package com.example.alinea.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.alinea.core.Punto
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Puntos normalizados (0..1) y tamaño de la imagen analizada. */
data class PoseFrame(val puntos: List<Punto>, val ancho: Int, val alto: Int)

class DetectorPose(context: Context) {
    private val _pose = MutableStateFlow<PoseFrame?>(null)
    val pose: StateFlow<PoseFrame?> = _pose.asStateFlow()

    @Volatile
    private var cerrado = false
    private val landmarker: PoseLandmarker = construir(context)

    private fun construir(context: Context): PoseLandmarker {
        val base = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_full.task")
            .setDelegate(Delegate.CPU)
            .build()
        val opciones = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { resultado, imagen ->
                publicar(resultado, imagen.width, imagen.height)
            }
            .setErrorListener { e -> Log.e("DetectorPose", e.message ?: "error") }
            .build()
        return PoseLandmarker.createFromOptions(context, opciones)
    }

    private fun publicar(resultado: PoseLandmarkerResult, ancho: Int, alto: Int) {
        val primera = resultado.landmarks().firstOrNull()
        _pose.value = if (primera == null) null
        else PoseFrame(primera.map { Punto(it.x(), it.y()) }, ancho, alto)
    }

    fun detectar(bitmap: Bitmap, tiempoMs: Long) {
        if (cerrado) return
        landmarker.detectAsync(BitmapImageBuilder(bitmap).build(), tiempoMs)
    }

    fun cerrar() {
        cerrado = true
        landmarker.close()
    }
}