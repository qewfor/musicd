/*
 *     Copyright 2014-2017 FuturemanGaming
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
import org.apache.commons.collections4.CollectionUtils
import org.json.JSONObject
import java.util.Queue
import java.util.concurrent.CompletableFuture
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
    internal val callbackMap: MutableMap<String, CompletableFuture<String>> = mutableMapOf()

    ///////////////////////////
    //// Terminal
    ///////////////////////////

    fun connect(success: (WebSocketClient) -> Unit = { }) {
        if (socket?.isOpen ?: false)
            return success(this) // Already connected

        status = INITIALIZING

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
        LOG.debug("<- Reconnect attempt")

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
        val obj: JSONObject = JSONObject(text)
        val map = obj.toMap()

        if (map.containsKey("autherror")) {
            //PROTOCOL_ERROR
            //1002 indicates that an endpoint is terminating the connection due to a protocol error.
            LOG.log("Received authentication error!", DEBUG, WARN)
            disconnect(1002)
            return
        }
        else if (map.containsKey("versionresult")) {
            reconnectTimeout = 2
            LOG.info("Authentication success!")
            return
        }

        // check query
        val nonce = if (obj.has("query_id")) obj["query_id"] as String else return
        val future = if (callbackMap.containsKey(nonce)) callbackMap[nonce] else return
        val hasResult = CollectionUtils.containsAny(map.keys, setOf("result", "results"))

        if (!hasResult) {
            if (map.containsKey("error"))
                future?.completeExceptionally(RuntimeException(obj["error"].toString()))
            else future?.completeExceptionally(RuntimeException("No error provided!\n" + obj.toString()))
            return
        }
        val key = if (map.containsKey("result")) "result" else if (map.containsKey("results")) "results" else null

        if (key !== null) future?.complete(obj[key].toString())
        else future?.completeExceptionally(RuntimeException("No result located: " + obj.toString()))
    }

    override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
        onTextMessage(websocket, String(binary!!))
    }

    override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
        LOG.trace("<- ${frame?.opcode}: ${frame?.payloadText?.replace(auth, "[REDACTED]")}")
    }

    override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
        LOG.log(cause!!)
    }

    override fun handleCallbackError(websocket: WebSocket?, cause: Throwable?) {
        LOG.log(cause!!)
    }

    ///////////////////////////
    //// Operations
    ///////////////////////////

    fun authenticate() {
        socket?.sendText(JSONObject().put("authenticate", auth).toString())
              ?.sendText(JSONObject().put("version", "version_check").toString())
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
        catch (ex: WebSocketException) {
            LOG.log(ex)
        }

        if (queue)
            sendQueue.offer(message)

        return socket
    }

    fun drainQueue() {
        LOG.debug("Draining queue....")
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

    //////////////////////
    //// PhantomAPI
    //////////////////////

    fun awaitNonce(nonce: String): String? {
        val promise: CompletableFuture<String> = this.callbackMap[nonce] ?: return null
        val obj = promise.get()
        this.callbackMap.remove(nonce)
        return obj
    }

    fun queryKeys(table: String, nonce: String): String? {
        val promise: CompletableFuture<String> = CompletableFuture()
        this.callbackMap[nonce] = promise
        val obj = JSONObject()
        obj["dbkeys"] = nonce
        obj["query"]  = JSONObject().set("table", table)
        this.send(obj.toString())
        return awaitNonce(nonce)
    }

}

operator fun JSONObject.set(key: String, value: Any): JSONObject {
    return this.put(key, value)
}

internal enum class ConnectionStatus {
    INITIALIZING,
    RECONNECTING,
    DISCONNECTED,
    CONNECTED
}
