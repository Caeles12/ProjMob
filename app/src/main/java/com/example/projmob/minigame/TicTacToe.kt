package com.example.projmob.minigame

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.example.projmob.R


class TicTacToe : Activity() {
    private lateinit var buttons: Array<Array<Button?>>
    private var playerTurn = true // true for Player 1 (X), false for Computer (O)
    private var moves = 0
    private var gameOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tictactoegame)
        initializeButtons()
    }

    // Initialize the buttons grid
    private fun initializeButtons() {
        buttons = Array(3) { arrayOfNulls<Button>(3) }

        for (i in 0 until 3) {
            for (j in 0 until 3) {
                val buttonId = "button_$i$j"
                val resourceId = resources.getIdentifier(buttonId, "id", packageName)
                buttons[i][j] = findViewById(resourceId)
                buttons[i][j]?.setOnClickListener(ButtonClickListener(i, j))
            }
        }
    }

    // Listener for button clicks
    inner class ButtonClickListener(private val row: Int, private val col: Int) :
        View.OnClickListener {
        override fun onClick(view: View) {
            if (!gameOver && buttons[row][col]?.text.isNullOrEmpty()) {
                // Player's turn
                buttons[row][col]?.text = "X"
                playerTurn = false
                moves++
                checkWinner()
                if (!gameOver) {
                    // Computer's turn
                    computerMove()
                    moves++
                    checkWinner()
                }
            }
        }
    }

    // Computer makes a random move
    private fun computerMove() {
        var row = (0..2).random()
        var col = (0..2).random()
        while (!buttons[row][col]?.text.isNullOrEmpty()) {
            row = (0..2).random()
            col = (0..2).random()
        }
        buttons[row][col]?.text = "O"
        playerTurn = true
    }

    private fun checkMagicSquare(player: String): Boolean {
        val board = Array(3) { IntArray(3) }

        for (i in 0 until 3) {
            for (j in 0 until 3) {
                board[i][j] = if (buttons[i][j]?.text == player) {
                    1
                } else {
                    0
                }
            }
        }
        for (i in 0 until 3) {
            if (board[i].sum() == 3 || board.map { it[i] }.sum() == 3) {
                return true
            }
        }
        if (board[0][0] + board[1][1] + board[2][2] == 3 || board[0][2] + board[1][1] + board[2][0] == 3) {
            return true
        }
        return false
    }

    private fun checkWinner() {
        if (checkMagicSquare("X")) {
            startActivity(Intent(this, Score::class.java).apply {
                putExtra("myScore", 1)
                putExtra("opponentScore", 0)
            })
            finish()
            gameOver = true
            return
        } else if (checkMagicSquare("O")) {
            startActivity(Intent(this, Score::class.java).apply {
                putExtra("myScore", 0)
                putExtra("opponentScore", 1)
            })
            finish()
            gameOver = true
            return
        }
        if (moves == 9) {
            startActivity(Intent(this, Score::class.java).apply {
                putExtra("myScore", 0)
                putExtra("opponentScore", 0)
            })
            finish()
            gameOver = true
            return
        }
    }

}

