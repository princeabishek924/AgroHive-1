package com.example.agrohive_1

import io.socket.client.IO
import io.socket.client.Socket

object SocketInstance {
    private var socket: Socket? = null
    private const val SERVER_URL = "https://backend-agrohive.onrender.com"

    fun getSocket(): Socket {
        if (socket == null) {
            try {
                socket = IO.socket(SERVER_URL)
            } catch (e: Exception) {
                // Log error but don't crash
                android.util.Log.e("SocketInstance", "Error initializing socket: ${e.message}", e)
            }
        }
        return socket ?: throw IllegalStateException("Socket could not be initialized")
    }
}