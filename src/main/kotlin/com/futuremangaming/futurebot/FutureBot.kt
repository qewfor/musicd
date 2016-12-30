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
@file:JvmName("FutureBotKt")
package com.futuremangaming.futurebot

import com.futuremangaming.futurebot.external.WebSocketClient
import net.dv8tion.jda.core.AccountType.BOT
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.io.File

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
class FutureBot(private val token: String, val guild: String) : ListenerAdapter() {

    companion object {
        val log = getLogger("FutureBot")
    }

    val client: WebSocketClient = WebSocketClient(Config.fromJSON(File(PATH + "database.json")))
    var api: JDA? = null
        private set

    fun connect() {
        client.connect {
            api = JDABuilder(BOT)
                    .setToken(this.token)
                    .addListener(this)
                    .buildAsync()
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event?.author?.isBot!! || event?.guild?.id!! != guild)
            return
        when (event?.message?.rawContent) {
            "!ping" -> event?.channel?.sendMessage("Pong!")?.queue()
        }
    }
}


fun main(vararg args: String) {
    val loginCfg: Config = Config.fromJSON(File(PATH + "login.json"))
    FutureBot(
            (loginCfg["token"] as? String) ?: throw IllegalStateException("Missing token field in login.json!"),
            (loginCfg["guild"] as? String) ?: throw IllegalStateException("Missing guild field in login.json")
    ).connect()
}
