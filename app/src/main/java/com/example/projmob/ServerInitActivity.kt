package com.example.projmob
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projmob.minigame.ChooseMinigame
import java.io.IOException
import java.util.UUID

class ServerInitActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val BLUETOOTH_PERMISSION_CODE = 200
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var accept: AcceptThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.serverinit)

        // Check and ask for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermission(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE),
                BLUETOOTH_PERMISSION_CODE
            )
        } else {
            checkPermission(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
                BLUETOOTH_PERMISSION_CODE
            )
        }

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        val mText = findViewById<TextView>(R.id.serverinittext)
        mText.text = "Démarrage du serveur..."

        val actionRequestIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        startActivityForResult(actionRequestIntent, 1)

        val mmServerSocket: BluetoothServerSocket? = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord("Salut", UUID.fromString(
            myUUID))
        if(mmServerSocket != null){
            accept =  AcceptThread(mmServerSocket)
            accept!!.start()
        }
    }

    private fun checkPermission(permissions: Array<String>, requestCode: Int) {
        for(p in permissions) {
            if(ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this, arrayOf(p), requestCode)
            }
        }
    }

    fun onConnected(socket: BluetoothSocket){
        runOnUiThread {
            bluetoothService = MyBluetoothService(socket, isServer = true)
            bluetoothService!!.connectThread.start()
            val messageActivityIntent = Intent(this, ChooseMinigame::class.java)
            startActivity(messageActivityIntent)
        }
    }

    private inner class AcceptThread(serverSocket: BluetoothServerSocket) : Thread() {

        val mmServerSocket: BluetoothServerSocket = serverSocket

        override fun run() {
            val mText = findViewById<TextView>(R.id.serverinittext)
            mText.text = "En attente d'une connexion..."
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    mmServerSocket.close()
                    shouldLoop = false
                    onConnected(socket)
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        accept?.cancel()
    }

}