package com.example.ghostdrive

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * RetrofitClient
 *
 * Retrofit is an HTTP client library for Android.
 * You describe your API as a Kotlin interface (GhostDriveApi),
 * and Retrofit generates the actual network code for you automatically.
 *
 * Why not lazy?
 *   The server IP is discovered at runtime via UDP broadcast.
 *   Using `lazy` would freeze the IP at first access (which is "127.0.0.1").
 *   Instead, we rebuild the Retrofit instance every time setServerIp() is called.
 */
object RetrofitClient {

    private var serverIp: String = "127.0.0.1"

    private var _api: GhostDriveApi = buildApi()

    val api: GhostDriveApi
        get() = _api

    fun setServerIp(ip: String) {
        serverIp = ip
        _api = buildApi()
    }

    fun getServerIp(): String = serverIp

    private fun buildApi(): GhostDriveApi =
        Retrofit.Builder()
            .baseUrl("http://$serverIp:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GhostDriveApi::class.java)
}
