package com.example.projmob

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button

class MultiplayerMenu : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer_menu)

        val clientInitIntent = Intent(this, ClientInitActivity::class.java)
        val serverInitIntent = Intent(this, ServerInitActivity::class.java)

        val startAsClientButton: Button = findViewById(R.id.startasclientbutton)
        val startAsServerButton: Button = findViewById(R.id.startasserverbutton)

        startAsClientButton.setOnClickListener(View.OnClickListener {
            startActivity(clientInitIntent)
        })

        startAsServerButton.setOnClickListener(View.OnClickListener {
            startActivity(serverInitIntent)
        })
    }
}