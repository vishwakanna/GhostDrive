package com.example.ghostdrive

import java.net.DatagramPacket
import java.net.DatagramSocket

object ServerDiscovery {

    fun discoverServer(): String? {

        return try {

            val socket = DatagramSocket(8888)
            socket.soTimeout = 10000

            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            socket.receive(packet)

            val message = String(packet.data, 0, packet.length)

            if (message == "GHOSTDRIVE_SERVER") {
                packet.address.hostAddress
            } else {
                null
            }

        } catch (e: Exception) {
            null
        }
    }
}