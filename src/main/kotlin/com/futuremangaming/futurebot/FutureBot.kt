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
@file:JvmName("FutureBotKt")
package com.futuremangaming.futurebot

import com.futuremangaming.futurebot.AnsiCode.Companion.ESC
import com.futuremangaming.futurebot.LoggerTag.INTERNAL
import com.futuremangaming.futurebot.external.LiveListener
import com.futuremangaming.futurebot.internal.CommandManagement
import com.futuremangaming.futurebot.internal.FutureEventManager
import net.dv8tion.jda.core.AccountType.BOT
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.SimpleLog
import java.io.File

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
class FutureBot(private val token: String, val guild: String){

    companion object {
        val LOG = getLogger("FutureBot")
    }

    //val client: WebSocketClient = WebSocketClient(Config.fromJSON("database", File(PATH + "database.json")))
    var api: JDA? = null
        private set

    fun connect() {
        //client.connect {
        api = JDABuilder(BOT)
                .setToken(this.token)
                .setEventManager(FutureEventManager(true))
                .addListener(CommandManagement(this))
                .addListener(LiveListener())
        //      .addListener(Chat())
                .setAudioEnabled(false)
                .buildAsync()
        //}
    }
}

class Chat : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event!!.message.rawContent.isBlank()) return
        Logger.lazy {
            val time  = Logger.timeStamp(event.message.creationTime)
            val guild = if (event.guild === null) "" else "[${AnsiCode.greenLight(event.guild.name)}] "
            val channel: String
            val user:    String = "${AnsiCode.yellow("${event.author.name}#${event.author.discriminator}")} "
            when (event.channelType) {
                TEXT -> channel = "#${AnsiCode.blueLight(event.channel.name)} "
                else -> channel = ""
            }

            "[$time] $guild$channel$user${event.message.content.replace(ESC, "")}"
        }

    }

}

fun main(vararg args: String) {
    SimpleLog.LEVEL = SimpleLog.Level.OFF
    SimpleLog.addListener(SimpleLogger())
    getLogger("WebSocket").level = INTERNAL
    val loginCfg: Config = Config.fromJSON("login", File(PATH + "login.json"))
    FutureBot(
            (loginCfg["token"] as? String) ?: throw IllegalStateException("Missing token field in login.json!"),
            (loginCfg["guild"] as? String) ?: throw IllegalStateException("Missing guild field in login.json!")
    ).connect()
}
