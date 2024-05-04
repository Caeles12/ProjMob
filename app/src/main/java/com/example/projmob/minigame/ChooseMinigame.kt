package com.example.projmob.minigame

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_CONNEXION_END
import com.example.projmob.TYPE_GAME_START
import com.example.projmob.TYPE_RESET_POINTS
import com.example.projmob.TYPE_SHOW_SCORES
import com.example.projmob.bluetoothService


class ChooseMinigame : Activity() {
    var minigames: MutableMap<String, GameInfo> = mutableMapOf()
    var gameSerie: List<String> = listOf()
    var gameNumber: Int = 0

    private val receiveStartHandler = bluetoothService?.MyHandler {
        Log.d(TAG, "Received ${it.what} (${it.content})")
        if (it.what == TYPE_GAME_START) {
            if(it.content in minigames.keys){
                startActivity(minigames[it.content]!!.intent)
            }
        }
        if(it.what == TYPE_CONNEXION_END){
            AlertDialog.Builder(this)
                .setTitle("Connexion terminée par l'autre joueur")
                .setPositiveButton("OK", DialogInterface.OnClickListener { _, _ ->
                    finish()
                })
                .show()
        }
        if(it.what == TYPE_RESET_POINTS) {
            bluetoothService!!.myPoints = 0
            bluetoothService!!.opponentPoints = 0
        }
        if(it.what == TYPE_SHOW_SCORES) {
            var scoreIntent = Intent(this, Score::class.java)
            scoreIntent = scoreIntent.putExtra("myScore", bluetoothService!!.myPoints)
                .putExtra("opponentScore", bluetoothService!!.opponentPoints).putExtra("countGlobal", false);
            startActivity(scoreIntent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_minigame)
        val ll: GridLayout =  findViewById(R.id.chooseminigamelinearlayout);
        val title: TextView =  findViewById(R.id.chooseMinigameViewTitle);
        val playGameButton: Button = findViewById(R.id.playGameButton);

        val fishingIntent = Intent(this, Fishing::class.java)
        minigames["fishing"] = GameInfo(resources.getString(R.string.fishing), fishingIntent)
        //val messagesIntent = Intent(this, MessageActivity::class.java)
        //minigames["messages"] = GameInfo("Messages", messagesIntent)
        val dancingIntent = Intent(this, Dance::class.java)
        minigames["dancing"] = GameInfo(resources.getString(R.string.dancing), dancingIntent)
        val targetIntent = Intent(this, Target::class.java)
        minigames["target"] = GameInfo(resources.getString(R.string.target), targetIntent)
        val catIntent = Intent(this, FeedGame::class.java)
        minigames["cat"] = GameInfo(resources.getString(R.string.feed), catIntent)
        val tttIntent = Intent(this, TicTacToe::class.java)
        minigames["TTT"] = GameInfo(resources.getString(R.string.tictactoe), tttIntent)

        val driveIntent = Intent(this, Driving::class.java)
        minigames["drive"] = GameInfo(resources.getString(R.string.driving), driveIntent)

        if(bluetoothService == null || bluetoothService!!.isServer) {
            if(bluetoothService != null) {
                title.text = getString(
                    R.string.a_parentheses_b,
                    resources.getString(R.string.multiplayer),
                    resources.getString(R.string.server)
                );
            } else {
                title.text = resources.getString(R.string.solo)
            }
            minigames.forEach { entry ->
                val button: Button = Button(this)
                button.text = entry.value.name
                button.setOnClickListener(View.OnClickListener {
                    bluetoothService?.connectThread?.write(
                        TYPE_GAME_START,
                        entry.key.toByteArray()
                    )
                    startActivity(entry.value.intent)
                })
                val param = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 0f), GridLayout.spec(GridLayout.UNDEFINED, 1f)
                )
                param.width = 0
                ll.addView(
                    button,
                    param
                )
            }

            playGameButton.setOnClickListener(View.OnClickListener {
                if(bluetoothService != null) {
                    bluetoothService!!.myPoints = 0
                    bluetoothService!!.opponentPoints = 0
                    bluetoothService?.connectThread?.write(
                        TYPE_RESET_POINTS,
                        "".toByteArray()
                    )
                }
                gameSerie = minigames.keys.shuffled().take(3)
                gameNumber = 0
                val game = gameSerie[gameNumber]

                bluetoothService?.connectThread?.write(
                    TYPE_GAME_START,
                    game.toByteArray()
                )
                startActivity(minigames[game]?.intent)
            })
        }

        if(bluetoothService != null && !bluetoothService!!.isServer) {
            title.text = getString(
                R.string.a_parentheses_b,
                resources.getString(R.string.multiplayer),
                resources.getString(R.string.client)
            );
            findViewById<LinearLayout>(R.id.chooseMinigameMainLinearLayout).visibility = ViewGroup.INVISIBLE;
            bluetoothService!!.subscribe(receiveStartHandler!!)
        }
    }

    override fun onBackPressed() {
        if(bluetoothService == null){
            finish()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Voulez vous vraiment quitter?")
                .setMessage("Cela terminera la connexion multijoueur!")
                .setNegativeButton("Non", null)
                .setPositiveButton("Oui", DialogInterface.OnClickListener { _, _ ->
                    bluetoothService!!.connectThread.write(
                        TYPE_CONNEXION_END,
                        "".encodeToByteArray())
                    finish()
                })
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if(bluetoothService != null && !bluetoothService!!.isServer) {
            bluetoothService!!.subscribe(receiveStartHandler!!)
        }
        if(gameSerie.isNotEmpty()){
            gameNumber ++
            if(gameNumber < gameSerie.size) {
                val game = gameSerie[gameNumber]

                bluetoothService?.connectThread?.write(
                    TYPE_GAME_START,
                    game.toByteArray()
                )
                startActivity(minigames[game]?.intent)
            } else {
                gameNumber = 0
                gameSerie = listOf()

                if(bluetoothService != null) {
                    bluetoothService?.connectThread?.write(
                        TYPE_SHOW_SCORES,
                        "".toByteArray()
                    )
                    var scoreIntent = Intent(this, Score::class.java)
                    scoreIntent = scoreIntent.putExtra("myScore", bluetoothService!!.myPoints)
                        .putExtra("opponentScore", bluetoothService!!.opponentPoints).putExtra("countGlobal", false);
                    startActivity(scoreIntent)
                }

            }
        }
    }

    override fun onDestroy() {
        if(isFinishing){
            if(bluetoothService != null){
                bluetoothService!!.connectThread.cancel()
            }
            bluetoothService = null
        }
        super.onDestroy()
    }
}