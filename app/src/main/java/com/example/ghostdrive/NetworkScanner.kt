package com.example.ghostdrive

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.InetAddress
import java.net.Socket

object NetworkScanner {

    suspend fun discoverServer(context: Context): String? = coroutineScope {

        val subnet = getLocalSubnet() ?: return@coroutineScope null

        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {

                val host = "$subnet.$i"

                println("Scanning $host")

                if (checkServer(host)) {
                    println("SERVER FOUND AT $host")
                    host
                } else {
                    null
                }
            }
        }

        jobs.awaitAll().firstOrNull { it != null }
    }

    private fun checkServer(host: String): Boolean {
        return try {

            val url = java.net.URL("http://$host:8080/api/files?path=/home/vishwa")
            val conn = url.openConnection() as java.net.HttpURLConnection

            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.requestMethod = "GET"

            val code = conn.responseCode

            println("Response from $host = $code")

            // Any response means the server exists
            code in 200..499

        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalSubnet(): String? {

        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()

        while (interfaces.hasMoreElements()) {

            val iface = interfaces.nextElement()

            val addresses = iface.inetAddresses

            while (addresses.hasMoreElements()) {

                val addr = addresses.nextElement()

                val ip = addr.hostAddress ?: continue

                if (
                    !addr.isLoopbackAddress &&
                    ip.contains(".") &&
                    (
                            ip.startsWith("10.") ||
                                    ip.startsWith("192.168.") ||
                                    ip.startsWith("172.")
                            )
                ) {

                    println("REAL LAN IP: $ip")

                    return ip.substringBeforeLast(".")
                }
            }
        }

        return null
    }
}