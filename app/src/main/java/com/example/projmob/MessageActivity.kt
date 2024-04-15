package com.example.projmob


import android.app.Activity
import android.content.Context
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

        val mainHandler = Handler(Looper.getMainLooper(), Handler.Callback {
            if(it.what == MESSAGE_WRITE && it.arg1 > 0){
                val mmBuffer: ByteArray = it.obj as ByteArray;
                Log.d(TAG, "Write : ${mmBuffer.decodeToString(endIndex = it.arg1)}")
            }else if(it.what == MESSAGE_READ && it.arg1 > 0){
                val mmBuffer: ByteArray = it.obj as ByteArray;
                Log.d(TAG, "Read : ${mmBuffer.decodeToString(endIndex = it.arg1)}<")
                messages.add("Reçu:  ${mmBuffer.decodeToString(endIndex = it.arg1)}")
                updateListView(this)
            }
            true
        })

        val service = MyBluetoothService(mainHandler)
        connectThread = service.ConnectedThread(currentBluetoothSocket!!)
        connectThread!!.start()


        val sendMessageButton: Button = findViewById(R.id.sendMessageButton)
        val editMessageContent: EditText = findViewById(R.id.messageContent)

        sendMessageButton.setOnClickListener(View.OnClickListener {
            val messageText = editMessageContent.text.toString()
            editMessageContent.setText("")
            Log.d(TAG, messageText)
            messages.add("Envoyé:  $messageText")
            updateListView(this)
            connectThread!!.write(messageText.encodeToByteArray())
        })
    }
}