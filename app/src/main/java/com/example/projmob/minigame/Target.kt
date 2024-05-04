package com.example.projmob.minigame

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import com.example.projmob.R
import com.example.projmob.TYPE_GAME_FINISH
import com.example.projmob.bluetoothService
import java.util.Random
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class Target : Activity() {
    private var ll: LinearLayout? = null

    private var targetScore: TextView? = null
    private var targetTimer: TextView? = null
    private var score: Int = 0

    private val GAME_DURATION: Int = 20000
    private val PENALITY: Int = 10
    private val MAX_TIME: Double = 1000.0
    private val MAX_POINTS: Double = 100.0

    private var myFinalScore: Int? = null
    private var opponentFinalScore: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target)
        ll =  findViewById(R.id.targetminigamell);
        targetScore = findViewById(R.id.targetgamescore)
        targetTimer = findViewById(R.id.targetgametimer)

        val gameThread: GameThread = GameThread(this)
        AlertDialog.Builder(this)
            .setTitle(resources.getString(R.string.target))
            .setMessage(resources.getString(R.string.target_instructions))
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

            var lastMoveTime: Long = System.nanoTime()
            val gameStartTime: Long = System.nanoTime()

            val ghost: TextView = TextView(context)
            var gWidth: Int = 0
            var gHeight: Int = 0
            var gX: Float = 0f
            var gY: Float = 0f

            val r = Random()
            ghost.text = "\uD83D\uDC7B"
            ghost.textSize = 48f
            ghost.alpha = 1f
            ghost.setTextColor(Color.BLACK)
            ghost.setShadowLayer(10f, 0f, 0f, Color.BLACK)
            ghost.doOnLayout {
                gWidth = it.measuredWidth
                gHeight = it.measuredHeight
            }

            ll?.doOnLayout {
                gX = gWidth + (r.nextFloat() * (it.measuredWidth - (gWidth * 2)))
                gY = gHeight + (r.nextFloat() * (it.measuredHeight - (gHeight * 2)))
                ghost.x = gX
                ghost.y = gY
            }
            ghost.setOnClickListener(View.OnClickListener {
                if(ghost.alpha <= 0f){
                    score = maxOf(0, score - PENALITY)
                } else {
                    val moveTime = (System.nanoTime() - lastMoveTime).toDouble() / 1000000
                    score += score(moveTime, MAX_TIME, MAX_POINTS).toInt()
                }
                ll?.doOnLayout {
                    gX = gWidth + (r.nextFloat() * (it.measuredWidth - (gWidth * 2)))
                    gY = gHeight + (r.nextFloat() * (it.measuredHeight - (gHeight * 2)))
                    ghost.x = gX
                    ghost.y = gY
                    ghost.alpha = 1.0f
                }
                lastMoveTime = System.nanoTime()
            })
            runOnUiThread {
                ll?.addView(ghost)
            }

            while(running) {
                startTime = System.nanoTime()
                runOnUiThread {
                    targetTimer!!.text = String.format(
                        "%.2f",
                        ((GAME_DURATION - ((startTime - gameStartTime) / 1000000).toFloat()) / 1000f)
                    )
                    targetScore!!.text = score.toString()
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

                ghost.alpha = maxOf(0f, ghost.alpha - 0.02f)
                if(ghost.alpha <= 0f) {
                    ghost.callOnClick()
                }

                ghost.rotation = sin(startTime.toDouble() / (1000000.0 * 100)).toFloat() * 45
                ghost.y = gY + sin(startTime.toDouble() / (1000000.0 * 1000)).toFloat() * 100

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