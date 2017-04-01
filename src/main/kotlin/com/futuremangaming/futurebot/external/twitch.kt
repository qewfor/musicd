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

package com.futuremangaming.futurebot.external

import club.minnced.kjda.builders.embed
import com.futuremangaming.futurebot.getConfig
import com.futuremangaming.futurebot.getLogger
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Game.GameType.TWITCH
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.user.UserGameUpdateEvent
import net.dv8tion.jda.core.hooks.EventListener
import org.apache.commons.lang3.exception.ExceptionUtils
import java.net.URLEncoder
import java.rmi.UnexpectedException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.jvm.JvmField as static

/**
 * @author Florian Spieß
 * @since  2016-12-31
 */

val twitchColor = Integer.decode("#6441A4")!!

class LiveListener : EventListener {

    companion object {
        @static
        val TWITCH_LIVE_KEY = "twitch.live"
        @static
        val TWITCH_USER_KEY = "twitch.user"
        @static
        val TWITCH_CHANNEL_KEY = "twitch.channel"
        @static
        val BOT_GUILD_KEY = "bot.guild"
        @static
        val CHANNEL_LIVE_KEY = "channel.live"
        @static
        val LOG = getLogger("Twitch")

        var CHANNEL: String? = System.getProperty(CHANNEL_LIVE_KEY, "-1")
        var GUILD: String = System.getProperty(BOT_GUILD_KEY, "-1")
        var USER: String = System.getProperty(TWITCH_USER_KEY, "-1")
        var TWITCH_ID: String = System.getProperty(TWITCH_CHANNEL_KEY, "-1")
    }

    var api: JDA? = null
    var lock = Any()
    var queryFailures: Int = 0

    override fun onEvent(event: Event?) {
        api = event!!.jda
        if (event is UserGameUpdateEvent) {
            synchronized(lock) {
                val user = event.user
                val member = event.guild.getMember(user)
                if (user.id != USER)
                    return

                if (member.game?.type === TWITCH)
                    queryTwitch() // did we start? faster than automated query
                else if (event.previousGame?.type === TWITCH)
                    queryTwitch() // did we stop?
            }
        }
    }

    fun onStream(stream: MessageEmbed?, isQuery: Boolean = false) {
        if (live()) {
            if (isQuery && queryFailures++ < 5)
                return // We make sure that it is actually offline by making 5 failure checks

            if (stream === null) {
                if (api?.presence?.game !== null)
                    api?.presence?.game = null
                System.setProperty(TWITCH_LIVE_KEY, "false")
            }
        }
        else if (stream !== null){
            val guild = api?.getGuildById(GUILD)
            val game = guild?.getMemberById(USER)?.game
            if (api?.presence?.game === null && game !== null && game.type === TWITCH)
                api?.presence?.game = game
            else
                api?.presence?.game = Game.of(stream.fields.firstOrNull()?.value ?: "Futureman is live!", stream.url)
            announce(
                api?.getTextChannelById(CHANNEL)
                   ?: guild?.publicChannel
                   ?: throw UnexpectedException("No announcement channel found"),
                stream
            )
            System.setProperty(TWITCH_LIVE_KEY, "true")
        }
    }

    fun announce(channel: TextChannel, stream: MessageEmbed) {
        if (live()) return
        try {
            channel.sendMessage(stream).queue { System.setProperty(TWITCH_LIVE_KEY, "true") }
        }
        catch (ex: Exception) {
            LOG.log(ex)
        }
    }

    fun queryTwitch() {
        synchronized(lock) {
            try {
                val stream = stream()
                val embed = createEmbed(stream)

                onStream(embed, isQuery = true)
            }
            catch (ex: UnirestException) {
                LOG.debug(ExceptionUtils.getStackTrace(ex))
            }
        }
    }

    fun stream(): Map<String, Any?>? {
        val client: String = (getConfig("login")["twitch_key"] as? String) ?: return null
        val response = Unirest.get("https://api.twitch.tv/kraken/streams/$TWITCH_ID") // `65311054` is futureman's twitch id
                .header("accept", "application/vnd.twitchtv.v5+json")
                .header("client-id", client)
                .asJson()
        if (response.status >= 300) {
            val msg = "Invalid response: " + response.statusText
            LOG error msg
            throw UnirestException(msg)
        }
        return response.body.`object`?.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    fun createEmbed(map: Map<String, Any?>?): MessageEmbed? {
        if (map === null) return null
        val stream   = map["stream"]     as? Map<String, Any> ?: return null
        val channel  = stream["channel"] as? Map<String, Any> ?: throw IllegalArgumentException("Channel is null")
        val previews = stream["preview"] as? Map<String, Any> ?: throw IllegalArgumentException("Previews is null")

        LiveListener.LOG internal stream.toString()

        val time       = System.currentTimeMillis()
        val status     = channel ["status"]     ?. toString()
        val logo       = channel ["logo"]       ?. toString()
        val preview    = previews["large"]      ?. toString()
        val created_at = stream  ["created_at"] ?. toString()

        return embed {
            title { "Futureman is live now!" }
            url { "https://twitch.tv/FuturemanGaming" }

            description { "<:fmgSUP:219939370575069194> $status" }

            author {
                name = "FuturemanGaming"
                url = "https://twitch.tv/FuturemanGaming"
                icon = logo
            }

            color { twitchColor }
            image { "$preview?live=true" }

            val dateTime = OffsetDateTime.parse(created_at ?: return@embed)

            time { dateTime }
            if (dateTime.until(OffsetDateTime.now(), ChronoUnit.SECONDS) < 15)
                return@embed

            var game = channel["game"] ?. toString() ?: return@embed

            field {
                name = "Directory"
                value = game
            }

            game = URLEncoder.encode(game, "UTF-8").replace("+", "%20")

            thumbnail { "https://static-cdn.jtvnw.net/ttv-boxart/$game-138x190.jpg?time=$time" }
        }

    }

    fun live() = System.getProperty(TWITCH_LIVE_KEY, "false").toBoolean()

    init {
        val priority = (Thread.MAX_PRIORITY + Thread.NORM_PRIORITY) / 2

        thread(name = "Twitch-API", isDaemon = true, priority = priority) {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    TimeUnit.MINUTES.sleep(1)
                    queryTwitch()
                }
            }
            catch (ex: InterruptedException) {
                LOG warn "Interrupted Thread: ${Thread.currentThread().name}"
            }
            catch (ex: Exception) {
                LOG log ex
            }
        }

    }


}
