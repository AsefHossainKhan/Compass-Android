package com.asef.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import com.asef.compass.ui.theme.CompassTheme
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class MainActivity : ComponentActivity() {
    private var rotationMatrix: FloatArray = FloatArray(9)
    private var accelerometerReading: FloatArray = FloatArray(3)
    private var magnetometerReading: FloatArray = FloatArray(3)
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private var accelerometer: Sensor? = null
    private lateinit var sensorEventListener: SensorEventListener
    private var mutableRotation = MutableLiveData(0.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // Update compass direction based on sensor data
                event.let {
                    if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(
                            event.values,
                            0,
                            accelerometerReading,
                            0,
                            accelerometerReading.size
                        )
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(
                            event.values,
                            0,
                            magnetometerReading,
                            0,
                            magnetometerReading.size
                        )
                    }
                    SensorManager.getRotationMatrix(
                        rotationMatrix,
                        null,
                        accelerometerReading,
                        magnetometerReading
                    )
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuth = orientationAngles[0]
                    val pitch = orientationAngles[1]
                    var roll = orientationAngles[2]
//                    var z = azimuth + atan((sin(roll) * tan(pitch)).toDouble() / cos(roll) * tan(pitch))
                    if (pitch > Math.PI / 2 || pitch < -Math.PI / 2 ) {
                        roll = 0f
                    }

                    val rollMatrix = arrayOf(
                        arrayOf(1f, 0f, 0f),
                        arrayOf(0f, cos(roll), sin(roll)),
                        arrayOf(0f, -sin(roll), cos(roll))
                    )
                    val pitchMatrix = arrayOf(
                        arrayOf(cos(pitch), 0f, -sin(pitch)),
                        arrayOf(0f, 1f, 0f),
                        arrayOf(sin(pitch), 0f, cos(pitch))
                    )
                    val x = arrayOf(arrayOf(azimuth), arrayOf(pitch), arrayOf(roll))
                    val y = multiplyMatrices(pitchMatrix, rollMatrix)
                    val z = multiplyMatrices(y, x)

                    mutableRotation.postValue(-((Math.toDegrees(z[0][0].toDouble()) + 360) % 360).toFloat())
//                    mutableRotation.postValue((-((Math.toDegrees(z)) + 360) % 360).toFloat())
                }

            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes
                Log.d("TAG", "onAccuracyChanged: $sensor --- $accuracy")
            }
        }

        setContent {
            CompassTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val rotation by animateFloatAsState(
                        mutableRotation.observeAsState().value!!,
                        label = "rotation animation value",
                        animationSpec = tween(200, easing = EaseInOut)
                    )
                    Compass(rotation)
                }
            }
        }

        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        sensorManager.registerListener(
            sensorEventListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorEventListener)
    }
}

@Preview
@Composable
fun SimpleComposablePreview() {
    Compass(0f)
}


