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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import com.asef.compass.ui.theme.Arc
import com.asef.compass.ui.theme.CardinalFont
import com.asef.compass.ui.theme.CardinalMarking
import com.asef.compass.ui.theme.Center
import com.asef.compass.ui.theme.CompassTheme
import com.asef.compass.ui.theme.Marking
import com.asef.compass.ui.theme.NorthCardinalFont
import com.asef.compass.ui.theme.Outer
import com.asef.compass.ui.theme.Pathing
import com.asef.compass.ui.theme.PathingFill
import kotlin.math.cos
import kotlin.math.sin

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
                    mutableRotation.postValue(-((Math.toDegrees(orientationAngles[0].toDouble()) + 360) % 360).toFloat())
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

@OptIn(ExperimentalTextApi::class)
@Composable
fun Compass(
    rotation: Float
) {
    val textMeasurer = rememberTextMeasurer()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.width / 3

            rotate(rotation) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Center,
                            Outer
                        )
                    ),
                    radius = radius
                )
                val markerRadius = radius - 20

                for (i in 0 until 360 step 15) {
                    val markerLength = if (i % 90 == 0) 75 else 50
                    val angle = Math.toRadians(i.toDouble())
                    val startX = centerX + sin(angle).toFloat() * markerRadius
                    val startY = centerY - cos(angle).toFloat() * markerRadius
                    val endX = centerX + sin(angle).toFloat() * (markerRadius - markerLength)
                    val endY = centerY - cos(angle).toFloat() * (markerRadius - markerLength)
                    drawLine(
                        color = if (i % 90 == 0) CardinalMarking else Marking,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i % 90 == 0) 10f else 5f
                    )
                }
                val fontStyleSWE = TextStyle(
                    fontSize = 24.sp,
                    color = CardinalFont,
                    fontFamily = FontFamily.Serif
                )
                // N
                val styleN = TextStyle(
                    fontSize = 24.sp,
                    color = NorthCardinalFont,
                    fontFamily = FontFamily.Serif
                )
                val textLayoutResultN = textMeasurer.measure("N", style = styleN)
                val offsetN = Offset(
                    size.width / 2 - textLayoutResultN.size.width / 2,
                    size.height / 2 - radius - textLayoutResultN.size.height
                )
                val rotationOffsetN = Offset(
                    size.width / 2,
                    size.height / 2 - radius - textLayoutResultN.size.height / 2
                )
                rotate(-rotation, rotationOffsetN) {
                    drawText(textLayoutResultN, topLeft = offsetN)
                }
                // S
                val textLayoutResultS = textMeasurer.measure("S", style = fontStyleSWE)
                val offsetS = Offset(
                    size.width / 2 - textLayoutResultS.size.width / 2,
                    size.height / 2 + radius
                )
                val rotationOffsetS = Offset(
                    size.height / 2,
                    size.width / 2 + radius + textLayoutResultS.size.height / 2
                )
                rotate(-rotation, rotationOffsetS) {
                    drawText(textLayoutResultS, topLeft = offsetS)
                }
                // E
                val textLayoutResultE = textMeasurer.measure("E", style = fontStyleSWE)
                val offsetE = Offset(
                    size.width / 2 + radius + 18,
                    size.height / 2 - textLayoutResultE.size.height / 2
                )
                val rotationOffsetE = Offset(
                    size.width / 2 + radius + textLayoutResultE.size.height / 2,
                    size.height / 2
                )
                rotate(-rotation, rotationOffsetE) {
                    drawText(textLayoutResultE, topLeft = offsetE)
                }
                // W
                val textLayoutResultW = textMeasurer.measure("W", style = fontStyleSWE)
                val offsetW = Offset(
                    size.width / 2 - radius - textLayoutResultW.size.width - 10,
                    size.height / 2 - textLayoutResultW.size.height / 2
                )
                val rotationOffsetW = Offset(
                    size.height / 2 - radius - textLayoutResultW.size.height / 2,
                    size.width / 2
                )
                rotate(-rotation, rotationOffsetW) {
                    drawText(textLayoutResultW, topLeft = offsetW)
                }
                val arcSize: Float = radius * 2f + 66
                val arcTopLeftOffset: Float = radius / 2f - 33

                drawArc(
                    color = Arc,
                    startAngle = 10f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(arcTopLeftOffset, arcTopLeftOffset),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = 20f, cap = StrokeCap.Round)
                )

                drawArc(
                    color = Arc,
                    startAngle = 100f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(arcTopLeftOffset, arcTopLeftOffset),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = 20f, cap = StrokeCap.Round)
                )

                drawArc(
                    color = Arc,
                    startAngle = 190f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(arcTopLeftOffset, arcTopLeftOffset),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = 20f, cap = StrokeCap.Round)
                )

                drawArc(
                    color = Arc,
                    startAngle = 280f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(arcTopLeftOffset, arcTopLeftOffset),
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = 20f, cap = StrokeCap.Round)
                )
                var outerRadius = 100f
                var innerRadius = 20f
                val starPath = Path()
                for (i in 0 until 5) {
                    // Outer point
                    val outerX =
                        centerX + outerRadius * cos(Math.toRadians(((i * 90) + 45).toDouble())).toFloat()
                    val outerY =
                        centerY + outerRadius * sin(Math.toRadians(((i * 90) + 45).toDouble())).toFloat()
                    if (i == 0) {
                        starPath.moveTo(outerX, outerY)
                    } else {
                        starPath.lineTo(outerX, outerY)
                    }
                    // Inner point
                    val innerX =
                        centerX + innerRadius * cos(Math.toRadians(((i * 90) + 90).toDouble())).toFloat()
                    val innerY =
                        centerY + innerRadius * sin(Math.toRadians(((i * 90) + 90).toDouble())).toFloat()
                    starPath.lineTo(innerX, innerY)
                }
                drawPath(
                    path = starPath,
                    color = PathingFill,
                    style = Fill
                )
                drawPath(
                    path = starPath,
                    color = Pathing,
                    style = Stroke(
                        miter = 10f,
                        width = 5f,
                        join = StrokeJoin.Round
                    )
                )
                val secondStarPath = Path()
                outerRadius = 150f
                innerRadius = 20f
                for (i in 0 until 5) {
                    // Outer point
                    val outerX =
                        centerX + outerRadius * cos(Math.toRadians((i * 90).toDouble())).toFloat()
                    val outerY =
                        centerY + outerRadius * sin(Math.toRadians((i * 90).toDouble())).toFloat()
                    if (i == 0) {
                        secondStarPath.moveTo(outerX, outerY)
                    } else {
                        secondStarPath.lineTo(outerX, outerY)
                    }
                    // Inner point
                    val innerX =
                        centerX + innerRadius * cos(Math.toRadians(((i * 90) + 45).toDouble())).toFloat()
                    val innerY =
                        centerY + innerRadius * sin(Math.toRadians(((i * 90) + 45).toDouble())).toFloat()
                    secondStarPath.lineTo(innerX, innerY)
                }
                drawPath(
                    path = secondStarPath,
                    color = Pathing,
                    style = Stroke(
                        miter = 10f,
                        width = 5f,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun SimpleComposablePreview() {
    Compass(0f)
}
