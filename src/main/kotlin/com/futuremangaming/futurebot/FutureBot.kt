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
import com.futuremangaming.futurebot.LoggerTag.valueOf
import com.futuremangaming.futurebot.external.LiveListener
import com.futuremangaming.futurebot.internal.CommandManagement
import com.futuremangaming.futurebot.internal.FutureEventManager
import com.futuremangaming.futurebot.music.MusicModule
import net.dv8tion.jda.core.AccountType.BOT
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.utils.SimpleLog
import java.io.File
import java.util.Properties

/**
 * @author Florian Spieß
 * @since  2016-12-30
 */
@Suppress("JAVA_CLASS_ON_COMPANION")
class FutureBot(token: String) {

    companion object {
        @field:JvmField
        val LOG = getLogger("Application")

        init {
            val props = Properties()
            val resource = this@Companion.javaClass.classLoader.getResourceAsStream("default.properties")
            props.load(resource)
            resource.close()

            val sysProps = System.getProperties()
            for ((key, value) in props) {
                sysProps.putIfAbsent(key, value)
            }

            SimpleLog.LEVEL = SimpleLog.Level.OFF
            SimpleLog.addListener(SimpleLogger())

            LOG.level = valueOf(sysProps.getProperty("app.log.level", "info").toUpperCase())
            LOG internal "Default properties: $props"
            LOG internal "System properties: $sysProps"
        }
    }

    //val client: WebSocketClient = WebSocketClient(Config.fromJSON("database", File(PATH + "database.json")))
    val musicModule = MusicModule()
    var api: JDA? = null
        private set

    fun connect() {
        //client.connect {
        api = JDABuilder(BOT)
                .setToken(System.getProperty("bot.token"))
                .setEventManager(FutureEventManager(true))
                .addListener(CommandManagement(this))
                .addListener(LiveListener())
        //      .addListener(Chat())
                .setAudioEnabled(true)
                .buildAsync()
        //}
    }

    init {
        System.getProperties()
              .putIfAbsent("bot.token", token)
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
    val loginCfg: Config = Config.fromJSON("login", File(PATH + "login.json"))
    FutureBot(
        loginCfg["token"] as? String
                ?: throw IllegalStateException("Missing token field in login.json!")
    ).connect()
}
