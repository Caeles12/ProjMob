package com.example.projmob.minigame

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.projmob.R
import kotlin.random.Random

class FeedGame : Activity() {
    private lateinit var scoreTextView: TextView
    private lateinit var catEmojiImageView: TextView
    private lateinit var emojiButton1: Button
    private lateinit var emojiButton2: Button

    private var score: Int = 0

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
        Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        updateScore()
        showHappyCatEmoji()
        displayNewChoices()
    }

    private fun onWrongEmojiClicked() {
        score--
        Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show()
        updateScore()
        showSadCatEmoji()
        displayNewChoices()
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