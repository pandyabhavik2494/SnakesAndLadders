package com.pandyabhavik.snakes.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/** Throws the die when the phone is given a firm shake, while [enabled]. At most once a second. */
@Composable
fun ShakeToRoll(enabled: Boolean, onShake: () -> Unit) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onShake)
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (g > 2.4f && now - last > 1000) { last = now; latest() }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accel != null) sensors.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors?.unregisterListener(listener) }
    }
}
