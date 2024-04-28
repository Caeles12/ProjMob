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
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import com.example.projmob.DeviceInfo
import com.example.projmob.MessageActivity
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_CONNEXION_END
import com.example.projmob.TYPE_GAME_START
import com.example.projmob.bluetoothService


class ChooseMinigame : Activity() {
    var minigames: MutableMap<String, GameInfo> = mutableMapOf()

    private val receiveStartHandler = bluetoothService!!.MyHandler {
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
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_minigame)
        val ll: LinearLayout =  findViewById(R.id.chooseminigamelinearlayout);

        val fishingIntent = Intent(this, Fishing::class.java)
        minigames["fishing"] = GameInfo("Fishing", fishingIntent)
        //val messagesIntent = Intent(this, MessageActivity::class.java)
        //minigames["messages"] = GameInfo("Messages", messagesIntent)
        val dancingIntent = Intent(this, Dance::class.java)
        minigames["dancing"] = GameInfo("Dancing", dancingIntent)
        val targetIntent = Intent(this, Target::class.java)
        minigames["target"] = GameInfo("Ghost Hunt", targetIntent)

        val catIntent = Intent(this, FeedGame::class.java)
        minigames["cat"] = GameInfo("CatFeeding", catIntent)

        if(bluetoothService == null || bluetoothService!!.isServer) {
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
                ll.addView(
                    button,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        }

        if(bluetoothService != null && !bluetoothService!!.isServer) {
            bluetoothService!!.subscribe(receiveStartHandler)
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
        bluetoothService!!.subscribe(receiveStartHandler)
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