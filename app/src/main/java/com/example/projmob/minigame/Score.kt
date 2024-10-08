package com.example.projmob.minigame

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.projmob.R
import com.example.projmob.TYPE_BASIC_ACTION
import android.media.MediaPlayer
import com.example.projmob.TYPE_GAME_START
import com.example.projmob.TYPE_SHOW_SCORES
import com.example.projmob.bluetoothService

class Score : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scorescreen)

        val winSound = MediaPlayer.create(this, R.raw.win);
        winSound.isLooping = false

        val looseSound = MediaPlayer.create(this, R.raw.fail);
        looseSound.isLooping = false

        val myScore: Int = intent.getIntExtra("myScore", -1);
        val countGlobalScore: Boolean = intent.getBooleanExtra("countGlobal", true);

        val finalScoreResultMessage: TextView = findViewById(R.id.scoreScreenResultMessage)
        val firstPlaceText: TextView = findViewById(R.id.scoreScreenFirstPlace)
        val secondPlaceText: TextView = findViewById(R.id.scoreScreenSecondPlace)
        val continueButton: Button = findViewById(R.id.scoreScreenContinueButton)
        var iAmOk: Boolean = false
        if(bluetoothService != null) {
            val opponentScore: Int = intent.getIntExtra("opponentScore", -1);
            var opponentIsOk: Boolean = false

            if (myScore > opponentScore) {
                finalScoreResultMessage.text = "Victoire!"
                firstPlaceText.text = "Vous: $myScore"
                secondPlaceText.text = "Adversaire: $opponentScore"
                winSound.start()
                if(countGlobalScore) bluetoothService!!.myPoints += 1
            } else if (myScore < opponentScore) {
                finalScoreResultMessage.text = "Défaite..."
                secondPlaceText.text = "Vous: $myScore"
                firstPlaceText.text = "Adversaire: $opponentScore"
                looseSound.start()
                if(countGlobalScore) bluetoothService!!.opponentPoints += 1
            } else {
                finalScoreResultMessage.text = "Égalité!"
                firstPlaceText.text = "Vous: $myScore"
                secondPlaceText.text = "Adversaire: $opponentScore"
                winSound.start()
                if(countGlobalScore) {
                    bluetoothService!!.myPoints += 1
                    bluetoothService!!.opponentPoints += 1
                }
            }

            val gameFinishedHandler = bluetoothService!!.MyHandler {
                if (it.what == TYPE_BASIC_ACTION) {
                    opponentIsOk = true
                    if (iAmOk) {
                        finish()
                    }
                }
                if (it.what == TYPE_GAME_START) {
                    for(k in minigames.keys) {
                        if (it.content.contains(k)) {
                            startActivity(minigames[k]!!.intent)
                            finish()
                        }
                    }
                }
                if(it.what == TYPE_SHOW_SCORES) {
                    var scoreIntent = Intent(this, Score::class.java)
                    scoreIntent = scoreIntent.putExtra("myScore", bluetoothService!!.myPoints)
                        .putExtra("opponentScore", bluetoothService!!.opponentPoints).putExtra("countGlobal", false);
                    startActivity(scoreIntent)
                }
            }
            bluetoothService!!.subscribe(gameFinishedHandler)

            continueButton.setOnClickListener(View.OnClickListener {
                iAmOk = true

                continueButton.isEnabled = false
                continueButton.isClickable = false
                continueButton.text = "En attente de l'adversaire..."

                bluetoothService!!.connectThread.write(
                    TYPE_BASIC_ACTION,
                    "Continue".encodeToByteArray()
                )
                if (opponentIsOk) {
                    finish()
                }
            })
        }else {
            if (intent.hasExtra("opponentScore")) {
                val opponentScore: Int = intent.getIntExtra("opponentScore", -1)
                if (myScore > opponentScore) {
                    finalScoreResultMessage.text = "Victoire!"
                    firstPlaceText.text = "Vous: $myScore"
                    secondPlaceText.text = "Ordinateur: $opponentScore"
                    winSound.start()
                } else if (myScore < opponentScore) {
                    finalScoreResultMessage.text = "Défaite..."
                    secondPlaceText.text = "Vous: $myScore"
                    firstPlaceText.text = "Ordinateur: $opponentScore"
                    looseSound.start()
                } else {
                    finalScoreResultMessage.text = "Égalité!"
                    firstPlaceText.text = "Vous: $myScore"
                    secondPlaceText.text = "Ordinateur: $opponentScore"
                    winSound.start()
                }
                continueButton.setOnClickListener(View.OnClickListener {
                    finish()
                })
            } else {
                finalScoreResultMessage.text = "Terminé!"
                firstPlaceText.text = "Score final: $myScore"

                continueButton.setOnClickListener(View.OnClickListener {
                    finish()
                })
                winSound.start()
            }

        }
    }
}
