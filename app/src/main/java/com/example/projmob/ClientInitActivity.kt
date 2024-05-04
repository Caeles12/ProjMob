package com.example.projmob

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.projmob.minigame.ChooseMinigame
import java.io.IOException
import java.util.UUID


class ClientInitActivity : Activity() {
    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val BLUETOOTH_PERMISSION_CODE = 200
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothDevices: ArrayList<DeviceInfo> = ArrayList<DeviceInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clientinit)

        var mListView = findViewById<ListView>(R.id.devices)

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "PAS DADAPTEUR AAAAA")
        }else {
            Log.d(TAG, "Bluetooth adapter youpi!")
        }

        // Check and ask for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkPermission(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), BLUETOOTH_PERMISSION_CODE)
        } else {
            checkPermission(arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), BLUETOOTH_PERMISSION_CODE)
        }

        // Activate bluetooth
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }else {
            Toast.makeText(this, "Bluetooth already enabled", Toast.LENGTH_SHORT).show()
        }

        // Check already paired devices
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.d(TAG, "$deviceName : $deviceHardwareAddress")
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_UUID)
        registerReceiver(receiver, filter)

        // Discover devices
        bluetoothAdapter?.startDiscovery()

        mListView.setOnItemClickListener { _, _, position, _ ->
            val device = bluetoothDevices[position];



            val socket: BluetoothSocket = device.device.createRfcommSocketToServiceRecord(UUID.fromString(myUUID))
            val connectThread: ConnectThread = ConnectThread(socket)
            connectThread.start()
        }

    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent!!.action!!
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!

                    val deviceName: String? = device.name
                    val deviceAddress: String? = device.address
                    device.fetchUuidsWithSdp()
                    // Add the device to your list
                    Log.d(TAG, "$deviceName discovered! ($deviceAddress)")

                }
                BluetoothDevice.ACTION_UUID -> {
                    val d: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)

                    if (d != null && uuidExtra != null) {
                        val uuids = uuidExtra.toList().map { it.toString() }
                        if(uuids.contains(myUUID)){
                            val deviceName: String? = d.name
                            val deviceAddress: String? = d.address
                            // Add the device to your list
                            Log.d(TAG, "${deviceName} is an available device!")
                            val deviceInfos: DeviceInfo = DeviceInfo(deviceName, deviceAddress, d)
                            bluetoothDevices.add(deviceInfos)
                            if(context != null){
                                var loadingProgressBar = findViewById<ProgressBar>(R.id.loadingProgressBar)
                                val mListView = findViewById<ListView>(R.id.devices)
                                loadingProgressBar.visibility = View.GONE
                                mListView.visibility = View.VISIBLE
                                val arrayAdapter = ArrayAdapter(context, R.layout.simplelistitem, bluetoothDevices.map { "${it.name} - ${it.address}" })
                                mListView.adapter = arrayAdapter
                            }
                        }
                    }
                }
            }
        }
    }


    private fun checkPermission(permissions: Array<String>, requestCode: Int) {
        for(p in permissions) {
            if(ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this, arrayOf(p), requestCode)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    fun onConnected(socket: BluetoothSocket){
        runOnUiThread {
            bluetoothService = MyBluetoothService(socket)
            bluetoothService!!.connectThread.start()
            val chooseMinigameActivity = Intent(this, ChooseMinigame::class.java)
            startActivity(chooseMinigameActivity)
            finish()
        }
    }

    fun onConnectionFailed() {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Oupsi")
                .setMessage("Ya un problème un peu")
                .setPositiveButton("ok", null)
                .show()
        }
    }

    private inner class ConnectThread(socket: BluetoothSocket) : Thread() {

        private val mmSocket: BluetoothSocket = socket

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            checkPermission(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), BLUETOOTH_PERMISSION_CODE)
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    onConnected(socket)
                } catch (e: Exception) {
                    Log.e(TAG, "socket error", e)
                    onConnectionFailed()
                    return
                }

            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}