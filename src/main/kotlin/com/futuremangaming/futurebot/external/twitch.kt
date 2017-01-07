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

import com.futuremangaming.futurebot.getConfig
import com.futuremangaming.futurebot.getLogger
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Game.GameType.TWITCH
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.user.UserGameUpdateEvent
import net.dv8tion.jda.core.hooks.EventListener
import org.apache.commons.lang3.exception.ExceptionUtils
import java.awt.Color
import java.net.URLEncoder
import java.rmi.UnexpectedException
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * @author Florian Spieß
 * @since  2016-12-31
 */

val twitchColor: Color = Color.decode("#6441A4")

class LiveListener : EventListener {

    companion object {
        val TWITCH_LIVE_KEY = "twitch.live"
        val TWITCH_USER_KEY = "twitch.user"
        val TWITCH_CHANNEL_KEY = "twitch.channel"
        val BOT_GUILD_KEY = "bot.guild"
        val CHANNEL_LIVE_KEY = "channel.live"


        var CHANNEL: String? = System.getProperty(CHANNEL_LIVE_KEY, "-1")
        var GUILD: String = System.getProperty(BOT_GUILD_KEY, "-1")
        var USER: String = System.getProperty(TWITCH_USER_KEY, "-1")
        var TWITCH_ID: String = System.getProperty(TWITCH_CHANNEL_KEY, "-1")
        val LOG = getLogger("Twitch")
    }

    var api: JDA? = null
    var lock = Any()

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

    fun onStream(stream: MessageEmbed?) {
        if (live()) {
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
                val embed = embed(stream)

                onStream(embed)
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
            LOG.error(msg)
            throw UnirestException(msg)
        }
        return response.body.`object`?.toMap()
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    fun embed(map: Map<String, Any?>?): MessageEmbed? {
        if (map === null) return null
        val stream   = map["stream"]     as? Map<String, Any> ?: return null
        val channel  = stream["channel"] as? Map<String, Any> ?: return null
        val previews = stream["preview"] as? Map<String, Any> ?: return null

        LiveListener.LOG.internal(stream.toString())

        val time       = System.currentTimeMillis()
        val status     = channel ["status"]     ?. toString()
        val logo       = channel ["logo"]       ?. toString()
        val preview    = previews["large"]      ?. toString()
        val created_at = stream  ["created_at"] ?. toString()

        val builder = EmbedBuilder()
        builder.setUrl("https://twitch.tv/FuturemanGaming")
        builder.setTitle("Futureman is live now!")
        builder.setDescription("<:fmgSUP:219939370575069194> $status") // what should we do if that emote is changed/removed
        builder.setAuthor("FuturemanGaming", "https://twitch.tv/FuturemanGaming/profile", logo)
        builder.setColor(twitchColor)
        builder.setImage("$preview?time=$time")

        val dateTime = OffsetDateTime.parse(created_at ?: return builder.build())
        builder.setTimestamp(dateTime)
        if (dateTime.until(OffsetDateTime.now(), ChronoUnit.SECONDS) < 15)
            return builder.build() // twitch takes some time to create a preview image, if we are too fast we omit it

        val game = URLEncoder.encode(channel["game"] ?. toString() ?: return builder.build())

        builder.addField("Directory", game, true)
        builder.setThumbnail("https://static-cdn.jtvnw.net/ttv-boxart/$game-138x190.jpg?time=$time")
        return builder.build()
    }

    fun live() = System.getProperty(TWITCH_LIVE_KEY, "false").toBoolean()

    init {
        val twitchQuery = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    TimeUnit.MINUTES.sleep(2)
                    queryTwitch()
                }
            }
            catch (ex: InterruptedException) {
                LOG.warn("Interrupted Thread: ${Thread.currentThread().name}")
            }
            catch (ex: Exception) {
                LOG.log(ex)
            }
        }

        twitchQuery.name = "Twitch-API"
        twitchQuery.isDaemon = true
        twitchQuery.priority = (Thread.MAX_PRIORITY + Thread.NORM_PRIORITY) / 2
        twitchQuery.start()
    }


}
