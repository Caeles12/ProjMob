package com.example.projmob.minigame

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_GAME_MESSAGE
import com.example.projmob.bluetoothService


class TicTacToe : Activity() {
    private lateinit var buttons: Array<Array<Button?>>
    private var playerTurn = true // true for Player 1 (X), false for Computer (O)
    private var moves = 0
    private var gameOver = false
    private var playerIsX = true

    private val receiveStartHandler = bluetoothService?.MyHandler {
        if (it.what == TYPE_GAME_MESSAGE) {
            val messageContent = it.content.toString()
            if (messageContent.startsWith("MOVE:")) {
                // Extract row and column from the message content
                val coordinates = messageContent.substringAfter("MOVE:").split(",")
                val row = coordinates[0].toInt()
                val col = coordinates[1].toInt()

                // Call receivedMove function to handle the received move
                receivedMove(row, col)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tictactoegame)
        if(bluetoothService!= null) {
            bluetoothService!!.subscribe(receiveStartHandler!!)
        }

        if(bluetoothService != null && !bluetoothService!!.isServer){
            playerTurn = false
            playerIsX = false
        }
        initializeButtons()
        enableButtons(playerTurn)
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

    private fun sendMove(row: Int, col: Int) {
        val moveMsg = "MOVE:$row,$col"
        bluetoothService?.connectThread?.write(TYPE_GAME_MESSAGE, moveMsg.toByteArray())
    }

    // Handle received move from opponent
    private fun receivedMove(row: Int, col: Int) {
        runOnUiThread {
            if (!gameOver && buttons[row][col]?.text.isNullOrEmpty()) {
                val symbol = if (playerIsX) "O" else "X"
                buttons[row][col]?.text = symbol
                moves++
                checkWinner()
                playerTurn = !playerTurn
                enableButtons(playerTurn)
            }
        }
    }

    // Enable or disable buttons based on player's turn
    private fun enableButtons(enable: Boolean) {
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                buttons[i][j]?.isEnabled = enable && buttons[i][j]?.text.isNullOrEmpty()
            }
        }
    }

    // Listener for button clicks
    inner class ButtonClickListener(private val row: Int, private val col: Int) :
        View.OnClickListener {
        override fun onClick(view: View) {
            if (!gameOver && buttons[row][col]?.text.isNullOrEmpty()) {
                // Player's turn
                buttons[row][col]?.text = if(playerIsX) { "X" } else { "O" }
                playerTurn = false
                sendMove(row, col)
                moves++
                checkWinner()
                enableButtons(false)
                if (!gameOver) {
                    if (bluetoothService == null) {
                        computerMove()
                        moves++
                        checkWinner()
                    }
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
        buttons[row][col]?.text = if(playerIsX) { "O" } else { "X" }
        playerTurn = true
        enableButtons(true)
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
        if (checkMagicSquare(if(playerIsX) { "X" } else { "O" })) {
            startActivity(Intent(this, Score::class.java).apply {
                putExtra("myScore", 1)
                putExtra("opponentScore", 0)
            })
            finish()
            gameOver = true
            return
        } else if (checkMagicSquare(if(playerIsX) { "O" } else { "X" })) {
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

