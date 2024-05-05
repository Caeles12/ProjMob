package com.example.projmob


import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.projmob.minigame.TicTacToe
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// Defines several constants used when transmitting messages between the
// service and the UI.
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2

const val TYPE_GAME_MESSAGE: Byte = 0x00
const val TYPE_BASIC_ACTION: Byte = 0x01
const val TYPE_GAME_START: Byte = 0x02
const val TYPE_GAME_FINISH: Byte = 0x03
const val TYPE_CONNEXION_END: Byte = 0x04
const val TYPE_RESET_POINTS: Byte = 0x05
const val TYPE_SHOW_SCORES: Byte = 0x06

class MyBluetoothService(private val mmSocket: BluetoothSocket, val isServer: Boolean = false) {
    private var currentHandler: MyHandler = MyHandler()
    val connectThread: ConnectedThread = ConnectedThread(mmSocket)
    public var myPoints: Int = 0
    public var opponentPoints: Int = 0

    private val handler = Handler(Looper.getMainLooper(), Handler.Callback {
        if(it.arg1 > 0){
            val mmBuffer: ByteArray = it.obj as ByteArray;
            val content: String = mmBuffer.decodeToString(startIndex = 1, endIndex = it.arg1)
            val msg: MyMessage = MyMessage(mmBuffer[0], content)
            //Log.d(TAG, "Received: (${mmBuffer[0]}) $content")
            currentHandler.callback(msg)
        }
        true
    })

    fun subscribe(h: MyHandler) {
        currentHandler = h
    }

    inner class MyHandler(var callback: (MyMessage) -> Unit = {}){}

    inner class MyMessage(val what: Byte, val content: String){}

    inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
        private var running: Boolean = true;

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (running) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(what: Byte, bytes: ByteArray) {
            try {
                mmOutStream.write(byteArrayOf(what) + bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }
            Log.d(TAG, "Sending ${bytes.decodeToString()}")
            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, mmBuffer)
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
                running = false
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}
