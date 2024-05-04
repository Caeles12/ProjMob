package com.example.projmob.minigame

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.projmob.R
import com.example.projmob.TYPE_GAME_FINISH
import com.example.projmob.bluetoothService
import kotlin.random.Random

class FeedGame : Activity() {
    private lateinit var scoreTextView: TextView
    private lateinit var catEmojiImageView: TextView
    private lateinit var emojiButton1: Button
    private lateinit var emojiButton2: Button

    private var score: Int = 0

    private var myFinalScore: Int? = null
    private var opponentFinalScore: Int? = null

    private val nbQuestion: Int = 6
    private var count: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_game)

        scoreTextView = findViewById(R.id.scoreTextView)
        catEmojiImageView = findViewById(R.id.catEmojiImageView)
        emojiButton1 = findViewById(R.id.emojiButton1)
        emojiButton2 = findViewById(R.id.emojiButton2)

        // Générer deux réponses aléatoires pour les boutons d'emoji
        val rightEmoji = getRandomRightEmoji()
        var wrongEmoji: String

        // Choose a wrong emoji different from the correct one
        do {
            wrongEmoji = getRandomWrongEmoji()
        } while (rightEmoji == wrongEmoji)

        val correctAnswer = Random.nextInt(2)

        if (correctAnswer == 0) {
            emojiButton1.text = rightEmoji
            emojiButton2.text = wrongEmoji
            emojiButton1.setOnClickListener { onCorrectEmojiClicked() }
            emojiButton2.setOnClickListener { onWrongEmojiClicked() }
        } else {
            emojiButton2.text = rightEmoji
            emojiButton1.text = wrongEmoji
            emojiButton2.setOnClickListener { onCorrectEmojiClicked() }
            emojiButton1.setOnClickListener { onWrongEmojiClicked() }
        }

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

    private fun showHappyCatEmoji() {
        catEmojiImageView.text = "😸"
    }

    private fun showSadCatEmoji() {
        catEmojiImageView.text = "😾"
    }

    private fun updateCatSize() {
        val initialSize = 1.0f

        // Facteur d'agrandissement de l'image en fonction du score
        val scaleFactor = 1.0f + (score * 0.1f)

        catEmojiImageView.scaleX = initialSize * scaleFactor
        catEmojiImageView.scaleY = initialSize * scaleFactor
    }

    private fun getRandomRightEmoji(): String {
        val emojis = arrayOf("🍗", "🥓", "🥨", "🐟", "🍤", "🍣", "🧀", "🌭", "🥐")
        return emojis.random()
    }

    private fun getRandomWrongEmoji(): String {
        val emojis = arrayOf("🍇", "🍎", "🍍", "🍌", "🧄", "🥥", "🥑", "🥕", "🍫")
        return emojis.random()
    }

    private fun onCorrectEmojiClicked() {
        score++
        updateScore()
        showHappyCatEmoji()
        count++
        if (count < nbQuestion) {
            displayNewChoices()
        } else {
            finishGame()
        }
    }

    private fun onWrongEmojiClicked() {
        score--
        updateScore()
        showSadCatEmoji()
        count++
        if (count < nbQuestion) {
            displayNewChoices()
        } else {
            finishGame()
        }
    }

    private fun finishGame() {
        myFinalScore = score;
        emojiButton1.visibility = View.INVISIBLE
        emojiButton2.visibility = View.INVISIBLE
        if(bluetoothService != null){
            bluetoothService!!.connectThread.write(TYPE_GAME_FINISH, myFinalScore.toString().encodeToByteArray())
            if(opponentFinalScore != null) {
                var scoreIntent = Intent(this, Score::class.java)
                scoreIntent = scoreIntent.putExtra("myScore", myFinalScore!!).putExtra("opponentScore", opponentFinalScore!!);
                startActivity(scoreIntent)
                finish()
            }
        } else {
            var scoreIntent = Intent(this, Score::class.java)
            scoreIntent = scoreIntent.putExtra("myScore", myFinalScore!!);
            startActivity(scoreIntent)
            finish()
        }
    }

    private fun displayNewChoices() {
        // Générer une nouvelle réponse aléatoire (0 ou 1)
        val correctAnswer = Random.nextInt(2)

        // Générer deux réponses aléatoires pour les boutons d'emoji
        val rightEmoji = getRandomRightEmoji()
        var wrongEmoji: String

        // Choose a wrong emoji different from the correct one
        do {
            wrongEmoji = getRandomWrongEmoji()
        } while (rightEmoji == wrongEmoji)

        // Afficher l'image appropriée du chat en fonction de la réponse correcte
        if (correctAnswer == 0) {
            emojiButton1.text = rightEmoji
            emojiButton2.text = wrongEmoji
            emojiButton1.setOnClickListener { onCorrectEmojiClicked() }
            emojiButton2.setOnClickListener { onWrongEmojiClicked() }
        } else {
            emojiButton2.text = rightEmoji
            emojiButton1.text = wrongEmoji
            emojiButton2.setOnClickListener { onCorrectEmojiClicked() }
            emojiButton1.setOnClickListener { onWrongEmojiClicked() }
        }
    }

    private fun updateScore() {
        scoreTextView.text = "Score: $score"
        updateCatSize()
    }
}