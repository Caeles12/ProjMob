package com.example.projmob

import android.app.Activity
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.example.projmob.minigame.ChooseMinigame
import com.example.projmob.minigame.Dance
import com.example.projmob.minigame.Fishing

const val TAG = "MainActivity"
const val myUUID = "9e87e367-72a0-446a-bb37-838de400db03"
var bluetoothService: MyBluetoothService? = null

class MainActivity : Activity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homescreen)
        val clientInitIntent = Intent(this, ClientInitActivity::class.java)
        val serverInitIntent = Intent(this, ServerInitActivity::class.java)
        val mygameintent = Intent(this, ChooseMinigame::class.java)

        val startAsClientButton: Button = findViewById(R.id.startasclientbutton)
        val startAsServerButton: Button = findViewById(R.id.startasserverbutton)
        val startMyGameButton: Button = findViewById(R.id.startmygamebutton)


        startAsClientButton.setOnClickListener(View.OnClickListener {
            startActivity(clientInitIntent)
        })

        startAsServerButton.setOnClickListener(View.OnClickListener {
            startActivity(serverInitIntent)
        })

        startMyGameButton.setOnClickListener(View.OnClickListener {
            startActivity(mygameintent)
        })

    }
}