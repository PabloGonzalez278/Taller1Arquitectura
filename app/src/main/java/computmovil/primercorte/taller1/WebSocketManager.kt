package computmovil.primercorte.taller1
import okhttp3.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebSocketManager(private val url: String) {
    private var webSocketClient: WebSocketClient? = null

    fun connect(onMessageReceived: (String) -> Unit) {
        val uri = URI(url)
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket Connected")
            }

            override fun onMessage(message: String?) {
                message?.let { onMessageReceived(it) }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("WebSocket Disconnected: $reason")
                reconnect()
            }

            override fun onError(ex: Exception?) {
                println("WebSocket Error: ${ex?.message}")
            }
        }
        webSocketClient?.connect()
    }

    fun sendMessage(message: String) {
        webSocketClient?.send(message)
    }

    private fun reconnect() {
        println("Reconnecting WebSocket...")
        webSocketClient?.reconnect()
    }

}
