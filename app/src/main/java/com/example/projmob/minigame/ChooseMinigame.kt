package com.example.projmob.minigame

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.example.projmob.MessageActivity
import com.example.projmob.R
import com.example.projmob.TAG
import com.example.projmob.TYPE_GAME_START
import com.example.projmob.bluetoothService

class ChooseMinigame : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_minigame)

        val fishingIntent = Intent(this, Fishing::class.java)
        val messagesIntent = Intent(this, MessageActivity::class.java)
        val dancingIntent = Intent(this, Dance::class.java)

        val startFishingButton: Button = findViewById(R.id.startfishing)
        val startMessagesButton: Button = findViewById(R.id.startmessaging)
        val startDancingButton: Button = findViewById(R.id.startdancing)

        if(bluetoothService != null) {
            if (bluetoothService!!.isServer) {
                startFishingButton.setOnClickListener(View.OnClickListener {
                    bluetoothService!!.connectThread.write(
                        TYPE_GAME_START,
                        "fishing".encodeToByteArray()
                    )
                    startActivity(fishingIntent)
                })
                startMessagesButton.setOnClickListener(View.OnClickListener {
                    bluetoothService!!.connectThread.write(
                        TYPE_GAME_START,
                        "messages".encodeToByteArray()
                    )
                    startActivity(messagesIntent)
                })
                startDancingButton.setOnClickListener(View.OnClickListener {
                    bluetoothService!!.connectThread.write(
                        TYPE_GAME_START,
                        "dancing".encodeToByteArray()
                    )
                    startActivity(dancingIntent)
                })
            } else {
                startFishingButton.visibility = View.INVISIBLE
                startMessagesButton.visibility = View.INVISIBLE
                val receiveStartHandler = bluetoothService!!.MyHandler {
                    Log.d(TAG, "Received ${it.what} (${it.content})")
                    if (it.what == TYPE_GAME_START) {
                        if (it.content == "fishing") {
                            startActivity(fishingIntent)
                        }
                        if (it.content == "messages") {
                            startActivity(messagesIntent)
                        }
                        if (it.content == "dancing") {
                            startActivity(dancingIntent)
                        }
                    }
                }
                bluetoothService!!.subscribe(receiveStartHandler)
            }
        } else {
            startMessagesButton.visibility = View.INVISIBLE
            startFishingButton.setOnClickListener(View.OnClickListener {
                startActivity(fishingIntent)
            })
            startDancingButton.setOnClickListener {
                startActivity(dancingIntent)
            }
        }
    }
}