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
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
class WebSocketClient(val config: Config) : WebSocketAdapter() {

    companion object {
        val LOG = getLogger("WebSocket")
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
    internal val sendQueue: Queue<String> = ConcurrentLinkedQueue()

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
        LOG.debug("<- Close: $closeCode")
        socket?.disconnect(closeCode)
        return this
    }

    fun reconnect() {
        synchronized(socket!!) {
            if (socket?.isOpen ?: false)
                disconnect()
        }

        reconnectTimeout =  Math.min(reconnectTimeout.shl(1), 900) // *2 up to 15 min
        LOG.info("Attempting to reconnect in $reconnectTimeout Seconds...")
        TimeUnit.SECONDS.sleep(reconnectTimeout)
        LOG.trace("<- Reconnect attempt")

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
        LOG.error("Connection failed: " + exception?.message?.replace(addr, "[REDACTED]"))
        if (reconnect)
            return reconnect()
        LOG.warn("Closing Session!")
        destroy()
    }

    override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?,
                                clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
        status = DISCONNECTED
        LOG.info("Disconnected: " + serverCloseFrame?.closeCode)
        if (reconnect)
            return reconnect()
        LOG.warn("Closing Session!")
        destroy()
    }

    override fun onConnected(websocket: WebSocket?, headers: MutableMap<String, MutableList<String>>?) {
        connectionSuccess?.invoke(this)
        connectionSuccess = null

        if (status === INITIALIZING)
            LOG.info("Connected Successfully")
        else
            LOG.info("Reconnected!")

        status = CONNECTED
        authenticate()
        LOG.debug("<- Authentication")
        drainQueue()
    }

    override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
        if (frame?.isPongFrame!!)
            pong = true
        LOG.trace("-> ${frame?.opcode}: ${frame?.payloadText?.replace(auth, "[REDACTED]")}")
        frameFailure = 0
    }

    override fun onTextMessage(websocket: WebSocket?, text: String?) {
        LOG.internal("Received Message: " + text)
    }

    override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
        onTextMessage(websocket, String(binary!!))
    }

    override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
        LOG.trace("<- ${frame?.opcode}: ${frame?.payloadText?.replace(auth, "[REDACTED]")}")
    }

    ///////////////////////////
    //// Operations
    ///////////////////////////

    fun authenticate() {
        send(JSONObject().put("authenticate", auth).toString())
    }

    fun setupKeepAlive() {
        keepAlive = Thread({
            while (!Thread.currentThread().isInterrupted) {
                if (!pong && frameFailure++ >= 6)
                    reconnect()

                socket?.sendPing(Logger.timeStamp())

                try { TimeUnit.SECONDS.sleep(15) }
                catch (ex: InterruptedException) { }
            }
        }, "KeepAlive-mWS")
        keepAlive?.isDaemon = true
        keepAlive?.priority = Thread.NORM_PRIORITY
        keepAlive?.start()
    }

    fun send(message: String, queue: Boolean = true): WebSocket? {
        try {
            if (socket?.isOpen ?: false)
                return socket?.sendText(message)
        }
        catch (ex: WebSocketException) { }

        if (queue)
            sendQueue.offer(message)

        return socket
    }

    fun drainQueue() {
        while (sendQueue.isNotEmpty() && socket?.isOpen ?: false)
            send(sendQueue.poll())
    }

    init {
        addr = (config["addr"] as? String) ?: throw IllegalArgumentException("Missing addr field in config!")
        auth = (config["auth"] as? String) ?: throw IllegalArgumentException("Missing auth field in config!")
        reconnect = (config["reconnect"] as? Boolean) ?: false
        setupKeepAlive()

        Runtime.getRuntime().addShutdownHook(Thread { this.destroy() })
    }

}

//////////////////////
//// PhantomAPI
//////////////////////

fun WebSocketClient.sendMessage(opCode: Int = 0, data: JSONObject = JSONObject()) {
    val obj = JSONObject()
    obj["op"] = opCode
    obj["d"]  = data

    this.send(obj.toString())
}

operator fun JSONObject.set(s: String, value: Any) {
    this.put(s, value)
}

internal enum class ConnectionStatus {
    INITIALIZING,
    RECONNECTING,
    DISCONNECTED,
    CONNECTED
}
