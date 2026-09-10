package com.soyache.blurgiro.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import kotlin.math.PI

/**
 * Convierte rotation vector (o giroscopio) en inclinación suavizada -1..1.
 * Pausa con [stop]; no registra nada si la pantalla está apagada.
 */
class TiltTracker(
    context: Context,
    private val smoothness: () -> Float,
    private val onTilt: (tiltX: Float, tiltY: Float) -> Unit,
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val rotationSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val gyroSensor: Sensor? =
        if (rotationSensor == null) sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) else null

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var smoothX = 0f
    private var smoothY = 0f
    private var lastGyroNanos = 0L
    private var listening = false

    val hasSensor: Boolean get() = rotationSensor != null || gyroSensor != null

    fun start() {
        if (listening) return
        val sensor = rotationSensor ?: gyroSensor ?: return
        listening = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        if (!listening) return
        listening = false
        sensorManager.unregisterListener(this)
        lastGyroNanos = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rawX: Float
        val rawY: Float
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            integrateGyro(event) ?: return
            val x = smoothX
            val y = smoothY
            mainHandler.post { onTilt(x, y) }
            return
        } else {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val (axisX, axisY) = remapAxes()
            SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
            SensorManager.getOrientation(remappedMatrix, orientation)
            val pitch = orientation[1]
            val roll = orientation[2]
            rawX = (roll / MAX_ANGLE).coerceIn(-1f, 1f)
            rawY = (-pitch / MAX_ANGLE).coerceIn(-1f, 1f)
        }

        val follow = 0.07f + (1f - smoothness().coerceIn(0f, 1f)) * 0.38f
        smoothX += (rawX - smoothX) * follow
        smoothY += (rawY - smoothY) * follow
        val x = smoothX
        val y = smoothY
        mainHandler.post { onTilt(x, y) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun integrateGyro(event: SensorEvent): Pair<Float, Float>? {
        val now = event.timestamp
        if (lastGyroNanos == 0L) {
            lastGyroNanos = now
            return null
        }
        val dt = ((now - lastGyroNanos).coerceAtLeast(0L) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastGyroNanos = now
        // values: rad/s en ejes del dispositivo. Integramos y decaemos hacia 0.
        smoothX = (smoothX + event.values[1] * dt * 1.6f) * 0.985f
        smoothY = (smoothY + event.values[0] * dt * 1.6f) * 0.985f
        return smoothX.coerceIn(-1f, 1f) to smoothY.coerceIn(-1f, 1f)
    }

    @Suppress("DEPRECATION")
    private fun remapAxes(): Pair<Int, Int> {
        val rotation = windowManager.defaultDisplay.rotation
        return when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
    }

    companion object {
        private const val MAX_ANGLE = (32f * PI / 180f).toFloat()
    }
}
