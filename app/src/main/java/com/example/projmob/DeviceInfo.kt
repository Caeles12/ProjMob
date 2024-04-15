package com.example.projmob

import android.bluetooth.BluetoothDevice

data class DeviceInfo(
    val name: String?,
    val address: String?,
    val device: BluetoothDevice
) {}