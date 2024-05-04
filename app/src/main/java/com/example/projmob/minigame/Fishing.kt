package com.example.projmob.minigame

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_GAME_FINISH
import com.example.projmob.bluetoothService
import kotlin.math.pow
import kotlin.math.sqrt


class Fishing : Activity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var mAccelerometer: Sensor? = null
    private var mGravity: Sensor? = null
    private var gravity = FloatArray(3) { 0f }
    private var acceleration = FloatArray(3) { 0f }
    private var rotMatrix = FloatArray(9) { 0f }

    private val CHEAT_PENALITY: Int = 50
    private val MAX_FISH_TIME: Double = 4000.0
    private val MAX_FISH_SCORE: Double = 100.0
    private val MIN_FISH_SPAWN_DELAY: Int = 2000
    private val MAX_FISH_SPAWN_DELAY: Int = 5000
    private val BOP_DELAY = 1000
    private val GAME_DURATION = 30000

    private var fishingScoreTextView: TextView? = null
    private var fishingMessage: TextView? = null
    private var fishingTimer: TextView? = null


    private var canFish = true

    private val game: GameThread = GameThread(this)

    private var myFinalScore: Int? = null
    private var opponentFinalScore: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fishinggame)

        fishingScoreTextView = findViewById(R.id.fishinggamescore)
        fishingMessage = findViewById(R.id.fishinggamemessage)
        fishingTimer = findViewById(R.id.fishinggametimer)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        mGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.fishing))
            .setMessage(resources.getString(R.string.fishing_instructions))
            .setPositiveButton(resources.getString(R.string.letsgo), DialogInterface.OnClickListener { _, _ ->
                game.setRunning(true)
                game.start()
            })
            .show()
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

    fun score(time: Double, maxTime: Double, maxScore: Double): Double {
        if(time > maxTime || time < 0) return 0.0

        return (1 - sqrt(1 - ((time - maxTime) / maxTime).pow(2.0))) * maxScore
    }

    private inner class GameThread(ctx: Context) : Thread() {

        private val context: Context = ctx
        private var running: Boolean = false
        private var action: Boolean = false
        private var score: Int = 0

        private val targetFPS = 60

        fun setRunning(isRunning: Boolean) {
            this.running = isRunning
        }

        fun action() {
            this.action = true;
        }

        fun vibrate(t: Long) {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings: LongArray = longArrayOf(t, 350, 25, 25)
                val amplitudes: IntArray = intArrayOf(255, 0, 0, 0)

                val repeatIndex = -1 // Do not repeat.

                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, repeatIndex))
            } else {
                //deprecated in API 26
                @Suppress("DEPRECATION")
                vib.vibrate(t);
            }
        }

        override fun run() {
            var startTime: Long
            var timeMillis: Long = 0
            var waitTime: Long
            val targetTime = (1000 / targetFPS).toLong()

            var fishSpawned: Boolean = false
            var gameStartTime: Long = System.nanoTime()
            var lastFishTime: Long = System.nanoTime()
            var lastBopTime: Long = System.nanoTime()

            var nextDelay: Long = (MIN_FISH_SPAWN_DELAY..MAX_FISH_SPAWN_DELAY).random().toLong()
            var bopCount: Int = (0..5).random()

            runOnUiThread {
                fishingScoreTextView!!.text = score.toString()
                fishingMessage!!.text = ""
                fishingTimer!!.text = GAME_DURATION.toString()
            }

            while(running) {
                startTime = System.nanoTime()
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
                        } else {
                            runOnUiThread {
                                AlertDialog.Builder(context)
                                    .setMessage(resources.getString(R.string.waiting))
                                    .show()
                            }
                        }
                    } else {
                        var scoreIntent = Intent(context, Score::class.java)
                        scoreIntent = scoreIntent.putExtra("myScore", myFinalScore!!);
                        startActivity(scoreIntent)
                        finish()
                    }

                }
                runOnUiThread {
                    fishingTimer!!.text = ((GAME_DURATION - ((startTime - gameStartTime) / 1000000)) / 1000).toInt().toString()
                }
                if(action) {
                    if(fishSpawned) {
                        val fishingTime = (startTime - lastFishTime).toDouble() / 1000000
                        val fishScore = score(fishingTime, MAX_FISH_TIME, MAX_FISH_SCORE).toInt()
                        score += fishScore
                        runOnUiThread {
                            fishingScoreTextView!!.text = score.toString()
                            fishingMessage!!.text = ""
                        }
                        fishSpawned = false
                        action = false
                        lastFishTime = System.nanoTime()
                        runOnUiThread {
                            if(fishScore > 50){
                                Toast.makeText(context, "Incroyable!", Toast.LENGTH_SHORT).show()
                            }
                            else if(fishScore > 30){
                                Toast.makeText(context, "Belle prise!", Toast.LENGTH_SHORT).show()
                            }
                            else if(fishScore > 10){
                                Toast.makeText(context, "Pas mal!", Toast.LENGTH_SHORT).show()
                            }
                            else if(fishScore > 0){
                                Toast.makeText(context, "Un peu lent...", Toast.LENGTH_SHORT).show()
                            }
                            else {
                                Toast.makeText(context, "Il s'est enfui...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        score -= CHEAT_PENALITY
                        score = maxOf(0, score)
                        fishingScoreTextView!!.text = score.toString()
                        action = false
                        runOnUiThread {
                            Toast.makeText(context, "Ça mord pas...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if(fishSpawned && startTime - lastFishTime > BOP_DELAY * 1000000) {
                    runOnUiThread {
                        fishingMessage!!.text = "Ça mords!"
                    }
                }

                if(startTime - lastFishTime >= (nextDelay * 1000000)
                    && !fishSpawned
                    && (startTime - lastBopTime >= BOP_DELAY * 1000000 || bopCount == 0)) {
                    bopCount -= 1
                    if(bopCount <= 0) {
                        action = false
                        vibrate(500)
                        lastFishTime = System.nanoTime()
                        fishSpawned = true
                        nextDelay = (MIN_FISH_SPAWN_DELAY..MAX_FISH_SPAWN_DELAY).random().toLong()
                        bopCount = (0..5).random()
                    }else{
                        lastBopTime = startTime
                        vibrate(200)
                    }
                }

                timeMillis = (System.nanoTime() - startTime)
                waitTime = targetTime - (timeMillis / 1000000)

                try {
                    sleep(waitTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun vectorMag(v: FloatArray): Float {
        return sqrt( v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    }

    private fun vectorNorm(v: FloatArray): FloatArray {
        val mag = vectorMag(v)
        return floatArrayOf(v[0] / mag, v[1] / mag, v[2] / mag)
    }

    private fun vectorCross(v1: FloatArray, v2:FloatArray): FloatArray {
        val a1 = v1[0]
        val a2 = v1[1]
        val a3 = v1[2]

        val b1 = v2[0]
        val b2 = v2[1]
        val b3 = v2[2]

        return floatArrayOf(a2 * b3 - a3 * b2, a3 * b1 - a1 * b3, a1 * b2 - a2 * b1)
    }

    private fun makeRotationDir(dir: FloatArray, up: FloatArray): FloatArray {
        val xAxis = vectorNorm(vectorCross(up, dir))
        val yAxis = vectorNorm(vectorCross(dir, xAxis))

        val rotMat = FloatArray(9)

        rotMat[0] = xAxis[0]
        rotMat[1] = yAxis[0]
        rotMat[2] = dir[0]

        rotMat[3] = xAxis[1]
        rotMat[4] = yAxis[1]
        rotMat[5] = dir[1]

        rotMat[6] = xAxis[2]
        rotMat[7] = yAxis[2]
        rotMat[8] = dir[2]

        return rotMat
    }

    private fun rotVector(rotMat: FloatArray, v: FloatArray): FloatArray {
        return floatArrayOf(
            v[0] * rotMat[0] + v[1] * rotMat[3] + v[2] * rotMat[6],
            v[0] * rotMat[1] + v[1] * rotMat[4] + v[2] * rotMat[7],
            v[0] * rotMat[2] + v[1] * rotMat[5] + v[2] * rotMat[8]
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_GRAVITY) {

                val mag = vectorMag(event.values)
                gravity[0] = event.values[0] / mag
                gravity[1] = event.values[1] / mag
                gravity[2] = event.values[2] / mag
                rotMatrix = makeRotationDir(gravity, floatArrayOf(0f, 0f, 1f))
            }
            if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
                acceleration[0] = event.values[0]
                acceleration[1] = event.values[1]
                acceleration[2] = event.values[2]
            }
            val rotated = rotVector(rotMatrix, acceleration)
            if(rotated[2] < -15 && canFish) {
                Log.d(TAG, rotated[2].toString())
                game.action()
                canFish = false
            }else if(rotated[2] > -2) {
                canFish = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
       // Do something
    }

    override fun onResume() {
        super.onResume()
        mAccelerometer?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        mGravity?.also { gravity ->
            sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onStop() {
        super.onStop()
        game.setRunning(false)
    }
}