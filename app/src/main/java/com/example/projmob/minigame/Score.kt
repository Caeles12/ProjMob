package com.example.projmob.minigame

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_BASIC_ACTION
import com.example.projmob.TYPE_GAME_FINISH
import com.example.projmob.bluetoothService

class Score : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scorescreen)
        val myScore: Int = intent.getIntExtra("myScore", -1);
        val opponentScore: Int = intent.getIntExtra("opponentScore", -1);

        val finalScoreResultMessage: TextView = findViewById(R.id.scoreScreenResultMessage)
        val firstPlaceText: TextView = findViewById(R.id.scoreScreenFirstPlace)
        val secondPlaceText: TextView = findViewById(R.id.scoreScreenSecondPlace)
        val continueButton: Button = findViewById(R.id.scoreScreenContinueButton)
        var iAmOk: Boolean = false
        var opponentIsOk: Boolean = false

        if(myScore > opponentScore) {
            finalScoreResultMessage.text = "Victoire!"
            firstPlaceText.text = "Vous: $myScore"
            secondPlaceText.text = "Adversaire: $opponentScore"
        } else if(myScore < opponentScore) {
            finalScoreResultMessage.text = "Défaite..."
            secondPlaceText.text = "Vous: $myScore"
            firstPlaceText.text = "Adversaire: $opponentScore"
        } else {
            finalScoreResultMessage.text = "Égalité!"
            firstPlaceText.text = "Vous: $myScore"
            secondPlaceText.text = "Adversaire: $opponentScore"
        }

        val gameFinishedHandler = bluetoothService!!.MyHandler {
            if(it.what == TYPE_BASIC_ACTION){
                opponentIsOk = true
                if(iAmOk) {
                    val messageActivityIntent = Intent(this, ChooseMinigame::class.java)
                    startActivity(messageActivityIntent)
                }
            }
        }
        bluetoothService!!.subscribe(gameFinishedHandler)

        continueButton.setOnClickListener(View.OnClickListener {
            iAmOk = true
            bluetoothService!!.connectThread.write(TYPE_BASIC_ACTION, "Continue".encodeToByteArray())
            if(opponentIsOk) {
                val messageActivityIntent = Intent(this, ChooseMinigame::class.java)
                startActivity(messageActivityIntent)
            }
        })
    }
}
