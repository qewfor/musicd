/*
 *     Copyright 2016 FuturemanGaming
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("PhantomApi")
package com.futuremangaming.futurebot.external

import com.futuremangaming.futurebot.Config
import com.futuremangaming.futurebot.Logger
import com.futuremangaming.futurebot.LoggerTag.DEBUG
import com.futuremangaming.futurebot.LoggerTag.WARN
import com.futuremangaming.futurebot.external.ConnectionStatus.CONNECTED
import com.futuremangaming.futurebot.external.ConnectionStatus.DISCONNECTED
import com.futuremangaming.futurebot.external.ConnectionStatus.INITIALIZING
import com.futuremangaming.futurebot.external.ConnectionStatus.RECONNECTING
import com.futuremangaming.futurebot.getLogger
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
class WebSocketClient(val config: Config) : WebSocketAdapter() {

    companion object {
        val log = getLogger("WebSocket")
        val factory = WebSocketFactory()
    }

    val addr: String
    val auth: String
    var reconnect: Boolean
    var reconnectTimeout: Long = 2
    var frameFailure: Int = 0
    var socket: WebSocket? = null
    var connectionSuccess: ((WebSocketClient) -> Unit)? = null
    internal var status: ConnectionStatus = DISCONNECTED
    internal var keepAlive: Thread? = null
    internal var pong = true

    ///////////////////////////
    //// Terminal
    ///////////////////////////

    fun connect(success: (WebSocketClient) -> Unit = { }) {
        if (socket?.isOpen ?: false)
            return success.invoke(this) // Already connected

        status = INITIALIZING
        reconnectTimeout = 2

        connectionSuccess = success
        socket = factory.setConnectionTimeout(TimeUnit.SECONDS.toMillis(30).toInt())
                        .createSocket(addr)
                        .addListener(this)
        socket?.connectAsynchronously()
    }

    fun disconnect(closeCode: Int = 1000): WebSocketClient {
        if (!socket?.isOpen!!)
            return this
        // send close
        log.debug("<- Close: $closeCode")
        socket?.disconnect(closeCode)
        return this
    }

    fun reconnect() {
        synchronized(socket!!) {
            if (socket?.isOpen ?: false)
                disconnect()
        }

        reconnectTimeout =  Math.min(reconnectTimeout.shl(1), 900) // *2 up to 15 min
        log.info("Attempting to reconnect in $reconnectTimeout Seconds...")
        TimeUnit.SECONDS.sleep(reconnectTimeout)
        log.trace("<- Reconnect attempt")

        status = RECONNECTING
        frameFailure = 0

        socket = socket?.recreate()
        socket?.connectAsynchronously()
    }

    fun destroy() {
        reconnect = false
        disconnect()
        keepAlive?.interrupt()
        keepAlive = null
    }

    ///////////////////////////
    //// Listener
    ///////////////////////////

    override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
        log.error("Connection failed: " + exception?.message?.replace(addr, "[REDACTED]"))
        if (reconnect)
            return reconnect()
        log.warn("Closing Session!")
        destroy()
    }

    override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?,
                                clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
        status = DISCONNECTED
        log.info("Disconnected: " + serverCloseFrame?.closeCode)
        if (reconnect)
            return reconnect()
        log.warn("Closing Session!")
        destroy()
    }

    override fun onConnected(websocket: WebSocket?, headers: MutableMap<String, MutableList<String>>?) {
        connectionSuccess?.invoke(this)
        connectionSuccess = null

        if (status === INITIALIZING)
            log.info("Connected Successfully")
        else
            log.info("Reconnected!")

        status = CONNECTED
        authenticate()
        log.debug("<- Authentication")
    }

    override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        if (frame?.isPongFrame!!)
            pong = true
        log.trace("-> ${frame?.opcode}: ${frame?.payloadText?.replace(auth, "[REDACTED]")}")
        frameFailure = 0
    }

    override fun onSendError(websocket: WebSocket?, cause: WebSocketException?, frame: WebSocketFrame?) {
        log.log("Failed Send-Frame: ${frame?.opcode}: ${frame?.payloadText}", WARN, DEBUG)
        if (frame?.isPingFrame!! && frameFailure++ > 6)
            disconnect()
    }

    override fun onTextMessage(websocket: WebSocket?, text: String?) {
        // TODO
    }

    override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
        onTextMessage(websocket, String(binary!!))
    }

    override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
        log.trace("<- ${frame?.opcode}: ${frame?.payloadText?.replace(auth, "[REDACTED]")}")
    }

    ///////////////////////////
    //// Operations
    ///////////////////////////

    fun authenticate() {
        socket?.sendText(JSONObject().put("authenticate", auth).toString())
    }

    fun setupKeepAlive() {
        keepAlive = Thread({
            while (!Thread.currentThread().isInterrupted) {
                if (!pong && frameFailure++ >= 6)
                    reconnect()
                socket?.sendPing(Logger.timeStamp())
                try {
                    TimeUnit.SECONDS.sleep(15)
                }
                catch (ex: InterruptedException) { }
            }
        }, "KeepAlive-mWS")
        keepAlive?.isDaemon = true
        keepAlive?.priority = Thread.NORM_PRIORITY
        keepAlive?.start()
    }

    init {
        addr = (config["addr"] as? String)!!
        auth = (config["auth"] as? String)!!
        reconnect = (config["reconnect"] as? Boolean) ?: false
        setupKeepAlive()

        Runtime.getRuntime().addShutdownHook(Thread { this.destroy() })
    }

}

internal enum class ConnectionStatus {
    INITIALIZING,
    RECONNECTING,
    DISCONNECTED,
    CONNECTED
}
