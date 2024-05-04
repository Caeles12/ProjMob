package com.example.projmob.minigame

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_GAME_FINISH
import com.example.projmob.bluetoothService
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class Driving : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var ll: LinearLayout? = null

    private var drivingScore: TextView? = null
    private var drivingTimer: TextView? = null
    private var score: Int = 0

    private val GAME_DURATION: Int = 30000
    private val PENALITY: Int = 10
    private val MAX_TIME: Double = 5000.0
    private val MAX_POINTS: Double = 100.0
    private val FLOWERS: Array<String> = arrayOf("\uD83C\uDF38", "\uD83D\uDCAE", "\uD83E\uDEB7", "\uD83C\uDFF5\uFE0F", "\uD83C\uDF39", "\uD83C\uDF3A", "\uD83C\uDF3B", "\uD83C\uDF3C", "\uD83C\uDF37", "\uD83E\uDEBB")

    private var myFinalScore: Int? = null
    private var opponentFinalScore: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)
        ll =  findViewById(R.id.drivinggamell);
        drivingScore = findViewById(R.id.drivinggamescore)
        drivingTimer = findViewById(R.id.drivinggametimer)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val gameThread: GameThread = GameThread(this)
        gameThread.setRunning(true)
        gameThread.start()

        var scoreIntent = Intent(this, Score::class.java)
        if(bluetoothService != null){
            val gameFinishedHandler = bluetoothService!!.MyHandler {
                if(it.what == TYPE_GAME_FINISH){
                    opponentFinalScore = it.content.toInt()
                    if(myFinalScore != null) {
                        scoreIntent = scoreIntent.putExtra("myScore", myFinalScore!!).putExtra("opponentScore", opponentFinalScore!!);
                        startActivity(scoreIntent)
                        finish()
                    }
                }
            }
            bluetoothService!!.subscribe(gameFinishedHandler)
        }
    }

    override fun onBackPressed() {
        // Do NOTHING
    }

    private inner class GameThread(ctx: Context) : Thread() {

        private val context: Context = ctx
        private var running: Boolean = false

        private val targetFPS = 60

        fun setRunning(isRunning: Boolean) {
            this.running = isRunning
        }

        fun score(time: Double, maxTime: Double, maxScore: Double): Double {
            if(time > maxTime || time < 0) return 0.0

            return (1 - sqrt(1 - ((time - maxTime) / maxTime).pow(2.0))) * maxScore
        }

        override fun run() {
            var startTime: Long
            var timeMillis: Long = 0
            var waitTime: Long
            val targetTime = (1000 / targetFPS).toLong()

            val gameStartTime: Long = System.nanoTime()
            var lastFlowerTime: Long = System.nanoTime()

            var score: Int = 0

            var gameWidth: Int = 0
            var gameHeight: Int = 0

            val taxi: TextView = TextView(context)
            taxi.text = "\uD83E\uDD8B"
            taxi.textSize = 48f
            taxi.alpha = 1f
            taxi.setTextColor(android.graphics.Color.BLACK)

            val flower: TextView = TextView(context)
            flower.text = FLOWERS.random()
            flower.textSize = 48f
            flower.alpha = 1f
            flower.setTextColor(android.graphics.Color.BLACK)

            var taxiWidth: Int = 0
            var taxiHeight: Int = 0
            var gX: Float = 0f
            var gY: Float = 0f

            ll?.doOnLayout { it ->
                gameHeight = it.measuredHeight
                gameWidth = it.measuredWidth
                taxi.doOnLayout {it2 ->
                    taxiWidth = it2.measuredWidth
                    taxiHeight = it2.measuredHeight
                    gX = (gameWidth / 2f) - (taxiWidth / 2f)
                    gY = (gameHeight / 2f) - (taxiHeight / 2f)
                    taxi.x = 0f
                    taxi.y = 0f
                    flower.x = (0..(gameWidth - taxiWidth)).random().toFloat()
                    flower.y = (0..(gameHeight - taxiHeight)).random().toFloat()
                }
            }

            runOnUiThread {
                ll?.addView(
                    taxi,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                ll?.addView(
                    flower,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            while(running) {
                startTime = System.nanoTime()

                runOnUiThread {
                    drivingTimer!!.text = String.format(
                        "%.2f",
                        ((GAME_DURATION - ((startTime - gameStartTime) / 1000000).toFloat()) / 1000f)
                    )
                    drivingScore!!.text = score.toString()
                }

                if(abs(orientationAngles[2]) > 0.1) {
                    gX += orientationAngles[2] * 20
                    taxi.rotationY += orientationAngles[2] * 20
                }
                if(abs(orientationAngles[1]) > 0.1) {
                    gY -= orientationAngles[1] * 20
                    taxi.rotationX += orientationAngles[1] * 20
                }

                if(gX < 0) {
                    gX = 0f
                }

                if(gX > gameWidth - taxiWidth) {
                    gX = (gameWidth - taxiWidth).toFloat()
                }

                if(gY < 0) {
                    gY = 0f
                }

                if(gY > gameHeight - taxiHeight) {
                    gY = (gameHeight - taxiHeight).toFloat()
                }

                taxi.x = gX
                taxi.y = gY
                taxi.z = gY

                if(sqrt((taxi.x - flower.x)*(taxi.x - flower.x) + (taxi.y - flower.y)*(taxi.y - flower.y)) < taxiHeight/2){
                    val moveTime = (System.nanoTime() - lastFlowerTime).toDouble() / 1000000
                    score += score(moveTime, MAX_TIME, MAX_POINTS).toInt()
                    lastFlowerTime = startTime
                    runOnUiThread {
                        flower.text = FLOWERS.random()
                        flower.x = (0..(gameWidth - taxiWidth)).random().toFloat()
                        flower.y = (0..(gameHeight - taxiHeight)).random().toFloat()
                    }
                }

                if((GAME_DURATION - ((startTime - gameStartTime) / 1000000)) < 0){
                    running = false;
                    myFinalScore = score;
                    if(bluetoothService != null){
                        bluetoothService!!.connectThread.write(TYPE_GAME_FINISH, myFinalScore.toString().encodeToByteArray())
                        if(opponentFinalScore != null) {
                            var scoreIntent = Intent(context, Score::class.java)
                            scoreIntent = scoreIntent.putExtra("myScore", myFinalScore!!).putExtra("opponentScore", opponentFinalScore!!);
                            startActivity(scoreIntent)
                            finish()
                        }
                    } else {
                        var scoreIntent = Intent(context, Score::class.java)
                        scoreIntent = scoreIntent.putExtra("myScore", myFinalScore!!);
                        startActivity(scoreIntent)
                        finish()
                    }
                }

                timeMillis = (System.nanoTime() - startTime)
                waitTime = targetTime - (timeMillis / 1000000)

                try {
                    if(waitTime > 0) {
                        sleep(waitTime)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                updateOrientationAngles()
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                updateOrientationAngles()
            }
        }
    }
    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.
    }



    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do something
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}