package com.example.projmob.minigame

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.widget.TextView
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_CONNEXION_END
import com.example.projmob.TYPE_GAME_FINISH
import com.example.projmob.bluetoothService
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

class Dance: Activity() {

    private val GAME_DURATION = 20000

    private val CHEAT_PENALITY: Int = 2
    private val MAX_TIME: Double = 1000.0
    private val MAX_POINTS: Double = 100.0

    private var danceScoreTextView: TextView? = null
    private var danceMessage: TextView? = null
    private var danceTimer: TextView? = null

    private var myFinalScore: Int? = null
    private var opponentFinalScore: Int? = null

    private var mVelocityTracker: VelocityTracker? = null

    private val danceIcons: Array<String> = arrayOf("⬆\uFE0F", "↗\uFE0F", "➡\uFE0F", "↘\uFE0F", "⬇\uFE0F", "↙\uFE0F", "⬅\uFE0F", "↖\uFE0F")
    private val danceDirections: Array<Pair<Float, Float>> = arrayOf(Pair(0f, -1f), Pair(1f, -1f), Pair(1f, 0f), Pair(1f, 1f), Pair(0f, 1f), Pair(-1f, 1f), Pair(-1f, 0f), Pair(-1f, -1f))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dance)

        danceScoreTextView = findViewById(R.id.dancegamescore)
        danceMessage = findViewById(R.id.dancegamemessage)
        danceTimer = findViewById(R.id.dancegametimer)

        danceMessage!!.text = "\uD83D\uDD7A"

        val gameThread: GameThread = GameThread(this)
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.dancing))
            .setMessage(resources.getString(R.string.dancing_instructions))
            .setPositiveButton(resources.getString(R.string.letsgo), DialogInterface.OnClickListener { _, _ ->
                gameThread.setRunning(true)
                gameThread.start()
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

    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Reset the velocity tracker back to its initial state.
                mVelocityTracker?.clear()
                // If necessary, retrieve a new VelocityTracker object to watch
                // the velocity of a motion.
                mVelocityTracker = mVelocityTracker ?: VelocityTracker.obtain()
                // Add a user's movement to the tracker.
                mVelocityTracker?.addMovement(event)
            }
            MotionEvent.ACTION_MOVE -> {
                mVelocityTracker?.apply {
                    val pointerId: Int = event.getPointerId(event.actionIndex)
                    addMovement(event)
                    computeCurrentVelocity(1000)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Return a VelocityTracker object back to be re-used by others.
                mVelocityTracker?.recycle()
                mVelocityTracker = null
            }
        }
        return true
    }

    override fun onBackPressed() {
        // Do NOTHING
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

        fun dot(ax: Float, ay: Float, bx: Float, by: Float): Float{
            return (ax * bx) + (ay * by)
        }

        fun getVelocityMagnitude(x: Float?, y: Float?): Float {
            return if(x == null || y == null) {
                0f
            } else {
                sqrt(x.pow(2) + y.pow(2) )
            }
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
            var direction = (danceIcons.indices).random()
            var hasMoved = false;

            var lastMoveTime: Long = System.nanoTime()

            while(running) {
                startTime = System.nanoTime()
                danceMessage!!.text = danceIcons[direction]
                danceTimer!!.text = String.format(
                    "%.2f",
                    max(0f, ((GAME_DURATION - ((startTime - gameStartTime) / 1000000).toFloat()) / 1000f))
                )


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
                val velX = mVelocityTracker?.xVelocity
                val velY = mVelocityTracker?.yVelocity
                val mag = getVelocityMagnitude(velX, velY)
                val dirMag = getVelocityMagnitude(danceDirections[direction].first, danceDirections[direction].second)
                if(mag > 1000 && velX != null && velY != null && !hasMoved) {
                    val dp = dot(velX/mag, velY/mag, danceDirections[direction].first/dirMag, danceDirections[direction].second/dirMag)
                    if(dp >= 0.95){
                        val moveTime = (startTime - lastMoveTime).toDouble() / 1000000
                        val moveScore = score(moveTime, MAX_TIME, MAX_POINTS).toInt()
                        score += moveScore
                        danceScoreTextView!!.text = score.toString()
                        val lastDir = direction
                        while(lastDir == direction){
                            direction = (danceIcons.indices).random()
                        }
                        lastMoveTime = System.nanoTime()
                        Log.d(TAG, "Nice! $dp")
                        hasMoved = true
                    } else if(dp <= -0.5){
                        score -= CHEAT_PENALITY
                        score = maxOf(0, score)
                        danceScoreTextView!!.text = score.toString()
                        Log.d(TAG, "Uh oh... $dp")
                        hasMoved = true
                    }
                }
                if(mag == 0f){
                    hasMoved = false
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
}