package com.example.vpdmoney

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SocketListener private constructor(): WebSocketListener() {

    private lateinit var listener: (SocketEvent) -> Unit

    fun setListener(callback: (SocketEvent) -> Unit) {
        listener = callback
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        listener(SocketEvent.Closed)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        listener(SocketEvent.Closing)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        listener(SocketEvent.Failure(t))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        listener(SocketEvent.Message(text))

    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        listener(SocketEvent.Message(bytes.utf8()))
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        listener(SocketEvent.Open)
    }

    companion object {

        private var instance: SocketListener? = null

        @Synchronized
        @JvmStatic
        fun getInstance(): SocketListener {
            if (instance == null) {
                instance = SocketListener()
            }
            return instance as SocketListener
        }

    }

    sealed class SocketEvent {
        object Closed: SocketEvent()
        object Closing: SocketEvent()
        data class Failure(val t: Throwable): SocketEvent()
        data class Message(val text: String): SocketEvent()
        object Open: SocketEvent()
    }
}