package com.example.projmob


import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView

class MessageActivity : Activity() {
    private var connectThread: MyBluetoothService.ConnectedThread? = null
    private var messages: ArrayList<String> = ArrayList<String>()

    fun updateListView(context: Context) {
        val mListView = findViewById<ListView>(R.id.messageList)
        val arrayAdapter = ArrayAdapter(context, R.layout.simplelistitem, messages)
        mListView.adapter = arrayAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.messagelayout)

        val mainHandlerCustom = bluetoothService!!.MyHandler {
            if(it.what == TYPE_GAME_MESSAGE){
                Log.d(TAG, "Read : ${it.content}")
                messages.add("Reçu:  ${it.content}")
                updateListView(this)
            }
        }
        bluetoothService!!.subscribe(mainHandlerCustom)


        val sendMessageButton: Button = findViewById(R.id.sendMessageButton)
        val editMessageContent: EditText = findViewById(R.id.messageContent)

        sendMessageButton.setOnClickListener(View.OnClickListener {
            val messageText = editMessageContent.text.toString()
            editMessageContent.setText("")
            Log.d(TAG, messageText)
            messages.add("Envoyé:  $messageText")
            updateListView(this)
            bluetoothService!!.connectThread.write(TYPE_GAME_MESSAGE, messageText.encodeToByteArray())
        })
    }
}